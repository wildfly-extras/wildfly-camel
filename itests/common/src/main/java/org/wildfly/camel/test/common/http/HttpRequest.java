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

package org.wildfly.camel.test.common.http;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public final class HttpRequest {

    // Hide ctor
    private HttpRequest(){
    }

    public static HttpRequestBuilder get(String url) {
        return new HttpRequestBuilder(url, "GET");
    }

    public static HttpRequestBuilder post(String url) {
        return new HttpRequestBuilder(url, "POST");
    }

    public static HttpRequestBuilder put(String url) {
        return new HttpRequestBuilder(url, "PUT");
    }

    public static HttpRequestBuilder delete(String url) {
        return new HttpRequestBuilder(url, "DELETE");
    }

    public static HttpRequestBuilder options(String url) {
        return new HttpRequestBuilder(url, "OPTIONS");
    }

    public static final class HttpRequestBuilder {
        private String requestUrl;
        private String method;
        private String content;
        private long timeout = 10;
        private boolean throwExceptionOnFailure = true;
        private Map<String, String> headers = new HashMap<>();
        private TimeUnit timeUnit = TimeUnit.SECONDS;

        HttpRequestBuilder(String url, String method) {
            this.requestUrl = url;
            this.method = method;
        }

        public HttpRequestBuilder content(String value) {
            this.content = value;
            return this;
        }

        public HttpRequestBuilder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public HttpRequestBuilder throwExceptionOnFailure(boolean value) {
            this.throwExceptionOnFailure = value;
            return this;
        }

        public HttpRequestBuilder timeout(long value) {
            this.timeout = value;
            return this;
        }

        public HttpRequestBuilder timeout(long value, TimeUnit unit) {
            this.timeout = value;
            this.timeUnit = unit;
            return this;
        }

        public HttpResponse getResponse() throws TimeoutException, IOException {
            Callable<HttpResponse> task = new Callable<HttpResponse>() {
                @Override
                public HttpResponse call() throws Exception {
                    URL url = new URL(requestUrl);
                    final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.setRequestMethod(method);

                    Set<String> headerNames = headers.keySet();
                    for(String headerName : headerNames) {
                        conn.setRequestProperty(headerName, headers.get(headerName));
                    }

                    if(method.equals("POST") || method.equals("PUT")) {
                        conn.setDoOutput(true);
                    }

                    if(content != null && !content.isEmpty()) {
                        OutputStream outputStream = conn.getOutputStream();
                        outputStream.write(content.getBytes("UTF-8"));
                        outputStream.flush();
                        outputStream.close();
                    }

                    return processResponse(conn);
                }
            };
            return executeRequest(task, timeout, timeUnit);
        }

        private HttpResponse executeRequest(final Callable<HttpResponse> task, final long timeout, final TimeUnit unit) throws TimeoutException, IOException {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Throwable lastCause = null;
                long endTime = System.currentTimeMillis() + unit.toMillis(timeout);
                while (System.currentTimeMillis() < endTime) {
                    Future<HttpResponse> result = executor.submit(task);
                    try {
                        return result.get(timeout, unit);
                    } catch (InterruptedException ex) {
                        throw new IllegalStateException(ex);
                    } catch (ExecutionException ex) {
                        lastCause = ex.getCause();

                        // HttpURLConnection throws FileNotFoundException on 404 so handle this
                        if (lastCause instanceof FileNotFoundException) {
                            HttpResponse httpResult = new HttpResponse();
                            httpResult.setStatusCode(HttpURLConnection.HTTP_NOT_FOUND);
                            return httpResult;
                        } else if (lastCause instanceof IOException) {
                            if (lastCause.getMessage().contains("405")) {
                                HttpResponse httpResult = new HttpResponse();
                                httpResult.setStatusCode(HttpURLConnection.HTTP_BAD_METHOD);
                                return httpResult;
                            }
                        } else {
                            continue;
                        }
                    }
                }
                TimeoutException toex = new TimeoutException();
                if (lastCause != null) {
                    toex.initCause(lastCause);
                }
                throw toex;
            } finally {
                executor.shutdownNow();
            }
        }

        private String read(final InputStream in) throws IOException {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return out.toString();
        }

        private HttpResponse processResponse(HttpURLConnection conn) throws IOException {
            int responseCode = conn.getResponseCode();

            if (throwExceptionOnFailure && responseCode != HttpURLConnection.HTTP_OK) {
                final InputStream err = conn.getErrorStream();
                if (err != null) {
                    try {
                        throw new IOException(read(err));
                    } finally {
                        err.close();
                    }
                }
            }
            final InputStream in = conn.getInputStream();
            try {
                HttpResponse result = new HttpResponse();
                result.setStatusCode(responseCode);
                result.setBody(read(in));

                Map<String, List<String>> headerFields = conn.getHeaderFields();
                for (String headerName : headerFields.keySet()) {
                    result.addHeader(headerName, conn.getHeaderField(headerName));
                }

                return result;
            } finally {
                in.close();
            }
        }
    }

    public static class HttpResponse {
        private int statusCode;
        private String body;
        private Map<String, String> headers = new HashMap<>();

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public Map<String, String> getHeaders() {
            return Collections.unmodifiableMap(headers);
        }

        public String getHeader(String headerName) {
            return headers.get(headerName);
        }

        void addHeader(String header, String value) {
            headers.put(header, value);
        }

        public String toString() {
            return "HttpResponse{status=" + statusCode + "}";
        }
    }
}
