/*
 * #%L
 * Wildfly Camel :: Example :: Camel REST
 * %%
 * Copyright (C) 2013 - 2014 RedHat
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
package org.wildfly.camel.test.openshift.domain;

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.HttpRequest;
import org.wildfly.camel.test.common.docker.DeployCommand;
import org.wildfly.camel.test.common.docker.RunCommand;


@RunAsClient
@RunWith(Arquillian.class)
public class DomainIntegrationTest {

    static String DEPLOYMENT_NAME = "domain-endpoint.war";
    
    @ContainerResource
    ManagementClient mgmtClient;
    
    @Test
    public void testEndpoint() throws Exception {
        
        String host = mgmtClient.getMgmtAddress();
        RunCommand.Result cmdres = getDeployCommand().connect("slave", "slave", host, 9990).exec();
        Assert.assertEquals(0, cmdres.exitValue());
        
        String reqspec = "http://" + host + ":8181/domain-endpoint";
        System.out.println(reqspec);
        
        String result = HttpRequest.get(reqspec, 10, TimeUnit.SECONDS);
        Assert.assertTrue("Starts with Hello: " + result, result.startsWith("Hello"));
    }

    private DeployCommand getDeployCommand() {
        String[] files = new File("target").list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".war");
            }
        });
        Assert.assertNotNull("File found", files);
        Assert.assertEquals("One file found", 1, files.length);
        
        String runtimeName = "domain-endpoint.war";
        DeployCommand docker = new DeployCommand(runtimeName, new File("target/" + files[0]));
        return docker;
    }
}
