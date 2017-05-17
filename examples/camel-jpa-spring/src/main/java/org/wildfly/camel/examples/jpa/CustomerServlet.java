/*
 * #%L
 * Wildfly Camel :: Example :: Camel JPA Spring
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
package org.wildfly.camel.examples.jpa;

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
import org.wildfly.camel.examples.jpa.model.Customer;

@SuppressWarnings("serial")
@WebServlet(name = "HttpServiceServlet", urlPatterns = { "/customers/*" }, loadOnStartup = 1)
public class CustomerServlet extends HttpServlet {

    @Resource(name = "java:jboss/camel/context/jpa-camel-context")
    private CamelContext camelContext;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        /*
         * Simple servlet to retrieve all customers from the in memory database for
         * output and display on customers.jsp
         */
        JpaComponent component = camelContext.getComponent("jpa", JpaComponent.class);
        EntityManagerFactory entityManagerFactory = component.getEntityManagerFactory();

        EntityManager em = entityManagerFactory.createEntityManager();

        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Customer> query = criteriaBuilder.createQuery(Customer.class);
        query.select(query.from(Customer.class));

        request.setAttribute("customers", em.createQuery(query).getResultList());
        request.getRequestDispatcher("customers.jsp").forward(request, response);
    }
}
