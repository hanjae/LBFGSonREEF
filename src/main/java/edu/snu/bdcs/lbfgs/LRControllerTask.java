/**
 * Copyright (C) 2014 Microsoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.snu.bdcs.lbfgs;

import com.microsoft.reef.task.Task;
import com.microsoft.reef.io.network.group.operators.Reduce;
import com.microsoft.reef.io.network.group.operators.Scatter;
import sun.reflect.LangReflectAccess;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Jaehyun on 2014-10-17.
 */
public class LRControllerTask implements Task {

    private final Logger logger = Logger.getLogger(LRControllerTask.class.getName());

    /**
     * The Group Communication Operators that are needed by this task. These
     * will be injected into the constructor by TANG. The operators used here
     * are complementary to the ones used in the ComputeTask
     */
    Scatter.Sender<LRArray> scatterSender;
    Reduce.Receiver<LRArray> reduceReceiver;


    // The values
    List<LRArray> X;

    /**
     * This class is instantiated by TANG
     *
     * @param scatterSender
     *            The sender for the scatter operation
     * @param reduceReceiver
     *            The receiver for the reduce operation
     */
    @Inject
    public LRControllerTask(Scatter.Sender<LRArray> scatterSender,
                          Reduce.Receiver<LRArray> reduceReceiver) {
        super();
        this.scatterSender = scatterSender;
        this.reduceReceiver = reduceReceiver;

        // For now initialize the matrix
        // TODO: Read from disk/hdfs
        int[][] matrix = {
                { 0, 1, 2, 3, 4 },
                { 5, 6, 7, 8, 9 },
                { 10, 11, 12, 13, 14 },
                { 15, 16, 17, 18, 19 },
                { 20, 21, 22, 23, 24 } };

        // Convert matrix into a list of row vectors
        X = new ArrayList<>(matrix.length);
        for (int i = 0; i < matrix.length; i++) {
            LRArray rowi = new LRArray(5);
            for (int j = 0; j < matrix[i].length; j++) {
                rowi.set(j, matrix[i][j]);
            }
            X.add(rowi);
        }
    }

    /**
     * Computes AX'
     */
    @Override
    public byte[] call(byte[] memento) throws Exception {
        // Scatter the matrix X
        logger.log(Level.FINE, "Scattering A");
        scatterSender.send(X);
        logger.log(Level.FINE, "Finished Scattering A");
        List<LRArray> result = new ArrayList<>();
/*
        LRArray sizeVec = new LRArray(1);
        sizeVec.set(0, (double) X.size());
        // TODO edit number
*/
        LRArray Ax = reduceReceiver.reduce();
        result.add(Ax);

        System.out.println(Ax.get(0) + "|" + Ax.get(1) + "|" + Ax.get(2) + "|" + Ax.get(3) + "|" + Ax.get(4) + "|");
        /*for (int i = 0; i < 5; i++) {

        }


        String resStr = resultString(A, X, result);
        return resStr.getBytes();*/
        return null;
    }

    /**
     * Construct the display string and send it to the driver
     *
     * @param A
     * @param X
     * @param result
     *            = AX'
     * @return A string indicating the matrices being multiplied and the result
     */
    /*
    private String resultString(List<Vector> A, List<Vector> X,
                                List<Vector> result) {
        StringBuilder sb = new StringBuilder();

        int[][] a = new int[A.size()][A.get(0).size()];
        for (int i = 0; i < a.length; i++) {
            Vector rowi = A.get(i);
            for (int j = 0; j < rowi.size(); j++)
                a[i][j] = (int) rowi.get(j);
        }

        int[][] x = new int[X.size()][X.get(0).size()];
        for (int j = 0; j < X.size(); j++) {
            Vector colj = X.get(j);
            for (int i = 0; i < colj.size(); i++) {
                x[i][j] = (int) colj.get(i);
            }
        }

        int[][] res = new int[result.size()][result.get(0).size()];
        for (int j = 0; j < result.size(); j++) {
            Vector colj = result.get(j);
            for (int i = 0; i < colj.size(); i++)
                res[i][j] = (int) colj.get(i);
        }

        for (int i = 0; i < a.length; i++) {
            if (i != (a.length / 2))
                sb.append(rowString(a[i]) + "       " + rowString(x[i])
                        + "         " + rowString(res[i]) + "\n");
            else
                sb.append(rowString(a[i]) + "    X  " + rowString(x[i])
                        + "    =    " + rowString(res[i]) + "\n");
        }

        return sb.toString();
    }

    private String rowString(int[] a) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            sb.append(String.format("%4d", a[i]));
            if (i < a.length - 1)
                sb.append(", ");
        }
        return sb.toString();
    }
    */

}
