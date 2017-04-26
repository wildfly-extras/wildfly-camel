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

package org.wildfly.camel.test.security;

import javax.security.auth.Subject;

import org.apache.camel.CamelAuthorizationException;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.security.subA.AnnotatedSLSB;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;
import org.wildfly.extension.camel.security.DomainPrincipal;
import org.wildfly.extension.camel.security.EncodedUsernamePasswordPrincipal;

@CamelAware
@RunWith(Arquillian.class)
public class AuthorizationPolicyTestCase {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "authorization-policy-route-test");
        archive.addAsResource("security/authorization-policy-camel-context.xml");
        return archive;
    }

    @Test
    public void testNoAuthenticationHeader() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("contextA");
        ProducerTemplate producer = camelctx.createProducerTemplate();
        try {
            producer.requestBody("direct:start", "Kermit", String.class);
            Assert.fail("CamelExecutionException expected");
        } catch (CamelExecutionException ex) {
            Throwable cause = ex.getCause();
            Assert.assertEquals(CamelAuthorizationException.class, cause.getClass());
            Assert.assertTrue(cause.getMessage(), cause.getMessage().startsWith("Cannot find the Authentication instance"));
        }
    }

    @Test
    public void testInvalidCredentials() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("contextA");
        ProducerTemplate producer = camelctx.createProducerTemplate();
        try {
            Subject subject = getAuthenticationToken("user-domain", AnnotatedSLSB.USERNAME, "bogus");
            producer.requestBodyAndHeader("direct:start", "Kermit", Exchange.AUTHENTICATION, subject, String.class);
            Assert.fail("CamelExecutionException expected");
        } catch (CamelExecutionException ex) {
            Throwable cause = ex.getCause();
            Assert.assertEquals(CamelAuthorizationException.class, cause.getClass());
            Assert.assertTrue(cause.getMessage(), cause.getMessage().contains("Password invalid/Password required"));
        }
    }

    @Test
    public void testAuthenticatedAccess() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("contextA");
        ProducerTemplate producer = camelctx.createProducerTemplate();
        Subject subject = getAuthenticationToken("user-domain", AnnotatedSLSB.USERNAME, AnnotatedSLSB.PASSWORD);
        String result = producer.requestBodyAndHeader("direct:start", "Kermit", Exchange.AUTHENTICATION, subject, String.class);
        Assert.assertEquals("Hello Kermit", result);
    }

    @Test
    public void testRoleBasedAccess() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("contextB");
        ProducerTemplate producer = camelctx.createProducerTemplate();
        Subject subject = getAuthenticationToken("user-domain", AnnotatedSLSB.USERNAME, AnnotatedSLSB.PASSWORD);
        String result = producer.requestBodyAndHeader("direct:start", "Kermit", Exchange.AUTHENTICATION, subject, String.class);
        Assert.assertEquals("Hello Kermit", result);
    }

    @Test
    public void testInsufficientRoles() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("contextC");
        ProducerTemplate producer = camelctx.createProducerTemplate();
        try {
            Subject subject = getAuthenticationToken("user-domain", AnnotatedSLSB.USERNAME, AnnotatedSLSB.PASSWORD);
            producer.requestBodyAndHeader("direct:start", "Kermit", Exchange.AUTHENTICATION, subject, String.class);
            Assert.fail("CamelExecutionException expected");
        } catch (CamelExecutionException ex) {
            Throwable cause = ex.getCause();
            Assert.assertEquals(CamelAuthorizationException.class, cause.getClass());
            Assert.assertTrue(cause.getMessage(), cause.getMessage().contains("User does not have required roles: [Role3]"));
        }
    }

    Subject getAuthenticationToken(String domain, String username, String password) {
        Subject subject = new Subject();
        subject.getPrincipals().add(new DomainPrincipal(domain));
        subject.getPrincipals().add(new EncodedUsernamePasswordPrincipal(username, password.toCharArray()));
        return subject;
    }
}
