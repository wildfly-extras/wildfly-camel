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

package org.wildfly.camel.test.logging;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.LogUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class LoggerIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "logger-tests");
        return archive.addClass(LogUtils.class);
    }
    
    @Test
    public void testSLF4J() throws Exception {
        org.slf4j.LoggerFactory.getLogger("logger-slf4j").info("Message from SLF4J");
        Assert.assertTrue("Verify log message", LogUtils.awaitLogMessage(".*logger-slf4j].*Message from SLF4J$", 5000));
    }

    @Test
    public void testJUL() throws Exception {
        java.util.logging.Logger.getLogger("logger-jul").info("Message from JUL");
        Assert.assertTrue("Verify log message", LogUtils.awaitLogMessage(".*logger-jul].*Message from JUL$", 5000));
    }

    @Test
    public void testCommonsLogging() throws Exception {
        org.apache.commons.logging.LogFactory.getLog("logger-acl").info("Message from ACL");
        Assert.assertTrue("Verify log message", LogUtils.awaitLogMessage(".*logger-acl].*Message from ACL$", 5000));
    }

    @Test
    public void testLog4J() throws Exception {
        org.apache.log4j.Logger.getLogger("logger-log4j").info("Message from Log4J");
        Assert.assertTrue("Verify log message", LogUtils.awaitLogMessage(".*logger-log4j].*Message from Log4J$", 5000));
    }

    @Test
    public void testLog4JV2() throws Exception {
        org.apache.logging.log4j.LogManager.getLogger("logger-log4j-v2").info("Message from Log4J-V2");
        Assert.assertTrue("Verify log message", LogUtils.awaitLogMessage(".*logger-log4j-v2].*Message from Log4J-V2$", 5000));
    }
}
