/*
 * #%L
 * Wildfly Camel :: Example :: Camel Mail
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
package org.wildfly.camel.examples.mail;

import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.builder.RouteBuilder;
import org.wildfly.extension.camel.CamelAware;

@Startup
@CamelAware
@ApplicationScoped
public class MailRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // Configure routes and endpoints to send and receive email over SMTP and POP3
        from("direct:sendmail").to("smtp://localhost:10025?session=#mailSession");

        from("pop3://user2@localhost:10110?consumer.delay=30000&session=#mailSession").to("log:emails?showAll=true&multiline=true");
    }
}
