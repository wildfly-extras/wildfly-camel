package org.wildfly.camel.test.dockerjava;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.http.HttpRequest.HttpRequestBuilder;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.ResponseItem.ProgressDetail;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;

public class DockerManager {

    final static Logger LOG = LoggerFactory.getLogger(DockerManager.class);
    
	private final DockerClient client;
	
	class ContainerState {
		
		private final CreateContainerCmd createCmd;
		private String containerId;
		
		private String awaitLogMessage;
		private String awaitHttpRequest;
		private Integer awaitHttpCode;
		private int sleepPolling = 200;
		
		ContainerState(CreateContainerCmd createCmd) {
			this.createCmd = createCmd;
		}
	}

	private final Map<String, ContainerState> mapping = new LinkedHashMap<>();
	private ContainerState auxState;
	
    public DockerManager() {
        
        // Derive system properties from our env vars
        System.getenv().entrySet().forEach(en -> {
            String key = en.getKey();
            String val = en.getValue();
            if (key.startsWith("DOCKER_JAVA_")) {
                key = key.substring(12).replace('_', '.').toLowerCase();
                if (System.getProperty(key) == null) {
                    System.setProperty(key, val);
                }
            }
        });
        
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        client = DockerClientBuilder.createClientBuilder(config).build();
    }
    
    public DockerManager(DockerClientConfig config) {
        client = DockerClientBuilder.createClientBuilder(config).build();
    }
    
	public DockerClient getDockerClient() {
		return client;
	}

	public DockerManager pullImage(String imgName) throws TimeoutException {
		PullImageResultCallback callback = new PullImageResultCallback() {
			@Override
			public void onNext(PullResponseItem item) {
				ProgressDetail detail = item.getProgressDetail();
				if (detail != null) {
                    Long current = detail.getCurrent();
                    Long total = detail.getTotal();
                    if (current != null && total != null) {
                        LOG.info("{}: {}%", imgName, 100 * current / total);
                    }
				}
				super.onNext(item);
			}
		};
		try {
			PullImageCmd pullCmd = client.pullImageCmd(imgName);
            AuthConfig authConfig = pullCmd.getAuthConfig();
			
            // export DOCKER_JAVA_REGISTRY_USERNAME=yourname
            // export DOCKER_JAVA_REGISTRY_PASSWORD=yourpass
            
			if (authConfig != null) {
			    
			    // Optional first token is the registry
	            String[] toks = imgName.split("/");
	            String imgRegistry = toks.length > 2 ? toks[0] : null;
	            
			    // Using auth config with no credetials on registry mismatch
	            String authRegistry = authConfig.getRegistryAddress();
	            if (imgRegistry != null && !authRegistry.contains(imgRegistry)) {
	                authConfig = new AuthConfig().withRegistryAddress(authRegistry);
	                pullCmd.withAuthConfig(authConfig);
	            }
	            
	            String username = authConfig.getUsername();
	            String password = authConfig.getPassword();
	            password = password != null ? "*******" : null;
	            LOG.info("Pull {}/{} {} {}", username, password, authRegistry, imgName);
	            
			} else {
			    
                LOG.info("Pull unauthorized {}", imgName);
			}
            
            if (!pullCmd.exec(callback).awaitCompletion(10, TimeUnit.MINUTES)) {
				throw new TimeoutException("Timeout pulling: " + imgName);
			}
		} catch (InterruptedException ex) {
			// ignore
		}
		return this;
	}
	
	public DockerManager createContainer(String imgName) {
		return createContainer(imgName, true);
	}
	
	public DockerManager createContainer(String imgName, boolean pull) {
		if (pull) {
			try {
				pullImage(imgName);
			} catch (TimeoutException ex) {
				throw new IllegalStateException(ex);
			}
		}
        auxState = new ContainerState(client.createContainerCmd(imgName));
		return this;
	}
	
	public DockerManager withName(String cntrName) {
		auxState.createCmd.withName(cntrName);
		return this;
	}

    public DockerManager withPortBindings(int port) {
        return withPortBindings(port + ":" + port);
    }
    
	public DockerManager withPortBindings(String... bindings) {
		List<ExposedPort> ports = new ArrayList<>();
        Ports portBindings = new Ports();
        for (String spec : bindings) {
        	String[] toks = spec.split(":");
        	ExposedPort exposedPort = ExposedPort.tcp(Integer.valueOf(toks[1]));
            Binding bindPort = Binding.bindPort(Integer.valueOf(toks[0]));
            portBindings.bind(exposedPort, bindPort);
			ports.add(exposedPort);
        }
        getHostConfig().withPortBindings(portBindings);
        auxState.createCmd.withHostConfig(getHostConfig());
        auxState.createCmd.withExposedPorts(ports);
		return this;
	}

