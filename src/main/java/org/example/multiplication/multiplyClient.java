package org.example.multiplication;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.example.grpc.MatrixMul;
import org.example.grpc.MatrixMultiplicationGrpc;

import java.util.Random;

public class multiplyClient {

    private static MatrixMul.Matrix generateRandomMatrix(int rows, int cols) {
        MatrixMul.Matrix.Builder matrix = MatrixMul.Matrix.newBuilder().setRows(rows).setCols(cols);
        Random random = new Random();
        for (int i = 0; i < rows * cols; i++) {
            matrix.addData(random.nextInt(10));
        }
        return matrix.build();
    }

    private static void printMatrix(MatrixMul.Matrix matrix) {
        int rows = matrix.getRows();
        int cols = matrix.getCols();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                System.out.printf("%4d",matrix.getData(i*cols + j));
            }
            System.out.println();
        }
    }
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 5003)
                .usePlaintext()
                .build();

        MatrixMultiplicationGrpc.MatrixMultiplicationBlockingStub stub = MatrixMultiplicationGrpc.newBlockingStub(channel);

        MatrixMul.Matrix matrixA = generateRandomMatrix(5, 2);
        MatrixMul.Matrix matrixB = generateRandomMatrix(2, 3);

        System.out.println("Matrix A:");
        printMatrix(matrixA);
        System.out.println("Matrix B:");
        printMatrix(matrixB);

        MatrixMul.MatrixMultiplicationRequest request = MatrixMul.MatrixMultiplicationRequest.newBuilder()
                .setMatrixA(matrixA)
                .setMatrixB(matrixB)
                .build();

        MatrixMul.MatrixMultiplicationReply reply = stub.multiply(request);

        System.out.println("The result matrix is: ");

        printMatrix(reply.getResult());
    }
}
