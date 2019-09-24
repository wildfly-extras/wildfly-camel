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
package org.wildfly.camel.test.thrift;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.thrift.ThriftDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.thrift.generated.Operation;
import org.wildfly.camel.test.thrift.generated.Work;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class ThriftIntegrationTest {

    private static final int WORK_TEST_NUM1 = 1;
    private static final int WORK_TEST_NUM2 = 100;
    private static final Operation WORK_TEST_OPERATION = Operation.MULTIPLY;
    private static final String WORK_TEST_COMMENT = "This is a test thrift data";

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-thrift-test");
        archive.addPackage(Work.class.getPackage());
        return archive;
    }

    @Test
    public void testMarshalAndUnmarshal() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                ThriftDataFormat format = new ThriftDataFormat(new Work());
                from("direct:in").marshal(format);
                from("direct:back").unmarshal(format).to("mock:reverse");
            }
        });

        camelctx.start();
        try {
            Work input = new Work();
            input.num1 = WORK_TEST_NUM1;
            input.num2 = WORK_TEST_NUM2;
            input.op = WORK_TEST_OPERATION;
            input.comment = WORK_TEST_COMMENT;

            MockEndpoint mock = camelctx.getEndpoint("mock:reverse", MockEndpoint.class);
            mock.expectedMessageCount(1);
            mock.message(0).body().isInstanceOf(Work.class);
            mock.message(0).body().isEqualTo(input);

            ProducerTemplate template = camelctx.createProducerTemplate();
            Object marshalled = template.requestBody("direct:in", input);

            template.sendBody("direct:back", marshalled);

            mock.assertIsSatisfied();

            Work output = mock.getReceivedExchanges().get(0).getIn().getBody(Work.class);
            Assert.assertEquals(WORK_TEST_COMMENT, output.getComment());
            Assert.assertEquals(WORK_TEST_OPERATION, output.getOp());
            Assert.assertEquals(WORK_TEST_NUM2, output.getNum2());
        } finally {
            camelctx.close();
        }
    }
}
