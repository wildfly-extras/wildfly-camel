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

package org.wildfly.camel.test.classloading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.utils.ObjectNameFactory;
import org.jboss.modules.management.DependencyInfo;
import org.jboss.modules.management.ModuleLoaderMXBean;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ExportedPathsTest {

    private static final File FILE_EXPORTED_PATHS = new File("../../modules/etc/baseline/exported-paths.txt");
    private static final File FILE_MODULE_INFOS = new File("target/module-infos.txt");

    private static final String MODULE_LOADER_OBJECT_NAME = "jboss.modules:type=ModuleLoader,name=LocalModuleLoader-2";
    private static final String MODULE_CAMEL_COMPONENT = "org.apache.camel.component";
    private static final String MODULE_CAMEL = "org.apache.camel";

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "exported-paths-tests");
        archive.addAsResource("classloading/path-patterns.txt", "path-patterns.txt");
        return archive;
    }

    @Before
    public void setUp() throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName oname = ObjectNameFactory.create(MODULE_LOADER_OBJECT_NAME);
        ModuleLoaderMXBean mbean = JMX.newMXBeanProxy(server, oname, ModuleLoaderMXBean.class);

        PrintWriter pw = new PrintWriter(new FileWriter(FILE_MODULE_INFOS));
        for (String module : new String[] { MODULE_CAMEL, MODULE_CAMEL_COMPONENT }) {
            for (DependencyInfo depinfo : mbean.getDependencies(module)) {
                String moduleName = depinfo.getModuleName();
                if (moduleName != null) {
                    pw.println(mbean.dumpModuleInformation(moduleName));
                }
            }
        }
        pw.flush();
        pw.close();

        pw = new PrintWriter(new FileWriter(FILE_EXPORTED_PATHS));
        for (String module : new String[] { MODULE_CAMEL, MODULE_CAMEL_COMPONENT }) {
            pw.println("[Exported Paths: " + module + "]");
            List<String> modulePaths = new ArrayList<>(mbean.getModulePathsInfo(module, true).keySet());
            Collections.sort(modulePaths);
            for (String path : modulePaths) {
                if (path.contains("/")) {
                    pw.println(path);
                }
            }
            pw.println();
        }

        pw.flush();
        pw.close();
    }
    
    @Test
    public void testExportedPaths() throws Exception {

        // Build the patterns
        List<Pattern> patterns = null;
        List<Pattern> includePatterns = new ArrayList<>();
        List<Pattern> excludePatterns = new ArrayList<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/path-patterns.txt")));
        String line = br.readLine();
        while (line != null) {
            if (line.startsWith("[includes]")) {
                patterns = includePatterns;
            } else if (line.startsWith("[excludes]")) {
                patterns = excludePatterns;
            } else if (line.length() > 0 && !line.startsWith("[")) {
                patterns.add(Pattern.compile(line.trim()));
            }
            line = br.readLine();
        }
        br.close();

        // Verify each line
        br = new BufferedReader(new FileReader(FILE_EXPORTED_PATHS));
        line = br.readLine();
        while (line != null) {
            if (line.length() > 0 && !line.startsWith("[")) {
                boolean match = false;
                for (Pattern pattern : includePatterns) {
                    if (pattern.matcher(line).matches()) {
                        match = true;
                        break;
                    }
                }
                for (Pattern pattern : excludePatterns) {
                    if (pattern.matcher(line).matches()) {
                        match = false;
                        break;
                    }
                }
                Assert.assertTrue("Matches: " + line, match);
            }
            line = br.readLine();
        }
        br.close();
    }
}
