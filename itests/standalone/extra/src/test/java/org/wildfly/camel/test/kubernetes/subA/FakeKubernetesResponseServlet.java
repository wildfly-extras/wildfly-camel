/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
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
package org.wildfly.camel.test.kubernetes.subA;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.test.common.utils.TestUtils;

/**
 * Simulates Kubernetes API REST endpoints and returns stubbed responses
 */

@WebServlet(name = "FakeKubernetesResponseServlet", urlPatterns = {"/fake-kubernetes/*"})
public class FakeKubernetesResponseServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(FakeKubernetesResponseServlet.class);
    private static final String KUBERENETES_API_BASE_PATH = "/api/v1/";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getPathInfo();
        LOG.info("GET: " + path);

        // Simulates the response to a pod list request
        if (path.matches("^" + KUBERENETES_API_BASE_PATH + ".*" + "pods")) {
            outputResponse(response, "/podList.json");
        } else {
            throw new IllegalStateException("Unknown Kubernetes API path: " + path);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getPathInfo();
        LOG.info("POST: " + path);

        // Simulates the response to a pod creation request
        if (path.matches("^" + KUBERENETES_API_BASE_PATH + ".*" + "pods")) {
            response.setStatus(201);
            outputResponse(response, "/podCreated.json");
        } else {
            throw new IllegalStateException("Unknown Kubernetes API path: " + path);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.info("DELETE: " + request.getPathInfo());
    }

    private void outputResponse(HttpServletResponse response, String jsonFile) throws IOException {
        String resource = TestUtils.getResourceValue(FakeKubernetesResponseServlet.class, jsonFile);
        response.getOutputStream().print(resource);
    }
}
