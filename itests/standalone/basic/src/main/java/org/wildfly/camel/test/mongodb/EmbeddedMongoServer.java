/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2016 RedHat
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

package org.wildfly.camel.test.mongodb;

import java.io.IOException;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;

import static de.flapdoodle.embed.mongo.distribution.Version.Main.PRODUCTION;
import static de.flapdoodle.embed.process.runtime.Network.localhostIsIPv6;

public class EmbeddedMongoServer {

    private final MongodExecutable mongodExecutable;

    public EmbeddedMongoServer(int port) throws Exception {
        IMongodConfig mongodConfig = new MongodConfigBuilder()
            .version(PRODUCTION)
            .net(new Net(port, localhostIsIPv6()))
            .build();
        mongodExecutable = MongodStarter.getDefaultInstance().prepare(mongodConfig);
    }

    public void start() throws IOException {
        mongodExecutable.start();
    }

    public void stop() {
        mongodExecutable.stop();
    }
}
