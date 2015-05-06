/*
 * Copyright 2013-2014 Sergey Ignatov, Alexander Zolotov, Mihai Toader
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

import com.goide.runconfig.GoModuleBasedConfiguration;
import com.goide.runconfig.GoRunConfigurationBase;
import com.goide.runconfig.testing.ui.GoTestRunConfigurationEditorForm;
import com.goide.sdk.GoSdkUtil;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class GoTestRunConfiguration extends GoRunConfigurationBase<GoTestRunningState> {
  private static final String PATTERN_ATTRIBUTE_NAME = "pattern";
  private static final String FILE_PATH_ATTRIBUTE_NAME = "filePath";
  private static final String DIRECTORY_ATTRIBUTE_NAME = "directory";
  private static final String PACKAGE_ATTRIBUTE_NAME = "package";
  private static final String KIND_ATTRIBUTE_NAME = "kind";

  @NotNull private String myPackage = "";
  @NotNull private String myFilePath = "";
  @NotNull private String myDirectoryPath = "";

  @NotNull private String myPattern = "";
  @NotNull private Kind myKind = Kind.DIRECTORY;

  public GoTestRunConfiguration(@NotNull Project project, String name, @NotNull ConfigurationType configurationType) {
    super(name, new GoModuleBasedConfiguration(project), configurationType.getConfigurationFactories()[0]);
  }

  @NotNull
  @Override
  protected ModuleBasedConfiguration createInstance() {
    return new GoTestRunConfiguration(getProject(), getName(), GoTestRunConfigurationType.getInstance());
  }

  @NotNull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new GoTestRunConfigurationEditorForm(getProject());
  }

  @NotNull
  @Override
  protected GoTestRunningState newRunningState(@NotNull ExecutionEnvironment env, @NotNull Module module) {
    return new GoTestRunningState(env, module, this);
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    super.checkConfiguration();
    GoModuleBasedConfiguration configurationModule = getConfigurationModule();
    switch (myKind) {
      case DIRECTORY:
        if (!FileUtil.isAncestor(getWorkingDirectory(), myDirectoryPath, false)) {
          throw new RuntimeConfigurationError("Working directory should be ancestor of testing directory");
        }
        VirtualFile testingDirectory = LocalFileSystem.getInstance().findFileByPath(myDirectoryPath);
        if (testingDirectory == null) {
          throw new RuntimeConfigurationError("Testing directory doesn't exist");
        }
        break;
      case PACKAGE:
        Module module = configurationModule.getModule();
        assert module != null;

        VirtualFile packageDirectory = GoSdkUtil.findFileByRelativeToLibrariesPath(myPackage, module.getProject(), module);
        if (packageDirectory == null || !packageDirectory.isDirectory()) {
          throw new RuntimeConfigurationError("Cannot find package '" + myPackage + "'");
        }
        for (VirtualFile file : packageDirectory.getChildren()) {
          if (GoTestFinder.isTestFile(file)) {
            return;
          }
        }
        throw new RuntimeConfigurationError("Cannot find Go test files in '" + myPackage + "'");
      case FILE:
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(myFilePath);
        if (virtualFile == null) {
          throw new RuntimeConfigurationError("Test file doesn't exist");
        }
        PsiFile file = PsiManager.getInstance(getProject()).findFile(virtualFile);
        if (file == null || !GoTestFinder.isTestFile(file)) {
          throw new RuntimeConfigurationError("File '" + myFilePath + "' is not test");
        }
        break;
    }
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    super.writeExternal(element);
    JDOMExternalizerUtil.addElementWithValueAttribute(element, KIND_ATTRIBUTE_NAME, myKind.name());
    if (!myPackage.isEmpty()) {
      JDOMExternalizerUtil.addElementWithValueAttribute(element, PACKAGE_ATTRIBUTE_NAME, myPackage);
    }
    if (!myDirectoryPath.isEmpty()) {
      JDOMExternalizerUtil.addElementWithValueAttribute(element, DIRECTORY_ATTRIBUTE_NAME, myDirectoryPath);
    }
    if (!myFilePath.isEmpty()) {
      JDOMExternalizerUtil.addElementWithValueAttribute(element, FILE_PATH_ATTRIBUTE_NAME, myFilePath);
    }
    if (!myPattern.isEmpty()) {
      JDOMExternalizerUtil.addElementWithValueAttribute(element, PATTERN_ATTRIBUTE_NAME, myPattern);
    }
  }

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    try {
      String kindName = JDOMExternalizerUtil.getFirstChildValueAttribute(element, KIND_ATTRIBUTE_NAME);
      myKind = kindName != null ? Kind.valueOf(kindName) : Kind.DIRECTORY;
    }
    catch (IllegalArgumentException e) {
      myKind = Kind.DIRECTORY;
    }
    myPackage = StringUtil.notNullize(JDOMExternalizerUtil.getFirstChildValueAttribute(element, PACKAGE_ATTRIBUTE_NAME));
    myDirectoryPath = StringUtil.notNullize(JDOMExternalizerUtil.getFirstChildValueAttribute(element, DIRECTORY_ATTRIBUTE_NAME));
    myFilePath = StringUtil.notNullize(JDOMExternalizerUtil.getFirstChildValueAttribute(element, FILE_PATH_ATTRIBUTE_NAME));
    myPattern = StringUtil.notNullize(JDOMExternalizerUtil.getFirstChildValueAttribute(element, PATTERN_ATTRIBUTE_NAME));
  }

  @NotNull
  public String getPattern() {
    return myPattern;
  }

  public void setPattern(@NotNull String pattern) {
    myPattern = pattern;
  }

  @NotNull
  public Kind getKind() {
    return myKind;
  }

  public void setKind(@NotNull Kind kind) {
    myKind = kind;
  }

  @NotNull
  public String getPackage() {
    return myPackage;
  }

  public void setPackage(@NotNull String aPackage) {
    myPackage = aPackage;
  }

  @NotNull
  public String getFilePath() {
    return myFilePath;
  }

  public void setFilePath(@NotNull String filePath) {
    myFilePath = filePath;
  }

  @NotNull
  public String getDirectoryPath() {
    return myDirectoryPath;
  }

  public void setDirectoryPath(@NotNull String directoryPath) {
    myDirectoryPath = directoryPath;
  }
}
