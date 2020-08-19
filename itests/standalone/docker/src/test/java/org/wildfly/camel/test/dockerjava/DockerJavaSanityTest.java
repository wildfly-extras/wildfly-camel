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
package org.wildfly.camel.test.dockerjava;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Info;

public class DockerJavaSanityTest {

    final static Logger LOG = LoggerFactory.getLogger(DockerManager.class);
    
    @Test
    public void testDockerJava() throws Exception {

        DockerClient client = DockerClientBuilder.createClientBuilder().build();

        Info info = client.infoCmd().exec();
        Assert.assertNotNull("Null info", info);
        Assert.assertNotNull("Null root dir in: " + info, info.getDockerRootDir());
    }

    @Test
    public void testListContainers() throws Exception {

        List<Container> containers = new DockerManager().listContainers();
        containers.forEach(c -> LOG.info("{} {} {} {}", 
                Arrays.asList(c.getNames()), c.getState(), c.getId(), c.getImage()));
    }

    @Test
    public void testAutheticatedPull() throws Exception {

        /*
        export DOCKER_JAVA_REGISTRY_USERNAME=yourname
        export DOCKER_JAVA_REGISTRY_PASSWORD=yourpass
        */
        
        new DockerManager().pullImage("wildflyext/wildfly-camel:12.0.0");
    }

    @Test
    public void testPortBinding() throws Exception {

        DockerManager dockerManager = new DockerManager()
            .createContainer("wildflyext/wildfly-camel:12.0.0")
            .withName("wfcamel")
            .withPortBindings("8081:8080")
            .startContainer();

        try {
            
            dockerManager
                .withAwaitHttp("http://localhost:8081/hawtio")
                .withResponseCode(200)
                .withSleepPolling(500)
                .awaitCompletion(60, TimeUnit.SECONDS);

            int status = HttpRequest.get("http://localhost:8081/hawtio")
                    .timeout(500).getResponse().getStatusCode();
            
            LOG.info("HawtIO status: {}", status);
            Assert.assertEquals(200, status);
            
        } finally {
            
            dockerManager.removeContainer();
        }
    }

    @Test
    public void testNetworkMode() throws Exception {

        String osName = EnvironmentUtils.getOSName();
        LOG.info("OS Name: {}", osName);

        Assume.assumeTrue(EnvironmentUtils.isLinux());
        
        DockerManager dockerManager = new DockerManager()
            .createContainer("wildflyext/wildfly-camel:12.0.0")
            .withName("wfcamel")
            .withNetworkMode("host")
            .startContainer();

        try {

            
            dockerManager
                .withAwaitHttp("http://localhost:8080/hawtio")
                .withResponseCode(200)
                .withSleepPolling(500)
                .awaitCompletion(60, TimeUnit.SECONDS);

            int status = HttpRequest.get("http://localhost:8080/hawtio")
                    .timeout(500).getResponse().getStatusCode();
            
            LOG.info("HawtIO status: {}", status);
            Assert.assertEquals(200, status);
            
        } finally {
            
            dockerManager.removeContainer();
        }
    }
}
