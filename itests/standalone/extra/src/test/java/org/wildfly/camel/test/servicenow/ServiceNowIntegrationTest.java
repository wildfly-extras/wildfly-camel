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
package org.wildfly.camel.test.servicenow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.servicenow.ServiceNowConstants;
import org.apache.camel.component.servicenow.ServiceNowParams;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.servicenow.subA.Incident;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class ServiceNowIntegrationTest {

    // Enum values correspond to environment variable names
    private enum ServiceNowOption {
        SERVICENOW_INSTANCE("instanceName"),
        SERVICENOW_USERNAME("userName"),
        SERVICENOW_PASSWORD("password"),
        SERVICENOW_CLIENT_ID("oauthClientId"),
        SERVICENOW_CLIENT_SECRET("oauthClientSecret"),
        SERVICENOW_RELEASE("release");

        private String configPropertyName;

        ServiceNowOption(String configPropertyName) {
            this.configPropertyName = configPropertyName;
        }
    }

    @Deployment
    public static JavaArchive deployment() {
        return ShrinkWrap.create(JavaArchive.class, "servicenow-tests")
            .addPackage(Incident.class.getPackage());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSearchIncidents() throws Exception {
        Map<String, Object> serviceNowOptions = createServiceNowOptions();

        Assume.assumeTrue("[#1674] Enable ServiceNow testing in Jenkins", 
                serviceNowOptions.size() == ServiceNowIntegrationTest.ServiceNowOption.values().length);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("servicenow:{{env:SERVICENOW_INSTANCE}}"
                    + "?userName={{env:SERVICENOW_USERNAME}}"
                    + "&password={{env:SERVICENOW_PASSWORD}}"
                    + "&oauthClientId={{env:SERVICENOW_CLIENT_ID}}"
                    + "&oauthClientSecret={{env:SERVICENOW_CLIENT_SECRET}}"
                    + "&release={{env:SERVICENOW_RELEASE}}"
                    + "&model.incident=org.wildfly.camel.test.servicenow.subA.Incident");
            }
        });

        camelctx.start();
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put(ServiceNowConstants.RESOURCE, "table");
            headers.put(ServiceNowConstants.ACTION, ServiceNowConstants.ACTION_RETRIEVE);
            headers.put(ServiceNowParams.PARAM_TABLE_NAME.getHeader(), "incident");

            ProducerTemplate producer = camelctx.createProducerTemplate();
            List<Incident> result = producer.requestBodyAndHeaders("direct:start", null, headers, List.class);

            Assert.assertNotNull(result);
            Assert.assertTrue(result.size() > 0);
        } finally {
            camelctx.stop();
        }
    }

    protected Map<String, Object> createServiceNowOptions() throws Exception {
        final Map<String, Object> options = new HashMap<>();

        for (ServiceNowIntegrationTest.ServiceNowOption option : ServiceNowIntegrationTest.ServiceNowOption.values()) {
            String envVar = System.getenv(option.name());
            if (envVar == null || envVar.length() == 0) {
                options.clear();
            } else {
                options.put(option.configPropertyName, envVar);
            }
        }

        return options;
    }
}
