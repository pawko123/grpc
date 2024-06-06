package org.example;

import io.grpc.stub.StreamObserver;
import org.example.grpc.Test;
import org.example.grpc.testGrpc;

public class testServerImpl extends testGrpc.testImplBase{
    public void sayHello(
        Test.HelloRequest request,
        StreamObserver<Test.HelloReply> responseObserver
    ) {
        Test.HelloReply reply = Test.HelloReply.newBuilder().setMessage("Hello " + request.getName()).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
