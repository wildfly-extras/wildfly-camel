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

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
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
    public void testAutheticatedPull() throws Exception {

        // export DOCKER_JAVA_REGISTRY_PASSWORD=********
        
        new DockerManager().pullImage("wildflyext/wildfly-camel:12.0.0");

    }
}
