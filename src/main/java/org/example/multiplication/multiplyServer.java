package org.example.multiplication;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class multiplyServer {
    public static void main(String[] args) throws IOException, InterruptedException {

        Server server = ServerBuilder.forPort(Integer.parseInt(args[0]))
                .addService(new multiplyServerImpl())
                .build();

        server.start();
        server.awaitTermination();
    }
}
