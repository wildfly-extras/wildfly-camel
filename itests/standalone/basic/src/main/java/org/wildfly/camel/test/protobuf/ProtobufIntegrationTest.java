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

package org.wildfly.camel.test.protobuf;

import java.io.ByteArrayOutputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.protobuf.ProtobufDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.protobuf.model.AddressBookProtos;
import org.wildfly.camel.test.protobuf.model.AddressBookProtos.Person;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class ProtobufIntegrationTest {

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "protobuf-tests");
        archive.addClasses(AddressBookProtos.class);
        return archive;
    }

    @Test
    public void testMarshall() throws Exception {

        final ProtobufDataFormat format = new ProtobufDataFormat(Person.getDefaultInstance());

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").marshal(format);
            }
        });

        Person person = Person.newBuilder().setId(1).setName("John Doe").build();

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", person, String.class);
            Assert.assertEquals("John Doe", result.trim());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testUnmarshall() throws Exception {

        final ProtobufDataFormat format = new ProtobufDataFormat(Person.getDefaultInstance());

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").unmarshal(format);
            }
        });

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Person person = Person.newBuilder().setId(1).setName("John Doe").build();
        person.writeTo(baos);

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            Person result = producer.requestBody("direct:start", baos.toByteArray(), Person.class);
            Assert.assertEquals("John Doe", result.getName().trim());
        } finally {
            camelctx.stop();
        }
    }
}
