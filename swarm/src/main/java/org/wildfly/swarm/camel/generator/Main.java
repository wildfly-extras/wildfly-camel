/*
 * #%L
 * Fuse Patch :: Core
 * %%
 * Copyright (C) 2015 Private
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
package org.wildfly.swarm.camel.generator;

import static org.wildfly.swarm.camel.generator.FractionsGenerator.LOG;

import java.io.IOException;
import java.nio.file.Path;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class Main {

    public static void main(String[] args) {
        try {
            new Main().process(args);
        } catch (Throwable th) {
            Runtime.getRuntime().exit(1);
        }
    }

    // Entry point with no system exit
    public void process(String[] args) throws Throwable {

        Options options = new Options();
        CmdLineParser parser = new CmdLineParser(options);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException ex) {
            helpScreen(parser);
            throw ex;
        }

        try {
            generate(options.outpath);
        } catch (Throwable th) {
            LOG.error("Error executing command", th);
            throw th;
        }
    }

    private void generate(Path outpath) throws IOException {
        FractionsGenerator tool = new FractionsGenerator(outpath);
        tool.generate();
    }

    private static void helpScreen(CmdLineParser cmdParser) {
        cmdParser.printUsage(System.err);
    }
}
