/*
 * Copyright 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.domain.smoke;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.arquillian.domain.api.TargetsServerGroup;

/**
 * For Domain server DeployableContainer implementations, the DeployableContainer will register
 * all groups/individual servers it controls as Containers in Arquillian's Registry during start.
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 */
@RunWith(Arquillian.class)
public class ManagedDomainTestCase {

    @Deployment(name = "dep1")
    @TargetsServerGroup("main-server-group")
    public static WebArchive create1() {
        return ShrinkWrap.create(WebArchive.class);
    }

    @Test
    @OperateOnDeployment("dep1")
    @TargetsContainer("master:server-one")
    public void shouldRunInContainer1() throws Exception {
        // Get the logger path which should contain the name of the server
        final String logDir = System.getProperty("jboss.server.log.dir");
        Assert.assertNotNull("Could not determine the jboss.server.log.dir property", logDir);
        Assert.assertTrue("Log dir should contain server-one: " + logDir, logDir.contains("server-one"));
    }

    @Test
    @TargetsContainer("master:server-two")
    public void shouldRunInContainer2() throws Exception {
        // Get the logger path which should contain the name of the server
        final String logDir = System.getProperty("jboss.server.log.dir");
        Assert.assertNotNull("Could not determine the jboss.server.log.dir property", logDir);
        Assert.assertTrue("Log dir should contain server-two: " + logDir, logDir.contains("server-two"));
    }
}
