/*
 * #%L
 * Wildfly Camel :: Example :: Camel Rest Swagger
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
package org.wildfly.camel.examples.rest.data;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.wildfly.camel.examples.rest.model.Customer;

@ApplicationScoped
@Named
public class CustomerRepository {

    @Inject
    private EntityManager em;

    @Inject
    private UserTransaction userTransaction;

    public List<Customer> findAll() {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Customer> query = criteriaBuilder.createQuery(Customer.class);
        query.select(query.from(Customer.class));
        return em.createQuery(query).getResultList();
    }

    public Customer findById(Long id) {
        return em.find(Customer.class, id);
    }

    public Customer save(Customer customer) {
        try {
            try {
                userTransaction.begin();
                return em.merge(customer);
            } finally {
                userTransaction.commit();
            }
        } catch (Exception e) {
            try {
                userTransaction.rollback();
            } catch (SystemException se) {
                throw new RuntimeException(se);
            }
            throw new RuntimeException(e);
        }
    }

    public void delete(Customer customer) {
        try {
            try {
                userTransaction.begin();
                if (!em.contains(customer)) {
                    customer = em.merge(customer);
                }
                em.remove(customer);
            } finally {
                userTransaction.commit();
            }
        } catch (Exception e) {
            try {
                userTransaction.rollback();
            } catch (SystemException se) {
                throw new RuntimeException(se);
            }
            throw new RuntimeException(e);
        }
    }
}
