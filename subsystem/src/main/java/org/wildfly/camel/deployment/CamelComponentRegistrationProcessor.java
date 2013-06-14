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

package org.wildfly.camel.deployment;

import static org.wildfly.camel.CamelMessages.MESSAGES;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.impl.DefaultComponentResolver;
import org.apache.camel.spi.ComponentResolver;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.Resource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Register a {@link ComponentResolver} service for each deployed component.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 12-Jun-2013
 */
public class CamelComponentRegistrationProcessor implements DeploymentUnitProcessor {

    static final AttachmentKey<AttachmentList<ServiceRegistration<?>>> COMPONENT_REGISTRATIONS = AttachmentKey.createList(ServiceRegistration.class);

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        Module module = depUnit.getAttachment(Attachments.MODULE);
        ModuleClassLoader classLoader = module.getClassLoader();
        Iterator<Resource> itres = classLoader.iterateResources(DefaultComponentResolver.RESOURCE_PATH, true);
        while (itres.hasNext()) {
            Resource res = itres.next();
            String fullname = res.getName();
            String cname = fullname.substring(fullname.lastIndexOf("/") + 1);

            // Load the component properties
            Properties props = new Properties();
            try {
                props.load(res.openStream());
            } catch (IOException ex) {
                throw MESSAGES.cannotLoadComponentProperties(ex, module.getIdentifier());
            }

            final Class<?> type;
            String className = props.getProperty("class");
            try {
                type = classLoader.loadClass(className);
            } catch (Exception ex) {
                throw MESSAGES.cannotLoadComponentType(ex, cname);
            }

            // Check component type
            if (Component.class.isAssignableFrom(type) == false)
                throw MESSAGES.componentTypeException(type);

            // Register the ComponentResolver service
            ComponentResolver service = new ComponentResolver() {
                @Override
                public Component resolveComponent(String name, CamelContext context) throws Exception {
                    return (Component) context.getInjector().newInstance(type);
                }
            };
            Hashtable<String, String> properties = new Hashtable<String, String>();
            properties.put("component", cname);
            BundleContext syscontext = depUnit.getAttachment(OSGiConstants.SYSTEM_CONTEXT_KEY);
            ServiceRegistration<ComponentResolver> sreg = syscontext.registerService(ComponentResolver.class, service, properties);
            depUnit.addToAttachmentList(COMPONENT_REGISTRATIONS, sreg);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        // Unregister the ComponentResolver services
        for (ServiceRegistration<?> sreg : depUnit.getAttachmentList(COMPONENT_REGISTRATIONS)) {
            sreg.unregister();
        }
    }
}
