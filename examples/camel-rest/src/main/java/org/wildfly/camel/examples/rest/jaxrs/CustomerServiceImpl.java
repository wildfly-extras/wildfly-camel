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
package org.wildfly.camel.examples.rest.jaxrs;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.ProxyBuilder;
import org.wildfly.camel.examples.rest.model.Customer;

public class CustomerServiceImpl implements CustomerService {

    @Inject
    private CamelContext context;

    private CustomerService customerServiceProxy;

    /**
     * Configures a proxy for the direct:rest endpoint
     */
    @PostConstruct
    public void initServiceProxy() throws Exception {
        customerServiceProxy = new ProxyBuilder(context).endpoint("direct:rest").binding(false).build(CustomerService.class);
    }

    /**
     * Invoke the proxied methods and pass on the arguments we received
     */
    @Override
    public Response getCustomers() {
        return customerServiceProxy.getCustomers();
    }

    @Override
    public Response updateCustomer(Customer customer) {
        return customerServiceProxy.updateCustomer(customer);
    }

    @Override
    public Response deleteCustomer(Long customerId) {
        return customerServiceProxy.deleteCustomer(customerId);
    }

    @Override
    public Response deleteCustomers() {
        return customerServiceProxy.deleteCustomers();
    }
}
