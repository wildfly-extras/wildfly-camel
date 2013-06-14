/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.wildfly.camel;

import javax.naming.Context;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.ComponentResolver;
import org.osgi.framework.BundleContext;

/**
 * The default Wildfly {@link CamelContext}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 18-Myy-2013
 */
public class WildflyCamelContext extends DefaultCamelContext {

    private final BundleContext syscontext;
    private Context namingContext;

    public WildflyCamelContext(BundleContext syscontext) {
        if (syscontext == null)
            throw CamelMessages.MESSAGES.illegalArgumentNull("syscontext");
        this.syscontext = syscontext;
    }

    @Override
    protected ComponentResolver createComponentResolver() {
        return new WildflyComponentResolver(syscontext);
    }

    public Context getNamingContext() {
        return namingContext;
    }

    public void setNamingContext(Context namingContext) {
        setJndiContext(this.namingContext = namingContext);
    }
}
