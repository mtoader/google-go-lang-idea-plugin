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
import com.goide.codeInsight.imports.GoGetPackageFix;
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

import javax.swing.event.HyperlinkEvent;

public class GoCoverageProgramRunner extends GenericProgramRunner {
  private static final String ID = "GoCoverageProgramRunner";
  private static final String GO_GET_BINARY_LINK = "goGetPackageCoverageLink";
  private static final String GO_GET_BINARY_NAME = "github.com/haya14busa/goverage";
  private static final String GO_GET_BINARY = "goverage";

  private class CoveragePatcher extends GoTestRunningState.ExecutorPatcher {
    private GoTestRunConfiguration myConfiguration;
    private CoverageEnabledConfiguration myCoverageConfiguration;
    private VirtualFile myPackageCoverageExecutable;

    public CoveragePatcher(CoverageEnabledConfiguration coverageConfiguration, GoTestRunConfiguration configuration) {
      super(configuration);
      myConfiguration = configuration;
      myCoverageConfiguration = coverageConfiguration;
      myPackageCoverageExecutable = GoSdkUtil.findExecutableInGoPath(
          GO_GET_BINARY,
          myConfiguration.getProject(),
          myConfiguration.getConfigurationModule().getModule());
    }

    private boolean hasPackageCoverageExecutable() {
      return myPackageCoverageExecutable != null;
    }

    @Override
    public void beforeTarget(@NotNull GoExecutor executor) {
      if (hasPackageCoverageExecutable()) {
        executor.withExePath(myPackageCoverageExecutable.getPath());
        executor.withParameters("-v");
      }
      else {
        super.beforeTarget(executor);
      }
      executor.withParameters("-coverprofile=" + myCoverageConfiguration.getCoverageFilePath(), "-covermode=atomic");
    }

    @Override
    public void afterTarget(@NotNull GoExecutor executor) {
      executor.withParameterString(myConfiguration.getGoToolParams());
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
      promptForPackageCoverageTool(runConfiguration);
    }

    ExecutionResult executionResult = state.execute(environment.getExecutor(), this);
    if (executionResult == null) {
      return null;
    }
    CoverageHelper.attachToProcess(runConfiguration, executionResult.getProcessHandler(), environment.getRunnerSettings());
    return new RunContentBuilder(executionResult, environment).showRunContent(environment.getContentToReuse());
  }

  private void promptForPackageCoverageTool(@NotNull final GoTestRunConfiguration configuration) {
    Notification notification = GoConstants.GO_NOTIFICATION_GROUP.createNotification(
        "Recursive Directory Coverage",
        "Directory coverage can be computed recursively if the <code>" + GO_GET_BINARY_NAME + "</code> package is installed.<br><br><a href=\"" + GO_GET_BINARY_LINK + "\">Install Package Coverage</a>",
        NotificationType.INFORMATION,
        (notification1, hyperlinkEvent) -> {
          if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            String description = hyperlinkEvent.getDescription();
            if (GO_GET_BINARY_LINK.equals(description)) {
              GoGetPackageFix.applyFix(
                configuration.getProject(),
                configuration.getConfigurationModule().getModule(),
                GO_GET_BINARY_NAME,
                false);
              notification1.expire();
            }
          }
        });
    Notifications.Bus.notify(notification, configuration.getProject());
  }
}
