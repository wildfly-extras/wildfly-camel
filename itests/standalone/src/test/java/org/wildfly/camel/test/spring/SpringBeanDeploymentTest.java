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
import org.apache.camel.ProducerTemplate;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.smoke.subA.HelloBean;
import org.wildfly.extension.camel.CamelConstants;
import org.wildfly.extension.camel.CamelContextRegistry;

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

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-deployment-tests");
        archive.addClasses(HelloBean.class);
        archive.addAsResource("spring/bean-transform-camel-context.xml", CamelConstants.CAMEL_CONTEXT_FILE_NAME);
        return archive;
    }

    @Test
    public void testBeanTransformFromModule() throws Exception {
        CamelContext camelctx = contextRegistry.getContext("spring-context");
        ProducerTemplate producer = camelctx.createProducerTemplate();
        String result = producer.requestBody("direct:start", "Kermit", String.class);
        Assert.assertEquals("Hello Kermit", result);
    }
}
