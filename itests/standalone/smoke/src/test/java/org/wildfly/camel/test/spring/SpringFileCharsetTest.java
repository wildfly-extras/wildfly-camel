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

package org.wildfly.camel.test.spring;

import org.apache.camel.CamelContext;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
@Ignore("[ENTESB-10033] File consumer with charset doesn't parse XML correctly")
public class SpringFileCharsetTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-charset-tests.jar");
        archive.addAsResource("spring/charset-camel-context.xml");
        return archive;
    }


    @Test
    public void testCharset() throws Exception {

        CamelContext camelctx = contextRegistry.getCamelContext("charset-context");

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedBodiesReceived("<bar>abc</bar>", "<bar>xyz</bar>", "<bar>åäö</bar>");
        mockEndpoint.assertIsSatisfied();
    }
}
