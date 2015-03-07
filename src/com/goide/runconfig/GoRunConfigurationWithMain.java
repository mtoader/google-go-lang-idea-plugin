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
import com.goide.psi.GoFunctionDeclaration;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;


public abstract class GoRunConfigurationWithMain<T extends GoRunningState> extends GoRunConfigurationBase<T> {
  private static final String FILE_PATH_NAME = "file_path";
  
  public GoRunConfigurationWithMain(String name, GoModuleBasedConfiguration configurationModule, ConfigurationFactory factory) {
    super(name, configurationModule, factory);
    setFilePath(getWorkingDirectory());
  }

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    String filePathValue = JDOMExternalizerUtil.getFirstChildValueAttribute(element, FILE_PATH_NAME);
    if (filePathValue != null) {
      setFilePath(filePathValue);
    }
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    super.writeExternal(element);
    if (StringUtil.isNotEmpty(getFilePath())) {
      JDOMExternalizerUtil.addElementWithValueAttribute(element, FILE_PATH_NAME, getFilePath());
    }
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    super.checkConfiguration();

    VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(VfsUtilCore.pathToUrl(getFilePath()));
    if (file == null) {
      throw new RuntimeConfigurationError("Main file is not specified");
    }
    PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);
    if (psiFile == null || !(psiFile instanceof GoFile)) {
      throw new RuntimeConfigurationError("Main file is invalid");
    }
    if (!GoConstants.MAIN.equals(((GoFile)psiFile).getPackageName())) {
      throw new RuntimeConfigurationError("Main file has non-main package");
    }
    GoFunctionDeclaration mainFunction = ((GoFile)psiFile).findMainFunction();
    if (mainFunction == null) {
      throw new RuntimeConfigurationError("Main file doesn't contain main function");
    }
  }

}
