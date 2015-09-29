/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2014 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package org.wildfly.camel.test.flatpack;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.flatpack.FlatpackDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class FlatpackIntegrationTest {

    private static final String FLATPACK_MAPPING_XML = "flatpack-mapping.xml";
    private static final String FLATPACK_INPUT_TXT = "people-fixed-length.txt";

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "flatpack-dataformat-tests");
        archive.addAsResource("flatpack/" + FLATPACK_MAPPING_XML, FLATPACK_MAPPING_XML);
        archive.addAsResource("flatpack/" + FLATPACK_INPUT_TXT, FLATPACK_INPUT_TXT);
        return archive;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUnmarshal() throws Exception {

        final FlatpackDataFormat format = new FlatpackDataFormat();
        format.setDefinition(FLATPACK_MAPPING_XML);
        format.setFixed(true);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .unmarshal(format);
            }
        });

        InputStream input = getClass().getClassLoader().getResourceAsStream(FLATPACK_INPUT_TXT);
        Assert.assertNotNull("Input not null", input);

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            List<Map<String, String>> result = producer.requestBody("direct:start", input, List.class);
            Assert.assertEquals("Expected size 4: " + result, 4, result.size());
            Assert.assertEquals("JOHN", result.get(0).get("FIRSTNAME"));
            Assert.assertEquals("JIMMY", result.get(1).get("FIRSTNAME"));
            Assert.assertEquals("JANE", result.get(2).get("FIRSTNAME"));
            Assert.assertEquals("FRED", result.get(3).get("FIRSTNAME"));
        } finally {
            camelctx.stop();
        }
    }
}
