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

import java.nio.file.Path;
import java.util.List;

/**
 * A Docker run command
 * 
 * @author tdiesler@redhat.com
 * @since 09-Dec-2014
 */
public class RunCommand extends DockerCommand {

    private String image;
    private String cmd;

    public RunCommand() {
        super("run");
    }
    
    public RunCommand remove() {
        options("--rm");
        return this;
    }
    
    public RunCommand entrypoint(String entrypoint) {
        options("--entrypoint=" + entrypoint);
        return this;
    }
    
    public RunCommand port(int host, int container) {
        options("-p", host + ":" + container);
        return this;
    }
    
    public RunCommand volume(Path host, Path container) {
        options("-v", host.toAbsolutePath() + ":" + container.toAbsolutePath());
        return this;
    }
    
    public RunCommand image(String image) {
        this.image = image;
        return this;
    }
    
    public RunCommand cmd(String cmd) {
        this.cmd = cmd;
        return this;
    }
    
    @Override
    protected void appendsArgs(List<String> carr) {
        // do nothing
    }

    @Override
    protected void buildCommand(List<String> carr) {
        super.buildCommand(carr);
        carr.add(image);
        if (cmd != null) {
            carr.add(cmd);
        }
        super.appendsArgs(carr);
    }
}