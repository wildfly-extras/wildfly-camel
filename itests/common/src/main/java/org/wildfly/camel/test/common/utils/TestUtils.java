/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.common.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;

import org.jboss.gravia.utils.IOUtils;
import org.jboss.gravia.utils.IllegalStateAssertion;

public final class TestUtils {
    private TestUtils() {
    }

    public static String getDockerHost() throws Exception {
        String dockerHost = System.getenv("DOCKER_HOST");
        if (dockerHost == null) {
            return InetAddress.getLocalHost().getHostName();
        }

        URI uri = new URI(dockerHost);
        return uri.getHost();
    }

    public static String getResourceValue (Class<?> clazz, String resname) throws IOException {
        try (InputStream in = clazz.getResourceAsStream(resname)) {
            IllegalStateAssertion.assertNotNull(in, "Cannot find resource: " + resname);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copyStream(in, out);
            return new String(out.toByteArray());
        }
    }
}
