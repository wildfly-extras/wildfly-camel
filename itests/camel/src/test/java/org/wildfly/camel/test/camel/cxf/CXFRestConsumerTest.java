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

package org.wildfly.camel.test.camel.cxf;

import java.net.URL;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.extension.camel.SpringCamelContextFactory;

public class CXFRestConsumerTest {

    static CamelContext camelctx;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ClassLoader classLoader = CXFRestConsumerTest.class.getClassLoader();
        URL resurl = classLoader.getResource("cxf/cxfrs-camel-context.xml");
        camelctx = SpringCamelContextFactory.createSingleCamelContext(resurl, classLoader);
        camelctx.start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (camelctx != null) {
            camelctx.stop();
        }
    }

    @Test
    public void testCXFConsumer() throws Exception {

        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());

        Client client = ClientBuilder.newClient();
        Object result = client.target("http://localhost:8080/cxfrestful/greet/hello/Kermit").request(MediaType.APPLICATION_JSON).get(String.class);
        Assert.assertEquals("Hello Kermit", result);
    }
}
