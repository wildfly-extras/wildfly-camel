/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2016 RedHat
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
package org.wildfly.camel.test.yaml;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.snakeyaml.SnakeYAMLDataFormat;
import org.apache.camel.component.snakeyaml.TypeFilters;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.dataformat.YAMLLibrary;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.types.Customer;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class YamlDataFormatIntegrationTest {

    public static final String CUSTOMER_YAML = "!!org.wildfly.camel.test.common.types.Customer {firstName: John, lastName: Doe}";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-yaml-tests")
            .addClass(Customer.class);
    }

    @Test
    public void testMarshalYaml() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .marshal().yaml(YAMLLibrary.SnakeYAML);
            }
        });

        ClassLoader loader = SnakeYAMLDataFormat.class.getClassLoader();
        loader = loader.loadClass("org.yaml.snakeyaml.Yaml").getClassLoader();
        System.out.println(loader);
        
        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            String result = template.requestBody("direct:start", new Customer("John", "Doe"), String.class);
            Assert.assertEquals(CUSTOMER_YAML, result.trim());
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testUnmarshalYaml() throws Exception {

        SnakeYAMLDataFormat yaml = new SnakeYAMLDataFormat();
        yaml.addTypeFilters(TypeFilters.types(Customer.class));

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .unmarshal(yaml);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            Customer result = template.requestBody("direct:start", CUSTOMER_YAML, Customer.class);
            Assert.assertNotNull(result);
            Assert.assertEquals("John", result.getFirstName());
            Assert.assertEquals("Doe", result.getLastName());
        } finally {
            camelctx.close();
        }
    }
}
