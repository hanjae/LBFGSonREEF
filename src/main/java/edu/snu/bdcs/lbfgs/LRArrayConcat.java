/**
 * Copyright (C) 2014 Microsoft Corporation
 *
 * using https://github.com/Microsoft-CISL/shimoga
 * VectorConcat.java
 */

package edu.snu.bdcs.lbfgs;

import com.microsoft.reef.io.network.group.operators.Reduce;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jaehyun on 2014-10-17.
 */
public class LRArrayConcat implements Reduce.ReduceFunction<LRArray> {

    @Inject
    public LRArrayConcat() {

    }

    /* simply averaging data */
    @Override
    public LRArray apply(Iterable<LRArray> elements) {
        double[] resultArr = null;
        int count = 0;
        for (LRArray element : elements) {
            if (resultArr == null) {
                resultArr = new double[element.size()];
                for (int i = 0; i < element.size(); i++) {
                    resultArr[i] = 0;
                }
            }
            for (int i = 0; i < element.size(); i++) {
                resultArr[i] += element.get(i);
            }
            count++;
        }
        for (int i = 0; i < resultArr.length; i++) {
            resultArr[i] /= count;
        }

        LRArray result = new LRArray(resultArr);
        return result;
    }
}
