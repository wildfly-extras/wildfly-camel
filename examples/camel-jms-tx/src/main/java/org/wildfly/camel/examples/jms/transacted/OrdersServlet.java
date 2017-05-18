/*
 * #%L
 * Wildfly Camel :: Example :: Camel Transacted JMS
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
package org.wildfly.camel.examples.jms.transacted;

import org.wildfly.camel.examples.jms.transacted.data.OrderRepository;
import org.wildfly.camel.examples.jms.transacted.model.Order;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@SuppressWarnings("serial")
@WebServlet(name = "HttpServiceServlet", urlPatterns = {"/orders"}, loadOnStartup = 1)
public class OrdersServlet extends HttpServlet {

    @Inject
    private OrderRepository orderRepository;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
         // Gets all orders saved to the in-memory database 'orders' table
        List<Order> orders = orderRepository.findAllOrders();

        request.setAttribute("orders", orders);
        request.getRequestDispatcher("orders.jsp").forward(request, response);
    }
}
