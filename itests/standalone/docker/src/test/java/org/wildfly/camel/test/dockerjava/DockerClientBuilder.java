package org.wildfly.camel.test.dockerjava;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.okhttp.OkDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

public class DockerClientBuilder {

	private final DockerClientConfig config;
	private final DockerHttpClient httpClient;
	
	// Hide ctor
	private DockerClientBuilder(DockerClientConfig config) {
    	this.config = config;
    	this.httpClient = new OkDockerHttpClient.Builder()
    	        .dockerHost(config.getDockerHost())
    	        .build();
	}
	
    public static DockerClientBuilder createClientBuilder() {
        return new DockerClientBuilder(DefaultDockerClientConfig.createDefaultConfigBuilder().build());
    }
    
    public static DockerClientBuilder createClientBuilder(DockerClientConfig config) {
        return new DockerClientBuilder(config);
    }
    
	public DockerClient build() {
    	DockerClient client = DockerClientImpl.getInstance(config, httpClient);
    	return client;
	}
}