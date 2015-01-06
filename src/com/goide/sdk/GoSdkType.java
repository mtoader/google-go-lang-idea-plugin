
/*
 * Copyright 2013-2014 Sergey Ignatov, Alexander Zolotov, Mihai Toader, Florin Patan
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

package com.goide.sdk;

import com.goide.GoIcons;
import com.goide.jps.model.JpsGoModelSerializerExtension;
import com.goide.jps.model.JpsGoSdkType;
import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class GoSdkType extends SdkType {

  public GoSdkType() {
    super(JpsGoModelSerializerExtension.GO_SDK_TYPE_ID);
  }

  @NotNull
  public static GoSdkType getInstance() {
    return SdkType.findInstance(GoSdkType.class);
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return GoIcons.ICON;
  }

  @NotNull
  @Override
  public Icon getIconForAddAction() {
    return getIcon();
  }

  @Nullable
  @Override
  public String suggestHomePath() {
    String fromEnv = findPathInEnvironment();
    if (fromEnv != null) return fromEnv;

    if (SystemInfo.isWindows) {
      return "C:\\cygwin\\bin";
    }

    String defaultPath = "/usr/local/go";
    if (new File(defaultPath).exists()) return defaultPath;

    if (SystemInfo.isMac) {
      String macPorts = "/opt/local/lib/go";
      if (new File(macPorts).exists()) return macPorts;
      return null;
    }

    if (SystemInfo.isLinux) {
      return "/usr/lib/go";
    }

    return null;
  }

  @Nullable
  private static String findPathInEnvironment() {
    String goExecName = "go";
    if (SystemInfo.isWindows) {
      goExecName += ".exe";
    }

    File fileFromPath = PathEnvironmentVariableUtil.findInPath(goExecName);
    if (fileFromPath != null) {
      File canonicalFile;
      try {
        canonicalFile = fileFromPath.getCanonicalFile();
        String path = canonicalFile.getPath();
        if (path.endsWith("bin/" + goExecName)) {
          return StringUtil.trimEnd(path, "bin/" + goExecName);
        }
      }
      catch (IOException ignore) {
      }
    }
    return null;
  }

  @Override
  public boolean isValidSdkHome(@NotNull String path) {
    path = GoSdkUtil.adjustSdkPath(path);
    return JpsGoSdkType.getGoExecutableFile(path).canExecute();
  }

  @NotNull
  @Override
  public String suggestSdkName(@Nullable String currentSdkName, @NotNull String sdkHome) {
    String version = getVersionString(sdkHome);
    if (version == null) return "Unknown Go version at " + sdkHome;
    return "Go " + version;
  }

  @Nullable
  @Override
  public String getVersionString(@NotNull String sdkHome) {
    return GoSdkUtil.getGoVersionFromPath(sdkHome);
  }

  @Nullable
  @Override
  public String getDefaultDocumentationUrl(@NotNull Sdk sdk) {
    return null;
  }

  @Nullable
  @Override
  public AdditionalDataConfigurable createAdditionalDataConfigurable(@NotNull SdkModel sdkModel, @NotNull SdkModificator sdkModificator) {
    return null;
  }

  @Override
  public void saveAdditionalData(@NotNull SdkAdditionalData additionalData, @NotNull Element additional) {
  }

  @NotNull
  @NonNls
  @Override
  public String getPresentableName() {
    return "Go SDK";
  }

  @Override
  public void setupSdkPaths(@NotNull Sdk sdk) {
    SdkModificator modificator = sdk.getSdkModificator();
    String path = sdk.getHomePath();
    if (path == null) return;
    path = GoSdkUtil.adjustSdkPath(path);
    modificator.setHomePath(path);
    File sdkPath = new File(path, GoSdkUtil.getSdkSourcePath(path));
    add(modificator, sdkPath);
    modificator.commitChanges();
  }

  private static void add(@NotNull SdkModificator modificator, @NotNull File file) {
    if (file.isDirectory()) {
      VirtualFile dir = LocalFileSystem.getInstance().findFileByIoFile(file);
      add(modificator, dir);
    }
  }

  private static void add(@NotNull SdkModificator modificator, @Nullable VirtualFile dir) {
    if (dir != null && dir.isDirectory()) {
      modificator.addRoot(dir, OrderRootType.CLASSES);
      modificator.addRoot(dir, OrderRootType.SOURCES);
    }
  }
}