	public DockerManager withNetworkMode(String networkMode) {
        getHostConfig().withNetworkMode(networkMode);
        auxState.createCmd.withHostConfig(getHostConfig());
		return this;
	}
	
	private HostConfig getHostConfig() {
		HostConfig hostConfig = auxState.createCmd.getHostConfig();
		return hostConfig != null ? hostConfig : new HostConfig();
	}

	public DockerManager withEnv(String... env) {
		auxState.createCmd.withEnv(env);
		return this;
	}
	
	public DockerManager withEntryPoint(String entrypoint) {
        String[] toks = entrypoint.split("\\s");
        auxState.createCmd.withEntrypoint(toks);
		return this;
	}
	
	public DockerManager withCmd(String cmd) {
        String[] toks = cmd.split("\\s");
        auxState.createCmd.withCmd(toks);
		return this;
	}

	public DockerManager startContainer() {
		auxState.containerId = auxState.createCmd.exec().getId();
    	client.startContainerCmd(auxState.containerId).exec();
    	String cntName = auxState.createCmd.getName();
    	mapping.put(cntName, auxState);
		return this;
	}

	public boolean removeContainer() {
		if (mapping.isEmpty()) return false;
		String cntName = mapping.keySet().iterator().next();
		return removeContainer(cntName);
	}

	public boolean removeContainer(String cntName) {
		ContainerState state = mapping.remove(cntName);
		if (state == null) return false;
		client.removeContainerCmd(state.containerId).withForce(true).exec();
		return true;
	}
	
	public DockerManager withAwaitLogMessage(String logMessage) {
		auxState.awaitLogMessage = logMessage;
		return this;
	}

	public DockerManager withAwaitHttp(String healthEndpoint) {
		auxState.awaitHttpRequest = healthEndpoint;
		return this;
	}

	public DockerManager withResponseCode(int code) {
		auxState.awaitHttpCode = code;
		return this;
	}
	
	public DockerManager withSleepPolling(int sleepPolling) {
		auxState.sleepPolling = sleepPolling;
		return this;
	}
	
	public boolean awaitCompletion(long timeout, TimeUnit timeUnit) throws Exception {
		
		boolean success = false;
		
		if (auxState.awaitLogMessage != null)
			success = awaitLogCompletion(timeout, timeUnit);
		
		else if (auxState.awaitHttpRequest != null)
			success = awaitHttpCompletion(timeout, timeUnit);
		
		else 
			throw new IllegalStateException("Undefined wait strategy");
		
		return success;
	}

	private boolean awaitLogCompletion(long timeout, TimeUnit timeUnit) throws Exception {
		
        LOG.info("Awaiting log message: {}", auxState.awaitLogMessage);
        
		try (ContainerLogCallback callback = new ContainerLogCallback()) {
			
			callback.execLogContainerCmd();
			
			if (!callback.awaitCompletion(timeout, timeUnit))
				throw new TimeoutException("Timeout waiting for log message: " + auxState.awaitLogMessage);
			
			return true;
		}
	}

	private boolean awaitHttpCompletion(long timeout, TimeUnit timeUnit) throws Exception {
		
		long tsnow = System.currentTimeMillis();
		long tsend = tsnow + timeUnit.toMillis(timeout);
		
		AtomicBoolean success = new AtomicBoolean(false);
		
		LOG.info("Awaiting {} {}", auxState.awaitHttpRequest, auxState.awaitHttpCode);
		
		try (ContainerLogCallback callback = new ContainerLogCallback()) {
			
			callback.execLogContainerCmd();
			
			while (tsnow < tsend) {
				
				try {
					HttpRequestBuilder builder = HttpRequest.get(auxState.awaitHttpRequest).timeout(500);
					int code = builder.getResponse().getStatusCode();
					if (code == auxState.awaitHttpCode) {
                        success.set(true);
						callback.close();
						break;
								
					}
				} catch (IOException ex) {
					// ignore
				}
				
				Thread.sleep(auxState.sleepPolling);
				tsnow = System.currentTimeMillis();
			}
		}
		
		if (!success.get())
			throw new TimeoutException("Timeout waiting for endpoint: " + auxState.awaitHttpRequest);
		
		return success.get();
	}
	
	class ContainerLogCallback extends ResultCallback.Adapter<Frame> {
		
		@Override
		public void onNext(Frame item) {
			String cntName = auxState.createCmd.getName();
			LOG.info("{}: {}", cntName, item);
			if (auxState.awaitLogMessage != null && item.toString().contains(auxState.awaitLogMessage)) { 
				try {
					close();
				} catch (IOException ex) {
					// ignore
				}
			}
		}
		
		void execLogContainerCmd() {
			
			client.logContainerCmd(auxState.containerId)
	        	.withFollowStream(true)
	            .withStdErr(true)
	            .withStdOut(true)
	            .withTailAll()
	            .exec(this);
		}
	}
}