/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.fastjson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.fastjson.FastjsonDataFormat;
import org.apache.camel.component.mock.MockEndpoint;
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
public class FastjsonIntegrationTest {

    @Deployment
    public static JavaArchive createdeployment() throws IOException {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-fastjson-tests");
        return archive;
    }

    @Test
    public void testMarshalAndUnmarshalMap() throws Exception {

        Map<String, String> in = new HashMap<String, String>();
        in.put("name", "Camel");

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                FastjsonDataFormat format = new FastjsonDataFormat();
                from("direct:in").marshal(format);
                from("direct:back").unmarshal(format).to("mock:reverse");
            }
        });

        MockEndpoint mockReverse = camelctx.getEndpoint("mock:reverse", MockEndpoint.class);
        mockReverse.expectedMessageCount(1);
        mockReverse.message(0).body().isInstanceOf(Map.class);
        mockReverse.message(0).body().isEqualTo(in);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            Object marshalled = template.requestBody("direct:in", in);
            String marshalledAsString = camelctx.getTypeConverter().convertTo(String.class, marshalled);
            Assert.assertEquals("{\"name\":\"Camel\"}", marshalledAsString);

            template.sendBody("direct:back", marshalled);
            mockReverse.assertIsSatisfied();
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testMarshalAndUnmarshalPojo() throws Exception {
        TestPojo in = new TestPojo();
        in.setName("Camel");

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                FastjsonDataFormat formatPojo = new FastjsonDataFormat(TestPojo.class);
                from("direct:inPojo").marshal(formatPojo);
                from("direct:backPojo").unmarshal(formatPojo).to("mock:reversePojo");
            }
        });

        MockEndpoint mockPojo = camelctx.getEndpoint("mock:reversePojo", MockEndpoint.class);
        mockPojo.expectedMessageCount(1);
        mockPojo.message(0).body().isInstanceOf(TestPojo.class);
        mockPojo.message(0).body().isEqualTo(in);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            Object marshalled = template.requestBody("direct:inPojo", in);
            String marshalledAsString = camelctx.getTypeConverter().convertTo(String.class, marshalled);
            Assert.assertEquals("{\"name\":\"Camel\"}", marshalledAsString);

            template.sendBody("direct:backPojo", marshalled);
            mockPojo.assertIsSatisfied();
        } finally {
            camelctx.close();
        }
    }

    static class TestPojo {

        private String name;

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object obj) {
            return this.name.equals(((TestPojo) obj).getName());
        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "TestPojo[" + name + "]";
        }
    }
}
