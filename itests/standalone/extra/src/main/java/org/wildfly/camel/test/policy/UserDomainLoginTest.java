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

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.security.LoginContextBuilder;
import org.wildfly.extension.camel.security.LoginContextBuilder.Type;

@CamelAware
@RunWith(Arquillian.class)
public class UserDomainLoginTest {

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "client-login-test");
        return archive;
    }

    @Test
    public void testOtherDomainAccess() throws Exception {
        LoginContextBuilder builder = new LoginContextBuilder(Type.AUTHENTICATION);
        LoginContext loginContext = builder.username("user1").password("appl-pa$$wrd1".toCharArray()).build();
        loginContext.login();
    }

    @Test
    public void testOtherDomainFail() throws Exception {
        LoginContextBuilder builder = new LoginContextBuilder(Type.AUTHENTICATION);
        LoginContext loginContext = builder.username("user2").password("appl-pa$$wrd2".toCharArray()).build();
        try {
            loginContext.login();
            Assert.fail("LoginException expected");
        } catch (LoginException e) {
            // expected
        }
    }

    @Test
    public void testUserDomainOtherAccess() throws Exception {
        LoginContextBuilder builder = new LoginContextBuilder(Type.AUTHENTICATION).domain("user-domain");
        LoginContext loginContext = builder.username("user2").encryptedPassword("appl-pa$$wrd2".toCharArray()).build();
        loginContext.login();
    }
}
