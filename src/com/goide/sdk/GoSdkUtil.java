
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

import com.goide.psi.GoFile;
import com.google.common.collect.ImmutableMap;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoSdkUtil {

  public static final String GOPATH = "GOPATH";

  private static final Logger LOG = Logger.getInstance(GoSdkUtil.class);

  private static final Pattern versionPattern =
    Pattern.compile("go version go(\\d+\\.\\d+\\.?\\d*).*", Pattern.CASE_INSENSITIVE);

  private static final ImmutableMap<String, String> GoVersionPackagesPath =
    ImmutableMap.of(
      "1.0", "/src/pkg",
      "1.1", "/src/pkg",
      "1.2", "/src/pkg",
      "1.3", "/src/pkg",
      "1.4", "/src"
    );

  @Nullable
  public static VirtualFile getSdkHome(@NotNull PsiElement context) {
    Module module = ModuleUtilCore.findModuleForPsiElement(context);

    Sdk sdk = module == null ? null : ModuleRootManager.getInstance(module).getSdk();
    if (sdk == null) {
      return guessSkdHome(context);
    }

    String sdkHomePath = sdk.getHomePath();
    if (sdkHomePath == null) {
      return guessSkdHome(context);
    }

    sdkHomePath = adjustSdkPath(sdkHomePath);
    VirtualFile result = LocalFileSystem.getInstance().findFileByPath((new File(sdkHomePath, getSdkSourcePath(sdkHomePath))).getAbsolutePath());

    return result != null ? result : guessSkdHome(context);
  }

  @Nullable
  public static GoFile findBuiltinFile(@NotNull PsiElement context) {
    VirtualFile home = getSdkHome(context);
    VirtualFile vBuiltin = home != null ? home.findFileByRelativePath("builtin/builtin.go") : null;
    if (vBuiltin != null) {
      PsiFile psiBuiltin = context.getManager().findFile(vBuiltin);
      if (psiBuiltin instanceof GoFile) return ((GoFile)psiBuiltin);
    }
    return null;
  }

  @Nullable
  private static VirtualFile guessSkdHome(@NotNull PsiElement context) {
    VirtualFile virtualFile = context.getContainingFile().getOriginalFile().getVirtualFile();
    return ProjectRootManager.getInstance(context.getProject()).getFileIndex().getClassRootForFile(virtualFile);
  }

  @NotNull
  public static List<VirtualFile> getGoPathsSources() {
    List<VirtualFile> result = ContainerUtil.newArrayList();
    String gopath = retrieveGoPath();
    if (gopath != null) {
      List<String> split = StringUtil.split(gopath, File.pathSeparator);
      String home = SystemProperties.getUserHome();
      for (String s : split) {
        if (home != null) {
          s = s.replaceAll("\\$HOME", home);
        }
        VirtualFile path = LocalFileSystem.getInstance().findFileByPath(s + "/src");
        ContainerUtil.addIfNotNull(result, path);
      }
    }
    return result;
  }

  @Nullable
  public static String retrieveGoPath() {
    String path = EnvironmentUtil.getValue(GOPATH);
    return path != null ? path : PathMacros.getInstance().getValue(GOPATH);
  }

  @NotNull
  public static String adjustSdkPath(@NotNull String path) {
    if (isAppEngine(path)) path = path + "/" + "goroot";
    return path;
  }

  private static boolean isAppEngine(@Nullable String path) {
    if (path == null) return false;
    return new File(path, "appcfg.py").exists();
  }

  @Nullable
  public static String getGoVersionFromPath(String sdkHome) {
    String binPath = sdkHome + "/bin/";

    String goBin = SystemInfo.isWindows ? "go.exe" : "go";
    File execPath = new File(binPath + goBin);

    if (!execPath.exists()) {
      return null;
    }

    try {
      GeneralCommandLine command = new GeneralCommandLine();
      command.setExePath(execPath.getCanonicalPath());
      command.addParameter("version");
      command.withWorkDirectory(binPath);
      command.getEnvironment().put("GOROOT", sdkHome);

      ProcessOutput output = new CapturingProcessHandler(
        command.createProcess(),
        Charset.defaultCharset(),
        command.getCommandLineString()).runProcess();

      if (output.getExitCode() != 0) {
        LOG.error("Go compiler exited with invalid exit code: " + output.getExitCode());
        return null;
      }

      String version = output.getStdout().trim();
      Matcher versionMatcher = versionPattern.matcher(version.trim());
      if (versionMatcher.find()) {
        return versionMatcher.group(1);
      }

      return null;
    } catch (Exception e) {
      LOG.error("Exception while executing the process:", e);
    }

    return null;
  }

  @NotNull
  public static String getSdkSourcePath(@NotNull String path) {
    if (path.contains("mockSdk-1.1.2")) {
      return "src/pkg";
    }

    String goVersion = getGoVersionFromPath(path);
    if (goVersion == null) {
      return "src";
    }

    goVersion = goVersion.substring(0, 3);

    String srcPath = GoVersionPackagesPath.get(goVersion);
    if (srcPath == null) {
      srcPath = "src";
    }

    return srcPath;
  }
}
