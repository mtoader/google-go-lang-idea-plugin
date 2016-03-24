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

package com.goide.runconfig.application;

import com.goide.runconfig.GoRunningState;
import com.goide.util.GoExecutor;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class GoApplicationRunningState extends GoRunningState<GoApplicationConfiguration> {

  public GoApplicationRunningState(@NotNull ExecutionEnvironment env, @NotNull Module module,
                                   @NotNull GoApplicationConfiguration configuration) {
    super(env, module, configuration);
  }

  @NotNull
  public String getTarget() {
    return myConfiguration.getKind() == GoApplicationConfiguration.Kind.PACKAGE
           ? myConfiguration.getPackage()
           : myConfiguration.getFilePath();
  }

  @Override
  protected GoExecutor patchExecutor(@NotNull GoExecutor executor) throws ExecutionException {
    if (isDebug()) {
      File dlv = dlv();
      return executor.withExePath(dlv.getAbsolutePath())
        .withParameters("--listen=localhost:" + myDebugPort, "--headless=true", "exec", myOutputFilePath, "--");
    }
    return executor.withExePath(myOutputFilePath);
  }
}
