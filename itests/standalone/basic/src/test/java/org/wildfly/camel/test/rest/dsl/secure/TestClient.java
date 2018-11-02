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
package org.wildfly.camel.test.rest.dsl.secure;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.function.Consumer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class TestClient implements Closeable {

    static final String HOST = "localhost";
    static final int PORT = 8080;

    private final CloseableHttpClient c;

    public TestClient() {
        this.c = HttpClients.createDefault();
    }

    public void anonymousUnauthorized(String path, String method)
            throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException,
            CertificateException, IOException {
        assertResponse(path, method, null, null, 401);
    }

    public void assertResponse(String path, String method, String user, String password, int responseCode)
            throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException,
            CertificateException, IOException {
        assertResponse(path, method, user, password, response -> {
            final int actualCode = response.getStatusLine().getStatusCode();
            Assert.assertEquals(method + ": " + path + " with user " + user, responseCode, actualCode);
            if (actualCode == 200) {
                final HttpEntity entity = response.getEntity();
                try {
                    final String body = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                    Assert.assertEquals(method + ": " + path, body);
                } catch (ParseException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void assertResponse(String path, String method, String user, String password, Consumer<HttpResponse> responseConsumer) throws KeyManagementException, UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        final HttpUriRequest request;
        final String uri = String.format("http://%s:%s%s", HOST, PORT, path);
        switch (method) {
        case "GET":
            request = new HttpGet(uri);
            break;
        case "POST":
            request = new HttpPost(uri);
            break;
        case "PUT":
            request = new HttpPut(uri);
            break;
        case "DELETE":
            request = new HttpDelete(uri);
            break;
        default:
            throw new IllegalStateException("Unexpected HTTP method " + method);
        }
        request.setHeader("Content-Type", "text/plain");

        if (user != null) {
            final String auth = user + ":" + password;
            final String authHeader = "Basic "
                    + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1));
            request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
        }

        try (CloseableHttpResponse response = c.execute(request)) {
            responseConsumer.accept(response);
        }
    }

    @Override
    public void close() throws IOException {
        c.close();
    }

}
