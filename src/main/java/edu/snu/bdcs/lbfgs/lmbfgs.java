/*
 *      Limited memory BFGS (L-BFGS).
 *
 * Copyright (c) 1990, Jorge Nocedal
 * Copyright (c) 2007-2010 Naoaki Okazaki
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.snu.bdcs.lbfgs;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.Reference;
import java.util.Scanner;

/**
 * Created by Jaehyun on 2014-10-12.
 */
public class lmbfgs {

    private static final int NUM_EPOCHS = 10000;
    private static final double LEARNING_RATE = 0.000001;
    private int num_features;
    private int num_data;

    private double[][] x;
    private double[] y;
    // Theta
    private double[] weights;


    public class iteration_data {
        public double[] alpha;
        public double[] s;    /* [n] */
        public double[] y;    /* [n] */
        public double ys;     /* vecdot(y, x) */
    }

    // utils

    private void veccpy(double[] y, final double[] x, final int n)
    {
        int i;
        for (i = 0; i < n; i++) {
            y[i] = x[i];
        }
    }
    private void vecncpy(double[] y, final double[] x, final int n) {
        int i;
        for (i = 0; i < n; i++) {
            y[i] = -x[i];
        }
    }
    private void vecadd(double[] y, final double[] x, final double c, final int n)
    {
        int i;
        for (i = 0; i < n; i++) {
            y[i] += c * x[i];
        }
    }

    private void vecdiff(double[] z, final double[] x, final double[] y, final int n)
    {
        int i;
        for (i = 0; i < n ; i++) {
            z[i] = x[i] - y[i];
        }
    }

    private void vecscale(double[] y, final double c, final int n)
    {
        int i;
        for (i = 0; i < n; i++) {
            y[i] *= c;
        }
    }

    private void vecdot(double[] s, final double[] x, final double[] y, final int n)
    {
        int i;
        s[0] = 0.;
        for (i = 0; i < n; i++) {
            s[0] += x[i] * y[i];
        }
    }

    private void vec2norm(double[] s, final double[]x, final int n)
    {
        vecdot(s, x, x, n);
        s[0] = Math.sqrt(s[0]);
    }

    private void vec2norminv(double[] s, final double[] x, final int n)
    {
        vec2norm(s, x, n);
        s[0] = 1.0 / s[0];
    }

    // check this works well
    private double sigmoid (final double[] x, double[] g, final int n) {
        int i;
        double fx = 0.0;

        for (i = 0; i < n; i += 2) {
            double t1 = 1.0 - x[i];
            double t2 = 10.0 * (x[i+1] - x[i] * x[i]);
            g[i+1] = 20.0 * t2;
            g[i] = -2.0 * (x[i] * g[i+1] + t1);
            fx += t1 * t1 + t2 * t2;
        }
        return fx;
    }

