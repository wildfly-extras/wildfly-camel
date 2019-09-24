/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.jcache;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.ManifestBuilder;
import org.wildfly.camel.utils.ObjectNameFactory;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

/**
 * [camel-jcache] MBeans are not properly undeployed
 *
 * https://issues.jboss.org/browse/ENTESB-9643
 */
@CamelAware
@RunWith(Arquillian.class)
public class JCacheMBeansTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @ArquillianResource
    ManagementClient managementClient;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "jcache-mbeans-tests");
        archive.addAsResource("jcache/jcache-camel-context.xml", "some-other-name.xml");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.jboss.as.controller-client");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testJCacheMBeans() throws Exception {

        MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        ObjectName onameAll = ObjectNameFactory.create("javax.cache:*");
        ObjectName onameThis = ObjectNameFactory.create("javax.cache:type=CacheStatistics,CacheManager=hazelcast,Cache=test-cache");

        URL resourceUrl = getClass().getResource("/some-other-name.xml");
        ServerDeploymentHelper helper = new ServerDeploymentHelper(managementClient.getControllerClient());
        String runtimeName = helper.deploy("jcache-camel-context.xml", resourceUrl.openStream());
        try {

            CamelContext camelctx = contextRegistry.getCamelContext("jcache-test");
            Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());

            Set<ObjectInstance> mbeans = server.queryMBeans(onameAll, null);
            System.out.println(">>>>>>>>> JCache MBeans: " + mbeans.size());
            mbeans.forEach(mb -> System.out.println(mb.getObjectName()));

            Assert.assertTrue(server.isRegistered(onameThis));

        } finally {
        	helper.undeploy(runtimeName);
        }

        Set<ObjectInstance> mbeans = server.queryMBeans(onameAll, null);
        System.out.println(">>>>>>>>> JCache MBeans: " + mbeans.size());
        mbeans.forEach(mb -> System.out.println(mb.getObjectName()));
        Assert.assertTrue(mbeans.isEmpty());
    }
}
