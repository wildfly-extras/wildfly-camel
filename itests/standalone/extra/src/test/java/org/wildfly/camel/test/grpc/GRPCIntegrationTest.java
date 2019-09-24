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
package org.wildfly.camel.test.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.SynchronizationAdapter;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.camel.test.grpc.subA.PingPongGrpc;
import org.wildfly.camel.test.grpc.subA.PingRequest;
import org.wildfly.camel.test.grpc.subA.PongResponse;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({GRPCIntegrationTest.ServerSetup.class})
public class GRPCIntegrationTest {

    private static final int GRPC_TEST_PING_ID_1 = 1;
    private static final int GRPC_TEST_PONG_ID_1 = 1;
    private static final int GRPC_TEST_PONG_ID_2 = 2;
    private static final String GRPC_TEST_PING_VALUE = "PING";
    private static final String GRPC_TEST_PONG_VALUE = "PONG";

    static class ServerSetup implements ServerSetupTask {

        private Server server;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {

            AvailablePortFinder.removeServerData("grpc-port");
            int port = AvailablePortFinder.getNextAvailable();

            System.out.println("GPRC Server port: " + port);

            try {

                server = ServerBuilder
                    .forPort(port)
                    .addService(new PingPongImpl())
                    .build()
                    .start();

                AvailablePortFinder.storeServerData("grpc-port", port);

            } catch (Exception ex) {
                ex.printStackTrace();
                throw ex;
            }

            System.out.println("GPRC Services");
            System.out.println("=============");
            server.getServices().forEach(s -> System.out.println(s.toString()));

        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (server != null) {
                server.shutdown();
            }
        }
    }

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-grpc-tests.jar")
            .addClasses(AvailablePortFinder.class, EnvironmentUtils.class)
            .addPackage(PingPongGrpc.class.getPackage())
            .addAsResource("grpc/pingpong.proto", "pingpong.proto");
    }

    @Test
    public void testGRPCSynchronousProducer() throws Exception {

        Future<String> future = AvailablePortFinder.readServerDataAsync("grpc-port");
        int port = Integer.parseInt(future.get(10, TimeUnit.SECONDS));

        DefaultCamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .toF("grpc://localhost:%d/%s?method=pingSyncSync&synchronous=true", port, "org.wildfly.camel.test.grpc.subA.PingPong");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            PingRequest pingRequest = PingRequest.newBuilder()
                .setPingName(GRPC_TEST_PING_VALUE)
                .setPingId(GRPC_TEST_PING_ID_1)
                .build();

            PongResponse response = template.requestBody("direct:start", pingRequest, PongResponse.class);
            Assert.assertNotNull(response);
            Assert.assertEquals(response.getPongId(), GRPC_TEST_PING_ID_1);
            Assert.assertEquals(response.getPongName(), GRPC_TEST_PING_VALUE + GRPC_TEST_PONG_VALUE);
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testGRPCAsynchronousProducer() throws Exception {

        Future<String> future = AvailablePortFinder.readServerDataAsync("grpc-port");
        int port = Integer.parseInt(future.get(10, TimeUnit.SECONDS));

        DefaultCamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .toF("grpc://localhost:%d/%s?method=pingAsyncSync", port, "org.wildfly.camel.test.grpc.subA.PingPong");
            }
        });

        camelctx.start();
        try {
            final CountDownLatch latch = new CountDownLatch(1);

            PingRequest pingRequest = PingRequest.newBuilder()
                .setPingName(GRPC_TEST_PING_VALUE)
                .setPingId(GRPC_TEST_PING_ID_1)
                .build();

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.asyncCallbackSendBody("direct:start", pingRequest, new SynchronizationAdapter() {
                @Override
                public void onComplete(Exchange exchange) {
                    latch.countDown();

                    @SuppressWarnings("unchecked")
                    List<PongResponse> response = exchange.getOut().getBody(List.class);

                    Assert.assertEquals(2, response.size());
                    Assert.assertEquals(response.get(0).getPongId(), GRPC_TEST_PONG_ID_1);
                    Assert.assertEquals(response.get(0).getPongName(), GRPC_TEST_PING_VALUE + GRPC_TEST_PONG_VALUE);
                    Assert.assertEquals(response.get(1).getPongId(), GRPC_TEST_PONG_ID_2);
                }
            });

            Assert.assertTrue("Gave up waiting for latch", latch.await(5, TimeUnit.SECONDS));
        } finally {
            camelctx.close();
        }
    }

    static class PingPongImpl extends PingPongGrpc.PingPongImplBase {
        @Override
        public void pingSyncSync(PingRequest request, StreamObserver<PongResponse> responseObserver) {
            PongResponse response = PongResponse.newBuilder().setPongName(request.getPingName() + GRPC_TEST_PONG_VALUE).setPongId(request.getPingId()).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public StreamObserver<PingRequest> pingAsyncSync(StreamObserver<PongResponse> responseObserver) {
            StreamObserver<PingRequest> requestObserver = new StreamObserver<PingRequest>() {

                @Override
                public void onNext(PingRequest request) {
                    PongResponse response = PongResponse.newBuilder().setPongName(request.getPingName() + GRPC_TEST_PONG_VALUE).setPongId(request.getPingId()).build();
                    responseObserver.onNext(response);
                }

                @Override
                public void onError(Throwable t) {
                    t.printStackTrace();
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();
                }
            };
            return requestObserver;
        }
    }
}
