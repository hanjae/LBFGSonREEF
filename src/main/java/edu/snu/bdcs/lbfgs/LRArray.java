/**
 * Copyright (C) 2014 Microsoft Corporation
 *
 * using https://github.com/Microsoft-CISL/shimoga
 * DenseVector.java
 */

package edu.snu.bdcs.lbfgs;

import java.io.Serializable;

/**
 * Created by Jaehyun on 2014-10-17.
 */
public class LRArray implements Serializable{

    private static final long serialVersionUID = 1L;
    private final double[] values;

    public LRArray(final int size) {
        this(new double[size]);
    }
    public LRArray(final double[] theValues) {
        this.values = theValues;
    }


    public int size() {
        return this.values.length;
    }

    public double get(final int i) {
        return this.values[i];
    }

    public void set(final int i, final double v) {
        this.values[i] = v;
    }
}
