/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
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
package org.wildfly.camel.test.freemarker;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.freemarker.FreemarkerConstants;
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
public class FreeMarkerIntegrationTest {

    private static final String TEMPLATE_FILE = "template.ftl";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-freemarker-tests.jar")
            .addAsResource("freemarker/" + TEMPLATE_FILE, TEMPLATE_FILE);
    }

    @Test
    public void testFreemarkerProducer() throws Exception {
    	
        try (CamelContext camelctx = new DefaultCamelContext()) {
        	
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start")
                    .toF("freemarker:%s", TEMPLATE_FILE + "?allowTemplateFromHeader=true");
                }
            });
            camelctx.start();
            
            Map<String, String> headerMap = new HashMap<>();
            headerMap.put("greeting", "Hello");
            headerMap.put("name", "Kermit");

            Map<String, Object> variableMap = new HashMap<>();
            variableMap.put("headers", headerMap);

            ProducerTemplate template = camelctx.createProducerTemplate();
            String result = template.requestBodyAndHeader("direct:start", null,
                FreemarkerConstants.FREEMARKER_DATA_MODEL, variableMap, String.class);

            Assert.assertEquals("Hello Kermit", result);
        }
    }
}
