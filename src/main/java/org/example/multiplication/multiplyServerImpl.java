package org.example.multiplication;

import org.example.grpc.MatrixMul;
import org.example.grpc.MatrixMultiplicationGrpc;

public class multiplyServerImpl extends MatrixMultiplicationGrpc.MatrixMultiplicationImplBase {
    public void multiply(
            MatrixMul.MatrixMultiplicationRequest request,
            io.grpc.stub.StreamObserver<MatrixMul.MatrixMultiplicationReply> responseObserver
    ) {
        MatrixMul.Matrix matrixA = request.getMatrixA();
        MatrixMul.Matrix matrixB = request.getMatrixB();

       if(matrixA.getCols() != matrixB.getRows()){
            responseObserver.onError(new IllegalArgumentException("Columns of matrix A must be equal to rows of matrix B"));
            return;
        }

        MatrixMul.Matrix result = multiplyMatrices(matrixA, matrixB);
        MatrixMul.MatrixMultiplicationReply reply = MatrixMul.MatrixMultiplicationReply.newBuilder().setResult(result).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
    private MatrixMul.Matrix multiplyMatrices(MatrixMul.Matrix matrixA, MatrixMul.Matrix matrixB){
        int rows1 = matrixA.getRows();
        int cols1 = matrixA.getCols();
        int rows2 = matrixB.getRows();
        int cols2 = matrixB.getCols();
        int[][] result = new int[rows1][cols2];
        for(int i = 0; i < rows1; i++){
            for(int j = 0; j < cols2; j++){
                for(int k = 0; k < cols1; k++){
                    result[i][j] += matrixA.getData(i*cols1 + k) * matrixB.getData(k*cols2 + j);
                }
            }
        }
        MatrixMul.Matrix.Builder reply = MatrixMul.Matrix.newBuilder()
                .setRows(rows1)
                .setCols(cols2);
        for(int i = 0; i < rows1; i++){
            for(int j = 0; j < cols2; j++){
                reply.addData(result[i][j]);
            }
        }
        return reply.build();
    }
}
