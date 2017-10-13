package org.wildfly.camel.test.grpc.subA;

import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;

/**
 * <pre>
 * The PingPong service definition.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.6.1)",
    comments = "Source: pingpong.proto")
public final class PingPongGrpc {

  private PingPongGrpc() {}

  public static final String SERVICE_NAME = "org.wildfly.camel.test.grpc.subA.test.PingPong";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<org.wildfly.camel.test.grpc.subA.PingRequest,
      org.wildfly.camel.test.grpc.subA.PongResponse> METHOD_PING_SYNC_SYNC =
      io.grpc.MethodDescriptor.<org.wildfly.camel.test.grpc.subA.PingRequest, org.wildfly.camel.test.grpc.subA.PongResponse>newBuilder()
          .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(generateFullMethodName(
              "org.wildfly.camel.test.grpc.subA.test.PingPong", "PingSyncSync"))
          .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              org.wildfly.camel.test.grpc.subA.PingRequest.getDefaultInstance()))
          .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              org.wildfly.camel.test.grpc.subA.PongResponse.getDefaultInstance()))
          .build();
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<org.wildfly.camel.test.grpc.subA.PingRequest,
      org.wildfly.camel.test.grpc.subA.PongResponse> METHOD_PING_SYNC_ASYNC =
      io.grpc.MethodDescriptor.<org.wildfly.camel.test.grpc.subA.PingRequest, org.wildfly.camel.test.grpc.subA.PongResponse>newBuilder()
          .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
          .setFullMethodName(generateFullMethodName(
              "org.wildfly.camel.test.grpc.subA.test.PingPong", "PingSyncAsync"))
          .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              org.wildfly.camel.test.grpc.subA.PingRequest.getDefaultInstance()))
          .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              org.wildfly.camel.test.grpc.subA.PongResponse.getDefaultInstance()))
          .build();
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<org.wildfly.camel.test.grpc.subA.PingRequest,
      org.wildfly.camel.test.grpc.subA.PongResponse> METHOD_PING_ASYNC_SYNC =
      io.grpc.MethodDescriptor.<org.wildfly.camel.test.grpc.subA.PingRequest, org.wildfly.camel.test.grpc.subA.PongResponse>newBuilder()
          .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
          .setFullMethodName(generateFullMethodName(
              "org.wildfly.camel.test.grpc.subA.test.PingPong", "PingAsyncSync"))
          .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              org.wildfly.camel.test.grpc.subA.PingRequest.getDefaultInstance()))
          .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              org.wildfly.camel.test.grpc.subA.PongResponse.getDefaultInstance()))
          .build();
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<org.wildfly.camel.test.grpc.subA.PingRequest,
      org.wildfly.camel.test.grpc.subA.PongResponse> METHOD_PING_ASYNC_ASYNC =
      io.grpc.MethodDescriptor.<org.wildfly.camel.test.grpc.subA.PingRequest, org.wildfly.camel.test.grpc.subA.PongResponse>newBuilder()
          .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
          .setFullMethodName(generateFullMethodName(
              "org.wildfly.camel.test.grpc.subA.test.PingPong", "PingAsyncAsync"))
          .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              org.wildfly.camel.test.grpc.subA.PingRequest.getDefaultInstance()))
          .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              org.wildfly.camel.test.grpc.subA.PongResponse.getDefaultInstance()))
          .build();

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PingPongStub newStub(io.grpc.Channel channel) {
    return new PingPongStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PingPongBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new PingPongBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PingPongFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new PingPongFutureStub(channel);
  }

  /**
   * <pre>
   * The PingPong service definition.
   * </pre>
   */
  public static abstract class PingPongImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Sending ping message and getting pong answer synchronously
     * </pre>
     */
    public void pingSyncSync(org.wildfly.camel.test.grpc.subA.PingRequest request,
        io.grpc.stub.StreamObserver<org.wildfly.camel.test.grpc.subA.PongResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_PING_SYNC_SYNC, responseObserver);
    }

    /**
     * <pre>
     * Sending ping message synchronously and getting pong answer asynchronously in streaming mode (multiple response messages)
     * </pre>
     */
    public void pingSyncAsync(org.wildfly.camel.test.grpc.subA.PingRequest request,
        io.grpc.stub.StreamObserver<org.wildfly.camel.test.grpc.subA.PongResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_PING_SYNC_ASYNC, responseObserver);
    }

    /**
     * <pre>
     * Sending ping message asynchronously and getting pong answer synchronously
     * </pre>
     */
    public io.grpc.stub.StreamObserver<org.wildfly.camel.test.grpc.subA.PingRequest> pingAsyncSync(
        io.grpc.stub.StreamObserver<org.wildfly.camel.test.grpc.subA.PongResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(METHOD_PING_ASYNC_SYNC, responseObserver);
    }

    /**
     * <pre>
     * Sending ping message asynchronously and getting pong answer asynchronously in streaming mode (multiple response messages)
     * </pre>
     */
    public io.grpc.stub.StreamObserver<org.wildfly.camel.test.grpc.subA.PingRequest> pingAsyncAsync(
        io.grpc.stub.StreamObserver<org.wildfly.camel.test.grpc.subA.PongResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(METHOD_PING_ASYNC_ASYNC, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_PING_SYNC_SYNC,
            asyncUnaryCall(
              new MethodHandlers<
                org.wildfly.camel.test.grpc.subA.PingRequest,
                org.wildfly.camel.test.grpc.subA.PongResponse>(
                  this, METHODID_PING_SYNC_SYNC)))
          .addMethod(
            METHOD_PING_SYNC_ASYNC,
            asyncServerStreamingCall(
              new MethodHandlers<
                org.wildfly.camel.test.grpc.subA.PingRequest,
                org.wildfly.camel.test.grpc.subA.PongResponse>(
                  this, METHODID_PING_SYNC_ASYNC)))
          .addMethod(
            METHOD_PING_ASYNC_SYNC,
            asyncClientStreamingCall(
              new MethodHandlers<
                org.wildfly.camel.test.grpc.subA.PingRequest,
                org.wildfly.camel.test.grpc.subA.PongResponse>(
                  this, METHODID_PING_ASYNC_SYNC)))
          .addMethod(
            METHOD_PING_ASYNC_ASYNC,
            asyncBidiStreamingCall(
              new MethodHandlers<
                org.wildfly.camel.test.grpc.subA.PingRequest,
                org.wildfly.camel.test.grpc.subA.PongResponse>(
                  this, METHODID_PING_ASYNC_ASYNC)))
          .build();
    }
  }

  /**
   * <pre>
   * The PingPong service definition.
   * </pre>
   */
  public static final class PingPongStub extends io.grpc.stub.AbstractStub<PingPongStub> {
    private PingPongStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PingPongStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PingPongStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PingPongStub(channel, callOptions);
    }

    /**
     * <pre>
     * Sending ping message and getting pong answer synchronously
     * </pre>
     */
    public void pingSyncSync(org.wildfly.camel.test.grpc.subA.PingRequest request,
        io.grpc.stub.StreamObserver<org.wildfly.camel.test.grpc.subA.PongResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_PING_SYNC_SYNC, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Sending ping message synchronously and getting pong answer asynchronously in streaming mode (multiple response messages)
     * </pre>
     */
    public void pingSyncAsync(org.wildfly.camel.test.grpc.subA.PingRequest request,
        io.grpc.stub.StreamObserver<org.wildfly.camel.test.grpc.subA.PongResponse> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(METHOD_PING_SYNC_ASYNC, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Sending ping message asynchronously and getting pong answer synchronously
     * </pre>
     */
    public io.grpc.stub.StreamObserver<org.wildfly.camel.test.grpc.subA.PingRequest> pingAsyncSync(
        io.grpc.stub.StreamObserver<org.wildfly.camel.test.grpc.subA.PongResponse> responseObserver) {
      return asyncClientStreamingCall(
          getChannel().newCall(METHOD_PING_ASYNC_SYNC, getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * Sending ping message asynchronously and getting pong answer asynchronously in streaming mode (multiple response messages)
     * </pre>
     */
    public io.grpc.stub.StreamObserver<org.wildfly.camel.test.grpc.subA.PingRequest> pingAsyncAsync(
        io.grpc.stub.StreamObserver<org.wildfly.camel.test.grpc.subA.PongResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(METHOD_PING_ASYNC_ASYNC, getCallOptions()), responseObserver);
    }
  }

  /**
   * <pre>
   * The PingPong service definition.
   * </pre>
   */
  public static final class PingPongBlockingStub extends io.grpc.stub.AbstractStub<PingPongBlockingStub> {
    private PingPongBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PingPongBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PingPongBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PingPongBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Sending ping message and getting pong answer synchronously
     * </pre>
     */
    public org.wildfly.camel.test.grpc.subA.PongResponse pingSyncSync(org.wildfly.camel.test.grpc.subA.PingRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_PING_SYNC_SYNC, getCallOptions(), request);
    }

    /**
     * <pre>
     * Sending ping message synchronously and getting pong answer asynchronously in streaming mode (multiple response messages)
     * </pre>
     */
    public java.util.Iterator<org.wildfly.camel.test.grpc.subA.PongResponse> pingSyncAsync(
        org.wildfly.camel.test.grpc.subA.PingRequest request) {
      return blockingServerStreamingCall(
          getChannel(), METHOD_PING_SYNC_ASYNC, getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * The PingPong service definition.
   * </pre>
   */
  public static final class PingPongFutureStub extends io.grpc.stub.AbstractStub<PingPongFutureStub> {
    private PingPongFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PingPongFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PingPongFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PingPongFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Sending ping message and getting pong answer synchronously
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<org.wildfly.camel.test.grpc.subA.PongResponse> pingSyncSync(
        org.wildfly.camel.test.grpc.subA.PingRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_PING_SYNC_SYNC, getCallOptions()), request);
    }
  }

  private static final int METHODID_PING_SYNC_SYNC = 0;
  private static final int METHODID_PING_SYNC_ASYNC = 1;
  private static final int METHODID_PING_ASYNC_SYNC = 2;
  private static final int METHODID_PING_ASYNC_ASYNC = 3;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final PingPongImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(PingPongImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PING_SYNC_SYNC:
          serviceImpl.pingSyncSync((org.wildfly.camel.test.grpc.subA.PingRequest) request,
              (io.grpc.stub.StreamObserver<org.wildfly.camel.test.grpc.subA.PongResponse>) responseObserver);
          break;
        case METHODID_PING_SYNC_ASYNC:
          serviceImpl.pingSyncAsync((org.wildfly.camel.test.grpc.subA.PingRequest) request,
              (io.grpc.stub.StreamObserver<org.wildfly.camel.test.grpc.subA.PongResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PING_ASYNC_SYNC:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.pingAsyncSync(
              (io.grpc.stub.StreamObserver<org.wildfly.camel.test.grpc.subA.PongResponse>) responseObserver);
        case METHODID_PING_ASYNC_ASYNC:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.pingAsyncAsync(
              (io.grpc.stub.StreamObserver<org.wildfly.camel.test.grpc.subA.PongResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static final class PingPongDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.wildfly.camel.test.grpc.subA.PingPongProto.getDescriptor();
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (PingPongGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new PingPongDescriptorSupplier())
              .addMethod(METHOD_PING_SYNC_SYNC)
              .addMethod(METHOD_PING_SYNC_ASYNC)
              .addMethod(METHOD_PING_ASYNC_SYNC)
              .addMethod(METHOD_PING_ASYNC_ASYNC)
              .build();
        }
      }
    }
    return result;
  }
}
