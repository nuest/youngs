/*
 * Copyright 2015-${currentYearDynamic} 52°North Initiative for Geospatial Open Source
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
package org.n52.youngs.transform.impl;

import com.github.autermann.yaml.Yaml;
import com.github.autermann.yaml.YamlNode;
import com.google.common.base.Joiner;
import org.n52.youngs.transform.MappingEntry;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.n52.youngs.transform.MappingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class YamlMappingConfiguration implements MappingConfiguration {

    private static final Logger log = LoggerFactory.getLogger(YamlMappingConfiguration.class);

    Collection<MappingEntry> entries = Lists.newArrayList();

    private String xPathVersion;

    private int version;

    private String name;

    private static XPathFactory factory = XPathFactory.newInstance();

    private Optional<XPathExpression> applicability;

    private final String type;

    private final String index;

    public YamlMappingConfiguration(File file, NamespaceContext nsContext) throws FileNotFoundException {
        this(new FileInputStream(file), nsContext);
        log.info("Created configuration from file {} with {} entries.", file, entries.size());
    }

    public YamlMappingConfiguration(String fileName, NamespaceContext nsContext) throws IOException {
        this(Resources.asByteSource(Resources.getResource(fileName)).openStream(),
                nsContext);
    }

    public YamlMappingConfiguration(InputStream input, NamespaceContext nsContext) {
        Yaml yaml = new Yaml();
        YamlNode configurationNodes = yaml.load(input);
        log.trace("Read configuration file with the root elements {}", Joiner.on(" ").join(configurationNodes));

        // read the entries from the config file
        this.name = configurationNodes.path("name").asTextValue(DEFAULT_NAME);
        this.version = configurationNodes.path("version").asIntValue(DEFAULT_VERSION);
        this.xPathVersion = configurationNodes.path("xpathversion").asTextValue(DEFAULT_XPATH_VERSION);
        this.type = configurationNodes.path("type").asTextValue(DEFAULT_TYPE);
        this.index = configurationNodes.path("index").asTextValue(DEFAULT_INDEX);

        XPath path = factory.newXPath();
        path.setNamespaceContext(nsContext);
        try {
            String applicabilityXPath = configurationNodes.path("applicable.xpath").asTextValue(DEFAULT_APPLICABILITY_PATH);
            applicability = Optional.of(path.compile(applicabilityXPath));
        } catch (XPathExpressionException e) {
            log.error("Could not compile applicability xpath, will always evalute to true", e);
        }

        log.info("Created configuration from stream {} with {} entries", input, entries.size());
    }

    @Override
    public Collection<MappingEntry> getEntries() {
        entries.add(new MappingEntryImpl("//gmd:MD_Metadata/gmd:fileIdentifier/gco:CharacterString", "id", false));
        entries.add(new MappingEntryImpl("//gmd:MD_Metadata/gmd:language/gmd:LanguageCode/@codeListValue", "language", false));
        entries.add(new MappingEntryImpl("//gmd:MD_Metadata/gmd:metadataStandardName/gco:CharacterString", "mdStandardName", false));
        entries.add(new MappingEntryImpl("//gmd:MD_Metadata/gmd:metadataStandardVersion/gco:CharacterString", "mdStandardVersion", false));

        return entries;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getVersion() {
        return this.version;
    }

    @Override
    public String getXPathVersion() {
        return this.xPathVersion;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public String getIndex() {
        return index;
    }

    @Override
    public boolean isApplicable(Document doc) {
        if (!this.applicability.isPresent()) {
            log.debug("No applicability xpath provided, returning TRUE.");
            return true;
        }

        boolean result;
        try {
            result = (boolean) this.applicability.get().evaluate(doc, XPathConstants.BOOLEAN);
        } catch (XPathExpressionException | RuntimeException e) {
            log.warn("Error executing applicability xpath on document, returning FALSE: {}", doc, e);
            return false;
        }

        return result;
    }

}
