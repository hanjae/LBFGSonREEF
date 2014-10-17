/**
 * Copyright (C) 2014 Microsoft Corporation
 * From https://github.com/Microsoft-CISL/shimoga
 */
package edu.snu.bdcs.lbfgs;

import com.microsoft.reef.driver.context.ActiveContext;
import com.microsoft.reef.driver.task.TaskConfiguration;
import com.microsoft.reef.io.network.group.config.GroupOperators;
import com.microsoft.reef.io.network.impl.BindNSToTask;
import com.microsoft.reef.io.network.naming.NameServer;
import com.microsoft.reef.io.network.util.StringIdentifier;
import com.microsoft.reef.io.network.util.StringIdentifierFactory;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.JavaConfigurationBuilder;
import com.microsoft.tang.Tang;
import com.microsoft.tang.exceptions.BindException;
import com.microsoft.wake.ComparableIdentifier;
import com.microsoft.wake.EventHandler;
import com.microsoft.wake.remote.NetUtils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TaskSubmitter is responsible for submitting tasks to running evaluators.
 * <p/>
 * This is an event handler for events containing an iterable of running
 * evaluators. send, it creates the necessary structures to create the group
 * communication operator configurations and first submits the compute tasks.
 * <p/>
 * When all the compute tasks start, the driver will signal start of
 * controller through submitControlTask
 *
 * @author shravan
 */
public class LRTaskSubmitter implements EventHandler<Iterable<ActiveContext>> {

    /**
     * Standard Java logger object
     */
    private final Logger logger = Logger.getLogger(LRTaskSubmitter.class.getName());

    /**
     * The number of compute tasks
     */
    private final int numberOfComputeTasks;

    /**
     * The ids of the compute tasks
     */
    private final List<ComparableIdentifier> computeTaskIds;

    /**
     * The port numbers on which network service starts
     */
    private final List<Integer> nsPorts;

    private final StringIdentifierFactory factory = new StringIdentifierFactory();
    private final String nameServiceAddr;
    private final int nameServicePort;
    private final NameServer nameService;

    /**
     * Id of controller
     */
    private final ComparableIdentifier controllerId = (ComparableIdentifier) factory
            .getNewInstance("ControllerTask");

    /**
     * port of controller
     */
    private final int controllerPort = 7000;

    /**
     * The group communication operator configurations are managed through this
     */
    private GroupOperators operators;

    /**
     * Handle to the running evaluator that should run the controller
     */
    private ActiveContext controllerContext;

    /**
     * Constructor
     *
     * @param numberOfComputeTasks
     * @param nameServicePort
     */
    public LRTaskSubmitter(int numberOfComputeTasks, int nameServicePort) {
        this.numberOfComputeTasks = numberOfComputeTasks;
        computeTaskIds = new ArrayList<>(numberOfComputeTasks);
        nsPorts = new ArrayList<>(computeTaskIds.size());

        logger.log(Level.INFO,
                "Setting Up identifiers & ports for the network service to listen on");
        for (int i = 1; i <= numberOfComputeTasks; i++) {
            computeTaskIds.add((StringIdentifier) factory
                    .getNewInstance("ComputeTask" + i));
            nsPorts.add(controllerPort + i);
        }

        // Starting Name Service
        nameServiceAddr = NetUtils.getLocalAddress();
        this.nameServicePort = nameServicePort;
        nameService = new NameServer(nameServicePort, factory);
    }

