/*
 * #%L
 * Wildfly Camel :: Example :: Camel JPA
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
package org.wildfly.camel.examples.jpa;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wildfly.camel.examples.jpa.model.Customer;

@SuppressWarnings("serial")
@WebServlet(name = "HttpServiceServlet", urlPatterns = { "/customers/*" }, loadOnStartup = 1)
public class SimpleServlet extends HttpServlet {

    static Path CUSTOMERS_PATH = new File(System.getProperty("jboss.server.data.dir")).toPath().resolve("customers");
    
    @Inject
    private CustomerRepository customerRepository;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        // Copy WEB-INF/customer.xml to the data dir 
        ServletContext servletContext = config.getServletContext();
        try {
            InputStream input = servletContext.getResourceAsStream("/WEB-INF/customer.xml");
            Path xmlPath = CUSTOMERS_PATH.resolve("customer.xml");
            Files.copy(input, xmlPath);
        } catch (IOException ex) {
            throw new ServletException(ex);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        /*
         * Simple servlet to retrieve all customers from the in memory database for
         * output and display on customers.jsp
         */

        List<Customer> customers = customerRepository.findAllCustomers();

        request.setAttribute("customers", customers);
        request.getRequestDispatcher("/WEB-INF/customers.jsp").forward(request, response);
    }
}
