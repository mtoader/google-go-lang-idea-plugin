/*
 * Copyright 2013-2015 Sergey Ignatov, Alexander Zolotov, Florin Patan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.goide.runconfig;

import com.goide.GoEnvironmentUtil;
import com.goide.dlv.DlvDebugProcess;
import com.goide.dlv.DlvRemoteVmConnection;
import com.goide.runconfig.application.GoApplicationConfiguration;
import com.goide.runconfig.application.GoApplicationRunningState;
import com.goide.runconfig.testing.GoTestRunConfiguration;
import com.goide.runconfig.testing.GoTestRunningState;
import com.goide.util.GoExecutor;
import com.goide.util.GoHistoryProcessListener;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.RunProfileStarter;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.runners.AsyncGenericProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.RunContentBuilder;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.internal.statistic.UsageTrigger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.net.NetUtils;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.debugger.connection.RemoteVmConnection;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class GoBuildingRunner extends AsyncGenericProgramRunner {
  private static final String ID = "GoBuildingRunner";

  @NotNull
  @Override
  public String getRunnerId() {
    return ID;
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    if (profile instanceof GoApplicationConfiguration) {
      return DefaultRunExecutor.EXECUTOR_ID.equals(executorId)
             || DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) && !DlvDebugProcess.IS_DLV_DISABLED;
    }
    if (profile instanceof GoTestRunConfiguration) {
      return DefaultRunExecutor.EXECUTOR_ID.equals(executorId);
    }
    return false;
  }

  @NotNull
  @Override
  protected Promise<RunProfileStarter> prepare(@NotNull ExecutionEnvironment environment, @NotNull final RunProfileState state)
    throws ExecutionException {
    FileDocumentManager.getInstance().saveAllDocuments();

    if (!(state instanceof GoRunningState)) {
      throw new ExecutionException("Run state is not a valid GoRunningState");
    }

    final AsyncPromise<RunProfileStarter> buildingPromise = new AsyncPromise<RunProfileStarter>();
    final GoHistoryProcessListener historyProcessListener = new GoHistoryProcessListener();

    String goToolCommand;
    File outputFile;
    String target;

    if (state instanceof GoApplicationRunningState) {
      outputFile = getOutputFile(environment, (GoApplicationRunningState)state);
      target = ((GoApplicationRunningState)state).getTarget();
      goToolCommand = "build";
    }
    else if (state instanceof GoTestRunningState) {
      goToolCommand = "test";
      target = ((GoTestRunningState)state).getTestTarget(((GoTestRunningState)state).getConfiguration());
      outputFile = getOutputFile(environment, (GoTestRunningState)state);
    }
    else {
      throw new ExecutionException("Invalid running state");
    }

    final File finalOutputFile = outputFile;
    GoExecutor executor = ((GoRunningState)state)
      .createCommonExecutor()
      .withParameters(goToolCommand);

    if (state instanceof GoApplicationRunningState) {
      executor.withParameterString(((GoApplicationRunningState)state).getGoBuildParams());
    }

    executor.withParameters("-o", outputFile.getAbsolutePath());

    if (state instanceof GoTestRunningState) {
      executor.withParameterString("-c");
    }
    else if (state instanceof GoApplicationRunningState && ((GoApplicationRunningState)state).isDebug()) {
      executor
        .withParameters("-gcflags", "-N -l");
    }


    executor
      .withParameters(target)
      .showNotifications(true)
      .showOutputOnError()
      .disablePty()
      .withPresentableName("go " + goToolCommand)
      .withProcessListener(historyProcessListener);

    executor.withProcessListener(new ProcessAdapter() {
      @Override
      public void processTerminated(ProcessEvent event) {
        super.processTerminated(event);
        if (event.getExitCode() != 0) {
          buildingPromise.setResult(null);
          buildingPromise.setError(new ExecutionException(event.getText()));
          return;
        }

        if (state instanceof GoApplicationRunningState && ((GoApplicationRunningState)state).isDebug()) {
          buildingPromise.setResult(new MyDebugStarter(finalOutputFile.getAbsolutePath(), historyProcessListener));
        }
        else {
          buildingPromise.setResult(new MyRunStarter(finalOutputFile.getAbsolutePath(), historyProcessListener));
        }
      }
    });

    executor.executeWithProgress(false);
    return buildingPromise;
  }

  @NotNull
  private static File getOutputFile(@NotNull ExecutionEnvironment environment, @NotNull GoRunningState state)
    throws ExecutionException {
    final File outputFile;
    String outputDirectoryPath = state.getConfiguration().getOutputFilePath();
    RunnerAndConfigurationSettings settings = environment.getRunnerAndConfigurationSettings();
    String configurationName = settings != null ? settings.getName() : "application";
    if (StringUtil.isEmpty(outputDirectoryPath)) {
      try {
        outputFile = FileUtil.createTempFile(configurationName, "go", true);
      }
      catch (IOException e) {
        throw new ExecutionException("Cannot create temporary output file", e);
      }
    }
    else {
      File outputDirectory = new File(outputDirectoryPath);
      if (outputDirectory.isDirectory() || !outputDirectory.exists() && outputDirectory.mkdirs()) {
        outputFile = new File(outputDirectoryPath, GoEnvironmentUtil.getBinaryFileNameForPath(configurationName));
        try {
          if (!outputFile.exists() && !outputFile.createNewFile()) {
            throw new ExecutionException("Cannot create output file " + outputFile.getAbsolutePath());
          }
        }
        catch (IOException e) {
          throw new ExecutionException("Cannot create output file " + outputFile.getAbsolutePath());
        }
      }
      else {
        throw new ExecutionException("Cannot create output file in " + outputDirectory.getAbsolutePath());
      }
    }
    if (!prepareFile(outputFile)) {
      throw new ExecutionException("Cannot make temporary file executable " + outputFile.getAbsolutePath());
    }
    return outputFile;
  }

  private static boolean prepareFile(@NotNull File file) {
    try {
      FileUtil.writeToFile(file, new byte[]{0x7F, 'E', 'L', 'F'});
    }
    catch (IOException e) {
      return false;
    }
    return file.setExecutable(true);
  }

  private class MyDebugStarter extends RunProfileStarter {
    private final String myOutputFilePath;
    private final GoHistoryProcessListener myHistoryProcessListener;


    private MyDebugStarter(@NotNull String outputFilePath, @NotNull GoHistoryProcessListener historyProcessListener) {
      myOutputFilePath = outputFilePath;
      myHistoryProcessListener = historyProcessListener;
    }

    @Nullable
    @Override
    public RunContentDescriptor execute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment env)
      throws ExecutionException {
      FileDocumentManager.getInstance().saveAllDocuments();
      if (state instanceof GoApplicationRunningState) {
        ((GoApplicationRunningState)state).setHistoryProcessHandler(myHistoryProcessListener);
        ((GoApplicationRunningState)state).setOutputFilePath(myOutputFilePath);

        final int port = findFreePort();
        ((GoApplicationRunningState)state).setDebugPort(port);

        // start debugger
        final ExecutionResult executionResult = state.execute(env.getExecutor(), GoBuildingRunner.this);
        if (executionResult == null) throw new ExecutionException("Cannot run debugger");

        UsageTrigger.trigger("go.dlv.debugger");

        return XDebuggerManager.getInstance(env.getProject()).startSession(env, new XDebugProcessStarter() {
          @NotNull
          @Override
          public XDebugProcess start(@NotNull XDebugSession session) throws ExecutionException {
            RemoteVmConnection connection = new DlvRemoteVmConnection();
            DlvDebugProcess process = new DlvDebugProcess(session, connection, executionResult);
            connection.open(new InetSocketAddress(NetUtils.getLoopbackAddress(), port));
            return process;
          }
        }).getRunContentDescriptor();
      }
      return null;
    }
  }

  private class MyRunStarter extends RunProfileStarter {
    private final String myOutputFilePath;
    private final GoHistoryProcessListener myHistoryProcessListener;


    private MyRunStarter(@NotNull String outputFilePath, @NotNull GoHistoryProcessListener historyProcessListener) {
      myOutputFilePath = outputFilePath;
      myHistoryProcessListener = historyProcessListener;
    }

    @Nullable
    @Override
    public RunContentDescriptor execute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment env)
      throws ExecutionException {
      FileDocumentManager.getInstance().saveAllDocuments();
      if (state instanceof GoApplicationRunningState) {
        ((GoApplicationRunningState)state).setHistoryProcessHandler(myHistoryProcessListener);
        ((GoApplicationRunningState)state).setOutputFilePath(myOutputFilePath);
      }
      else if (state instanceof GoTestRunningState) {
        ((GoTestRunningState)state).setHistoryProcessHandler(myHistoryProcessListener);
        ((GoTestRunningState)state).setOutputFilePath(myOutputFilePath);
      }
      else {
        return null;
      }
      ExecutionResult executionResult = state.execute(env.getExecutor(), GoBuildingRunner.this);
      return executionResult != null ? new RunContentBuilder(executionResult, env).showRunContent(env.getContentToReuse()) : null;
    }
  }

  private static int findFreePort() {
    ServerSocket socket = null;
    try {
      //noinspection SocketOpenedButNotSafelyClosed
      socket = new ServerSocket(0);
      socket.setReuseAddress(true);
      return socket.getLocalPort();
    }
    catch (Exception ignore) {
    }
    finally {
      if (socket != null) {
        try {
          socket.close();
        }
        catch (Exception ignore) {
        }
      }
    }
    throw new IllegalStateException("Could not find a free TCP/IP port to start dlv");
  }
}
