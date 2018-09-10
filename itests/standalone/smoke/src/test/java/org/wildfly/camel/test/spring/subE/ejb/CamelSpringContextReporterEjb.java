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
package org.wildfly.camel.test.spring.subE.ejb;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.apache.camel.CamelContext;
import org.wildfly.camel.utils.IllegalStateAssertion;
import org.wildfly.extension.camel.CamelAware;

@Startup
@Singleton
@CamelAware
public class CamelSpringContextReporterEjb {

    @Resource(lookup = "java:jboss/camel/context/jndi-delayed-binding-spring-context")
    private CamelContext camelctxA;

    @Resource(mappedName = "java:jboss/camel/context/jndi-delayed-binding-spring-context")
    private CamelContext camelctxB;

    @Resource(name = "java:jboss/camel/context/jndi-delayed-binding-spring-context")
    private CamelContext camelctxC;

    @PostConstruct
    public void initialize() {
        String dataDir = System.getProperty("jboss.server.data.dir");
        IllegalStateAssertion.assertNotNull(dataDir, "Property 'jboss.server.data.dir' not set");
        IllegalStateAssertion.assertTrue(new File(dataDir).isDirectory(), "Not a directory: " + dataDir);
        Path filePath = Paths.get(dataDir, "camel-context-status.txt");

        try (FileWriter fw = new FileWriter(filePath.toFile())) {
            if (camelctxA != null) {
                fw.write("camelctxA,");
            }

            if (camelctxB != null) {
                fw.write("camelctxB,");
            }

            if (camelctxC != null) {
                fw.write("camelctxC");
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
