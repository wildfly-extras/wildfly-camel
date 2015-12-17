/*
 * #%L
 * Wildfly Camel :: Testsuite
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

package org.wildfly.camel.test.switchyard;

import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.camel.test.switchyard.subA.JavaDSL;
import org.wildfly.camel.test.switchyard.subA.JavaDSLBuilder;

/**
 * Verify that a deployment with a META-INF/switchyard.xml file
 * disables camel from being added to your deployment.
 */
@RunWith(Arquillian.class)
public class SwitchyardDeploymentTest {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "switchyard-modules-tests.jar");
        archive.addClasses(JavaDSL.class, JavaDSLBuilder.class, EnvironmentUtils.class);
        archive.addAsManifestResource("switchyard/switchyard.xml", "switchyard.xml");
        archive.addAsManifestResource("switchyard/route.xml", "route.xml");
        return archive;
    }

    @Test
    public void testCamelDoesNotLoad() throws Exception {
        Assume.assumeFalse(EnvironmentUtils.switchyardSupport());
        try {
            new DefaultCamelContext();
            Assert.fail("NoClassDefFoundError expected");
        } catch (NoClassDefFoundError er) {
            // expected
        }
    }

}
