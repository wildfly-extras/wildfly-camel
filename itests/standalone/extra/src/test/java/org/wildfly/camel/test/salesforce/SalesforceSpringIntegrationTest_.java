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

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.salesforce.dto.Account;
import org.wildfly.camel.test.salesforce.dto.Opportunity;
import org.wildfly.camel.test.salesforce.dto.Order;
import org.wildfly.camel.test.salesforce.dto.QueryRecordsAccount;
import org.wildfly.camel.test.salesforce.dto.QueryRecordsOpportunity;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
// [#2521] Add support for camel context autoStartup=false
public class SalesforceSpringIntegrationTest_ {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-salesforce-spring-tests.jar");
        archive.addAsResource("salesforce/salesforce-camel-context.xml");
        archive.addPackage(Order.class.getPackage());
        return archive;
    }

    @Test
    public void testSalesforceQueryProducer() throws Exception {

        Map<String, Object> salesforceOptions = createSalesforceOptions();
        Assume.assumeTrue("[#1676] Enable Salesforce testing in Jenkins",
                salesforceOptions.size() == SalesforceOption.values().length);

        CamelContext camelctx = contextRegistry.getCamelContext("salesforce-context");
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());

        ProducerTemplate template = camelctx.createProducerTemplate();

        QueryRecordsOpportunity oppRecords = template.requestBody("direct:opportunity", null, QueryRecordsOpportunity.class);
        Assert.assertNotNull("Expected query records result to not be null", oppRecords);
        Assert.assertTrue("Expected some records", oppRecords.getRecords().size() > 0);

        Opportunity oppItem = oppRecords.getRecords().get(0);
        Assert.assertNotNull("Expected Oportunity Id", oppItem.getId());
        Assert.assertNotNull("Expected Oportunity Name", oppItem.getName());

        QueryRecordsAccount accRecords = template.requestBody("direct:account", null, QueryRecordsAccount.class);
        Assert.assertNotNull("Expected query records result to not be null", accRecords);

        Account accItem = accRecords.getRecords().get(0);
        Assert.assertNotNull("Expected Account Id", accItem.getId());
        Assert.assertNotNull("Expected Account Number", accItem.getAccountNumber());
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
