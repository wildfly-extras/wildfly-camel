/*
 * #%L
 * Wildfly Camel Subsystem
 * %%
 * Copyright (C) 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */


package org.wildfly.camel.service;

import static org.wildfly.camel.CamelLogger.LOGGER;
import static org.wildfly.camel.CamelMessages.MESSAGES;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executors;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.server.Services;
import org.jboss.gravia.provision.DefaultProvisioner;
import org.jboss.gravia.provision.DefaultResourceHandle;
import org.jboss.gravia.provision.Environment;
import org.jboss.gravia.provision.ProvisionException;
import org.jboss.gravia.provision.Provisioner;
import org.jboss.gravia.provision.Provisioner.ResourceHandle;
import org.jboss.gravia.repository.Repository;
import org.jboss.gravia.repository.RepositoryContent;
import org.jboss.gravia.resolver.Resolver;
import org.jboss.gravia.resource.Capability;
import org.jboss.gravia.resource.IdentityNamespace;
import org.jboss.gravia.resource.Requirement;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.ResourceIdentity;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.shrinkwrap.api.ConfigurationBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.wildfly.camel.CamelConstants;

/**
 * Service providing the {@link Provisioner}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 27-Jun-2013
 */
public class ProvisionerService extends AbstractService<Provisioner> {

    private final InjectedValue<ModelController> injectedController = new InjectedValue<ModelController>();
    private final InjectedValue<Environment> injectedEnvironment = new InjectedValue<Environment>();
    private final InjectedValue<Repository> injectedRepository = new InjectedValue<Repository>();
    private final InjectedValue<Resolver> injectedResolver = new InjectedValue<Resolver>();
    private ServerDeploymentManager serverDeploymentManager;
    private ModelControllerClient modelControllerClient;
    private Provisioner provisioner;

    public static ServiceController<Provisioner> addService(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        ProvisionerService service = new ProvisionerService();
        ServiceBuilder<Provisioner> builder = serviceTarget.addService(CamelConstants.PROVISIONER_SERVICE_NAME, service);
        builder.addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, service.injectedController);
        builder.addDependency(CamelConstants.ENVIRONMENT_SERVICE_NAME, Environment.class, service.injectedEnvironment);
        builder.addDependency(CamelConstants.REPOSITORY_SERVICE_NAME, Repository.class, service.injectedRepository);
        builder.addDependency(CamelConstants.RESOLVER_SERVICE_NAME, Resolver.class, service.injectedResolver);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    // Hide ctor
    private ProvisionerService() {
    }

    @Override
    public void start(StartContext startContext) throws StartException {

        ModelController modelController = injectedController.getValue();
        modelControllerClient = modelController.createClient(Executors.newCachedThreadPool());
        serverDeploymentManager = ServerDeploymentManager.Factory.create(modelControllerClient);

        Resolver resolver = injectedResolver.getValue();
        Repository repository = injectedRepository.getValue();
        provisioner = new DefaultProvisioner(resolver, repository) {

            @Override
            protected Environment createEnvironment() {
                return injectedEnvironment.getValue();
            }

            @Override
            public ResourceHandle installResource(Resource resource, Map<Requirement, Resource> mapping) throws ProvisionException {
                return installResourceInternal(resource, mapping);
            }
        };
    }

    @Override
    public void stop(StopContext context) {
        try {
            modelControllerClient.close();
        } catch (IOException ex) {
            // ignore
        }
    }

    @Override
    public Provisioner getValue() throws IllegalStateException {
        return provisioner;
    }

    private ResourceHandle installResourceInternal(Resource res, Map<Requirement, Resource> mapping) throws ProvisionException {

        RepositoryContent content = res.adapt(RepositoryContent.class);
        if (content == null) {
            return new DefaultResourceHandle(res);
        }

        final String runtimeName = res.getIdentity().getSymbolicName();
        final ServerDeploymentHelper serverDeployer = new ServerDeploymentHelper(serverDeploymentManager);
        try {
            InputStream input = getWrappedResourceContent(res, mapping);
            serverDeployer.deploy(runtimeName, input);
        } catch (Throwable th) {
            throw MESSAGES.cannotProvisionResource(th, res);
        }

        return new DefaultResourceHandle(res) {

            @Override
            public void uninstall() throws ProvisionException {
                try {
                    serverDeployer.undeploy(runtimeName);
                } catch (Throwable th) {
                    throw MESSAGES.cannotUninstallProvisionedResource(th, getResource());
                }
            }
        };
    }

    // Wrap the resource and add a generated jboss-deployment-structure.xml
    private InputStream getWrappedResourceContent(Resource res, Map<Requirement, Resource> mapping) {
        ResourceIdentity resid = res.getIdentity();
        ConfigurationBuilder config = new ConfigurationBuilder().classLoaders(Collections.singleton(ShrinkWrap.class.getClassLoader()));
        JavaArchive archive = ShrinkWrap.createDomain(config).getArchiveFactory().create(JavaArchive.class, "wrapped-resource.jar");
        archive.as(ZipImporter.class).importFrom(((RepositoryContent) res).getContent());
        JavaArchive wrapper = ShrinkWrap.createDomain(config).getArchiveFactory().create(JavaArchive.class, "wrapped:" + resid.getSymbolicName());
        wrapper.addAsManifestResource(getDeploymentStructureAsset(res, mapping), "jboss-deployment-structure.xml");
        wrapper.add(archive, "/", ZipExporter.class);
        return wrapper.as(ZipExporter.class).exportAsInputStream();
    }

    private Asset getDeploymentStructureAsset(Resource res, Map<Requirement, Resource> mapping) {
        LOGGER.infof("Generating dependencies for: %s", res);
        StringBuffer buffer = new StringBuffer();
        buffer.append("<jboss-deployment-structure xmlns='urn:jboss:deployment-structure:1.2'>");
        buffer.append(" <deployment>");
        buffer.append("  <resources>");
        buffer.append("   <resource-root path='wrapped-resource.jar' use-physical-code-source='true'/>");
        buffer.append("  </resources>");
        buffer.append("  <dependencies>");
        for (Requirement req : res.getRequirements(IdentityNamespace.IDENTITY_NAMESPACE)) {
            Resource depres = mapping.get(req);
            if (depres != null) {
                Capability icap = depres.getIdentityCapability();
                String type = (String) icap.getAttribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
                String modname = depres.getIdentity().getSymbolicName();
                if (!IdentityNamespace.TYPE_MODULE.equals(type)) {
                    modname = "deployment." + modname;
                }
                buffer.append("<module name='" + modname + "'/>");
                LOGGER.infof("  %s", modname);
            }
        }
        buffer.append("  </dependencies>");
        buffer.append(" </deployment>");
        buffer.append("</jboss-deployment-structure>");
        return new StringAsset(buffer.toString());
    }
}