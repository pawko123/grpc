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
    private static MatrixMul.Matrix[] splitMatrixA(MatrixMul.Matrix matrixA) {
        int rows = matrixA.getRows();
        int cols = matrixA.getCols();

        int subMatrixCount = rows < 4 ? 1 : 4; // Handle small matrices directly
        MatrixMul.Matrix[] subMatrices = new MatrixMul.Matrix[subMatrixCount];

        for (int i = 0; i < subMatrixCount; i++) {
            MatrixMul.Matrix.Builder builder = MatrixMul.Matrix.newBuilder();
            int startRow = (i / 2) * (rows / 2);
            int endRow = (i / 2 == 1) ? rows : startRow + (rows / 2);

            builder.setRows(endRow - startRow);
            builder.setCols(cols);

            for (int r = startRow; r < endRow; r++) {
                for (int c = 0; c < cols; c++) {
                    builder.addData(matrixA.getData(r * cols + c));
                }
            }
            subMatrices[i] = builder.build();
        }

        return subMatrices;
    }

    private static MatrixMul.Matrix combineMatrices(MatrixMul.MatrixMultiplicationReply[] replies, int totalRows, int totalCols) {
        MatrixMul.Matrix.Builder builder = MatrixMul.Matrix.newBuilder();
        builder.setRows(totalRows);
        builder.setCols(totalCols);

        for (int i = 0; i < totalRows * totalCols; i++) {
            builder.addData(0);
        }

        int halfRows = totalRows / 2;

        for (int i = 0; i < 4; i++) {
            MatrixMul.Matrix subMatrix = replies[i].getResult();
            for (int r = 0; r < subMatrix.getRows(); r++) {
                for (int c = 0; c < subMatrix.getCols(); c++) {
                    int globalRow = (i / 2) * halfRows + r;
                    int globalCol = c;
                    int globalIndex = globalRow * totalCols + globalCol;
                    builder.setData(globalIndex, subMatrix.getData(r * subMatrix.getCols() + c));
                }
            }
        }

        return builder.build();
    }

    public static void main(String[] args) {
        ManagedChannel[] channels = new ManagedChannel[4];
        for (int i = 0; i < 4; i++) {
            channels[i] = ManagedChannelBuilder.forAddress("localhost", 5001 + i)
                    .usePlaintext()
                    .build();
        }


        MatrixMul.Matrix matrixA = generateRandomMatrix(10, 10);
        MatrixMul.Matrix matrixB = generateRandomMatrix(10, 10);

        MatrixMul.Matrix[] subMatricesA = splitMatrixA(matrixA);

        Thread[] threads = new Thread[4];
        MatrixMul.MatrixMultiplicationReply[] replies = new MatrixMul.MatrixMultiplicationReply[4];

        for (int i = 0; i < 4; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                MatrixMultiplicationGrpc.MatrixMultiplicationBlockingStub stub = MatrixMultiplicationGrpc.newBlockingStub(channels[index]);

                MatrixMul.MatrixMultiplicationRequest request = MatrixMul.MatrixMultiplicationRequest.newBuilder()
                        .setMatrixA(subMatricesA[index])
                        .setMatrixB(matrixB)
                        .build();

                replies[index] = stub.multiply(request);
            });
            threads[i].start();
        }
        for(Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Matrix A:");
        printMatrix(matrixA);
        System.out.println("Matrix B:");
        printMatrix(matrixB);

        MatrixMul.Matrix resultMatrix = combineMatrices(replies, matrixA.getRows(), matrixB.getCols());

        System.out.println("The result matrix is: ");

        printMatrix(resultMatrix);
    }
}
