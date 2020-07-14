/*
 * #%L
 * Wildfly Camel :: Subsystem
 * %%
 * Copyright (C) 2013 - 2018 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wildfly.extension.camel.service;

import static org.wildfly.extension.camel.CamelLogger.LOGGER;
import static org.wildfly.extension.camel.service.CamelEndpointDeployerService.deployerServiceName;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.camel.service.CamelEndpointDeployerService.EndpointHttpHandler;

import io.undertow.server.HttpHandler;

/**
 * A service that either schedules HTTP endpoints for deployment once {@link #deployerService} becomes available or
 * deploys them instantly if the {@link #deployerService} is available already. The split between
 * {@link CamelEndpointDeploymentSchedulerService} and {@link CamelEndpointDeployerService} is necessary because the
 * requests to deploy HTTP endpoints may come in phases before the {@link CamelEndpointDeployerService} is available.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CamelEndpointDeploymentSchedulerService implements Service<CamelEndpointDeploymentSchedulerService> {

    private static final String SERVICE_NAME = CamelEndpointDeploymentSchedulerService.class.getSimpleName();

    private static final String DEPLOYMENT_CL_NAME_PREFIX = "deployment.";
    private static final String WAR_SUFFIX = ".war";
    private static final String EAR_INFIX = ".ear.";

    private final DeploymentUnit deploymentUnit;
    private final ServiceTarget serviceTarget;
    private final ServiceName serviceName;

    CamelEndpointDeploymentSchedulerService(DeploymentUnit deploymentUnit, ServiceName serviceName, ServiceTarget serviceTarget) {
        this.deploymentUnit = deploymentUnit;
        this.serviceTarget = serviceTarget;
        this.serviceName = serviceName;
    }

    public static ServiceController<CamelEndpointDeploymentSchedulerService> addService(DeploymentUnit depUnit, ServiceTarget serviceTarget) {
        ServiceName serviceName = deploymentSchedulerServiceName(depUnit.getServiceName());
        CamelEndpointDeploymentSchedulerService service = new CamelEndpointDeploymentSchedulerService(depUnit, serviceName, serviceTarget);
		return serviceTarget.addService(serviceName, service).install();
    }

    public static ServiceName deploymentSchedulerServiceName(ClassLoader deploymentClassLoader) {
        if (deploymentClassLoader == null) {
            deploymentClassLoader = SecurityActions.getContextClassLoader();
        }
        if (deploymentClassLoader instanceof ModuleClassLoader) {
            ModuleClassLoader moduleClassLoader = (ModuleClassLoader) deploymentClassLoader;
            final String clName = moduleClassLoader.getName();
            if (clName.startsWith(DEPLOYMENT_CL_NAME_PREFIX)) {
                if (clName.endsWith(WAR_SUFFIX)) {
                    final String deploymentName = clName.substring(DEPLOYMENT_CL_NAME_PREFIX.length());
                    final int earInfixPos = deploymentName.indexOf(EAR_INFIX);
                    if (earInfixPos >= 0) {
                        final String earName = deploymentName.substring(0, earInfixPos + EAR_INFIX.length() - 1);
                        final String warName = deploymentName.substring(earInfixPos + EAR_INFIX.length());
                        return ServiceName.of("jboss", "deployment", "subunit", earName, warName, SERVICE_NAME);
                    } else {
                        return ServiceName.of("jboss", "deployment", "unit", deploymentName, SERVICE_NAME);
                    }
                } else {
                    throw new IllegalStateException(String.format("Expected a %s name ending with '%s'; found %s",
                            ModuleClassLoader.class.getName(), WAR_SUFFIX, clName));
                }
            } else {
                throw new IllegalStateException(String.format("Expected a %s name starting with '%s'; found %s",
                        ModuleClassLoader.class.getName(), DEPLOYMENT_CL_NAME_PREFIX, clName));
            }
        } else {
            throw new IllegalStateException(
                    String.format("Expected a %s; found %s", ModuleClassLoader.class.getName(), deploymentClassLoader));
        }
    }

    public static ServiceName deploymentSchedulerServiceName(ServiceName deploymentUnitServiceName) {
        return deploymentUnitServiceName.append(SERVICE_NAME);
    }

    public void schedule(URI uri, EndpointHttpHandler httpHandler) {
    	scheduleInternal(uri, httpHandler);
    }

    public void schedule(URI uri, HttpHandler httpHandler) {
    	scheduleInternal(uri, httpHandler);
    }

    @SuppressWarnings("deprecation")
	private void scheduleInternal(URI uri, Object httpHandler) {
		ServiceName auxServiceName = serviceName.append(uri.getPath());
		ServiceName deployerServiceName = deployerServiceName(deploymentUnit.getServiceName());
	    InjectedValue<CamelEndpointDeployerService> deployerServiceSupplier = new InjectedValue<>();
		ServiceBuilder<Void> sb = serviceTarget.addService(auxServiceName, new AbstractService<Void>() {

			@Override
			public void start(StartContext context) throws StartException {
				CamelEndpointDeployerService deployerService = deployerServiceSupplier.getValue();
            	if (httpHandler instanceof EndpointHttpHandler)
            		deployerService.deploy(uri, (EndpointHttpHandler) httpHandler);
            	else if (httpHandler instanceof HttpHandler)
            		deployerService.deploy(uri, (HttpHandler) httpHandler);
			}

			@Override
			public void stop(StopContext context) {
				CamelEndpointDeployerService deployerService = deployerServiceSupplier.getValue();
                deployerService.undeploy(uri);
			}
		});
		sb.addDependency(deployerServiceName, CamelEndpointDeployerService.class, deployerServiceSupplier);
		ServiceController<Void> controller = sb.install();
		try {
			controller.awaitValue(4, TimeUnit.SECONDS);
		} catch (InterruptedException ex) {
			// ignore
		} catch (TimeoutException ex) {				
			LOGGER.debug("Endpoint service for {} from deployment {} faild to start up in time", uri, deploymentUnit.getName());
		}
    }

    public void unschedule(URI uri) {
		ServiceName auxServiceName = serviceName.append(uri.getPath());
        ServiceContainer serviceContainer = CurrentServiceContainer.getServiceContainer();
		ServiceController<?> controller = serviceContainer.getRequiredService(auxServiceName);
		controller.setMode(Mode.REMOVE);
    }

    @Override
    public void start(StartContext context) throws StartException {
        LOGGER.debug("{} started for deployment {}", SERVICE_NAME, deploymentUnit.getName());
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public CamelEndpointDeploymentSchedulerService getValue() throws IllegalStateException {
        return this;
    }
}
