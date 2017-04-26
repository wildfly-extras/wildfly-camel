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

import javax.ejb.EJBAccessException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.login.LoginContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.security.subA.AnnotatedSLSB;
import org.wildfly.camel.test.security.subA.SecureRouteBuilder;
import org.wildfly.extension.camel.security.LoginContextBuilder;
import org.wildfly.extension.camel.security.LoginContextBuilder.Type;


@RunWith(Arquillian.class)
public class EJBSecurityTestCase {

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "ejb-security-test.jar");
        archive.addClasses(AnnotatedSLSB.class, SecureRouteBuilder.class);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    @Test
    public void testAccessAllowed() throws Exception {

        AnnotatedSLSB bean = lookup(new InitialContext(), AnnotatedSLSB.class, AnnotatedSLSB.class);
        Assert.assertEquals("Hello Kermit", bean.doAnything("Kermit"));
    }

    @Test
    public void testAuthorizedAccess() throws Exception {

        AnnotatedSLSB bean = lookup(new InitialContext(), AnnotatedSLSB.class, AnnotatedSLSB.class);
        LoginContextBuilder builder = new LoginContextBuilder(Type.CLIENT).domain("user-domain");
        LoginContext loginContext = builder.username(AnnotatedSLSB.USERNAME).encryptedPassword(AnnotatedSLSB.PASSWORD.toCharArray()).build();
        loginContext.login();
        try {
            Assert.assertEquals("Hello Kermit", bean.doSelected("Kermit"));
        } finally {
            loginContext.logout();
        }
    }

    @Test
    public void testCallerPricipalPropagation() throws Exception {

        AnnotatedSLSB bean = lookup(new InitialContext(), AnnotatedSLSB.class, AnnotatedSLSB.class);
        LoginContextBuilder builder = new LoginContextBuilder(Type.CLIENT).domain("user-domain");
        LoginContext loginContext = builder.username(AnnotatedSLSB.USERNAME).encryptedPassword(AnnotatedSLSB.PASSWORD.toCharArray()).build();
        loginContext.login();
        try {
            Assert.assertEquals("Hello Kermit", bean.secureRouteAccess("Kermit"));
        } finally {
            loginContext.logout();
        }
    }

    @Test
    public void testUnauthorizedAccess() throws Exception {

        AnnotatedSLSB bean = lookup(new InitialContext(), AnnotatedSLSB.class, AnnotatedSLSB.class);
        try {
            bean.doSelected("Kermit");
            Assert.fail("Call to doSelected() method was expected to fail");
        } catch (EJBAccessException ejbae) {
            //expected
        }

        LoginContextBuilder builder = new LoginContextBuilder(Type.CLIENT);
        LoginContext loginContext = builder.username("user1").password("wrongpass".toCharArray()).build();
        loginContext.login();
        try {
            bean.doSelected("Kermit");
            Assert.fail("Call to doSelected() method was expected to fail");
        } catch (EJBAccessException ejbae) {
            //expected
        } finally {
            loginContext.logout();
        }
    }

    @Test
    public void testAccessDenied() throws Exception {

        AnnotatedSLSB bean = lookup(new InitialContext(), AnnotatedSLSB.class, AnnotatedSLSB.class);
        try {
            bean.restrictedMethod();
            Assert.fail("Call to restrictedMethod() method was expected to fail");
        } catch (EJBAccessException ejbae) {
            //expected
        }
    }

    private <T> T lookup(Context ctx, final Class<?> beanClass, final Class<T> viewClass) throws NamingException {
        return viewClass.cast(ctx.lookup("java:module/" + beanClass.getSimpleName() + "!" + viewClass.getName()));
    }
}
