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
package org.wildfly.camel.test.jsonvalidator;

import java.io.File;
import java.nio.file.Paths;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ValidationException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.FileUtil;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.FileUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class JsonValidatorIntegrationTest {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "json-validator-tests");
        archive.addAsResource("jsonvalidator/schema.json");
        archive.addClasses(FileUtils.class);
        return archive;
    }


    @Before
    public void setUp() throws Exception {
        FileUtils.deleteDirectory(Paths.get("target/validator"));
    }

    @Test
    public void testValidMessage() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {
                from("file:target/validator?noop=true")
                .to("json-validator:jsonvalidator/schema.json")
                .to("mock:valid");
            }
        });

        MockEndpoint mockValid = camelctx.getEndpoint("mock:valid", MockEndpoint.class);
        mockValid.expectedMessageCount(1);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeader("file:target/validator",
                    "{ \"name\": \"Joe Doe\", \"id\": 1, \"price\": 12.5 }",
                    Exchange.FILE_NAME, "valid.json");

            mockValid.assertIsSatisfied();

            Assert.assertTrue("Can delete the file", FileUtil.deleteFile(new File("target/validator/valid.json")));

        } finally {
          camelctx.close();
        }
    }

    @Test
    public void testInvalidMessage() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {
                from("file:target/validator?noop=true")
                    .doTry()
                        .to("json-validator:jsonvalidator/schema.json")
                    .doCatch(ValidationException.class)
                        .to("mock:invalid")
                    .end();
            }
        });

        MockEndpoint mockInvalid = camelctx.getEndpoint("mock:invalid", MockEndpoint.class);
        mockInvalid.expectedMessageCount(1);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeader("file:target/validator",
                    "{ \"name\": \"Joe Doe\", \"id\": \"AA1\", \"price\": 12.5 }",
                    Exchange.FILE_NAME, "invalid.json");

            mockInvalid.assertIsSatisfied();


            Assert.assertTrue("Can delete the file", FileUtil.deleteFile(new File("target/validator/invalid.json")));

        } finally {
          camelctx.close();
        }
    }
}
