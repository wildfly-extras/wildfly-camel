/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2018 RedHat
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
package org.wildfly.camel.test.pgevent;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.DMRUtils;
import org.wildfly.extension.camel.CamelAware;

@Ignore
@CamelAware
@RunWith(ArquillianConditionalRunner.class)
@ServerSetup({ PGEventIntegrationTest.DataSourceServerSetupTask.class })
@RequiresDocker
public class PGEventIntegrationTest {

    private static final String CONTAINER_NAME = "postgres";

    @ArquillianResource
    private CubeController cubeController;

    static class DataSourceServerSetupTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode batchNode = DMRUtils.batchNode()
                .addStep("subsystem=datasources/jdbc-driver=pgsql", "add(driver-name=pgsql,driver-module-name=com.impossibl.pgjdbc)")
                .addStep("subsystem=datasources/data-source=PostgreSQLDS", "add(driver-name=pgsql,jndi-name=java:jboss/datasources/PostgreSQLDS,"
                    + "password=s3cret,user-name=postgres,connection-url=jdbc:pgsql://127.0.0.1:42654/postgres,"
                    + "pool-name=PostgreSQLDS)")
                .build();

            managementClient.getControllerClient().execute(batchNode);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode batchNode = DMRUtils.batchNode()
                .addStep("subsystem=datasources/data-source=PostgreSQLDS", "remove")
                .addStep("subsystem=datasources/jdbc-driver=pgsql", "remove")
                .build();

            managementClient.getControllerClient().execute(batchNode);

        }
    }

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-pgevent-tests.jar");
    }

    @Before
    public void setUp() throws Exception {
        cubeController.create(CONTAINER_NAME);
        cubeController.start(CONTAINER_NAME);
    }

    @After
    public void tearDown() throws Exception {
        cubeController.stop(CONTAINER_NAME);
        cubeController.destroy(CONTAINER_NAME);
    }

    @Test
    public void testPGEventComponent() throws Exception {
        String uri = String.format("pgevent:///postgres/testchannel?datasource=#java:jboss/datasources/PostgreSQLDS");
        String body = "Hello Kermit";

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("timer://test?repeatCount=1&period=1")
                .setBody(constant(body))
                .toF(uri);

                fromF(uri)
                .to("mock:result");
            }
        });

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedBodiesReceived(body);

        camelctx.start();
        try {
            mockEndpoint.assertIsSatisfied(5000);
        } finally {
            camelctx.stop();
        }
    }
}
