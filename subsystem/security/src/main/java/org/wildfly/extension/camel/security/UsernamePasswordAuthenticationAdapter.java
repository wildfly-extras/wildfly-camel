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

package org.wildfly.extension.camel.security;

import java.util.Set;

import javax.security.auth.Subject;

import org.apache.camel.component.spring.security.DefaultAuthenticationAdapter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

public class UsernamePasswordAuthenticationAdapter extends DefaultAuthenticationAdapter {

    protected Authentication convertToAuthentication(Subject subject) {
        AbstractAuthenticationToken authToken = null;
        Set<UsernamePasswordPrincipal> principalSet  = subject.getPrincipals(UsernamePasswordPrincipal.class);
        if (principalSet.size() > 0) {
            UsernamePasswordPrincipal upp = principalSet.iterator().next();
            authToken = new UsernamePasswordAuthenticationToken(upp.getName(), upp.getPassword());
        }
        if (authToken != null) {
            Set<DomainPrincipal> auxset = subject.getPrincipals(DomainPrincipal.class);
            if (auxset.size() > 0) {
                String domain = auxset.iterator().next().getName();
                authToken.setDetails(domain);
            }
        }
        return authToken;
    }
}
