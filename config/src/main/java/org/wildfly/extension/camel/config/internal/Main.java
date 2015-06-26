/*
 * Copyright 2015 JBoss Inc
 *
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
 */
package org.wildfly.extension.camel.config.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.wildfly.extension.camel.config.ConfigException;
import org.wildfly.extension.camel.config.ConfigLogger;
import org.wildfly.extension.camel.config.ConfigSupport;

/**
 * Main class that invokes the configured config plugins
 */
public class Main {

    static String PROCESS_NAME = "wildfly-camel-config.jar";

    public static void main(String[] args) {
        try {
            mainInternal(args);
        } catch (Throwable th) {
            Runtime.getRuntime().exit(1);
        }
    }

    // Entry point with no system exit
    public static void mainInternal(String[] args) throws Exception {
        
        Options options = new Options();
        CmdLineParser parser = new CmdLineParser(options);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            helpScreen(parser);
            return;
        }

        try {
            List<String> configs = new ArrayList<>();
            if (options.configs != null) {
                configs.addAll(Arrays.asList(options.configs.split(",")));
            }
            ConfigSupport.applyConfigChange(ConfigSupport.getJBossHome(), configs, options.enable);
        } catch (ConfigException ex) {
            ConfigLogger.error(ex);
            throw ex;
        } catch (Throwable th) {
            ConfigLogger.error(th);
            throw th;
        }
    }

    private static void helpScreen(CmdLineParser cmdParser) {
        ConfigLogger.error("fuseconfig [options...]");
        cmdParser.printUsage(System.err);
    }
}
