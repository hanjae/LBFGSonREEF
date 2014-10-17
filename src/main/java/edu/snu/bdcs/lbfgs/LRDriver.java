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

import com.microsoft.reef.driver.context.ActiveContext;
import com.microsoft.reef.driver.context.ContextConfiguration;
import com.microsoft.reef.driver.evaluator.AllocatedEvaluator;
import com.microsoft.reef.driver.evaluator.EvaluatorRequest;
import com.microsoft.reef.driver.evaluator.EvaluatorRequestor;
import com.microsoft.reef.driver.task.CompletedTask;
import com.microsoft.reef.driver.task.RunningTask;
import com.microsoft.reef.driver.task.TaskConfiguration;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;
import com.microsoft.tang.annotations.Parameter;
import com.microsoft.tang.annotations.Unit;
import com.microsoft.tang.exceptions.BindException;
import com.microsoft.wake.EventHandler;
import com.microsoft.wake.impl.BlockingEventHandler;
import com.microsoft.wake.time.event.StartTime;

import javax.inject.Inject;
import javax.naming.Context;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Jaehyun on 2014-10-17.
 */
@Unit
public class LRDriver {

    private static final Logger LOG = Logger.getLogger(HelloDriver.class.getName());


    private final int computeTasks;

    private static final int controllerTasks = 1;

    private final AtomicInteger compTasksRunning = new AtomicInteger(0);

    private final LRTaskSubmitter taskSubmitter;

    private final BlockingEventHandler<ActiveContext> contextAccumulator;

    private final EvaluatorRequestor requestor;

    public static class Parameters {
        @NamedParameter(default_value = "5", doc = "The number of compute tasks to spawn")
        public static class ComputeTasks implements Name<Integer> {
        }

        @NamedParameter(default_value = "5678", doc = "Port on which Name Service should listen")
        public static class NameServicePort implements Name<Integer> {
        }
    }

    /**
     * Job driver constructor - instantiated via TANG.
     * @param requestor evaluator requestor object used to create new evaluator containers.
     */
    @Inject
    public LRDriver(
            final EvaluatorRequestor requestor,
            final @Parameter(Parameters.ComputeTasks.class) int computeTasks,
            final @Parameter(Parameters.NameServicePort.class) int nameServicePort) {
        this.requestor = requestor;

        this.computeTasks = computeTasks;
        this.taskSubmitter = new LRTaskSubmitter(this.computeTasks, nameServicePort);
        this.contextAccumulator = new BlockingEventHandler<>(this.computeTasks+ this.controllerTasks, this.taskSubmitter);
    }

    /**
     * Handles the StartTime event: Request as single Evaluator.
     */
    public final class StartHandler implements EventHandler<StartTime> {
        @Override
        public void onNext(final StartTime startTime) {
            LOG.log(Level.INFO, "Requested Evaluator.");

            LRDriver.this.requestor.submit(EvaluatorRequest.newBuilder()
                    .setMemory(128)
                    .setNumber(computeTasks + controllerTasks)
                    .build());
        }
    }

    /**
     * Handles AllocatedEvaluator: Build and Context & Task Configuration
     * and submit them to the Driver
     */
    public final class EvaluatorAllocatedHandler implements EventHandler<AllocatedEvaluator> {
        @Override
        public void onNext(final AllocatedEvaluator allocatedEvaluator) {
            LOG.log(Level.INFO, "Submitting LogisticRegression task to AllocatedEvaluator: {0}", allocatedEvaluator);
            try {
                final Configuration contextConfiguration = ContextConfiguration.CONF
                        .set(ContextConfiguration.IDENTIFIER, "LRContext").build();

                // Let's submit context and task to the evaluator
                //allocatedEvaluator.submitContextAndTask(contextConfiguration, taskConfiguration);
                allocatedEvaluator.submitContext(contextConfiguration);
            } catch (final BindException ex) {
                throw new RuntimeException("Unable to setup Task or Context configuration.", ex);
            }
        }
    }

    public final class TaskRunningHandler implements EventHandler<RunningTask> {
        @Override
        public final void onNext(final RunningTask task) {
            LOG.log(Level.INFO, "Task \"{0}\" is running!", task.getId());
            if (compTasksRunning.incrementAndGet() == computeTasks) {
                // All compute tasks are running - launch controller task
                taskSubmitter.submitControlTask();
            }
        }
    }
    public final class ContextActiveHandler implements EventHandler<ActiveContext> {
        @Override
        public void onNext(final ActiveContext activeContext) {
            LOG.log(Level.INFO, "Received a RunningEvaluator with ID: {0}", activeContext.getId());
            contextAccumulator.onNext(activeContext);
        }
    }

    final class TaskCompletedHandler implements EventHandler<CompletedTask> {
        @Override
        @SuppressWarnings("ConvertToTryWithResources")
        public final void onNext(final CompletedTask completed) {
            LOG.log(Level.INFO, "Task {0} is done.", completed.getId());
            if (taskSubmitter.controllerCompleted(completed.getId())) {
                // Get results from controller
                System.out.println("****************** RESULT ******************");
//                System.out.println(new String(completed.get()));
                System.out.println("********************************************");
            }
            final ActiveContext context = completed.getActiveContext();
            LOG.log(Level.INFO, "Releasing Context {0}.", context.getId());
            context.close();
        }
    }
}
