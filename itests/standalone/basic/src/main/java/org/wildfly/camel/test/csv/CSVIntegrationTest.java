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

package org.wildfly.camel.test.csv;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.dozer.DozerBeanMapperConfiguration;
import org.apache.camel.converter.dozer.DozerTypeConverterLoader;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.types.Customer;

@RunWith(Arquillian.class)
public class CSVIntegrationTest {

    private static final String DOZER_MAPPINGS_XML = "dozer-mappings.xml";

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "csv-dataformat-tests");
        archive.addAsResource("csv/" + DOZER_MAPPINGS_XML, DOZER_MAPPINGS_XML);
        archive.addClasses(Customer.class);
        return archive;
    }

    @Test
    public void testMarshal() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").convertBodyTo(Map.class).marshal().csv();
            }
        });

        DozerBeanMapperConfiguration mconfig = new DozerBeanMapperConfiguration();
        mconfig.setMappingFiles(Arrays.asList(new String[] { DOZER_MAPPINGS_XML }));
        new DozerTypeConverterLoader(camelctx, mconfig);

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", new Customer("John", "Doe"), String.class);
            Assert.assertEquals("John,Doe", result.trim());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUnmarshal() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .unmarshal().csv();
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            List<List<String>> result = producer.requestBody("direct:start", "John,Doe", List.class);
            Assert.assertEquals("Expected size 1: " + result, 1, result.size());
            List<String> line = result.get(0);
            Assert.assertEquals("Expected size 2: " + line, 2, line.size());
            Assert.assertEquals("John", line.get(0));
            Assert.assertEquals("Doe", line.get(1));
        } finally {
            camelctx.stop();
        }
    }
}
