/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2015 RedHat
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

package org.wildfly.camel.test.salesforce;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.salesforce.SalesforceComponent;
import org.apache.camel.component.salesforce.SalesforceLoginConfig;
import org.apache.camel.component.salesforce.api.dto.bulk.ContentType;
import org.apache.camel.component.salesforce.api.dto.bulk.JobInfo;
import org.apache.camel.component.salesforce.api.dto.bulk.OperationEnum;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.IntrospectionSupport;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.salesforce.dto.Opportunity;
import org.wildfly.camel.test.salesforce.dto.Opportunity_StageNameEnum;
import org.wildfly.camel.test.salesforce.dto.QueryRecordsOpportunity;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class SalesforceIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-salesforce-tests.jar")
            .addPackage(Opportunity.class.getPackage());
    }

    @Test
    public void testComponentLoads() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        Endpoint endpoint = camelctx.getEndpoint("salesforce:getVersions");
        Assert.assertNotNull(endpoint);
        Assert.assertEquals(endpoint.getClass().getName(), "org.apache.camel.component.salesforce.SalesforceEndpoint");
    }

    @Test
    public void testSalesforceQueryProducer() throws Exception {
        Map<String, Object> salesforceOptions = createSalesforceOptions();

        Assume.assumeTrue("[#1676] Enable Salesforce testing in Jenkins",
                salesforceOptions.size() == SalesforceOption.values().length);

        SalesforceLoginConfig loginConfig = new SalesforceLoginConfig();
        IntrospectionSupport.setProperties(loginConfig, salesforceOptions);

        SalesforceComponent component = new SalesforceComponent();
        component.setPackages("org.wildfly.camel.test.salesforce.dto");
        component.setLoginConfig(loginConfig);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addComponent("salesforce",  component);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:query")
                .to("salesforce:query?sObjectQuery=SELECT id,name from Opportunity&sObjectClass=" + QueryRecordsOpportunity.class.getName());
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            QueryRecordsOpportunity queryRecords = template.requestBody("direct:query", null, QueryRecordsOpportunity.class);

            Assert.assertNotNull("Expected query records result to not be null", queryRecords);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testSalesforceTopicConsumer() throws Exception {
        Map<String, Object> salesforceOptions = createSalesforceOptions();

        Assume.assumeTrue("[#1676] Enable Salesforce testing in Jenkins",
                salesforceOptions.size() == SalesforceOption.values().length);

        SalesforceLoginConfig loginConfig = new SalesforceLoginConfig();
        IntrospectionSupport.setProperties(loginConfig, salesforceOptions);

        SalesforceComponent component = new SalesforceComponent();
        component.setPackages("org.wildfly.camel.test.salesforce.dto");
        component.setLoginConfig(loginConfig);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addComponent("salesforce",  component);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("salesforce:CamelTestTopic?notifyForFields=ALL&"
                    + "notifyForOperationCreate=true&notifyForOperationDelete=true&notifyForOperationUpdate=true&"
                    + "sObjectName=Opportunity&"
                    + "updateTopic=true&sObjectQuery=SELECT Id, Name FROM Opportunity")
                .to("mock:result");
            }
        });

        Opportunity opportunity = new Opportunity();
        opportunity.setName("test");
        opportunity.setStageName(Opportunity_StageNameEnum.PROSPECTING);
        opportunity.setCloseDate(ZonedDateTime.now());

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedMinimumMessageCount(1);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("salesforce:upsertSObject?SObjectIdName=Name", opportunity);

            mockEndpoint.assertIsSatisfied(5000);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testSalesforceCreateJob() throws Exception {
        Map<String, Object> salesforceOptions = createSalesforceOptions();

        Assume.assumeTrue("[#1676] Enable Salesforce testing in Jenkins",
            salesforceOptions.size() == SalesforceOption.values().length);

        SalesforceLoginConfig loginConfig = new SalesforceLoginConfig();
        IntrospectionSupport.setProperties(loginConfig, salesforceOptions);

        SalesforceComponent component = new SalesforceComponent();
        component.setPackages("org.wildfly.camel.test.salesforce.dto");
        component.setLoginConfig(loginConfig);

        JobInfo jobInfo = new JobInfo();
        jobInfo.setOperation(OperationEnum.QUERY);
        jobInfo.setContentType(ContentType.CSV);
        jobInfo.setObject(Opportunity.class.getSimpleName());

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addComponent("salesforce",  component);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("salesforce:createJob");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            JobInfo result = template.requestBody("direct:start", jobInfo, JobInfo.class);

            Assert.assertNotNull("Expected JobInfo result to not be null", result);
            Assert.assertNotNull("Expected JobInfo result ID to not be null", result.getId());
        } finally {
            camelctx.stop();
        }
    }

    protected Map<String, Object> createSalesforceOptions() throws Exception {
        final Map<String, Object> options = new HashMap<>();

        for (SalesforceOption option : SalesforceOption.values()) {
            String envVar = System.getenv(option.name());
            if (envVar == null || envVar.length() == 0) {
                options.clear();
            } else {
                options.put(option.configPropertyName, envVar);
            }
        }

        return options;
    }

    // Enum values correspond to environment variable names
    private enum SalesforceOption {
        SALESFORCE_CONSUMER_KEY("clientId"),
        SALESFORCE_CONSUMER_SECRET("clientSecret"),
        SALESFORCE_USER("userName"),
        SALESFORCE_PASSWORD("password");

        private String configPropertyName;

        SalesforceOption(String configPropertyName) {
            this.configPropertyName = configPropertyName;
        }
    }
}
