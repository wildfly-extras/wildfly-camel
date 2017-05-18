/*
 * #%L
 * Wildfly Camel :: Example :: Camel Transacted JMS Spring
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
package org.wildfly.camel.examples.jms.transacted;

import java.io.IOException;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.camel.CamelContext;
import org.apache.camel.component.jpa.JpaComponent;
import org.wildfly.camel.examples.jms.transacted.model.Order;

@SuppressWarnings("serial")
@WebServlet(name = "HttpServiceServlet", urlPatterns = {"/orders"}, loadOnStartup = 1)
public class OrdersServlet extends HttpServlet {

    @Resource(name = "java:jboss/camel/context/jms-camel-context")
    private CamelContext camelContext;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
         // Gets all orders saved to the in-memory database 'orders' table

        JpaComponent component = camelContext.getComponent("jpa", JpaComponent.class);
        EntityManagerFactory entityManagerFactory = component.getEntityManagerFactory();

        EntityManager entityManager = entityManagerFactory.createEntityManager();

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Order> query = criteriaBuilder.createQuery(Order.class);
        query.select(query.from(Order.class));

        request.setAttribute("orders", entityManager.createQuery(query).getResultList());
        request.getRequestDispatcher("orders.jsp").forward(request, response);
    }
}
