package org.wildfly.camel.test.dockerjava;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.okhttp.OkDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

public class DockerClientBuilder {

	private DockerClientConfig config;
	private DockerHttpClient httpClient;
	
	// Hide ctor
	private DockerClientBuilder() {
    	config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    	httpClient = new OkDockerHttpClient.Builder()
    	        .dockerHost(config.getDockerHost())
    	        .build();
	}
	
	public static DockerClientBuilder createDefaultClientBuilder() {
		return new DockerClientBuilder();
	}
	
	public DockerClient build() {
    	DockerClient client = DockerClientImpl.getInstance(config, httpClient);
    	return client;
	}
}