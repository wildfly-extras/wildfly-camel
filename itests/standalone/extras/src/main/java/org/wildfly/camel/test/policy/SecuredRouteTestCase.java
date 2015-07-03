/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.camel.test.policy;

import java.security.Principal;

import javax.security.auth.Subject;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.wildfly.extension.camel.security.ClientLoginAuthorizationPolicy;

@RunWith(Arquillian.class)
public class SecuredRouteTestCase {

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "secured-route-test");
        return archive;
    }

    @Test
    public void testRoleBasedAccessDenied() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .policy(new ClientLoginAuthorizationPolicy())
                .transform(body().prepend("Hello "));
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            try {
                producer.requestBody("direct:start", "Kermit", String.class);
                Assert.fail("CamelExecutionException expected");
            } catch (CamelExecutionException e) {
                // expected
            }
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testRoleBasedAccessAllowed() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .policy(new ClientLoginAuthorizationPolicy())
                .transform(body().prepend("Hello "));
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            Subject subject = getAuthenticationToken(EJBSecurityTestCase.USERNAME, EJBSecurityTestCase.PASSWORD);
            String result = producer.requestBodyAndHeader("direct:start", "Kermit", Exchange.AUTHENTICATION, subject, String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            camelctx.stop();
        }
    }

    private Subject getAuthenticationToken(String username, String password) {
        Subject subject = new Subject();
        Principal principal = new UsernamePasswordAuthenticationToken(username, password);
        subject.getPrincipals().add(principal);
        return subject;
    }
}
