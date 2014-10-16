package edu.snu.bdcs.lbfgs;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Created by Jaehyun on 2014-10-14.
 * Test class for simple Gradient Descent logistic regression.
 *
 * Referenced from https://github.com/econner/MachineLearning
 *
 */
public class logreg {

    private static final int NUM_EPOCHS = 10000;
    private static final double LEARNING_RATE = 0.000001;
    private static final int NUM_FEATURES = 3;


    // Theta
    private double[] weights;

    /**
     *
     * @param x
     * @return calculated Sigmoid for logistic regression : 1 / 1 + e^(-eTx)
     */
    private double sigmoid(double[] x) {
        double linearTerm = 0;
        for(int i = 0; i < x.length; i++)
        {
            linearTerm += weights[i] * x[i];
        }
        return 1.0 / (1.0 + Math.exp(-linearTerm));
    }

    private void trainLogisticRegression() {
        double[] newWeights = new double[NUM_FEATURES];

    }

    public static void main(String[] args) throws FileNotFoundException {
        // write your code here
        System.out.println("ABCD");
        Scanner scanner = new Scanner(new File("binary.csv"));
    }


}
