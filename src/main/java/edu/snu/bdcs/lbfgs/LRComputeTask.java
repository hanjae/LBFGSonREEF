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
        System.out.println(partialX.get(0).get(0) + "|" + partialX.get(0).get(1));
        reduceSender.send(partialX.get(0));
        //System.out.println(partialX.get(0) + "|" + partialX.get(1) + "|" + partialX.get(2) + "|" + partialX.get(3) + "|" + partialX.get(4) + "|");
        return null;
    }


}
