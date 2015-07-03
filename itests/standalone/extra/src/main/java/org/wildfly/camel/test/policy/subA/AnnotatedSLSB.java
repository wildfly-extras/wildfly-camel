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

package org.wildfly.camel.test.policy.subA;

import java.security.Principal;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJBContext;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.security.auth.Subject;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.jboss.ejb3.annotation.SecurityDomain;
import org.wildfly.extension.camel.security.UsernamePasswordAuthentication;

@Stateless
@DeclareRoles({ "Role1", "Role2", "Role3" })
@LocalBean
@SecurityDomain("other")
public class AnnotatedSLSB {

    public static final String USERNAME = "user1";
    public static final String PASSWORD = "appl-pa$$wrd1";

    @Resource(name = "java:jboss/camel/context/secured-context")
    CamelContext camelctx;

    @Resource EJBContext ejbctx;

    @PermitAll
    public String doAnything(String msg) {
        return "Hello " + msg;
    }

    @RolesAllowed({ "Role1" })
    public String doSelected(String msg) {
        return "Hello " + msg;
    }

    @RolesAllowed({ "Role1" })
    public String secureRouteAccess(String msg) {

        // [TODO #725] Add support for security context propagation
        Subject subject = new Subject();
        String username = ejbctx.getCallerPrincipal().getName();
        Principal principal = new UsernamePasswordAuthentication(username, PASSWORD.toCharArray());
        subject.getPrincipals().add(principal);

        ProducerTemplate producer = camelctx.createProducerTemplate();
        return producer.requestBodyAndHeader("direct:start", msg, Exchange.AUTHENTICATION, subject, String.class);
    }

    @DenyAll
    public void restrictedMethod() {
        throw new RuntimeException("This method was supposed to be restricted to all!");
    }
}
