/*
 * #%L
 * Wildfly Camel :: Example :: Camel ActiveMQ
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
package org.wildfly.camel.examples.activemq;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
@WebServlet(name = "HttpServiceServlet", urlPatterns = {"/orders/*"}, loadOnStartup = 1)
public class SimpleServlet extends HttpServlet {

    private static final String[] COUNTRIES = {"UK", "US", "Others"};
    private static final Path ORDERS_PATH = new File(System.getProperty("jboss.server.data.dir")).toPath().resolve("orders");

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        // Copy WEB-INF/order.xml to the data dir
        ServletContext servletContext = config.getServletContext();
        try {
            InputStream input = servletContext.getResourceAsStream("/WEB-INF/order.xml");
            Path xmlPath = ORDERS_PATH.resolve("order.xml");
            Files.copy(input, xmlPath);
        } catch (IOException ex) {
            throw new ServletException(ex);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //Work out a count of order files processed for each country
        Map<String, Integer> orderCounts = new HashMap<String, Integer>();

        for (String country : COUNTRIES) {
            int orderCount = countOrdersForCountry(country);

            if (orderCount > 0) {
                orderCounts.put(country, orderCount);
            }
        }

        request.setAttribute("orders", orderCounts);
        request.getRequestDispatcher("/WEB-INF/orders.jsp").forward(request, response);
    }

    private int countOrdersForCountry(String country) throws IOException {
        Path countryPath = new File(System.getProperty("jboss.server.data.dir")).toPath().resolve("orders/processed/" + country);
        File file = countryPath.toFile();

        if (file.isDirectory()) {
            return file.list().length;
        }

        return 0;
    }
}
