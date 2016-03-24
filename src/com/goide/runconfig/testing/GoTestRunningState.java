/*
 * Copyright 2013-2016 Sergey Ignatov, Alexander Zolotov, Florin Patan
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

package com.goide.runconfig.testing;

import com.goide.psi.GoFile;
import com.goide.psi.GoFunctionDeclaration;
import com.goide.runconfig.GoConsoleFilter;
import com.goide.runconfig.GoRunningState;
import com.goide.util.GoExecutor;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.testframework.AbstractTestProxy;
import com.intellij.execution.testframework.TestFrameworkRunningModel;
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction;
import com.intellij.execution.testframework.autotest.ToggleAutoTestAction;
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil;
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Getter;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Function;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class GoTestRunningState extends GoRunningState<GoTestRunConfiguration> {
  private String myCoverageFilePath;
  private String myFailedTestsPattern;

  public GoTestRunningState(@NotNull ExecutionEnvironment env, @NotNull Module module, @NotNull GoTestRunConfiguration configuration) {
    super(env, module, configuration);
  }

  @NotNull
  @Override
  public ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
    ProcessHandler processHandler = startProcess();
    TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(myConfiguration.getProject());
    setConsoleBuilder(consoleBuilder);

    GoTestConsoleProperties consoleProperties = new GoTestConsoleProperties(myConfiguration, executor);
    // todo: replace with simple create console
    ConsoleView consoleView = SMTestRunnerConnectionUtil.createConsoleWithCustomLocator(myConfiguration.getTestFramework().getName(),
                                                                                        consoleProperties, getEnvironment(),
                                                                                        new GoTestLocationProvider());
    consoleView.attachToProcess(processHandler);
    consoleView.addMessageFilter(new GoConsoleFilter(myConfiguration.getProject(), myModule, myConfiguration.getWorkingDirectoryUrl()));

    DefaultExecutionResult executionResult = new DefaultExecutionResult(consoleView, processHandler);
    AbstractRerunFailedTestsAction rerunFailedTestsAction = consoleProperties.createRerunFailedTestsAction(consoleView);
    if (rerunFailedTestsAction != null) {
      rerunFailedTestsAction.setModelProvider(new Getter<TestFrameworkRunningModel>() {
        @Override
        public TestFrameworkRunningModel get() {
          return ((SMTRunnerConsoleView)consoleView).getResultsViewer();
        }
      });
      executionResult.setRestartActions(rerunFailedTestsAction, new ToggleAutoTestAction());
    }
    else {
      executionResult.setRestartActions(new ToggleAutoTestAction());
    }
    return executionResult;
  }

  @Override
  protected GoExecutor patchExecutor(@NotNull GoExecutor executor) throws ExecutionException {
    ParametersList buildFlags = new ParametersList();
    boolean isDebug = isDebug();
    if (!isDebug) {
      executor.withParameters("test", "-v");
      executor.withParameterString(myConfiguration.getGoToolParams());
    }
    else if (!myConfiguration.getGoToolParams().isEmpty()) {
      buildFlags.addAll(myConfiguration.getGoToolParams());
    }

    String testTarget = getTestTarget(myConfiguration);
    if (testTarget.isEmpty()) throw new ExecutionException("Invalid run configuration");

    switch (myConfiguration.getKind()) {
      case DIRECTORY:
        String relativePath = FileUtil.getRelativePath(myConfiguration.getWorkingDirectory(),
                                                       myConfiguration.getDirectoryPath(),
                                                       File.separatorChar);
        // TODO Once Go gets support for covering multiple packages the ternary condition should be reverted
        // See https://golang.org/issues/6909
        if (relativePath == null) {
          executor.withWorkDirectory(myConfiguration.getDirectoryPath());
        }
        addFilterParameter(executor, ObjectUtils.notNull(myFailedTestsPattern, myConfiguration.getPattern()));
        break;
      case PACKAGE:
        addFilterParameter(executor, ObjectUtils.notNull(myFailedTestsPattern, myConfiguration.getPattern()));
        break;
      case FILE:
        String filePath = myConfiguration.getFilePath();
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath);
        if (virtualFile == null) {
          throw new ExecutionException("Test file doesn't exist");
        }
        PsiFile file = PsiManager.getInstance(myConfiguration.getProject()).findFile(virtualFile);
        if (file == null || !GoTestFinder.isTestFile(file)) {
          throw new ExecutionException("File '" + filePath + "' is not test file");
        }

        String importPath = ((GoFile)file).getImportPath(false);
        if (StringUtil.isEmpty(importPath)) {
          throw new ExecutionException("Cannot find import path for " + filePath);
        }

        executor.withParameters(importPath);
        addFilterParameter(executor, myFailedTestsPattern != null ? myFailedTestsPattern : buildFilterPatternForFile((GoFile)file));
        break;
    }

    if (isDebug) {
      File dlv = dlv();
      executor.withExePath(dlv.getAbsolutePath())
        .withDebuggerParameters("--listen=localhost:" + myDebugPort, "--headless=true", "exec", myOutputFilePath, "--", "-test.v");
      return executor;
    }

    if (myCoverageFilePath != null) {
      executor.withParameters("-coverprofile=" + myCoverageFilePath, "-covermode=atomic");
      executor.withParameters(buildFlags.getArray());
    }

    return executor;
  }

  @NotNull
  protected String buildFilterPatternForFile(GoFile file) {
    Collection<String> testNames = ContainerUtil.newLinkedHashSet();
    for (GoFunctionDeclaration function : file.getFunctions()) {
      ContainerUtil.addIfNotNull(testNames, GoTestFinder.isTestOrExampleFunction(function) ? function.getName() : null);
    }
    return "^" + StringUtil.join(testNames, "|") + "$";
  }

  protected void addFilterParameter(@NotNull GoExecutor executor, String pattern) {
    if (StringUtil.isNotEmpty(pattern)) {
      String run = "-run";
      if (isDebug()) run = "-test.run";
      executor.withParameters(run + "='" + pattern + "'");
    }
  }

  public void setCoverageFilePath(@Nullable String coverageFile) {
    myCoverageFilePath = coverageFile;
  }

  public void setFailedTests(@NotNull List<AbstractTestProxy> failedTests) {
    myFailedTestsPattern = "^" + StringUtil.join(failedTests, new Function<AbstractTestProxy, String>() {
      @Override
      public String fun(AbstractTestProxy proxy) {
        return proxy.getName();
      }
    }, "|") + "$";
  }

  public String getTestTarget(GoTestRunConfiguration configuration) {
    switch (configuration.getKind()) {
      case DIRECTORY:
        String relativePath = FileUtil.getRelativePath(configuration.getWorkingDirectory(),
                                                       configuration.getDirectoryPath(),
                                                       File.separatorChar);
        // TODO Once Go gets support for covering multiple packages the ternary condition should be reverted
        // See https://golang.org/issues/6909
        String pathSuffix = myCoverageFilePath == null ? "..." : ".";
        return relativePath != null ? "./" + relativePath + "/" + pathSuffix : "./" + pathSuffix;
      case PACKAGE:
        return configuration.getPackage();
      case FILE:
        String filePath = configuration.getFilePath();
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath);
        if (virtualFile == null) return "";
        PsiFile file = PsiManager.getInstance(configuration.getProject()).findFile(virtualFile);
        if (file == null || !GoTestFinder.isTestFile(file)) return "";

        return ((GoFile)file).getImportPath(false);
      default:
        return "";
    }
  }
}
