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

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.solr.SolrConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(ArquillianConditionalRunner.class)
@RequiresDocker
public class SolrIntegrationTest {

    private static final String CONTAINER_NAME = "solr";
    private static final String ID = "12345";

    @ArquillianResource
    private CubeController cubeController;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-solr-tests.jar")
            .addClass(TestUtils.class);
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
    public void testSolrComponent() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
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
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            template.sendBody("direct:insert", ID);
            template.sendBody("direct:commit", null);

            String result = solrQuery(template);
            Assert.assertTrue("Expected Solr query result to return 1 result", result.contains("\"numFound\":1"));

            template.requestBody("direct:delete", ID);
            template.sendBody("direct:commit", null);

            result = solrQuery(template);
            Assert.assertTrue("Expected Solr query result to return 0 results", result.contains("\"numFound\":0"));

        } finally {
            camelctx.stop();
        }
    }

    private String solrQuery(ProducerTemplate template) {
        return template.requestBody("http4://localhost:8983/solr/wfc/select/?q=" + "id%3A" + ID, null, String.class);
    }
}
