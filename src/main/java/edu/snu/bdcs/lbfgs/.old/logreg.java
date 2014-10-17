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
    private int num_features;
    private int num_data;

    private double[][] x;
    private double[] y;
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
        double[] newWeights = new double[num_features];

    }

    public void loadDataset() throws FileNotFoundException {
        Scanner scanner = new Scanner(new File("binary.csv"));
        // Skip the first line since it is header
        scanner.nextLine();
        int i = 0;
        x = new double[1000][3];
        y = new double[1000];
        while(scanner.hasNextLine()) {
            String line = scanner.nextLine();
            // parse data y,x0,x1,x2
            String[] realData = line.split(",");
            y[i] = Double.parseDouble(realData[0]);
            num_features = realData.length - 1;
            for (int j = 1; j < realData.length; j++) {
                x[i][j-1] = Double.parseDouble(realData[j]);
            }
            i++;
        }
        num_data = i;
    }
    public static void main(String[] args) throws FileNotFoundException {
        System.out.println("ABCD");
        logreg engine = new logreg();
        engine.loadDataset();
    }
}
