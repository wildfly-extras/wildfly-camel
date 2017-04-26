package org.wildfly.camel.test.hystrix.subA;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "delayed-http-response", loadOnStartup = 1, urlPatterns = {"/delay-me/*"})
public class DelayedHttpResponseServlet extends HttpServlet{

    private static final Logger LOG = LoggerFactory.getLogger(DelayedHttpResponseServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG.info("[wfc#1507] Delayed request start: {}", System.currentTimeMillis());
        try {
            Thread.sleep(2100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        LOG.info("[wfc#1507] Delayed request response send: {}", System.currentTimeMillis());
        resp.getOutputStream().print("Hello World");

        LOG.info("[wfc#1507] Delayed request complete: {}", System.currentTimeMillis());
    }
}
