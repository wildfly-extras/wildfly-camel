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
package org.wildfly.extension.camel.config;

/**
 * Main class that executes the wildfly-camel transformations on the EAP
 * configuration files
 */
public class ConfigMain {

    static String PROCESS_NAME = "wildfly-camel-config.jar";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println(PROCESS_NAME + " [disable|enable]");
            System.exit(1);
        } else {
            try {
                if (args[0].equals("enable")) {
                    ConfigSupport.applyConfigChange(ConfigSupport.getJBossHome(), true, new WildFlyCamelConfigEditor());
                } else if (args[0].equals("disable")) {
                    ConfigSupport.applyConfigChange(ConfigSupport.getJBossHome(), false, new WildFlyCamelConfigEditor());
                } else {
                    System.out.println("\t" + PROCESS_NAME + " [disable|enable]");
                    System.exit(1);
                }
            } catch (ConfigSupport.BadDocument e) {
                System.out.println(e.getMessage());
                System.exit(1);
            } catch (ConfigSupport.CommandException e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }
        }
    }

}
