package org.example;


import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.example.grpc.Test;
import org.example.grpc.testGrpc;

public class testClient{
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 5003)
                .usePlaintext()
                .build();
        testGrpc.testBlockingStub stub = testGrpc.newBlockingStub(channel);
        Test.HelloRequest request = Test.HelloRequest.newBuilder().setName("World").build();

        Test.HelloReply reply = stub.sayHello(request);
        System.out.println(reply.getMessage());
    }
}
