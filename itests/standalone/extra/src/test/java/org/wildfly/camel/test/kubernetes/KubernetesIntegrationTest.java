/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
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
package org.wildfly.camel.test.kubernetes;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kubernetes.KubernetesConstants;
import org.apache.camel.component.kubernetes.pods.KubernetesPodsEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.camel.test.kubernetes.subA.FakeKubernetesResponseServlet;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class KubernetesIntegrationTest {

    private static final String DEFAULT_KUBERNETES_MASTER = "http://localhost:8080/camel-kubernetes-tests/fake-kubernetes";
    private static final String DEFAULT_KUBERNETES_NAMESPACE = "default";
    private static final String POD_NAME = "wildfly-camel-test";

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "camel-kubernetes-tests.war")
            .addPackage(FakeKubernetesResponseServlet.class.getPackage())
            .addClass(TestUtils.class)
            .addAsResource("kubernetes/event.json", "event.json")
            .addAsResource("kubernetes/podCreated.json", "podCreated.json")
            .addAsResource("kubernetes/podList.json", "podList.json");
    }

    @Test
    public void testKubernetesConsumer() throws Exception {
        String kubernetesConsumerURI = String.format("kubernetes-pods:%s?category=pods", getKubernetesMaster());

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(kubernetesConsumerURI)
                .to("mock:result");
            }
        });

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedMinimumMessageCount(1);

        ProducerTemplate template = camelctx.createProducerTemplate();
        String podName = POD_NAME + "-consumer";

        camelctx.start();

        KubernetesPodsEndpoint kubernetesEndpoint = camelctx.getEndpoint(kubernetesConsumerURI, KubernetesPodsEndpoint.class);
        String kubernetesNamespace = getNamespace(kubernetesEndpoint.getKubernetesClient());

        try {
            // Create a pod so we can generate some watch events
            Map<String, Object> headers = new HashMap<>();
            headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, kubernetesNamespace);
            headers.put(KubernetesConstants.KUBERNETES_POD_NAME, podName);
            headers.put(KubernetesConstants.KUBERNETES_POD_SPEC, createPodSpec());
            template.requestBodyAndHeaders("kubernetes-pods:" + getKubernetesMaster() + "?category=pods&operation=createPod", null, headers);

            mockEndpoint.assertIsSatisfied();
        } finally {
            // Clean up
            Map<String, Object> headers = new HashMap<>();
            headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, kubernetesNamespace);
            headers.put(KubernetesConstants.KUBERNETES_POD_NAME, podName);
            template.requestBodyAndHeaders("kubernetes-pods:" + getKubernetesMaster() + "?category=pods&operation=deletePod", null, headers);

            camelctx.stop();
        }
    }

    @Test
    public void testKubernetesProducer() throws Exception {
        String kubernetesProducerURI = String.format("kubernetes-pods:%s?category=pods&operation=createPod", getKubernetesMaster());

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to(kubernetesProducerURI);
            }
        });

        ProducerTemplate template = camelctx.createProducerTemplate();
        String podName = POD_NAME + "-producer";

        camelctx.start();

        KubernetesPodsEndpoint kubernetesEndpoint = camelctx.getEndpoint(kubernetesProducerURI, KubernetesPodsEndpoint.class);
        String kubernetesNamespace = getNamespace(kubernetesEndpoint.getKubernetesClient());

        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, kubernetesNamespace);
            headers.put(KubernetesConstants.KUBERNETES_POD_NAME, podName);
            headers.put(KubernetesConstants.KUBERNETES_POD_SPEC, createPodSpec());

            Pod pod = template.requestBodyAndHeaders("direct:start", null, headers, Pod.class);

            Assert.assertNotNull("Expected pod to be not null", pod);
            Assert.assertEquals("Pending", pod.getStatus().getPhase());
        } finally {
            // Clean up
            Map<String, Object> headers = new HashMap<>();
            headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, kubernetesNamespace);
            headers.put(KubernetesConstants.KUBERNETES_POD_NAME, podName);
            template.requestBodyAndHeaders("kubernetes:" + getKubernetesMaster() + "?category=pods&operation=deletePod", null, headers);

            camelctx.stop();
        }
    }

    private PodSpec createPodSpec() throws IOException {
        PodSpec podSpec = new PodSpec();
        podSpec.setHostname("localhost");

        Container container = new Container();
        container.setImage("docker.io/wildflyext/wildfly-camel:latest");
        container.setName("wildfly-camel-test");

        ContainerPort port = new ContainerPort();
        port.setHostIP("0.0.0.0");
        port.setContainerPort(8080);

        List<ContainerPort> ports = new ArrayList<>();
        ports.add(port);
        container.setPorts(ports);

        List<Container> containers = new ArrayList<>();
        containers.add(container);

        podSpec.setContainers(containers);

        return podSpec;
    }

    private String getNamespace(KubernetesClient client) {
        if (client != null && client.getConfiguration().getNamespace() != null) {
            return client.getConfiguration().getNamespace();
        }
        return DEFAULT_KUBERNETES_NAMESPACE;
    }

    private String getKubernetesMaster() {
        String kubernetesMaster = System.getenv("KUBERNETES_MASTER");
        return kubernetesMaster == null ? DEFAULT_KUBERNETES_MASTER : kubernetesMaster;
    }
}
