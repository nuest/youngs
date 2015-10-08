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
package org.n52.youngs.transform.impl;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.xpath.XPathExpression;
import org.n52.youngs.transform.MappingEntry;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class MappingEntryImpl implements MappingEntry {

    private final XPathExpression xPath;

    private final Map<String, Object> indexProperties = Maps.newHashMap();

    private Optional<Boolean> identifier = Optional.empty();

    private Optional<List<XPathExpression[]>> coordinates = Optional.empty();

    private Optional<String> coordinatesType = Optional.empty();

    private Optional<Boolean> raw = Optional.empty();

    private Optional<Map<String, String>> replacements = Optional.empty();

    public MappingEntryImpl(XPathExpression xPath, Map<String, Object> indexProperties,
            boolean identifier, boolean rawXml) {
        this.xPath = xPath;
        this.indexProperties.putAll(indexProperties);
        this.identifier = Optional.of(identifier);
        this.raw = Optional.of(rawXml);
    }

    @Override
    public XPathExpression getXPath() {
        return xPath;
    }

    @Override
    public String getFieldName() {
        return (String) indexProperties.get(INDEX_NAME);
    }

    public MappingEntryImpl addIndexProperty(String key, Object value) {
        this.indexProperties.put(key, value);
        return this;
    }

    @Override
    public Map<String, Object> getIndexProperties() {
        return this.indexProperties;
    }

    @Override
    public Object getIndexPropery(String name) {
        return this.indexProperties.get(name);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("xpath", xPath)
                .add("identifer", identifier)
                .add("raw", raw)
                .add("properties", Arrays.deepToString(indexProperties.entrySet().toArray()))
                .omitNullValues()
                .toString();
    }

    @Override
    public boolean isIdentifier() {
        return identifier.isPresent() && identifier.get();
    }

    @Override
    public boolean hasCoordinates() {
        return coordinates.isPresent();
    }

    @Override
    public List<XPathExpression[]> getCoordinatesXPaths() {
        return coordinates.get();
    }

    public MappingEntryImpl setCoordinatesXPaths(List<XPathExpression[]> coords) {
        this.coordinates = Optional.of(coords);
        return this;
    }

    @Override
    public boolean hasCoordinatesType() {
        return this.coordinatesType.isPresent();
    }

    @Override
    public String getCoordinatesType() {
        return this.coordinatesType.get();
    }

    public MappingEntryImpl setCoordinatesType(String type) {
        this.coordinatesType = Optional.of(type);
        return this;
    }

    @Override
    public boolean isRawXml() {
        return raw.isPresent() && raw.get();
    }

    @Override
    public boolean hasReplacements() {
        return replacements.isPresent();
    }

    @Override
    public Map<String, String> getReplacements() {
        return replacements.get();
    }

    public MappingEntryImpl setReplacements(Map<String, String> replacements) {
        this.replacements = Optional.of(replacements);
        return this;
    }
}
