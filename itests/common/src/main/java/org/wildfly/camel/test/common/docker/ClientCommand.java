/*
 * #%L
 * Wildfly Camel :: Testsuite :: Common
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
package org.wildfly.camel.test.common.docker;


/**
 * A Docker command for the wildfly client
 * 
 * @author tdiesler@redhat.com
 * @since 09-Dec-2014
 */
public class ClientCommand extends RunCommand {
    
    public ClientCommand() {
        remove().image("wildflyext/wildfly-camel").cmd("/opt/jboss/wildfly/bin/jboss-cli.sh");
    }
    
    public ClientCommand connect(String username, String password, String host, int port) {
        args("-c", "-u=" + username, "-p=" + password, "--controller=" + host + ":" + port);
        return this;
    }
}