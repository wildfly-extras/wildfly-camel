/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2015 RedHat
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
package org.wildfly.camel.test.cdi.subA;

import java.util.concurrent.CountDownLatch;

import javax.resource.spi.IllegalStateException;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.component.quartz2.QuartzComponent;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@ContextName("contextF")
public class RouteBuilderF extends RouteBuilder {

    public static final String MOCK_RESULT_URI = "mock:result?expectedCount=3";

    @Override
    public void configure() throws Exception {
        
        final CountDownLatch startLatch = new CountDownLatch(1);
        
        // verify that a component can be added manually
        getContext().addComponent("quartz2", new QuartzComponent() {
            @Override
            public void onCamelContextStarted(CamelContext context, boolean alreadyStarted) throws Exception {
                super.onCamelContextStarted(context, alreadyStarted);
                startLatch.countDown();
            }
        });
        
        from("quartz2://mytimer?trigger.repeatCount=3&trigger.repeatInterval=100")
        .process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                if (startLatch.getCount() > 0)
                    throw new IllegalStateException("onCamelContextStarted not called");
            }
        })
        .to(MOCK_RESULT_URI);
    }
}
