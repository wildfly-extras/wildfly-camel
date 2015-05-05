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

import static org.wildfly.extension.camel.config.ConfigSupport.BadDocument;
import static org.wildfly.extension.camel.config.ConfigSupport.CommandException;
import static org.wildfly.extension.camel.config.ConfigSupport.applyConfigChange;
import static org.wildfly.extension.camel.config.ConfigSupport.getJBossHome;

/**
 * Main class that executes the wildfly-camel transformations on the EAP
 * configuration files
 */
public class ConfigMain {
    static String PROCESS_NAME = "wildfly-camel-config.jar";

    public static void main(String[] args) throws Exception {
        if(args.length!=1){
            System.out.println(PROCESS_NAME + " [disable|enable]");
            System.exit(1);
        } else {
            try {
                if (args[0].equals("enable")) {
                    applyConfigChange(getJBossHome(), true, new WildflyCamelConfigEditor());
                } else if (args[0].equals("disable")) {
                    applyConfigChange(getJBossHome(), false, new WildflyCamelConfigEditor());
                } else {
                    System.out.println("\t"+ PROCESS_NAME +" [disable|enable]");
                    System.exit(1);
                }
            } catch (BadDocument e) {
                System.out.println(e.getMessage());
                System.exit(1);
            } catch (CommandException e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }
        }
    }

}
