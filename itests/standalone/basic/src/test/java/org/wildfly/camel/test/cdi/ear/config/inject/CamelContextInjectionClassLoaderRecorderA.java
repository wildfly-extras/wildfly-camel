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
package org.wildfly.camel.test.cdi.ear.config.inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@Singleton
@Startup
public class CamelContextInjectionClassLoaderRecorderA {

    @Inject
    CamelContext camelctx;

    @PostConstruct
    public void init() {
        Path dataDir = Paths.get(System.getProperty("jboss.server.data.dir"));
        String moduleName = TestUtils.getClassLoaderModuleName(camelctx.getApplicationContextClassLoader());
        try {
            Files.write(dataDir.resolve("injected-context-a.txt"), moduleName.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // Ignore
        }
    }
}
