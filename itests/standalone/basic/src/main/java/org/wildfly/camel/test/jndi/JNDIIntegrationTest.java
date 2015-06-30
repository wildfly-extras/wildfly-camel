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

package org.wildfly.camel.test.jndi;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.gravia.runtime.ServiceLocator;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.types.HelloBean;
import org.wildfly.extension.camel.CamelConstants;
import org.wildfly.extension.camel.CamelContextFactory;
import org.wildfly.extension.camel.WildFlyCamelContext;

/**
 * Deploys a module which registers a {@link HelloBean} in JNDI, which is later used in a route.
 *
 * @author thomas.diesler@jboss.com
 * @since 03-Jun-2013
 */
@RunWith(Arquillian.class)
public class JNDIIntegrationTest {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "jndi-integration-tests");
        archive.addClasses(HelloBean.class);
        return archive;
    }

    @Test
    public void testArquillianResource(@ArquillianResource CamelContextFactory contextFactory) throws Exception {
        WildFlyCamelContext camelctx = contextFactory.createCamelContext();
        assertBeanBinding(camelctx);
    }

    @Test
    public void testCamelContextFactoryLookup() throws Exception {
        InitialContext inicxt = new InitialContext();
        CamelContextFactory factory = (CamelContextFactory) inicxt.lookup(CamelConstants.CAMEL_CONTEXT_FACTORY_BINDING_NAME);
        WildFlyCamelContext camelctx = factory.createCamelContext();
        assertBeanBinding(camelctx);
    }

    @Test
    public void testCamelContextFactoryService() throws Exception {
        CamelContextFactory contextFactory = ServiceLocator.getRequiredService(CamelContextFactory.class);
        WildFlyCamelContext camelctx = contextFactory.createCamelContext(getClass().getClassLoader());
        assertBeanBinding(camelctx);
    }

    private void assertBeanBinding(WildFlyCamelContext camelctx) throws NamingException, Exception {
        Context jndictx = camelctx.getNamingContext();
        jndictx.bind("helloBean", new HelloBean());
        try {
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start").beanRef("helloBean");
                }
            });

            camelctx.start();
            try {
                ProducerTemplate producer = camelctx.createProducerTemplate();
                String result = producer.requestBody("direct:start", "Kermit", String.class);
                Assert.assertEquals("Hello Kermit", result);
            } finally {
                camelctx.stop();
            }
        } finally {
            jndictx.unbind("helloBean");
        }
    }

}
