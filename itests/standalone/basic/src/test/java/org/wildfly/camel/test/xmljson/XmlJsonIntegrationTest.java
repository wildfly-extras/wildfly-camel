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

package org.wildfly.camel.test.xmljson;

import java.io.InputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.xmljson.XmlJsonDataFormat;
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
public class XmlJsonIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-xmljson-tests");
        archive.addAsResource("xmljson/testMessage1.xml");
        return archive;
    }

    @Test
    public void testMarshalAndUnmarshal() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                XmlJsonDataFormat format = new XmlJsonDataFormat();
                from("direct:marshal").marshal(format).to("mock:json");
                from("direct:unmarshal").unmarshal(format).to("mock:xml");
            }
        });

        camelctx.start();
        try {
            MockEndpoint mockJSON = camelctx.getEndpoint("mock:json", MockEndpoint.class);
            mockJSON.expectedMessageCount(1);
            mockJSON.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/json");
            mockJSON.message(0).body().isInstanceOf(byte[].class);

            MockEndpoint mockXML = camelctx.getEndpoint("mock:xml", MockEndpoint.class);
            mockXML.expectedMessageCount(1);
            mockXML.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/xml");
            mockXML.message(0).body().isInstanceOf(String.class);

            InputStream inStream = getClass().getResourceAsStream("/xmljson/testMessage1.xml");
            String in = camelctx.getTypeConverter().convertTo(String.class, inStream);

            ProducerTemplate template = camelctx.createProducerTemplate();
            Object json = template.requestBody("direct:marshal", in);
            String jsonString = camelctx.getTypeConverter().convertTo(String.class, json);
            
            String expString = "{'a':'1','b':'2','c':{'a':'c.a.1','b':'c.b.2'},'d':['a','b','c'],'e':['1','2','3'],'f':'true','g':[]}";
            Assert.assertEquals(expString, jsonString.replace('"', '\''));

            template.sendBody("direct:unmarshal", jsonString);

            mockJSON.assertIsSatisfied();
            mockXML.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }
}
