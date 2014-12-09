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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A Docker run command
 * 
 * @author tdiesler@redhat.com
 * @since 09-Dec-2014
 */
public class RunCommand {

    protected List<String> options = new ArrayList<>();
    protected List<String> args = new ArrayList<>();
    private String image;
    private String cmd;

    public RunCommand rm() {
        options.add("--rm");
        return this;
    }
    
    public RunCommand port(int host, int container) {
        options.add("-p");
        options.add(host + ":" + container);
        return this;
    }
    
    public RunCommand volume(Path host, Path container) {
        options.add("-v");
        options.add(host.toAbsolutePath() + ":" + container.toAbsolutePath());
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
    
    public RunCommand options(String... opts) {
        for (String opt : opts) {
            options.add(opt);
        }
        return this;
    }
    
    public RunCommand args(String... params) {
        for (String p : params) {
            args.add(p);
        }
        return this;
    }
    
    public RunCommand.Result exec() throws Exception {
        
        List<String> carr = new ArrayList<>();
        carr.add("docker");
        carr.add("run");
        carr.addAll(options);
        carr.add(image);
        if (cmd != null) {
            carr.add(cmd);
        }
        carr.addAll(args);
        
        StringBuffer cbuf = new StringBuffer();
        for (String item : carr) {
            cbuf.append(item + " ");
        }
        System.out.println("Exec: " + cbuf);
        
        Process process = Runtime.getRuntime().exec(carr.toArray(new String[carr.size()]));
        process.waitFor();
        return new Result(process);
    }
    
    public static class Result {

        private Process process;
        private BufferedReader output;
        
        Result(Process process) {
            this.process = process;
            this.output = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            // Print the error lines
            BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            try {
                String line = error.readLine();
                while (line != null) {
                    System.err.println(line);
                    line = error.readLine();
                }
            } catch (IOException ex) {
                // ignore
            } 
        }
        
        public String outputLine() {
            Iterator<String> it = outputLines();
            return it.hasNext() ? it.next() : null;
        }
        
        public Iterator<String> outputLines() {
            return new Iterator<String> () {
                
                String nextLine;
                
                @Override
                public boolean hasNext() {
                    return nextLine() != null;
                }

                @Override
                public String next() {
                    String result = nextLine();
                    if (result == null)
                        throw new NoSuchElementException();
                    
                    nextLine = null;
                    return result;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
                
                private String nextLine() {
                    if (nextLine == null)
                    try {
                        nextLine = output.readLine();
                    } catch (IOException e) {
                        return null;
                    }
                    return nextLine;
                }
            };
        }
        
        public int exitValue() {
            return process.exitValue();
        }
    }
}