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

package org.wildfly.camel.test.quartz;

import org.apache.camel.CamelContext;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class QuartzPersistentStoreTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "quartz-tests");
        archive.addAsManifestResource("quartz/quartz-camel-context.xml");
        archive.addAsResource("quartz/sql/db-schema.sql", "db-schema.sql");
        return archive;
    }

    @Test
    public void restartRouteTest() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("quartz-context");
        Assert.assertNotNull(camelctx);

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedMinimumMessageCount(2);

        mockEndpoint.assertIsSatisfied();

        // restart route
        camelctx.stopRoute("myRoute");
        mockEndpoint.reset();
        mockEndpoint.expectedMessageCount(0);

        // wait a bit
        Thread.sleep(2000);

        mockEndpoint.assertIsSatisfied();

        // start route, and we got messages again
        mockEndpoint.reset();
        mockEndpoint.expectedMinimumMessageCount(2);

        camelctx.startRoute("myRoute");

        mockEndpoint.assertIsSatisfied();
    }
}
