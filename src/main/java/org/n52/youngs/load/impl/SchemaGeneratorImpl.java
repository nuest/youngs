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
package org.n52.youngs.load.impl;

import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import static javax.management.Query.value;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.n52.youngs.exception.SinkError;
import org.n52.youngs.load.SchemaGenerator;
import org.n52.youngs.transform.MappingConfiguration;
import org.n52.youngs.transform.MappingEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class SchemaGeneratorImpl implements SchemaGenerator {

    private static final Logger log = LoggerFactory.getLogger(SchemaGeneratorImpl.class);

    public SchemaGeneratorImpl() {
        //
    }

    @Override
    public Map<String, Object> generate(MappingConfiguration mapping) {
        Map<String, Object> schema = Maps.newHashMap();
        schema.put("dynamic", mapping.isDynamicMappingEnabled());

        Map<String, Object> fields = Maps.newHashMap();
        mapping.getEntries().forEach((MappingEntry entry) -> {
            Map<String, Object> properties = Maps.newHashMap();
            for (Entry<String, Object> entryProps : entry.getIndexProperties().entrySet()) {
                properties.put(entryProps.getKey(), entryProps.getValue());
            }
            fields.put(entry.getFieldName(), properties);
        });
        schema.put("properties", fields);

        log.info("Created {} schema with {} fields", mapping.isDynamicMappingEnabled() ? "dynamic" : "", fields.size());
        log.debug("Created schema with {} first level elements: {}", schema.size(), Arrays.deepToString(schema.entrySet().toArray()));
        return schema;
    }

}