    /**
     * Process lbfgs port of lbfgs.c
     * @param n The number of variables.
     * @param x The array of variables.
     * @return ptr_fx the variable that receives the final
     *         value of the objective function for the variables.
     */
    public double process(int n, double[] x) {
        double ptr_fx;

        int ret;
        int i, j, k, ls, end, bound;
        double[] step = null;
        // The number of corrections to approximate the inverse hessian matrix.
        final int m = 6;

        double[] xp = null;
        double[] g = null;
        double[] gp = null;
        double[] d = null;
        double[] w = null;
        iteration_data[] lm;
        double[] ys = null;
        double[] yy = null;
        double[] xnorm = null;
        double[] gnorm = null;
        double[] beta = null;
        double[] fx = null;
        double rate = 0;

        /* params */
        double epsilon = 1e-5;
        double max_iterations = 10000;

        /* Allocate working space. */
        xp = new double[n];
        g = new double[n];
        gp = new double[n];
        d = new double[n];
        w = new double[n];
        // errchk

        /* Allocate size-1 array */
        step = new double[1];
        xnorm = new double[1];
        gnorm = new double[1];
        beta = new double[1];
        fx = new double[1];
        fx[0] = 0;
        ys = new double[1];
        yy = new double[1];

        lm = new iteration_data[m];
        // errchk

        for (i = 0; i < m; i++) {
            lm[i] = new iteration_data();
            lm[i].ys = 0;
            lm[i].s = new double[n];
            lm[i].y = new double[n];
            lm[i].alpha = new double[1];
            lm[i].alpha[0] = 0;
            // errchk
        }

        /* Evaluate the function value and its gradient. */
        fx[0] = sigmoid(x, g, n);

        /*
            Compute the direction;
            we assume the initial hessian matrix H_0 as the identity matrix.
         */
        vecncpy(d, g, n);

        /*
            Make sure that the initial variables are not a minimizer.
         */
        vec2norm(xnorm, x, n);
        vec2norm(gnorm, g, n);
        if (xnorm[0] < 1.0) xnorm[0] = 1.0;
        if (gnorm[0] / xnorm[0] <= epsilon) {
            //ret = LBFGS_ALREADY_MINIMIZED;
            //goto exit;
            //TODO implement goto
        }

        /* Compute the initial step:
            step = 1.0 / sqrt(vecdot(d, d, n))
         */
        vec2norminv(step, d, n);

        k = 1;
        end = 0;
        for (;;) {
            /* Store the current position and gradient vectors. */
            veccpy(xp, x, n);
            veccpy(gp, g, n);

            /* Search for an optimal step. */
            ls = line_search_backtracking(n, x, fx, g, d, step, xp);
            if (ls < 0) {
            /* Revert to the previous point. */
                veccpy(x, xp, n);
                veccpy(g, gp, n);
                //ret = ls;
                break;
            }

            /* Compute x and g norms. */
            vec2norm(xnorm, x, n);
            vec2norm(gnorm, g, n);

            /*
                Convergence test.
                The criterion is given by the following formula:
                    |g(x)| / \max(1, |x|) < \epsilon
             */
            if (xnorm[0] < 1.0) xnorm[0] = 1.0;
            if (gnorm[0] / xnorm[0] <= epsilon) {
                /* Convergence. */
                // ret = LBFGS_SUCCESS;
                break;
            }


            // TODO hanjae max iteration
            if (max_iterations != 0 && max_iterations < k + 1) {
                /* Maximum number of iterations. */
                //ret = LBFGSERR_MAXIMUMITERATION;
                break;
            }

            /*
                Update vectors s and y:
                    s_{k+1} = x_{k+1} - x_{k} = \step * d_{k}.
                    y_{k+1} = g_{k+1} - g_{k}.
             */
            vecdiff(lm[end].s, x, xp, n);
            vecdiff(lm[end].y, g, gp, n);

            /*
                Compute scalars ys and yy:
                    ys = y^t \cdot s = 1 / \rho.
                    yy = y^t \cdot y.
                Notice that yy is used for scaling the hessian matrix H_0 (Cholesky factor).
             */
            vecdot(ys, lm[end].y, lm[end].s, n);
            vecdot(yy, lm[end].y, lm[end].y, n);
            lm[end].ys = ys[0];

            /*
                Recursive formula to compute dir = -(H \cdot g).
                    This is described in page 779 of:
                    Jorge Nocedal.
                    Updating Quasi-Newton Matrices with Limited Storage.
                    Mathematics of Computation, Vol. 35, No. 151,
                    pp. 773--782, 1980.
            */
            bound = (m <= k) ? m : k;
            k++;
            end = (end + 1) % m;

            /* Compute the steepest direction. */
            /* Compute the negative of gradients. */
            vecncpy(d, g, n);


            j = end;
            for (i = 0; i < bound; i++) {
                j = (j + m - 1) % m;    /* if (--j == -1) j = m-1; */
                /* \alpha_{j} = \rho_{j} s^{t}_{j} \cdot q_{k+1}. */
                vecdot(lm[j].alpha, lm[j].s, d, n);
                lm[j].alpha[0] /= lm[j].ys;
                /* q_{i} = q_{i+1} - \alpha_{i} y_{i}. */
                vecadd(d, lm[j].y, -lm[j].alpha[0], n);
            }

            vecscale(d, ys[0] / yy[0], n);

            for (i = 0; i < bound; i++) {
                /* \beta_{j} = \rho_{j} y^t_{j} \cdot \gamma_{i}. */
                vecdot(beta, lm[j].y, d, n);
                beta[0] /= lm[j].ys;
                /* \gamma_{i+1} = \gamma_{i} + (\alpha_{j} - \beta_{j}) s_{j}. */
                vecadd(d, lm[j].s, lm[j].alpha[0] - beta[0], n);
                j = (j + 1) % m;        /* if (++j == m) j = 0; */
            }

        /*
            Now the search direction d is ready. We try step = 1 first.
         */
            step[0] = 1.0;
        }
        // TODO implement goto
        //lbfgs_exit:
        /* Return the final value of the objective function. */
        ptr_fx = fx[0];
        return ptr_fx;
    }

