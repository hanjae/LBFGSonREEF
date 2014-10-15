package edu.snu.bdcs.lbfgs;

/**
 * Created by Jaehyun on 2014-10-12.
 */
public class lmbfgs {
    public double ComputeGradient(double x, double y, double w, double wtx) {
        return 0.0f;
    }

    public void lmbfgs(double[] x, double[] y, double lambda) {
        double[] w; // size should be f
        double[][] wtx; // NxC

        double g = ComputeGradient(x, y, w, wtx);

    }
    public static void main(String[] args) {
        // write your code here
    }
}
