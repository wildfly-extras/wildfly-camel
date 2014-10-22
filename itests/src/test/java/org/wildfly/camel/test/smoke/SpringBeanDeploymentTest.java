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

package org.wildfly.camel.test.smoke;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.CamelConstants;
import org.wildfly.camel.CamelContextRegistry;
import org.wildfly.camel.test.smoke.subA.HelloBean;

/**
 * Deploys a module/bundle which contain a {@link HelloBean} referenced from a spring context definition.
 *
 * The tests expect the {@link CamelContext} to be created/started during deployment.
 * The tests then perfom a {@link CamelContext} lookup and do a simple invokation.
 *
 * @author thomas.diesler@jboss.com
 * @since 21-Apr-2013
 */
@RunWith(Arquillian.class)
public class SpringBeanDeploymentTest {

    static final String SPRING_CAMEL_CONTEXT_XML = "bean-transform-camel-context.xml";

    static final String CAMEL_MODULE = "camel-module.jar";

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @ArquillianResource
    Deployer deployer;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-deployment-tests");
        return archive;
    }

    @Test
    public void testBeanTransformFromModule() throws Exception {
        deployer.deploy(CAMEL_MODULE);
        try {
            CamelContext camelctx = contextRegistry.getCamelContext("spring-context");
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "Kermit", String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            deployer.undeploy(CAMEL_MODULE);
        }
    }

    @Deployment(name = CAMEL_MODULE, managed = false, testable = false)
    public static JavaArchive getModule() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CAMEL_MODULE);
        archive.addClasses(HelloBean.class);
        archive.addAsResource("camel/simple/" + SPRING_CAMEL_CONTEXT_XML, CamelConstants.CAMEL_CONTEXT_FILE_NAME);
        return archive;
    }
}