    private int line_search_backtracking(
        int n,
        double[] x,
        double[] f,
        double[] g,
        double[] s,
        double[] stp,
        final double[] xp
    )
    {
        int count = 0;
        double width;
        double finit, dgtest;
        double[] dginit;
        final double dec = 0.5;
        final double ftol = 1e-4;
        final double min_step = 1e-20;
        final double max_step = 1e+20;
        final double max_linesearch = 20;

        /* Allocate size-1 array */
        dginit = new double[1];
        dginit[0] = 0;
        /* Check the input parameters for errors. */
        if (stp[0] <= 0.) {
            //return LBFGSERR_INVALIDPARAMETERS;
            return -1;
        }

        /* Compute the initial gradient in the search direction. */
        vecdot(dginit, g, s, n);

        /* Make sure that s points to a descent direction. */
        if (0 < dginit[0]) {
            //return LBFGSERR_INCREASEGRADIENT;
            return -1;
        }

        /* The initial value of the objective function. */
        finit = f[0];
        dgtest = ftol * dginit[0];

        for (;;) {
            veccpy(x, xp, n);
            vecadd(x, s, stp[0], n);

            /* Evaluate the function and gradient values. */
            //f[0] = cd->proc_evaluate(x, g, cd->n, *stp);
            f[0] = sigmoid(x, g, n);

            count++;

            if (f[0] > finit + stp[0] * dgtest) {
                width = dec;
            } else {
                /* The sufficient decrease condition (Armijo condition). */
                /* Exit with the Armijo condition. */
                return count;
            }

            if (stp[0] < min_step) {
                /* The step is the minimum value. */
                //return LBFGSERR_MINIMUMSTEP;
                return -1;
            }
            if (stp[0] > max_step) {
                /* The step is the maximum value. */
                //return LBFGSERR_MAXIMUMSTEP;
                return -1;
            }
            if (max_linesearch <= count) {
                /* Maximum number of iteration. */
                //return LBFGSERR_MAXIMUMLINESEARCH;
                return -1;
            }
            stp[0] *= width;
        }
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
        lmbfgs engine = new lmbfgs();
        engine.loadDataset();
        /*
        double[] x;
        double[] y;
        x = new double[5];
        y = new double[5];
        x[0] = 12;
        x[1] = 2;
        x[2] = 6;
        x[3] = 8;
        x[4] = 19;
        //engine.vecncpy(y, x, 5);
        y[0] = x[4];
        y[0] = 2;
        System.out.println(x[4]);
        */
        int n = 100;
        double ret;
        double[] x;
        x = new double[n];
        for (int i = 0; i < n; i += 2) {
            x[i] = -1.2;
            x[i+1] = 1.0;
        }
        ret = engine.process(n, x);
        System.out.println("ret : " + ret);

    }
}