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

import com.microsoft.reef.client.DriverConfiguration;
import com.microsoft.reef.client.DriverLauncher;
import com.microsoft.reef.client.LauncherStatus;
import com.microsoft.reef.runtime.local.client.LocalRuntimeConfiguration;
import com.microsoft.reef.util.EnvironmentUtils;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.exceptions.BindException;
import com.microsoft.tang.exceptions.InjectionException;

import edu.snu.bdcs.lbfgs.HelloDriver;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Client for Hello REEF example.
 */
public final class HelloREEF {

  private static final Logger LOG = Logger.getLogger(HelloREEF.class.getName());

  /**
   * Number of milliseconds to wait for the job to complete.
   */
  private static final int JOB_TIMEOUT = 10000; // 10 sec.

  /**
   * @return the configuration of the HelloREEF driver.
   */
  public static Configuration getDriverConfiguration() {
    final Configuration driverConfiguration = DriverConfiguration.CONF
    		.set(DriverConfiguration.DRIVER_IDENTIFIER, "Hello")
    		.set(DriverConfiguration.GLOBAL_LIBRARIES, EnvironmentUtils.getClassLocation(HelloDriver.class))
    		.set(DriverConfiguration.ON_DRIVER_STARTED, HelloDriver.StartHandler.class)
    		.set(DriverConfiguration.ON_EVALUATOR_ALLOCATED, HelloDriver.EvaluatorAllocatedHandler.class)
    		.build();

    // TODO : build DriverConfiguration
    return driverConfiguration;
  }

  public static LauncherStatus runHelloReef(final Configuration runtimeConf, final int timeOut)
    throws BindException, InjectionException {
    final Configuration driverConf = getDriverConfiguration();

    // DriverLauncher launches Driver to run the application.
    return DriverLauncher.getLauncher(runtimeConf).run(driverConf, timeOut);
  }

  /**
   * Start Hello REEF job. Runs method runHelloReef().
   *
   * @param args command line parameters.
   * @throws com.microsoft.tang.exceptions.BindException      configuration error.
   * @throws com.microsoft.tang.exceptions.InjectionException configuration error.
   */
  public static void main(final String[] args) throws BindException, InjectionException {
    // TODO : build LocalRuntimeConfiguration
    final Configuration runtimeConfiguration = LocalRuntimeConfiguration.CONF
    		.set(LocalRuntimeConfiguration.NUMBER_OF_THREADS, 6).build();

    final LauncherStatus status = runHelloReef(runtimeConfiguration, JOB_TIMEOUT);
    LOG.log(Level.INFO, "REEF job completed: {0}", status);
  }
}