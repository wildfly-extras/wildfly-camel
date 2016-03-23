/*
 * #%L
 * Wildfly Camel :: Testsuite
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

package org.wildfly.camel.test.docker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunAsClient
@RunWith(Arquillian.class)
public class HawtIOAccessTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

    @Test
    public void testAccessHawtIO() throws Exception {

        URL url = new URL("http://localhost:8080/hawtio/jolokia/read/java.lang:type=Memory/HeapMemoryUsage/used");
        URLConnection conn = url.openConnection();
        try {
            // Accessing hawtio requires an admin id/password.
            String userpass = USERNAME + ":" + PASSWORD;
            String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());
            conn.setRequestProperty("Authorization", basicAuth);

            int code = ((HttpURLConnection) conn).getResponseCode();
            assertEquals(200, code);
            String content = new String(readFully((InputStream) conn.getContent()), "UTF-8");

            // Let check to see if it has some of the expected output.
            assertTrue("Unexpected content: "+content, content.contains("\"mbean\":\"java.lang:type=Memory\""));

        } finally {
            try {
                conn.getInputStream().close();
            } catch (Throwable ignore) {
            }
        }

    }

    @Test
    public void testUnauthedAccessHawtIO() throws Exception {

        URL url = new URL("http://localhost:8080/hawtio/jolokia/read/java.lang:type=Memory/HeapMemoryUsage/used");
        URLConnection conn = url.openConnection();
        try {
            // Accessing hawtio requires an admin id/password since none is
            // given in this case, we should get a 403 error code.
            int code = ((HttpURLConnection) conn).getResponseCode();
            assertEquals(403, code);
        } finally {
            try {
                conn.getInputStream().close();
            } catch (Throwable ignore) {
            }
        }

    }


    private byte[] readFully(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos.toByteArray();
    }
}
