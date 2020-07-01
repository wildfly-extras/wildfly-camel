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
package org.wildfly.camel.test.solr;

import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.solr.SolrConstants;
import org.apache.camel.impl.DefaultCamelContext;
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
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.camel.test.dockerjava.DockerManager;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({SolrIntegrationTest.ContainerSetupTask.class})
public class SolrIntegrationTest {

    private static final String CONTAINER_NAME = "solr";
    private static final String ID = "12345";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-solr-tests.jar")
            .addClasses(TestUtils.class, HttpRequest.class);
    }

    static class ContainerSetupTask implements ServerSetupTask {

    	private DockerManager dockerManager;

        @Override
        public void setup(ManagementClient managementClient, String someId) throws Exception {
        	
            String dockerHost = TestUtils.getDockerHost();
            
			/*
			docker run --detach \
				--name solr \
				-p 8983:8983 \
				solr:8.4.1 solr-precreate wfc
			*/
        	
        	dockerManager = new DockerManager()
        			.createContainer("solr:8.4.1", true)
        			.withName(CONTAINER_NAME)
        			.withPortBindings("8983:8983")
        			.withCmd("solr-precreate wfc")
        			.startContainer();

			dockerManager
				.withAwaitHttp("http://" + dockerHost + ":8983/solr/wfc/select")
				.withResponseCode(200)
				.withSleepPolling(500)
				.awaitCompletion(60, TimeUnit.SECONDS);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String someId) throws Exception {
        	if (dockerManager != null) {
            	dockerManager.removeContainer();
        	}
        }
    }

    @Test
    public void testSolrComponent() throws Exception {
        
    	try(CamelContext camelctx = new DefaultCamelContext()) {
    		
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:insert")
                        .setHeader(SolrConstants.OPERATION, constant(SolrConstants.OPERATION_INSERT))
                        .setHeader(SolrConstants.FIELD + "id", body())
                        .toF("solr://%s:8983/solr/wfc", TestUtils.getDockerHost());

                    from("direct:commit")
                        .setHeader(SolrConstants.OPERATION, constant(SolrConstants.OPERATION_COMMIT))
                        .toF("solr://%s:8983/solr/wfc", TestUtils.getDockerHost());

                    from("direct:delete")
                        .setHeader(SolrConstants.OPERATION, constant(SolrConstants.OPERATION_DELETE_BY_ID))
                        .toF("solr://%s:8983/solr/wfc", TestUtils.getDockerHost());
                }
            });

            camelctx.start();
            
            ProducerTemplate template = camelctx.createProducerTemplate();

            template.sendBody("direct:insert", ID);
            template.sendBody("direct:commit", null);

            String result = querySolr();
            Assert.assertTrue("Expected Solr query result to return 1 result", result.contains("\"numFound\":1"));

            template.requestBody("direct:delete", ID);
            template.sendBody("direct:commit", null);

            result = querySolr();
            Assert.assertTrue("Expected Solr query result to return 0 results", result.contains("\"numFound\":0"));
    	}
    }

    private String querySolr() throws Exception {
    	String queryUrl = String.format("http://%s:8983/solr/wfc/select/?q=id%%3A%s", TestUtils.getDockerHost(), ID);
        return HttpRequest.get(queryUrl).getResponse().getBody();
    }
}
