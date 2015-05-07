/*
 * #%L
 * Wildfly Camel :: Example :: Camel REST
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
package org.wildfly.camel.examples.rest.data;


import org.wildfly.camel.examples.rest.model.Customer;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.util.List;

@Stateless
public class CustomerRepository {

    @PersistenceContext(unitName = "camel")
    EntityManager em;

    /**
     * Find all customer records
     *
     * @return A list of customers
     */
    public List<Customer> findAllCustomers() {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Customer> query = criteriaBuilder.createQuery(Customer.class);
        query.select(query.from(Customer.class));

        return em.createQuery(query).getResultList();
    }

    /**
     * CRUD Operations to be invoked by REST endpoints
     */
    public void createCustomer(Customer customer) {
        em.persist(customer);
    }

    public Customer readCustomer(Long customerId) {
        return em.find(Customer.class, customerId);
    }

    public void updateCustomer(Customer customer) {
        em.merge(customer);
    }

    public void deleteCustomer(Long customerId) {
        Customer customer = em.getReference(Customer.class, customerId);
        em.remove(customer);
    }

    public void deleteCustomers() {
        em.createQuery("DELETE FROM Customer").executeUpdate();
    }
}
