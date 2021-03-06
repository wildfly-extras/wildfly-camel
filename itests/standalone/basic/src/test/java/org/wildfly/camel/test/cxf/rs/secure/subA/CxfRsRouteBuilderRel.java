/*
 * #%L
 * Wildfly Camel :: Example :: Camel CXF JAX-WS CDI Secure
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
package org.wildfly.camel.test.cxf.rs.secure.subA;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.jaxrs.CxfRsEndpoint;

@ApplicationScoped
public class CxfRsRouteBuilderRel extends RouteBuilder {

    @Inject
    @Named("greetingsProcessor")
    Processor greetingsProcessor;

    @Inject
    @Named("cxfConsumerEndpointRel")
    CxfRsEndpoint cxfConsumerEndpointRel;

    @Inject
    @Named("cxfProducerEndpointRel")
    CxfRsEndpoint cxfProducerEndpointRel;

    @Override
    public void configure() throws Exception {

        from("direct:start3")
        .to(this.cxfProducerEndpointRel);

        from(this.cxfConsumerEndpointRel)
        .process(this.greetingsProcessor);

    }
}
