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

import java.io.File;

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
    public void testDockerComponentForUnixSocket() throws Exception {
        File dockerSocket = new File("/var/run/docker.sock");

        Assume.assumeTrue("[#2287] Docker component cannot use TLS", System.getenv("DOCKER_TLS_VERIFY") == null);
        Assume.assumeTrue("Docker socket /var/run/docker.sock does not exist or is not writable", dockerSocket.exists() && dockerSocket.canWrite());

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .toF("docker:version?socket=true");
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

    @Test
    public void testDockerComponentForHostnameAndPort() throws Exception {
        Assume.assumeTrue("[#2287] Docker component cannot use TLS", System.getenv("DOCKER_TLS_VERIFY") == null);
        Assume.assumeNotNull("DOCKER_HOST environment variable is not set", System.getenv("DOCKER_HOST"));

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .toF("docker:version?host=%s&port=%d", TestUtils.getDockerHost(), TestUtils.getDockerPort());
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
