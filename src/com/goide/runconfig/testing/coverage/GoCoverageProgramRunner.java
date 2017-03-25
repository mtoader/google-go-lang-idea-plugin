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

package com.goide.runconfig.testing.coverage;

import com.goide.GoConstants;
import com.goide.runconfig.testing.GoTestRunConfiguration;
import com.goide.runconfig.testing.GoTestRunningState;
import com.goide.sdk.GoSdkUtil;
import com.goide.util.GoExecutor;
import com.intellij.coverage.CoverageExecutor;
import com.intellij.coverage.CoverageHelper;
import com.intellij.coverage.CoverageRunnerData;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.ConfigurationInfoProvider;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.configurations.coverage.CoverageEnabledConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.runners.RunContentBuilder;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.notification.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GoCoverageProgramRunner extends GenericProgramRunner {
  private class CoveragePatcher extends GoTestRunningState.ExecutorPatcher {
    private boolean isRecursiveCoverage;
    private GoTestRunConfiguration myConfiguration;
    private CoverageEnabledConfiguration myCoverageConfiguration;
    private VirtualFile myPackageCoverageExecutable;

    public CoveragePatcher(CoverageEnabledConfiguration coverageConfiguration, GoTestRunConfiguration configuration) {
      super(configuration);
      myConfiguration = configuration;
      myCoverageConfiguration = coverageConfiguration;
      myPackageCoverageExecutable = GoSdkUtil.findExecutableInGoPath(
          "package-coverage",
          myConfiguration.getProject(),
          myConfiguration.getConfigurationModule().getModule());
    }

    @Override
    public boolean canRecursiveCoverage() {
      return hasPackageCoverageExecutable();
    }

    private boolean hasPackageCoverageExecutable() {
      return myPackageCoverageExecutable != null;
    }

    @Override
    public void beforeTarget(@NotNull GoExecutor executor) {
      if (hasPackageCoverageExecutable()) {
        executor.withExePath(myPackageCoverageExecutable.getPath());
        executor.withParameters("-p", "-q=false", "-c");
      }
      else {
        super.beforeTarget(executor);
      }
    }

    @Override
    public void afterTarget(@NotNull GoExecutor executor) {
      if (hasPackageCoverageExecutable()) {
        executor.withParameters("--", "-v");
        executor.withParameterString(myConfiguration.getGoToolParams());
      }
      else {
        executor.withParameters("-coverprofile=" + myCoverageConfiguration.getCoverageFilePath(), "-covermode=atomic");
      }
    }
  }

  @NotNull
  @Override
  public String getRunnerId() {
    return ID;
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return executorId.equals(CoverageExecutor.EXECUTOR_ID) && profile instanceof GoTestRunConfiguration;
  }

  @Override
  public RunnerSettings createConfigurationData(ConfigurationInfoProvider settingsProvider) {
    return new CoverageRunnerData();
  }

  @Nullable
  @Override
  protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment)
    throws ExecutionException {
    assert state instanceof GoTestRunningState;
    GoTestRunningState runningState = (GoTestRunningState)state;
    GoTestRunConfiguration runConfiguration = ObjectUtils.tryCast(environment.getRunProfile(), GoTestRunConfiguration.class);
    if (runConfiguration == null) {
      return null;
    }
    FileDocumentManager.getInstance().saveAllDocuments();
    CoverageEnabledConfiguration coverageEnabledConfiguration = CoverageEnabledConfiguration.getOrCreate(runConfiguration);
    CoveragePatcher runningStatePatcher = new CoveragePatcher(coverageEnabledConfiguration, runConfiguration);
    runningState.setPatcher(runningStatePatcher);

    if (runConfiguration.getKind() == GoTestRunConfiguration.Kind.DIRECTORY &&
        !runningStatePatcher.hasPackageCoverageExecutable()) {
      Notification notification = GoConstants.GO_NOTIFICATION_GROUP.createNotification(
          "Recursive Directory Coverage",
          "Directory coverage can be computed recursively if the <code>corsc/go-tools/package-coverage</code> package is installed on this computer",
          NotificationType.INFORMATION,
          null);
      Notifications.Bus.notify(notification, null);
    }

    ExecutionResult executionResult = state.execute(environment.getExecutor(), this);
    if (executionResult == null) {
      return null;
    }
    executionResult.getProcessHandler().addProcessListener(new GoCoverageProcessListener(
        runConfiguration.getDirectoryPath(),
        coverageEnabledConfiguration.getCoverageFilePath()));
    CoverageHelper.attachToProcess(runConfiguration, executionResult.getProcessHandler(), environment.getRunnerSettings());
    return new RunContentBuilder(executionResult, environment).showRunContent(environment.getContentToReuse());
  }
}
