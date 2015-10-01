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
package org.n52.youngs.test;

import com.google.common.collect.ImmutableList;
import java.net.URL;
import java.util.Collection;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import org.junit.Assert;
import org.junit.Test;
import org.n52.youngs.harvest.CswSource;
import org.n52.youngs.impl.NamespaceContextImpl;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class CswSourceIT {

    @Test
    public void namespaceParameterCreation() throws Exception {
        CswSource source = new CswSource(new URL("http://api.eurogeoss-broker.eu/dab/services/cswiso"),
                (Collection<String>) ImmutableList.of("http://www.opengis.net/cat/csw/2.0.2"), NamespaceContextImpl.create(), "csw:Record", "http://www.opengis.net/cat/csw/2.0.2");

        long count = source.getRecordCount();
        Assert.assertThat("record count is higher than last manual check", count, is(greaterThan(900000l)));
    }

}