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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class ExportedPathsTest {

    private static class PatternWithExceptions {
        public static PatternWithExceptions parse(String rawPatternWithExceptions) {
            final String DELIM = " except ";
            final int delim = rawPatternWithExceptions.indexOf(DELIM);
            final String pattern;
            final List<Pattern> exceptionPatterns;
            if (delim < 0) {
                pattern = rawPatternWithExceptions;
                exceptionPatterns = Collections.emptyList();
            } else {
                pattern = rawPatternWithExceptions.substring(0, delim);
                exceptionPatterns = Arrays.stream(rawPatternWithExceptions.substring(delim + DELIM.length()).split(" ")).map(str -> Pattern.compile(str)).collect(Collectors.toList());
            }
            return new PatternWithExceptions(rawPatternWithExceptions, Pattern.compile(pattern), exceptionPatterns);
        }
        private final String rawPatternWithExceptions;
        private final Pattern pattern;
        private final List<Pattern> exceptPatterns;
        public PatternWithExceptions(String rawPatternWithExceptions, Pattern pattern, List<Pattern> exceptPatterns) {
            super();
            this.rawPatternWithExceptions = rawPatternWithExceptions;
            this.pattern = pattern;
            this.exceptPatterns = exceptPatterns;
        }
        public boolean matches(String line) {
            for (Pattern p : exceptPatterns) {
                if (p.matcher(line).matches()) {
                    return false;
                }
            }
            return pattern.matcher(line).matches();
        }

        @Override
        public String toString() {
            return rawPatternWithExceptions;
        }

    }

    private static final String FILE_BASEDIR = "basedir.txt";
    private static final String FILE_EXPORTED_PATH_PATTERNS = "exported-path-patterns.txt";
    private static final String BASELINE_EXPORTED_PATHS_TXT = "src/test/resources/classloading/exported-paths.txt";
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
        archive.addClasses(TestUtils.class);
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
        List<PatternWithExceptions> patterns = null;
        List<PatternWithExceptions> includePatterns = new ArrayList<>();
        List<PatternWithExceptions> excludePatterns = new ArrayList<>();
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
                    patterns.add(PatternWithExceptions.parse(line));
                }
                line = reader.readLine();
            }
        } finally {
            reader.close();
        }

        List<PatternWithExceptions> usedPatterns = new ArrayList<>(includePatterns);

        // A map from exported paths to violated patterns
        final Map<String, List<String>> violations = new LinkedHashMap<>();

        // Verify each line
        Path exportedPaths = resolvePath(FILE_EXPORTED_PATHS);
        reader = new BufferedReader(new FileReader(exportedPaths.toFile()));
        try {
            String line = reader.readLine();
            while (line != null) {
                if (line.length() > 0 && !line.startsWith("[")) {
                    boolean match = false;

                    // match include patterns
                    for (PatternWithExceptions pattern : includePatterns) {
                        if (pattern.matches(line)) {
                            usedPatterns.remove(pattern);
                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        violations.putIfAbsent(line, new ArrayList<>());
                        violations.get(line).add("no matching include pattern");
                    }

                    // match exclude patterns
                    for (PatternWithExceptions pattern : excludePatterns) {
                        if (pattern.matches(line)) {
                            violations.putIfAbsent(line, new ArrayList<>());
                            violations.get(line).add("excluded by pattern "+ pattern);
                        }
                    }

                    if (!violations.isEmpty()) {
                        StringBuilder msg = new StringBuilder(FILE_EXPORTED_PATHS.getFileName().toString()) //
                                .append(" do not comply with ").append(FILE_EXPORTED_PATH_PATTERNS);
                        for (Map.Entry<String, List<String>> en : violations.entrySet()) {
                            msg.append("\n    path: ").append(en.getKey()) //
                            .append(" violations: ").append(en.getValue().stream().collect(Collectors.joining(", ")));
                        }
                        Assert.fail(msg.toString());
                    }
                }
                line = reader.readLine();
            }
        } finally {
            reader.close();
        }

        Assert.assertTrue("Unused patterns: " + usedPatterns, usedPatterns.isEmpty());
    }

    private Path resolvePath(Path other) throws IOException {
        return Paths.get(TestUtils.getResourceValue(getClass(), "/" + FILE_BASEDIR)).resolve(other);
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
