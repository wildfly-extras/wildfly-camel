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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.SpringCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.wildfly.camel.test.spring.subD.MyConfiguration.Counter;
import org.wildfly.camel.test.spring.subD.MyConfiguration;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class SpringExplicitConfigurationTest {

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-spring-config-tests");
        archive.addClasses(MyConfiguration.class);
        return archive;
    }

    @Test
    public void testSpringJavaConfig() throws Exception {

        AnnotationConfigApplicationContext appctx = new AnnotationConfigApplicationContext(MyConfiguration.class);
        CamelContext camelctx = new SpringCamelContext(appctx);
        camelctx.addRoutes(appctx.getBean(RouteBuilder.class));

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "Kermit", String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            camelctx.close();
        }
    }

    @Test
    @SuppressWarnings("resource")
    public void testCounterA() throws Exception {

        ApplicationContext appctx = new AnnotationConfigApplicationContext(MyConfiguration.class);
        Assert.assertEquals(1, appctx.getBean(Counter.class).getCount());
        Assert.assertEquals(1, appctx.getBean(Counter.class).getCount());
    }
}
