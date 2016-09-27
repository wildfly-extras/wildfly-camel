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
package org.wildfly.camel.test.cassandra;


import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.cassandraunit.CassandraCQLUnit;
import org.cassandraunit.dataset.CQLDataSet;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({ CassandraIntegrationTest.CassandraServerSetup.class })
public class CassandraIntegrationTest {

    static final String HOST = "127.0.0.1";
    static final String KEYSPACE = "camel_ks";
    
    static final String CQL = "select login, first_name, last_name from camel_user";
    
    static class CassandraServerSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            EmbeddedCassandraServerHelper.startEmbeddedCassandra("/camel-cassandra.yaml", "target/camel-cassandra", 30000);
            new LoadableCassandraCQLUnit(new ClassPathCQLDataSet("cassandra/BasicDataSet.cql", KEYSPACE), "/camel-cassandra.yaml").setup();
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
        }
        
        static class LoadableCassandraCQLUnit extends CassandraCQLUnit {

            LoadableCassandraCQLUnit(CQLDataSet dataSet, String configFile) {
                super(dataSet, configFile);
            }

            void setup() {
                super.load();
            }
        } 
    }
    
    @Deployment
    public static JavaArchive createdeployment() {
        return ShrinkWrap.create(JavaArchive.class, "cassandra-tests");
    }

    @Test
    public void testConsumeAll() throws Exception {
        
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("cql://localhost/camel_ks?cql=" + CQL)
                .to("seda:end");
            }
        });

        camelctx.start();
        try {
            ConsumerTemplate consumer = camelctx.createConsumerTemplate();
            List<?> result = consumer.receiveBody("seda:end", 3000, List.class);
            Assert.assertNotNull("Result not null", result);
            Assert.assertEquals("Two records selected", 2, result.size());
        } finally {
            camelctx.stop();
        }
    }
}
