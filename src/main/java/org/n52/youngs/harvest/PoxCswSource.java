/*
 * Copyright 2015-2015 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.youngs.harvest;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import net.opengis.csw.v_2_0_2.AbstractRecordType;
import net.opengis.csw.v_2_0_2.ElementSetNameType;
import net.opengis.csw.v_2_0_2.ElementSetType;
import net.opengis.csw.v_2_0_2.GetRecordsResponseType;
import net.opengis.csw.v_2_0_2.GetRecordsType;
import net.opengis.csw.v_2_0_2.ObjectFactory;
import net.opengis.csw.v_2_0_2.QueryType;
import net.opengis.csw.v_2_0_2.ResultType;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class PoxCswSource extends CswSource {

    private static final Logger log = LoggerFactory.getLogger(PoxCswSource.class);

    private Optional<Marshaller> marshaller = Optional.empty();

    public PoxCswSource(String url, NamespaceContext nsContext) throws MalformedURLException, JAXBException {
        super(url, nsContext);
    }

    public PoxCswSource(String url, Collection<String> namespaces, NamespaceContext nsContext, String typeName, String outputSchema) throws MalformedURLException, JAXBException {
        super(url, namespaces, nsContext, typeName, outputSchema);
    }

    public PoxCswSource(URL url, String namespacesParameter, String typeName, String outputSchema) throws JAXBException {
        super(url, namespacesParameter, typeName, outputSchema);
    }

    public PoxCswSource(URL url, Collection<String> namespaces, NamespaceContext nsContext, String typeName, String outputSchema) throws JAXBException {
        super(url, namespaces, nsContext, typeName, outputSchema);
    }

    @Override
    public Collection<SourceRecord> getRecords(long startPosition, long maxRecords) {
        log.debug("Requesting {} records from catalog starting at {}", maxRecords, startPosition);
        Collection<SourceRecord> records = org.elasticsearch.common.collect.Lists.newArrayList();

        HttpEntity entity = createRequest(startPosition, maxRecords);

        try {
            log.trace("GetRecords request: {}", EntityUtils.toString(entity));

            InputStream response = Request.Post(getEndpoint().toString()).body(entity)
                    .addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_XML.toString())
                    .execute().returnContent().asStream();
            JAXBElement<GetRecordsResponseType> jaxb_response = unmarshaller.unmarshal(new StreamSource(response),
                    GetRecordsResponseType.class);
            BigInteger numberOfRecordsReturned = jaxb_response.getValue().getSearchResults().getNumberOfRecordsReturned();
            log.debug("Got response with {} records", numberOfRecordsReturned);

            List<Object> nodes = jaxb_response.getValue().getSearchResults().getAny();
            if (!nodes.isEmpty()) {
                log.trace("Found {} \"any\" nodes.", nodes.size());
                nodes.stream()
                        .filter(n -> n instanceof Node)
                        .map(n -> (Node) n)
                        .map(n -> new NodeSourceRecord(n))
                        .forEach(records::add);
            }

            List<JAXBElement<? extends AbstractRecordType>> jaxb_records = jaxb_response.getValue().getSearchResults().getAbstractRecord();
            if (!jaxb_records.isEmpty()) {
                log.trace("Found {} \"AbstractRecordType\" records.", jaxb_records.size());
                DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                jaxb_records.stream()
                        .map(type -> {
                            return getNode(type, context, db);
                        })
                        .filter(Objects::nonNull)
                        .map(n -> new NodeSourceRecord(n))
                        .forEach(records::add);
            }
        } catch (IOException | JAXBException | ParserConfigurationException e) {
            log.error("Could not retrieve records from endpoint {}", getEndpoint(), e);
        }

        return records;
    }

    @Override
    protected Supplier<? extends Long> getAndStoreRecordCount() {
        Supplier<Long> s = new CswRecordCountSupplier();
        recordCount = Optional.of(s.get());
        return s;
    }

    private Marshaller getMarshaller() throws JAXBException {
        if (!marshaller.isPresent()) {
            marshaller = Optional.of(context.createMarshaller());
            marshaller.get().setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        }
        return marshaller.get();
    }

    private HttpEntity createRequest(long startPosition, long maxRecords) {
        Marshaller m;
        try {
            m = getMarshaller();
        } catch (JAXBException e) {
            log.error("Could not create marshaller", e);
            return null;
        }

        GetRecordsType getRecords = new GetRecordsType();
        ObjectFactory objectFactory = new ObjectFactory();
        JAXBElement<GetRecordsType> jaxb_getRecords = objectFactory.createGetRecords(getRecords);

        getRecords.setResultType(ResultType.RESULTS);
        getRecords.setOutputSchema(getOutputSchemaParameter());
        getRecords.setStartPosition(BigInteger.valueOf(startPosition));
        getRecords.setMaxRecords(BigInteger.valueOf(maxRecords));
        QueryType query = new QueryType();
        ElementSetNameType esn = new ElementSetNameType();
        esn.setValue(ElementSetType.FULL);
        query.setElementSetName(esn);
        query.setTypeNames(Lists.newArrayList(
                new QName(getOutputSchemaParameter(),
                        getTypeNamesParameter().substring(getTypeNamesParameter().indexOf(":") + 1))));
        getRecords.setAbstractQuery(objectFactory.createQuery(query));

        Writer w = new StringWriter();
        try {
            m.marshal(jaxb_getRecords, w);
        } catch (JAXBException e) {
            log.error("Error marshalling request", e);
        }

        log.trace("Created GetRecords request starting at {} for {} records:\n{}", startPosition, maxRecords, w.toString());

        StringEntity entity = new StringEntity(w.toString(), Charsets.UTF_8);
        return entity;
    }

    private class CswRecordCountSupplier implements Supplier<Long> {

        public CswRecordCountSupplier() {
            //
        }

        @Override
        public Long get() {
            URL url = getEndpoint();
            log.debug("Requesting record count at {} using {} and {}", url, getTypeNamesParameter(), getNamespacesParameter());
            Long count = Long.MIN_VALUE;

            Marshaller m;
            try {
                m = getMarshaller();
            } catch (JAXBException e) {
                log.error("Could not create marshaller", e);
                return count;
            }

            GetRecordsType getRecords = new GetRecordsType();
            ObjectFactory objectFactory = new ObjectFactory();
            JAXBElement<GetRecordsType> jaxb_getRecords = objectFactory.createGetRecords(getRecords);

            getRecords.setResultType(ResultType.HITS);
            getRecords.setOutputSchema(getOutputSchemaParameter());
            QueryType query = new QueryType();
            ElementSetNameType esn = new ElementSetNameType();
            esn.setValue(ElementSetType.FULL);
            query.setElementSetName(esn);
            query.setTypeNames(Lists.newArrayList(
                    new QName(getOutputSchemaParameter(),
                            getTypeNamesParameter().substring(getTypeNamesParameter().indexOf(":") + 1))));
            getRecords.setAbstractQuery(objectFactory.createQuery(query));

            Writer w = new StringWriter();
            try {
                m.marshal(jaxb_getRecords, w);
            } catch (JAXBException e) {
                log.error("Error marshalling request", e);
            }

            log.trace("Sending GetRecords request:\n{}", w.toString());
            try {
                InputStream response = Request.Post(getEndpoint().toString())
                        .bodyString(w.toString(), ContentType.APPLICATION_XML)
                        .execute().returnContent().asStream();

                GetRecordsResponseType getRecordsResponse = unmarshaller.unmarshal(new StreamSource(response), GetRecordsResponseType.class).getValue();
                BigInteger numberOfRecordsMatched = getRecordsResponse.getSearchResults().getNumberOfRecordsMatched();
                count = numberOfRecordsMatched.longValue();
                log.debug("Found {} records", count);
            } catch (IOException | JAXBException e) {
                log.error("Could not retrieve record count using request {}", w.toString(), e);
            }

            return count;
        }
    }

}