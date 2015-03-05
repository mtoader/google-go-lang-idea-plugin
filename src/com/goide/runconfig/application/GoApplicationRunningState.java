/*
 * Copyright 2013-2014 Sergey Ignatov, Alexander Zolotov
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

package com.goide.runconfig.application;

import com.goide.runconfig.GoRunningState;
import com.goide.util.GoExecutor;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class GoApplicationRunningState extends GoRunningState<GoApplicationRunConfiguration> {
  private File myTempFile;

  public GoApplicationRunningState(@NotNull ExecutionEnvironment env, @NotNull Module module,
                                   @NotNull GoApplicationRunConfiguration configuration) {
    super(env, module, configuration);
  }

  @NotNull
  @Override
  protected ProcessHandler startProcess() throws ExecutionException {
    setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(myModule.getProject()));
    try {
      myTempFile = FileUtil.createTempFile(myConfiguration.getName(), "go", true);
      //noinspection ResultOfMethodCallIgnored
      myTempFile.setExecutable(true);
    }
    catch (IOException e) {
      throw new ExecutionException("Can't create temporary output file", e);
    }
    try {
      ProcessOutput processOutput = new ProcessOutput();
      VirtualFile[] sourceRoots = ModuleRootManager.getInstance(myModule).getSourceRoots(false); //Eventually to support multiple module dependencies
      boolean success = GoExecutor.in(myModule)
        .addParameters("build", "-o", myTempFile.getAbsolutePath())
        .withWorkDirectory(sourceRoots[0].getCanonicalPath())
        .withProcessOutput(processOutput)
        .showOutputOnError()
        .execute();
      if (!success) {
        throw new ExecutionException("Build failure. `go build` is finished with exit code " + processOutput.getExitCode());
      }

      return super.startProcess();
    }
    finally {
      //noinspection ResultOfMethodCallIgnored
      myTempFile.delete();
    }
  }


  @Override
  protected GoExecutor patchExecutor(@NotNull GoExecutor executor) throws ExecutionException {
    return executor.withExePath(myTempFile.getAbsolutePath());
  }
}
