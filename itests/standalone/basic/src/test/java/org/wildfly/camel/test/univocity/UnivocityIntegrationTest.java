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

package org.wildfly.camel.test.univocity;

import static java.lang.System.lineSeparator;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.univocity.UniVocityCsvDataFormat;
import org.apache.camel.dataformat.univocity.UniVocityFixedWidthDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.dataformat.UniVocityTsvDataFormat;
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
public class UnivocityIntegrationTest {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "csv-dataformat-tests");
        return archive;
    }

    @Test
    public void testMarshalUnmarshalCSV() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                UniVocityCsvDataFormat dataFormat = new UniVocityCsvDataFormat();
                from("direct:marshal").marshal(dataFormat);
                from("direct:unmarshal").unmarshal(dataFormat);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            Map<String, String> input = asMap("A", "1", "B", "2", "C", "3");
            String res1 = producer.requestBody("direct:marshal", input, String.class);
            Assert.assertEquals(join("1,2,3"), res1);

            List<?> res2 = producer.requestBody("direct:unmarshal", res1, List.class);
            Assert.assertEquals("Expected one row", 1, res2.size());
            Assert.assertEquals(Arrays.asList("1" ,"2", "3"), res2.get(0));
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testMarshalUnmarshalTSV() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                UniVocityTsvDataFormat dataFormat = new UniVocityTsvDataFormat();
                from("direct:marshal").marshal(dataFormat);
                from("direct:unmarshal").unmarshal(dataFormat);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            Map<String, String> input = asMap("A", "1", "B", "2", "C", "3");
            String res1 = producer.requestBody("direct:marshal", input, String.class);
            Assert.assertEquals(join("1\t2\t3"), res1);

            List<?> res2 = producer.requestBody("direct:unmarshal", res1, List.class);
            Assert.assertEquals("Expected one row", 1, res2.size());
            Assert.assertEquals(Arrays.asList("1" ,"2", "3"), res2.get(0));
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testMarshalUnmarshalFixed() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                UniVocityFixedWidthDataFormat dataFormat = new UniVocityFixedWidthDataFormat().setFieldLengths(new int[]{2, 2, 2});
                from("direct:marshal").marshal(dataFormat);
                from("direct:unmarshal").unmarshal(dataFormat);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            Map<String, String> input = asMap("A", "1", "B", "2", "C", "3");
            String res1 = producer.requestBody("direct:marshal", input, String.class);
            Assert.assertEquals(join("1 2 3 "), res1);

            List<?> res2 = producer.requestBody("direct:unmarshal", res1, List.class);
            Assert.assertEquals("Expected one row", 1, res2.size());
            Assert.assertEquals(Arrays.asList("1" ,"2", "3"), res2.get(0));
        } finally {
            camelctx.stop();
        }
    }

    private Map<String, String> asMap(String... keyValues) {
        if (keyValues == null || keyValues.length % 2 == 1) {
            throw new IllegalArgumentException("You must specify key values with an even number.");
        }

        Map<String, String> result = new LinkedHashMap<String, String>(keyValues.length / 2);
        for (int i = 0; i < keyValues.length; i += 2) {
            result.put(keyValues[i], keyValues[i + 1]);
        }
        return result;
    }

    private String join(String... lines) {
        if (lines == null || lines.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append(lineSeparator());
        }
        return sb.toString();
    }
}
