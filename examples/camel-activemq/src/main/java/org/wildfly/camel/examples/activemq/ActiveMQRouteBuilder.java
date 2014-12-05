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

import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;

@Startup
@ApplicationScoped
@ContextName("amq-cdi-context")
public class ActiveMQRouteBuilder extends RouteBuilder {

    private static String BROKER_URL = "vm://localhost?broker.persistent=false&broker.useJmx=false" +
            "&broker.useShutdownHook=false";

    @Override
    public void configure() throws Exception {

        ActiveMQComponent activeMQComponent = new ActiveMQComponent();
        activeMQComponent.setBrokerURL(BROKER_URL);
        getContext().addComponent("activemq", activeMQComponent);

        from("file://{{jboss.server.data.dir}}/orders")
            .log("Receiving order for ${file:name}")
            .to("activemq:queue:testQueue");

        from("activemq:queue:testQueue")
            .choice()
                .when(xpath("/order/customer/country = 'UK'"))
                    .log("Sending order ${file:name} to the UK")
                    .to("file:{{jboss.server.data.dir}}/orders/processed/UK")
                .when(xpath("/order/customer/country = 'US'"))
                    .log("Sending order ${file:name} to the US")
                    .to("file:{{jboss.server.data.dir}}/orders/processed/US")
                .otherwise()
                    .log("Sending order ${file:name} to another country")
                    .to("file://{{jboss.server.data.dir}}/orders/processed/Others");
    }
}
