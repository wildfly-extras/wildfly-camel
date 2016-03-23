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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A Docker command
 *
 * @author tdiesler@redhat.com
 * @since 09-Dec-2014
 */
public class DockerCommand<T extends DockerCommand<?>> {

    private List<String> options = new ArrayList<>();
    private List<String> args = new ArrayList<>();
    private String cmd;

    public DockerCommand(String cmd) {
        this.cmd = cmd;
    }

    @SuppressWarnings("unchecked")
    public T options(String... opts) {
        for (String p : opts) {
            options.add(p);
        }
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T args(String... params) {
        for (String p : params) {
            args.add(p);
        }
        return (T) this;
    }

    protected void buildCommand(List<String> carr) {
        carr.add("docker");
        carr.add(cmd);
        carr.addAll(options);
        appendsArgs(carr);
    }

    protected void appendsArgs(List<String> carr) {
        carr.addAll(args);
    }

    public final DockerCommand.Result exec() {

        List<String> carr = new ArrayList<>();
        buildCommand(carr);

        StringBuffer cbuf = new StringBuffer();
        for (String item : carr) {
            cbuf.append(item + " ");
        }
        System.out.println("DOCKER> " + cbuf);

        Process process;
        try {
            process = Runtime.getRuntime().exec(carr.toArray(new String[carr.size()]));
            process.waitFor();
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        Result result = new Result(process);
        return result;
    }

    public static class Result {

        private Process process;
        private BufferedReader output;

        Result(Process process) {
            this.process = process;
        }

        public String outputLine() {
            Iterator<String> it = outputLines();
            return it.hasNext() ? it.next() : null;
        }

        private BufferedReader getOutputReader() {
            if (output == null) {
                output = new BufferedReader(new InputStreamReader(process.getInputStream()));
            }
            return output;
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
                        nextLine = getOutputReader().readLine();
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

        public Result printOut(PrintStream out) {
            Iterator<String> itout = outputLines();
            while (itout.hasNext()) {
                out.println(itout.next());
            }
            return this;
        }

        public Result printErr(PrintStream out) {
            BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            try {
                String line = error.readLine();
                while (line != null) {
                    out.println(line);
                    line = error.readLine();
                }
            } catch (IOException ex) {
                // ignore
            }
            return this;
        }
    }
}