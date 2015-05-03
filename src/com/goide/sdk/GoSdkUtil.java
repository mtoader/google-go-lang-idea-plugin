/*
 * Copyright 2013-2015 Sergey Ignatov, Alexander Zolotov, Mihai Toader, Florin Patan
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

import com.goide.GoConstants;
import com.goide.GoEnvironmentUtil;
import com.goide.project.GoLibrariesService;
import com.goide.psi.GoFile;
import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.ObjectUtils;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.VersionComparatorUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.util.containers.ContainerUtil.newLinkedHashSet;

public class GoSdkUtil {
  private static final String GO_VERSION_PATTERN = "theVersion\\s*=\\s*`go([\\d.]+)`";
  private static final String GAE_VERSION_PATTERN = "theVersion\\s*=\\s*`go([\\d.]+)( \\(appengine-[\\d.]+\\))?`";
  private static final String GO_DEVEL_VERSION_PATTERN = "theVersion\\s*=\\s*`(devel[\\d.]+)`";

  // todo: caching
  @Nullable
  public static VirtualFile getSdkSrcDir(@NotNull PsiElement context) {
    Module module = ModuleUtilCore.findModuleForPsiElement(context);
    VirtualFile sdkSrcDir = getSdkSrcDir(context.getProject(), module);
    return sdkSrcDir != null ? sdkSrcDir : guessSkdSrcDir(context);
  }

  @Nullable
  private static VirtualFile getSdkSrcDir(@NotNull Project project, @Nullable Module module) {
    String sdkHomePath = GoSdkService.getInstance(project).getSdkHomePath(module);
    String sdkVersionString = GoSdkService.getInstance(project).getSdkVersion(module);
    VirtualFile sdkSrcDir = null;
    if (sdkHomePath != null && sdkVersionString != null) {
      File sdkSrcDirFile = new File(sdkHomePath, getSrcLocation(sdkVersionString));
      sdkSrcDir = LocalFileSystem.getInstance().findFileByIoFile(sdkSrcDirFile);
    }
    return sdkSrcDir;
  }

  @Nullable
  public static GoFile findBuiltinFile(@NotNull PsiElement context) {
    final Project project = context.getProject();
    final Module module = ModuleUtilCore.findModuleForPsiElement(context);
    UserDataHolder holder = ObjectUtils.notNull(module, project);
    VirtualFile file = CachedValuesManager.getManager(context.getProject()).getCachedValue(holder, new CachedValueProvider<VirtualFile>() {
      @Nullable
      @Override
      public Result<VirtualFile> compute() {
        VirtualFile sdkSrcDir = getSdkSrcDir(project, module);
        VirtualFile result = sdkSrcDir != null ? sdkSrcDir.findFileByRelativePath(GoConstants.BUILTIN_FILE_PATH) : null;
        return Result.create(result, getSdkAndLibrariesCacheDependencies(project, module, result));
      }
    });

    if (file == null) return null;
    PsiFile psiBuiltin = context.getManager().findFile(file);
    return (psiBuiltin instanceof GoFile) ? (GoFile)psiBuiltin : null;
  }

  @Nullable
  public static VirtualFile findExecutableInGoPath(@NotNull String executableName, @NotNull Project project, @Nullable Module module) {
    executableName = GoEnvironmentUtil.getBinaryFileNameForPath(executableName);
    Collection<VirtualFile> roots = getGoPathRoots(project, module);
    for (VirtualFile file : roots) {
      VirtualFile child = file.findChild("bin/" + executableName);
      if (child != null) return child;
    }
    File fromPath = PathEnvironmentVariableUtil.findInPath(executableName);
    return fromPath != null ? VfsUtil.findFileByIoFile(fromPath, true) : null;
  }

  @NotNull
  private static Collection<VirtualFile> getGoPathRoots(@NotNull Project project, @Nullable Module module) {
    Collection<VirtualFile> roots = ContainerUtil.newArrayList(getGoPathsRootsFromEnvironment());
    roots.addAll(module != null ? GoLibrariesService.getUserDefinedLibraries(module) : GoLibrariesService.getUserDefinedLibraries(project));
    return roots;
  }

  @NotNull
  public static Collection<VirtualFile> getGoPathSources(@NotNull Project project, @Nullable Module module) {
    return ContainerUtil.mapNotNull(getGoPathRoots(project, module), new RetrieveSubDirectoryOrSelfFunction("src"));
  }

  @NotNull
  public static Collection<VirtualFile> getGoPathBins(@NotNull Project project, @Nullable Module module) {
    Collection<VirtualFile> result = newLinkedHashSet(ContainerUtil.mapNotNull(getGoPathRoots(project, module),
                                                                               new RetrieveSubDirectoryOrSelfFunction("bin")));
    String executableGoPath = GoSdkService.getInstance(project).getGoExecutablePath(module);
    if (executableGoPath != null) {
      VirtualFile executable = VirtualFileManager.getInstance().findFileByUrl(VfsUtilCore.pathToUrl(executableGoPath));
      if (executable != null) ContainerUtil.addIfNotNull(result, executable.getParent());
    }

    return result;
  }

  @NotNull
  public static Collection<String> mapVirtualFilesToCanonicalPaths(Collection<VirtualFile> files) {
    Collection<String> result = new ArrayList<String>(files.size());

    for (VirtualFile vfile : files) {
      result.add(vfile.getCanonicalPath());
    }

    return result;
  }

  /**
   * Retrieves root directories from GOPATH env-variable.
   * This method doesn't consider user defined libraries,
   * for that case use {@link {@link this#getGoPathRoots(Project, Module)}
   */
  @NotNull
  public static Collection<VirtualFile> getGoPathsRootsFromEnvironment() {
    Set<VirtualFile> result = newLinkedHashSet();
    String goPath = GoEnvironmentUtil.retrieveGoPathFromEnvironment();
    if (goPath != null) {
      String home = SystemProperties.getUserHome();
      for (String s : StringUtil.split(goPath, File.pathSeparator)) {
        if (home != null) {
          s = s.replaceAll("\\$HOME", home);
        }
        ContainerUtil.addIfNotNull(result, LocalFileSystem.getInstance().findFileByPath(s));
      }
    }
    return result;
  }

  @NotNull
  public static String retrieveGoPath(@NotNull Project project, @Nullable Module module) {
    return StringUtil.join(mapVirtualFilesToCanonicalPaths(getGoPathRoots(project, module)), File.pathSeparator);
  }

  @NotNull
  public static String retrieveEnvironmentPathForGo(@NotNull Project project, @Nullable Module module) {
    return StringUtil.join(mapVirtualFilesToCanonicalPaths(getGoPathBins(project, module)), File.pathSeparator);
  }

  @NotNull
  static String getSrcLocation(@NotNull String version) {
    if (version.startsWith("devel")) {
      return "src";
    }
    return compareVersions(version, "1.4") < 0 ? "src/pkg" : "src";
  }

  static int compareVersions(@NotNull String lhs, @NotNull String rhs) {
    return VersionComparatorUtil.compare(lhs, rhs);
  }

  @Nullable
  private static VirtualFile guessSkdSrcDir(@NotNull PsiElement context) {
    VirtualFile virtualFile = context.getContainingFile().getOriginalFile().getVirtualFile();
    return ProjectRootManager.getInstance(context.getProject()).getFileIndex().getClassRootForFile(virtualFile);
  }

  @Nullable
  public static VirtualFile findFileByRelativeToLibrariesPath(@NotNull String path, @NotNull Project project, @Nullable Module module) {
    for (VirtualFile root : getGoPathSources(project, module)) {
      VirtualFile file = root.findFileByRelativePath(path);
      if (file != null) {
        return file;
      }
    }
    return null;
  }

  @Nullable
  @Contract("null -> null")
  public static String getImportPath(@Nullable final PsiDirectory psiDirectory) {
    if (psiDirectory == null) {
      return null;
    }
    return CachedValuesManager.getCachedValue(psiDirectory, new CachedValueProvider<String>() {
      @Nullable
      @Override
      public Result<String> compute() {
        Project project = psiDirectory.getProject();
        Module module = ModuleUtilCore.findModuleForPsiElement(psiDirectory);
        String path = getPathRelativeToSdkAndLibraries(psiDirectory.getVirtualFile(), project, module);
        return Result.create(path, getSdkAndLibrariesCacheDependencies(psiDirectory));
      }
    });
  }

  @Nullable
  public static String getPathRelativeToSdkAndLibraries(@NotNull VirtualFile file, @Nullable Project project, @Nullable Module module) {
    if (project != null) {
      Collection<VirtualFile> roots = newLinkedHashSet(getGoPathSources(project, module));
      ContainerUtil.addIfNotNull(roots, getSdkSrcDir(project, module));

      for (VirtualFile root : roots) {
        String relativePath = VfsUtilCore.getRelativePath(file, root, '/');
        if (StringUtil.isNotEmpty(relativePath)) {
          return relativePath;
        }
      }
    }

    String filePath = file.getPath();
    int src = filePath.lastIndexOf("/src/");
    if (src > -1) {
      return filePath.substring(src + 5);
    }
    return null;
  }

  @Nullable
  public static VirtualFile suggestSdkDirectory() {
    if (SystemInfo.isWindows) {
      return ObjectUtils.chooseNotNull(LocalFileSystem.getInstance().findFileByPath("C:\\Go"),
                                       LocalFileSystem.getInstance().findFileByPath("C:\\cygwin"));
    }
    if (SystemInfo.isMac || SystemInfo.isLinux) {
      String fromEnv = suggestSdkDirectoryPathFromEnv();
      if (fromEnv != null) {
        return LocalFileSystem.getInstance().findFileByPath(fromEnv);
      }
      return LocalFileSystem.getInstance().findFileByPath("/usr/local/go");
    }
    if (SystemInfo.isMac) {
      String macPorts = "/opt/local/lib/go";
      return LocalFileSystem.getInstance().findFileByPath(macPorts);
    }
    return null;
  }

  @Nullable
  private static String suggestSdkDirectoryPathFromEnv() {
    File fileFromPath = PathEnvironmentVariableUtil.findInPath("go");
    if (fileFromPath != null) {
      File canonicalFile;
      try {
        canonicalFile = fileFromPath.getCanonicalFile();
        String path = canonicalFile.getPath();
        if (path.endsWith("bin/go")) {
          return StringUtil.trimEnd(path, "bin/go");
        }
      }
      catch (IOException ignore) {
      }
    }
    return null;
  }

  @Nullable
  public static String retrieveGoVersion(@NotNull final String sdkPath) {
    try {
      String oldStylePath = new File(sdkPath, "src/pkg/" + GoConstants.GO_VERSION_FILE_PATH).getPath();
      String newStylePath = new File(sdkPath, "src/" + GoConstants.GO_VERSION_FILE_PATH).getPath();
      File zVersionFile = FileUtil.findFirstThatExist(oldStylePath, newStylePath);
      if (zVersionFile == null) return null;
      String text = FileUtil.loadFile(zVersionFile);
      Matcher matcher = Pattern.compile(GO_VERSION_PATTERN).matcher(text);
      if (matcher.find()) {
        return matcher.group(1);
      }
      matcher = Pattern.compile(GAE_VERSION_PATTERN).matcher(text);
      if (matcher.find()) {
        return matcher.group(1) + matcher.group(2);
      }
      matcher = Pattern.compile(GO_DEVEL_VERSION_PATTERN).matcher(text);
      if (matcher.find()) {
        return matcher.group(1);
      }
    }
    catch (IOException e) {
      return null;
    }
    return null;
  }

  @NotNull
  public static String adjustSdkPath(@NotNull String path) {
    if (new File(path, GoConstants.LIB_EXEC_DIRECTORY).exists()) {
      path += File.separatorChar + GoConstants.LIB_EXEC_DIRECTORY;
    }

    File possibleGCloudSdk = new File(path, GoConstants.GCLOUD_APP_ENGINE_DIRECTORY_PATH);
    if (possibleGCloudSdk.exists() && possibleGCloudSdk.isDirectory()) {
      if (isAppEngine(possibleGCloudSdk.getAbsolutePath())) {
        return possibleGCloudSdk.getAbsolutePath() + GoConstants.APP_ENGINE_GO_ROOT_DIRECTORY_PATH;
      }
    }

    return isAppEngine(path) ? path + GoConstants.APP_ENGINE_GO_ROOT_DIRECTORY_PATH : path;
  }


  private static boolean isAppEngine(@NotNull String path) {
    return new File(path, GoConstants.APP_ENGINE_MARKER_FILE).exists();
  }

  @NotNull
  public static Collection<VirtualFile> getSdkDirectoriesToAttach(@NotNull String sdkPath, @NotNull String versionString) {
    String srcPath = getSrcLocation(versionString);
    // scr is enough at the moment, possible process binaries from pkg
    VirtualFile src = VirtualFileManager.getInstance().findFileByUrl(VfsUtilCore.pathToUrl(FileUtil.join(sdkPath, srcPath)));
    if (src != null && src.isDirectory()) {
      return Collections.singletonList(src);
    }
    return Collections.emptyList();
  }

  @NotNull
  public static Collection<Object> getSdkAndLibrariesCacheDependencies(@NotNull PsiElement context, Object... extra) {
    return getSdkAndLibrariesCacheDependencies(context.getProject(), ModuleUtilCore.findModuleForPsiElement(context),
                                               ArrayUtil.append(extra, context));
  }

  @NotNull
  public static Collection<Object> getSdkAndLibrariesCacheDependencies(@NotNull Project project, @Nullable Module module, Object... extra) {
    Collection<Object> dependencies = ContainerUtil.<Object>newArrayList(GoLibrariesService.getModificationTrackers(project, module));
    ContainerUtil.addAllNotNull(dependencies, GoSdkService.getInstance(project));
    ContainerUtil.addAllNotNull(dependencies, extra);
    return dependencies;
  }

  public static class RetrieveSubDirectoryOrSelfFunction implements Function<VirtualFile, VirtualFile> {
    @NotNull private final String mySubdirName;

    public RetrieveSubDirectoryOrSelfFunction(@NotNull String subdirName) {
      mySubdirName = subdirName;
    }

    @Override
    public VirtualFile fun(VirtualFile file) {
      return file == null || FileUtil.namesEqual(mySubdirName, file.getName()) ? file : file.findChild(mySubdirName);
    }
  }
}
