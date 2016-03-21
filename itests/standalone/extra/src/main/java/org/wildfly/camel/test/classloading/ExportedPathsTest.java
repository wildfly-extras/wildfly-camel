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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.regex.Matcher;
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
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class ExportedPathsTest {

    private static final String FILE_BASEDIR = "basedir.txt";
    private static final String FILE_EXPORTED_PATH_PATTERNS = "exported-path-patterns.txt";
    private static final String BASELINE_EXPORTED_PATHS_TXT = "src/main/resources/classloading/exported-paths.txt";
    private static final Path FILE_EXPORTED_PATHS = Paths.get(System.getProperty("exportedPathsTxt", BASELINE_EXPORTED_PATHS_TXT));
    private static final Path FILE_MODULE_INFOS = Paths.get("target/module-infos.txt");

    private static final String MODULE_LOADER_OBJECT_NAME = "jboss.modules:type=ModuleLoader,name=LocalModuleLoader-2";
    private static final String MODULE_CAMEL_COMPONENT = "org.apache.camel.component";
    private static final String MODULE_CAMEL = "org.apache.camel";
    private static final String MODULE_WILDFLY_CAMEL_EXTRAS = "org.wildfly.camel.extras:main";

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "exported-paths-tests");
        archive.addAsResource("classloading/" + FILE_EXPORTED_PATH_PATTERNS, FILE_EXPORTED_PATH_PATTERNS);
        archive.addAsResource(new StringAsset(System.getProperty("basedir")), FILE_BASEDIR);
        return archive;
    }

    @Before
    public void setUp() throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName oname = ObjectNameFactory.create(MODULE_LOADER_OBJECT_NAME);
        ModuleLoaderMXBean mbean = JMX.newMXBeanProxy(server, oname, ModuleLoaderMXBean.class);

        Path moduleInfos = resolvePath(FILE_MODULE_INFOS);
        PrintWriter pw = new PrintWriter(new FileWriter(moduleInfos.toFile()));
        try {
            for (String module : new String[] { MODULE_CAMEL, MODULE_CAMEL_COMPONENT }) {
                pw.println(mbean.dumpModuleInformation(module));
                for (DependencyInfo depinfo : mbean.getDependencies(module)) {
                    String moduleName = depinfo.getModuleName();
                    if (moduleName != null && !moduleName.equals(MODULE_WILDFLY_CAMEL_EXTRAS)) {
                        String modinfo;
                        try {
                            modinfo = mbean.dumpModuleInformation(moduleName);
                        } catch (RuntimeException ex) {
                            String msg = ex.getMessage();
                            if (depinfo.isOptional() && msg.contains(moduleName + " not found")) {
                                continue;
                            } else {
                                throw ex;
                            }
                        }
                        pw.println(modinfo);
                        pw.println("[Exported Paths: " + moduleName + "]");
                        List<String> modulePaths = new ArrayList<>(mbean.getModulePathsInfo(moduleName, true).keySet());
                        Collections.sort(modulePaths);
                        for (String path : modulePaths) {
                            if (path.contains("/") && !path.equals("org/apache")) {
                                pw.println(path);
                            }
                        }
                        pw.println();
                    }
                }
            }
            pw.flush();
        } finally {
            pw.close();
        }

        Path exportedPaths = resolvePath(FILE_EXPORTED_PATHS);
        pw = new PrintWriter(new FileWriter(exportedPaths.toFile()));
        List<String> camelExtraDeps = getDependentModuleNames(mbean);
        try {
            for (String module : new String[] { MODULE_CAMEL, MODULE_CAMEL_COMPONENT }) {
                pw.println("[Exported Paths: " + module + "]");

                SortedMap<String, List<String>> modulePathsInfo = mbean.getModulePathsInfo(module, true);
                List<String> modulePaths = new ArrayList<>(modulePathsInfo.keySet());
                Collections.sort(modulePaths);
                for (String path : modulePaths) {
                    String moduleName = getPathModuleLoaderName(modulePathsInfo.get(path));

                    // Ignore paths exported from wildfly.camel.extras as they are not guaranteed to be present
                    if (path.contains("/") && !path.equals("org/apache") && !camelExtraDeps.contains(moduleName)) {
                        pw.println(path);
                    }
                }
                pw.println();
            }
            pw.flush();
        } finally {
            pw.close();
        }
    }

    @Test
    public void testExportedPaths() throws Exception {

        // Build the patterns
        List<Pattern> patterns = null;
        List<Pattern> includePatterns = new ArrayList<>();
        List<Pattern> excludePatterns = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/" + FILE_EXPORTED_PATH_PATTERNS)));
        try {
            String line = reader.readLine();
            while (line != null) {
                if ((line = line.trim()).startsWith("#")) {
                    line = reader.readLine();
                    continue;
                }
                if (line.startsWith("[includes]")) {
                    patterns = includePatterns;
                } else if (line.startsWith("[excludes]")) {
                    patterns = excludePatterns;
                } else if (line.length() > 0 && !line.startsWith("[")) {
                    patterns.add(Pattern.compile(line));
                }
                line = reader.readLine();
            }
        } finally {
            reader.close();
        }

        List<Pattern> usedPatterns = new ArrayList<>(includePatterns);

        // Verify each line
        Path exportedPaths = resolvePath(FILE_EXPORTED_PATHS);
        reader = new BufferedReader(new FileReader(exportedPaths.toFile()));
        try {
            String line = reader.readLine();
            while (line != null) {
                if (line.length() > 0 && !line.startsWith("[")) {
                    boolean match = false;

                    // match include patterns
                    for (Pattern pattern : includePatterns) {
                        if (pattern.matcher(line).matches()) {
                            usedPatterns.remove(pattern);
                            match = true;
                            break;
                        }
                    }

                    // match exclude patterns
                    if (match) {
                        for (Pattern pattern : excludePatterns) {
                            if (pattern.matcher(line).matches()) {
                                match = false;
                                break;
                            }
                        }
                    }

                    Assert.assertTrue("Matching path: " + line, match);
                }
                line = reader.readLine();
            }
        } finally {
            reader.close();
        }

        Assert.assertTrue("Unused patterns: " + usedPatterns, usedPatterns.isEmpty());
    }

    private Path resolvePath(Path other) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/" + FILE_BASEDIR)));
        try {
            return Paths.get(reader.readLine()).resolve(other);
        } finally {
            reader.close();
        }
    }

    private List<String> getDependentModuleNames(ModuleLoaderMXBean mbean) {
        List<String> moduleNames = new ArrayList<>();

        if (mbean.queryLoadedModuleNames().contains(MODULE_WILDFLY_CAMEL_EXTRAS)) {
            for (DependencyInfo dependencyInfo : mbean.getDependencies(MODULE_WILDFLY_CAMEL_EXTRAS)) {
                String moduleName = dependencyInfo.getModuleName();
                if (moduleName != null) {
                    moduleNames.add(moduleName);
                }
            }
        }

        return moduleNames;
    }

    private String getPathModuleLoaderName(List<String> moduleLoaders) {
        String moduleName = "";
        if (moduleLoaders.size() > 0) {
            Pattern p = Pattern.compile(".*\\\"(.*)\\\".*");
            Matcher m = p.matcher(moduleLoaders.get(0));
            if (m.matches()) {
                moduleName = m.group(1);
            }
        }
        return moduleName;
    }

}
