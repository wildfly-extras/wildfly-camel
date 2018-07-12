/*
* #%L
* Wildfly Camel :: Testsuite
* %%
* Copyright (C) 2013 - 2018 RedHat
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
package org.wildfly.camel.test.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A utility to run WildFly CLI scripts using {@code jboss-cli.[sh|bat]}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class WildFlyCli {

    private static final String DEFAULT_TIMEOUT = "60000";
    private final Path wildFlyHome;

    public WildFlyCli(Path wildFlyHome) {
        super();
        this.wildFlyHome = wildFlyHome;
    }

    /**
     * Run the given {@code cliScript} using {@code jboss-cli.[sh|bat]} and the appropriate current platform's script
     * interpreter.
     *
     * @param cliScript the {@link Path} to the script to run
     * @param cliArgs additional arguments to pass to {@code jboss-cli.[sh|bat]} executable on startup
     * @return a {@link WildFlyCliResult}
     * @throws IOException
     * @throws InterruptedException
     */
    public WildFlyCliResult run(Path cliScript, String... cliArgs) throws IOException, InterruptedException {
        final ProcessBuilder pb = new ProcessBuilder();
        final String ext = EnvironmentUtils.isWindows() ? "bat" : "sh";
        final String jbossCliPath = wildFlyHome.resolve("bin/jboss-cli." + ext).normalize().toString();
        final List<String> command = new ArrayList<>();
        command.add(jbossCliPath);
        command.add("--connect");
        command.add("--echo-command");
        command.add("--file=" + cliScript.normalize().toString());
        command.addAll(Arrays.asList(cliArgs));

        // Add default timeout arg if not provided
        if (!command.stream().anyMatch(arg -> arg.startsWith("--timeout"))) {
            command.add("--timeout=" + DEFAULT_TIMEOUT);
        }

        pb.command(command);
        pb.environment().put("NOPAUSE", "Y");
        Process process = pb.start();
        StreamGobbler stdOut = new StreamGobbler(process.getInputStream());
        stdOut.start();
        StreamGobbler stdErr = new StreamGobbler(process.getErrorStream());
        stdErr.start();
        int exitCode = process.waitFor();
        stdOut.join();
        stdErr.join();

        return new WildFlyCliResult(command, exitCode, stdOut.getString(), stdErr.getString());
    }

    /**
     * Writes the given string {@code cliScript} to a temporary file and calls {@link #run(Path)}.
     *
     * @param cliScript the CLI script as a string
     * @param cliArgs additional arguments to pass to {@code jboss-cli.[sh|bat]} executable on startup
     * @return a {@link WildFlyCliResult}
     * @throws IOException
     * @throws InterruptedException
     */
    public WildFlyCliResult run(String cliScript, String... cliArgs) throws IOException, InterruptedException {
        Path path = Files.createTempFile(WildFlyCli.class.getSimpleName(), ".cli");
        Files.write(path, cliScript.getBytes(StandardCharsets.UTF_8));
        return run(path, cliArgs);
    }

    /**
     * Copies the content of the given {@link URL} to a temporary file and calls {@link #run(Path)}.
     *
     * @param cliScript the URL of the script to run
     * @param cliArgs additional arguments to pass to {@code jboss-cli.[sh|bat]} executable on startup
     * @return a {@link WildFlyCliResult}
     * @throws IOException
     * @throws InterruptedException
     */
    public WildFlyCliResult run(URL cliScript, String... cliArgs) throws IOException, InterruptedException {
        Path path = Files.createTempFile(WildFlyCli.class.getSimpleName(), ".cli");
        FileUtils.copy(cliScript, path);
        return run(path, cliArgs);
    }

    /**
     * The usual friend of {@link Process#getInputStream()} / {@link Process#getErrorStream()}.
     */
    private static class StreamGobbler extends Thread {
        private final StringBuilder buffer = new StringBuilder();
        private IOException exception;
        private final InputStream in;

        private StreamGobbler(InputStream in) {
            this.in = in;
        }

        public String getString() throws IOException {
            if (exception != null) {
                throw exception;
            } else {
                return buffer.toString();
            }
        }

        @Override
        public void run() {
            try (InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                int ch;
                while ((ch = r.read()) >= 0) {
                    buffer.append((char) ch);
                }
            } catch (IOException e) {
                exception = e;
            }
        }
    }

    /**
     * Encapsulates a result of running a WildFly CLI script.
     */
    public static class WildFlyCliResult {
        private final List<String> command;
        private final int exitValue;
        private final String stdErr;
        private final String stdOut;

        WildFlyCliResult(List<String> command, int exitValue, String stdOut, String stdErr) {
            super();
            this.command = command;
            this.exitValue = exitValue;
            this.stdOut = stdOut;
            this.stdErr = stdErr;
        }

        /**
         * @return this {@link WildFlyCliResult}
         * @throws RuntimeException in case {@link #exitValue} != 0 or {@link #stdErr} is not empty
         */
        public WildFlyCliResult assertSuccess() {
            if (exitValue != 0) {
                throw new RuntimeException(String.format("Command %s returned %d.\n\nstdout: %s\n\nstdErr: %s",
                        String.join(",", command), exitValue, stdOut, stdErr));
            }
            if (!stdErr.isEmpty()) {
                throw new RuntimeException(
                        String.format("Command %s exited with non empty stdErr: %s", String.join(",", command), stdErr));
            }
            return this;
        }

        /**
         * @return the numeric exit value returned by the interpreter running {@code jboss-cli}
         */
        public int getExitValue() {
            return exitValue;
        }

        /**
         * @return the text the {@code jboss-cli} process has written to {@code stderr} or an empty string if no text
         *         was writtent to {@code stderr}
         */
        public String getStdErr() {
            return stdErr;
        }

        /**
         * @return the text the {@code jboss-cli} process has written to {@code stdout} or an empty string if no text
         *         was writtent to {@code stdout}
         */
        public String getStdOut() {
            return stdOut;
        }
    }

}
