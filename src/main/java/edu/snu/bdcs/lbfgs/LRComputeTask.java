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

import javax.inject.Inject;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Created by Jaehyun on 2014-10-17.
 */
public class LRComputeTask implements Task {
    private final Logger logger = Logger.getLogger(LRComputeTask.class.getName());

    private int num_features;
    private int num_data;

    Scatter.Receiver<LRArray> scatterReceiver;
    Reduce.Sender<LRArray> reduceSender;


    @Inject
    public LRComputeTask(Scatter.Receiver<LRArray> scatterReceiver,
                       Reduce.Sender<LRArray> reduceSender) {
        super();
        this.scatterReceiver = scatterReceiver;
        this.reduceSender = reduceSender;
    }


    @Override
    public byte[] call(byte[] memento) throws Exception {
        logger.log(Level.FINE, "Waiting for scatterReceive");
        List<LRArray> partialX = scatterReceiver.receive();
        logger.log(Level.FINE, "Received: " + partialX);

        num_data = partialX.size();
        num_features = partialX.get(0).size() - 1;

        double[] data;

        data = new double[num_data*(num_features + 1)];
        int j = 0;
        for (int i = 0; i < num_data; i++) {
            for (int k = 1; k < num_features + 1; k++) {
                data[j++] = partialX.get(i).get(k); // x[0] ~ x[n]
            }
            data[j++] = partialX.get(i).get(0); // y
        }
        lmbfgs engine = new lmbfgs();

        double ret = engine.process(j, data);

        LRArray resultArr = new LRArray(num_features + 1);
        for (int i = 0; i < num_features; i++) {
            // first (num_features) data is trained coefficients
            resultArr.set(i,data[i]);
        }
        reduceSender.send(resultArr);
        String resultStr = "ComputeTask Trained : ";
        resultStr += resultArr.get(0) + " " + resultArr.get(1) + " " + resultArr.get(2);
        return resultStr.getBytes();
    }


}
