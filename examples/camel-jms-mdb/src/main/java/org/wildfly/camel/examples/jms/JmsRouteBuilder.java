/*
 * #%L
 * Wildfly Camel :: Example :: Camel JMS MDB
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
package org.wildfly.camel.examples.jms;

import java.util.Date;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.jms.ConnectionFactory;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.component.jms.JmsComponent;

@ApplicationScoped
@ContextName("camel-jms-mdb-context")
public class JmsRouteBuilder extends RouteBuilder {

    @Resource(mappedName = "java:jboss/DefaultJMSConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Override
    public void configure() throws Exception {
        /**
         * Configure the JMSComponent to use the connection factory
         * injected into this class
         */
        JmsComponent component = new JmsComponent();
        component.setConnectionFactory(connectionFactory);

        getContext().addComponent("jms", component);

        /**
         * This route uses the timer component to generate a message which is sent to
         * the JMS OrdersQueue
         */
        from("timer:produceJMSMessage?period=5000")
        .process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                Integer count = exchange.getProperty(Exchange.TIMER_COUNTER, Integer.class);
                Date date = exchange.getProperty(Exchange.TIMER_FIRED_TIME, Date.class);

                exchange.getOut().setBody(String.format("Message %d created at %s", count, date.toString()));
            }
        })
        .to("jms:queue:OrdersQueue");

        /**
         * This route is invoked by the {@link MessageDrivenBean} message consumer and outputs
         * the message payload to the console log
         */
        from("direct:jmsIn")
        .log("Received message: ${body}");
    }
}
