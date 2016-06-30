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

import java.security.Principal;
import java.security.acl.Group;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.wildfly.extension.camel.security.LoginContextBuilder.Type;

public class DomainAuthenticationManager implements AuthenticationManager {

    @Override
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        
        if (!(auth instanceof UsernamePasswordAuthenticationToken))
            throw new BadCredentialsException("Unsupported authentication type: " + auth);
        
        LoginContextBuilder builder = new LoginContextBuilder(Type.AUTHENTICATION);
        UsernamePasswordAuthenticationToken authToken = (UsernamePasswordAuthenticationToken) auth;
        
        Object details = auth.getDetails();
        builder.domain(details instanceof String ? (String) details : "other");
        
        Object principal = authToken.getPrincipal();
        if (principal instanceof String) {
            builder.username((String) principal);
        } else {
            throw new UsernameNotFoundException("Unsupported principal: " + principal);
        }
        
        Object credentials = authToken.getCredentials();
        if (credentials instanceof char[]) {
            builder.password((char[]) credentials);
        } else {
            throw new BadCredentialsException("Unsupported credentials: " + credentials);
        }
        
        LoginContext context;
        try {
            context = builder.build();
        } catch (LoginException ex) {
            throw new AuthenticationServiceException("Cannot build login context", ex);
        }
        try {
            context.login();
        } catch (LoginException ex) {
            throw new AuthenticationServiceException("Password invalid/Password required", ex);
        }
        
        Collection<GrantedAuthority> authorities = new HashSet<>();
        Set<Group> groups = context.getSubject().getPrincipals(Group.class);
        if (groups != null) {
            for (Group group : groups) {
                if ("Roles".equals(group.getName())) {
                    Enumeration<? extends Principal> members = group.members();
                    while (members.hasMoreElements()) {
                        Principal member = members.nextElement();
                        authorities.add(new SimpleGrantedAuthority(member.getName()));
                    }
                }
            }
        }
        
        AbstractAuthenticationToken result = new UsernamePasswordAuthenticationToken(principal, credentials, authorities);
        result.setDetails(details);
        
        return result;
    }
}
