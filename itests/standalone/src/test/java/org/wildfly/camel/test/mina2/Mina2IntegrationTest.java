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

package org.wildfly.camel.test.mina2;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class Mina2IntegrationTest {

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-mina2-tests");
        return archive;
    }

    @Test
    public void testComponentLoad() throws Exception {
        
        // [FIXME #291] Usage of camel-mina2 depends on TCCL
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        
        CamelContext camelctx = new DefaultCamelContext();
        Endpoint endpoint = camelctx.getEndpoint("mina2:tcp://localhost:6200");
        Assert.assertNotNull(endpoint);
        Assert.assertEquals(endpoint.getClass().getName(), "org.apache.camel.component.mina2.Mina2Endpoint");
        camelctx.stop();
    }

}
