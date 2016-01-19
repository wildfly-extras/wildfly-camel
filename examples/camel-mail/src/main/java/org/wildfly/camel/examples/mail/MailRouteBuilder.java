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

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.annotation.Resource;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mail.MailConfiguration;
import org.apache.camel.component.mail.MailEndpoint;
import org.wildfly.extension.camel.CamelAware;

@Startup
@CamelAware
@ApplicationScoped
public class MailRouteBuilder extends RouteBuilder {

    // Injects a java mail session
    @Resource(mappedName = "java:jboss/mail/greenmail")
    private Session mailSession;

    @Override
    public void configure() throws Exception {
        // Configure routes and endpoints to send and receive email over SMTP and POP3
        MailEndpoint sendMailEndpoint = getContext().getEndpoint("smtp://localhost", MailEndpoint.class);
        configureMailEndpoint(sendMailEndpoint);

        MailEndpoint receiveMailEndpoint = getContext().getEndpoint("pop3://user2@localhost?consumer.delay=30000", MailEndpoint.class);
        configureMailEndpoint(receiveMailEndpoint);

        from("direct:sendmail")
            .to(sendMailEndpoint);

        from(receiveMailEndpoint)
            .to("log:emails?showAll=true&multiline=true");
    }

    private void configureMailEndpoint(MailEndpoint endpoint) throws UnknownHostException {
        MailConfiguration configuration = endpoint.getConfiguration();

        // Copy authentication details from the mail session to the camel mail endpoint config
        String protocol = configuration.getProtocol();
        String host = mailSession.getProperty("mail." + protocol + ".host");
        String user = mailSession.getProperty("mail." + protocol + ".user");

        int port = Integer.parseInt(mailSession.getProperty("mail." + protocol + ".port"));
        InetAddress address = InetAddress.getByName(host);

        PasswordAuthentication auth = mailSession.requestPasswordAuthentication(address, port, protocol, null, user);
        configuration.setPort(port);
        configuration.setUsername(auth.getUserName());
        configuration.setPassword(auth.getPassword());
    }
}
