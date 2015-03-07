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

package com.goide.runconfig;

import com.goide.GoConstants;
import com.goide.psi.GoFile;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GoRunConfigurationProducerBase<T extends GoRunConfigurationBase> extends RunConfigurationProducer<T> implements Cloneable {
  protected GoRunConfigurationProducerBase(@NotNull ConfigurationType configurationType) {
    super(configurationType);
  }

  @Override
  protected boolean setupConfigurationFromContext(@NotNull T configuration, @NotNull ConfigurationContext context, Ref<PsiElement> sourceElement) {
    PsiFile file = getFileFromContext(context);
    if (file != null) {
      if (GoConstants.MAIN.equals(((GoFile)file).getPackageName()) && ((GoFile)file).findMainFunction() != null) {
        setConfigurationName(configuration, file);
        configuration.setFilePath(file.getVirtualFile().getPath());
        Module module = context.getModule();
        if (module != null) {
          configuration.setModule(module);
        }
        return true;
      }
    }
    return false;
  }

  protected void setConfigurationName(@NotNull T configuration, @NotNull PsiFile file) {
    configuration.setName(file.getName());
  }

  @Override
  public boolean isConfigurationFromContext(@NotNull T configuration, ConfigurationContext context) {
    GoFile file = getFileFromContext(context);
    return file != null && FileUtil.pathsEqual(configuration.getFilePath(), file.getVirtualFile().getPath());
  }

  @Nullable
  private static GoFile getFileFromContext(@Nullable ConfigurationContext context) {
    if (context == null) {
      return null;
    }
    PsiElement psiElement = context.getPsiLocation();
    if (psiElement == null || !psiElement.isValid()) {
      return null;
    }
    PsiFile file = psiElement.getContainingFile();
    return file instanceof GoFile ? (GoFile)file : null;
  }
}
