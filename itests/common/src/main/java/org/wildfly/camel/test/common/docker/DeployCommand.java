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

import java.io.File;

/**
 * A Docker deploy command
 * 
 * @author tdiesler@redhat.com
 * @since 09-Dec-2014
 */
public class DeployCommand extends ClientCommand {
    
    public DeployCommand(String runtimeName, File file) {
        volume(file.getParentFile().toPath(), new File("/opt/volume").toPath());
        args("deploy /opt/volume/" + file.getName() + " --name=" + runtimeName + " --all-server-groups");
    }
}