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

        // Calculate the number of rows per submatrix, distributing the remainder rows as evenly as possible
        int baseRowsPerSubMatrix = rows / subMatrixCount;
        int remainder = rows % subMatrixCount;

        int currentRow = 0;
        for (int i = 0; i < subMatrixCount; i++) {
            MatrixMul.Matrix.Builder builder = MatrixMul.Matrix.newBuilder();

            // Each submatrix gets baseRowsPerSubMatrix rows, plus one more if there are remainder rows left
            int rowsForThisSubMatrix = baseRowsPerSubMatrix + (i < remainder ? 1 : 0);

            // Determine the start and end row for each submatrix
            int startRow = currentRow;
            int endRow = currentRow + rowsForThisSubMatrix;

            // Set submatrix dimensions
            builder.setRows(rowsForThisSubMatrix);
            builder.setCols(cols);

            // Add the data to the submatrix
            for (int r = startRow; r < endRow; r++) {
                for (int c = 0; c < cols; c++) {
                    builder.addData(matrixA.getData(r * cols + c));
                }
            }

            subMatrices[i] = builder.build();
            currentRow = endRow; // Update currentRow to the end of the last processed submatrix
        }

        return subMatrices;
    }

    private static MatrixMul.Matrix mergeSubMatrices(MatrixMul.MatrixMultiplicationReply[] subMatrices) {
        // Assume all submatrices have the same number of columns
        int totalRows = 0;
        int cols = subMatrices[0].getResult().getCols();

        // Calculate the total number of rows in the resulting matrix
        for (MatrixMul.MatrixMultiplicationReply subMatrix : subMatrices) {
            totalRows += subMatrix.getResult().getRows();
        }

        MatrixMul.Matrix.Builder builder = MatrixMul.Matrix.newBuilder();
        builder.setRows(totalRows);
        builder.setCols(cols);

        // Add the data from each submatrix in order
        for (MatrixMul.MatrixMultiplicationReply subMatrix : subMatrices) {
            for (int r = 0; r < subMatrix.getResult().getRows(); r++) {
                for (int c = 0; c < cols; c++) {
                    builder.addData(subMatrix.getResult().getData(r * cols + c));
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


        MatrixMul.Matrix matrixA = generateRandomMatrix(5, 4);
        MatrixMul.Matrix matrixB = generateRandomMatrix(4, 4);

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
        System.out.println("\n");

        System.out.println("Replies from the servers:");
        for(MatrixMul.MatrixMultiplicationReply reply : replies) {
            System.out.println(reply.getResult().getDataList());
        }
        System.out.println("\n");

        MatrixMul.Matrix result = mergeSubMatrices(replies);

        System.out.println("The merged result matrix is: ");
        printMatrix(result);

    }
}
