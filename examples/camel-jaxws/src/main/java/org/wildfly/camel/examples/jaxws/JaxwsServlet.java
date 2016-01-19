/*
 * #%L
 * Wildfly Camel :: Example :: Camel JAX-WS
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
package org.wildfly.camel.examples.jaxws;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.IOException;
import java.net.URL;

@SuppressWarnings("serial")
@WebServlet(name = "HttpServiceServlet", urlPatterns = { "/jaxws/*" }, loadOnStartup = 1)
public class JaxwsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        /**
         * Get message and name parameters sent on the POST request
         */
        String message = request.getParameter("message");
        String name = request.getParameter("name");

        /**
         * Create a JAX-WS client to invoke the greeting web service
         */
        URL wsdlLocation = new URL("http://localhost:8080/example-camel-jaxws/greeting?wsdl");
        QName serviceName = new QName("http://jaxws.examples.camel.wildfly.org/", "greeting");
        Service service = Service.create(wsdlLocation, serviceName);
        GreetingService greetingService = service.getPort(GreetingService.class);

        /**
         * Invoke the web service methods
         */
        String greeting;
        if(message != null && !message.isEmpty() && name != null && !name.isEmpty()) {
            greeting = greetingService.greetWithMessage(message, name);
        } else if((message == null || message.isEmpty()) && name != null && !name.isEmpty()) {
            greeting = greetingService.greet(name);
        } else {
            greeting = "Hello unknown";
        }

        request.setAttribute("greeting", greeting);
        request.getRequestDispatcher("/greeting.jsp").forward(request, response);
    }
}