    /**
     * We have our list of {@link computeTasks} running Set up structures
     * required for group communication
     */
    @Override
    public void onNext(Iterable<ActiveContext> contexts) {
        logger.log(Level.INFO, "All context are running");
        logger.log(Level.INFO, "Setting Up Structures for creating Group Comm Operator Configurations");

        int runnEvalCnt = -1;
        List<ActiveContext> contextList = new ArrayList<>(numberOfComputeTasks);
        Map<ComparableIdentifier, Integer> id2port = new HashMap<>();
        for (ActiveContext context : contexts) {
            if (runnEvalCnt != -1) {
                // Computer Context
                contextList.add(context);
                final String hostAddr = context.getEvaluatorDescriptor().getNodeDescriptor()
                        .getInetSocketAddress().getHostName();
//				String hostAddr = Utils.getLocalAddress();
                final int port = nsPorts.get(runnEvalCnt);
                final ComparableIdentifier compTaskId = computeTaskIds.get(runnEvalCnt);
                logger.log(Level.INFO, "Registering " + compTaskId + " with " + hostAddr + ":" + port);
                nameService.register(compTaskId, new InetSocketAddress(hostAddr,
                        port));
                id2port.put(compTaskId, port);
            } else {
                // Controller Context
                controllerContext = context;
                final String hostAddr = context.getEvaluatorDescriptor().getNodeDescriptor()
                        .getInetSocketAddress().getHostName();
//				String hostAddr = Utils.getLocalAddress();
                nameService.register(controllerId, new InetSocketAddress(
                        hostAddr, controllerPort));
                id2port.put(controllerId, controllerPort);
            }
            ++runnEvalCnt;
        }

        logger.log(Level.INFO, "Creating Operator Configs");

        operators = new GroupOperators(LRArrayCodec.class, LRArrayConcat.class,
                nameServiceAddr, nameServicePort, id2port);

        operators.addScatter().setSender(controllerId)
                .setReceivers(computeTaskIds);
        /*operators.addBroadCast().setSender(controllerId)
                .setReceivers(computeTaskIds);*/
        operators.addReduce().setReceiver(controllerId)
                .setSenders(computeTaskIds).setRedFuncClass(LRArrayConcat.class);
        // Launch ComputeTasks first
        for (int i = 0; i < contextList.size(); i++) {
            final ComparableIdentifier compTaskId = computeTaskIds.get(i);
            contextList.get(i).submitTask(getComputeTaskConfig(compTaskId));
        }
    }

    /**
     * The {@link Configuration} for a {@link ComputeTask}
     * <p/>
     * Given the task id, the {@link GroupOperators} object will get you the
     * {@link Configuration} needed for Group Communication Operators on that
     * task
     *
     * @param compTaskId
     * @return
     */
    private Configuration getComputeTaskConfig(final ComparableIdentifier compTaskId) {
        try {
            // System.out.println(ConfigurationFile.toConfigurationString(operators.getConfig(compTaskId)));
            final JavaConfigurationBuilder b = Tang.Factory.getTang().newConfigurationBuilder();
            b.addConfiguration(operators.getConfig(compTaskId));
            b.addConfiguration(TaskConfiguration.CONF
                    .set(TaskConfiguration.IDENTIFIER, compTaskId.toString())
                    .set(TaskConfiguration.TASK, LRComputeTask.class)
                    .set(TaskConfiguration.ON_TASK_STARTED, BindNSToTask.class)
                    .build());
            return b.build();
        } catch (BindException e) {
            logger.log(
                    Level.SEVERE,
                    "BindException while creating GroupCommunication operator configurations",
                    e.getCause());
            throw new RuntimeException(e);
        }
    }

    /**
     * Submits the {@link LRControllerTask} using the {@link EvaluatorRunning}
     * stored. We get the group communication configuration from
     * {@link GroupOperators} object
     */
    public void submitControlTask() {
        try {
            final JavaConfigurationBuilder b = Tang.Factory.getTang().newConfigurationBuilder();
            b.addConfiguration(operators.getConfig(controllerId));
            b.addConfiguration(TaskConfiguration.CONF
                    .set(TaskConfiguration.IDENTIFIER, controllerId.toString())
                    .set(TaskConfiguration.TASK, LRControllerTask.class)
                    .set(TaskConfiguration.ON_TASK_STARTED, BindNSToTask.class)
                    .build());
            controllerContext.submitTask(b.build());
        } catch (BindException e) {
            logger.log(
                    Level.SEVERE,
                    "BindException while creating GroupCommunication operator configurations",
                    e.getCause());
            throw new RuntimeException(e);
        }
    }

    /**
     * Check if the id of the completed task matches that of the controller
     *
     * @param id
     * @return true if it matches false otherwise
     */
    public boolean controllerCompleted(String id) {
        return factory.getNewInstance(id).equals(controllerId);
    }

}
