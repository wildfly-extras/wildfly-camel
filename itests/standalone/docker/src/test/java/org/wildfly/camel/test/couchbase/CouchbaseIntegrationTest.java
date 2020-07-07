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
package org.wildfly.camel.test.couchbase;

import java.util.concurrent.TimeUnit;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.camel.test.dockerjava.DockerManager;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({CouchbaseIntegrationTest.ContainerSetupTask.class})
@Ignore("[CAMEL-15259] camel-couchbase seems outdated or broken  [Target 3.4.1]")
public class CouchbaseIntegrationTest {

    private static final String CONTAINER_NAME = "couchbase";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-couchbase-tests.jar")
            .addClasses(TestUtils.class, HttpRequest.class);
    }

    static class ContainerSetupTask implements ServerSetupTask {

    	private DockerManager dockerManager;

        @Override
        public void setup(ManagementClient managementClient, String someId) throws Exception {
        	
            String dockerHost = TestUtils.getDockerHost();
            
			/*
			docker run --detach \
				--name couchbase \
				-p 8091:8091 \
				-p 8092:8092 \
				-p 8093:8093 \
				-p 8094:8094 \
				-p 11210:11210 \
				couchbase:community-6.5.1
			*/
        	
        	dockerManager = new DockerManager()
        			.createContainer("couchbase:community-6.5.1", true)
        			.withName(CONTAINER_NAME)
        			.withPortBindings("8091:8091", "8092:8092", "8093:8093", "8094:8094", "11210:11210")
        			.startContainer();

			dockerManager
				.withAwaitHttp("http://" + dockerHost + ":8091")
				.withResponseCode(200)
				.withSleepPolling(500)
				.awaitCompletion(60, TimeUnit.SECONDS);
			
        	/* 
			Setup a new cluster
			https://docs.couchbase.com/server/current/cli/cbcli/couchbase-cli-cluster-init.html
			
			docker exec couchbase \
				couchbase-cli cluster-init -c 127.0.0.1 --cluster-username Administrator --cluster-password password \
				--cluster-name default --cluster-ramsize 1024 \
				--services data,index,query
				
			Load the beer sample data
			https://docs.couchbase.com/server/current/cli/cbdocloader-tool.html
			
			docker exec couchbase \
				cbdocloader -c couchbase://127.0.0.1 -u Administrator -p password \
				-v -m 1024 -b beer-sample -d /opt/couchbase/samples/beer-sample.zip
        	*/
        	
        }

        @Override
        public void tearDown(ManagementClient managementClient, String someId) throws Exception {
        	if (dockerManager != null) {
            	dockerManager.removeContainer();
        	}
        }
    }

    @Test
    public void testComponent() throws Exception {

        String dockerHost = TestUtils.getDockerHost();
        
        try (DefaultCamelContext camelctx = new DefaultCamelContext()) {
        	
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
					fromF("couchbase:http://%s/beer-sample?password=password&designDocumentName=&viewName=&limit=10", dockerHost)
                    .to("mock:result");
                }
            });

            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mockEndpoint.expectedMessageCount(10);

            camelctx.start();
            
            mockEndpoint.assertIsSatisfied();
        }
    }
}
