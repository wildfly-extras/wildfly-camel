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
package org.wildfly.camel.test.docker;

import com.github.dockerjava.api.model.Version;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(ArquillianConditionalRunner.class)
@RequiresDocker
public class DockerIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-docker-tests.jar")
            .addClass(TestUtils.class);
    }

    @Test
    public void testDockerComponent() throws Exception {
        Assume.assumeTrue("[#2287] Docker component cannot use TLS", System.getenv("DOCKER_TLS_VERIFY") == null);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // Default to connecting via a unix socket
                String uriOptions = "socket=true";

                if (System.getenv("DOCKER_HOST") != null) {
                    // Set the host and port options
                    uriOptions = String.format("host=%s&port=%d", TestUtils.getDockerHost(), TestUtils.getDockerPort());
                }

                from("direct:start")
                .toF("docker:version?%s", uriOptions);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            Version dockerVersion = template.requestBody("direct:start", null, Version.class);
            Assert.assertNotNull("Docker version not null", dockerVersion);
            Assert.assertFalse("Docker version was empty", dockerVersion.getVersion().isEmpty());
        } finally {
            camelctx.stop();
        }
    }
}
