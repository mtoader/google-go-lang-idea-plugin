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

package com.goide.util;

import com.goide.GoConstants;
import com.goide.project.GoBuildTargetSettings;
import com.goide.project.GoExcludedPathsSettings;
import com.goide.psi.*;
import com.goide.runconfig.testing.GoTestFinder;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.Function;
import com.intellij.util.ThreeState;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class GoUtil {
  public static final String PLUGIN_VERSION = getPlugin().getVersion();
  public static final Function<VirtualFile, String> RETRIEVE_FILE_PATH_FUNCTION = new Function<VirtualFile, String>() {
    @Override
    public String fun(VirtualFile file) {
      return file.getPath();
    }
  };
  public static final Function<VirtualFile, VirtualFile> RETRIEVE_FILE_PARENT_FUNCTION = new Function<VirtualFile, VirtualFile>() {
    @Override
    public VirtualFile fun(VirtualFile file) {
      return file.getParent();
    }
  };
  private static final String PLUGIN_ID = "ro.redeul.google.go";
  private static final Key<CachedValue<Collection<String>>> PACKAGES_CACHE = Key.create("packages_cache");
  private static final Key<CachedValue<Collection<String>>> PACKAGES_TEST_TRIMMED_CACHE = Key.create("packages_test_trimmed_cache");

  public static boolean allowed(@NotNull PsiFile file) {
    GoBuildTargetSettings targetSettings = GoBuildTargetSettings.getInstance(file.getProject());
    return new GoBuildMatcher(targetSettings.getTargetSystemDescriptor(ModuleUtilCore.findModuleForPsiElement(file))).matchFile(file);
  }

  public static boolean isExcludedFile(@NotNull final GoFile file) {
    return CachedValuesManager.getCachedValue(file, new CachedValueProvider<Boolean>() {
      @Nullable
      @Override
      public Result<Boolean> compute() {
        String importPath = file.getImportPath();
        GoExcludedPathsSettings excludedSettings = GoExcludedPathsSettings.getInstance(file.getProject());
        return Result.create(importPath != null && excludedSettings.isExcluded(importPath), file, excludedSettings);
      }
    });
  }

  @NotNull
  public static String systemOS() {
    // TODO android? dragonfly nacl? netbsd openbsd plan9
    if (SystemInfo.isMac) {
      return "darwin";
    }
    else if (SystemInfo.isFreeBSD) {
      return "freebsd";
    }
    else if (SystemInfo.isLinux) {
      return "linux";
    }
    else if (SystemInfo.isSolaris) {
      return "solaris";
    }
    else if (SystemInfo.isWindows) {
      return "windows";
    }
    return "unknown";
  }

  @NotNull
  public static String systemArch() {
    if (SystemInfo.is64Bit) {
      return "amd64";
    }
    else if (SystemInfo.is32Bit) {
      return "386";
    }
    return "unknown";
  }

  @NotNull
  public static ThreeState systemCgo(@NotNull String os, @NotNull String arch) {
    return GoConstants.KNOWN_CGO.contains(os + "/" + arch) ? ThreeState.YES : ThreeState.NO;
  }

  @Contract("null -> true")
  public static boolean importPathToIgnore(@Nullable String importPath) {
    if (importPath != null) {
      for (String part : StringUtil.split(importPath, "/")) {
        if (directoryToIgnore(part)) return false;
      }
    }
    return true;
  }

  public static boolean libraryDirectoryToIgnore(@NotNull String name) {
    return directoryToIgnore(name) || GoConstants.TESTDATA_NAME.equals(name);
  }
  
  public static boolean directoryToIgnore(@NotNull String name) {
    return StringUtil.startsWithChar(name, '_') || StringUtil.startsWithChar(name, '.');
  }

  @NotNull
  public static GlobalSearchScope moduleScope(@NotNull PsiElement element) {
    return moduleScope(element.getProject(), ModuleUtilCore.findModuleForPsiElement(element));
  }

  @NotNull
  public static GlobalSearchScope moduleScope(@NotNull Project project, @Nullable Module module) {
    return module != null ? moduleScope(module) : GlobalSearchScope.projectScope(project);
  }

  @NotNull
  public static GlobalSearchScope moduleScope(@NotNull Module module) {
    return GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module).uniteWith(module.getModuleContentWithDependenciesScope());
  }

  @NotNull
  public static GlobalSearchScope moduleScopeWithoutLibraries(@NotNull Module module) {
    return GlobalSearchScope.moduleWithDependenciesScope(module).uniteWith(module.getModuleContentWithDependenciesScope());
  }

  public static IdeaPluginDescriptor getPlugin() {
    return PluginManager.getPlugin(PluginId.getId(PLUGIN_ID));
  }

  /**
   * isReferenceTo optimization. Before complex checking via resolve we can say for sure that element
   * can't be a reference to given declaration in following cases:<br/>
   * – GoLabelRef can't be resolved to anything but GoLabelDefinition<br/>
   * – GoTypeReferenceExpression (not from receiver type) can't be resolved to anything but GoTypeSpec<br/>
   * – Definition is private and reference in different package<br/>
   * – Definition is public, reference in different package and reference containing file doesn't have an import of definition package
   */
  public static boolean couldBeReferenceTo(@NotNull PsiElement definition, @NotNull PsiElement reference) {
    if (definition instanceof PsiDirectory && reference instanceof GoReferenceExpressionBase) return true;
    if (reference instanceof GoLabelRef && !(definition instanceof GoLabelDefinition)) return false;
    if (reference instanceof GoTypeReferenceExpression &&
        !(reference.getParent() instanceof GoReceiverType) &&
        !(definition instanceof GoTypeSpec)) {
      return false;
    }

    PsiFile definitionFile = definition.getContainingFile();
    PsiFile referenceFile = reference.getContainingFile();
    if (!(definitionFile instanceof GoFile) || !(referenceFile instanceof GoFile)) {
      return false; // todo: zolotov, are you sure? cross refs, for instance?
    }

    boolean inSameFile = definitionFile.isEquivalentTo(referenceFile);
    if (!inSameFile) {
      String referencePackage = ((GoFile)referenceFile).getPackageName();
      String definitionPackage = ((GoFile)definitionFile).getPackageName();
      boolean inSamePackage = referencePackage != null && referencePackage.equals(definitionPackage);

      if (!inSamePackage) {
        if (reference instanceof GoNamedElement && !((GoNamedElement)reference).isPublic()) {
          return false;
        }
        String path = ((GoFile)definitionFile).getImportPath();
        if (!((GoFile)referenceFile).getImportedPackagesMap().containsKey(path)) {
          return GoConstants.BUILTIN_PACKAGE_NAME.equals(path);
        }
      }
    }
    return true;
  }

  @NotNull
  public static Collection<String> getAllPackagesInDirectory(@Nullable final PsiDirectory dir, final boolean trimTestSuffices) {
    if (dir == null) return Collections.emptyList();
    Key<CachedValue<Collection<String>>> key = trimTestSuffices ? PACKAGES_TEST_TRIMMED_CACHE : PACKAGES_CACHE;
    return CachedValuesManager.getManager(dir.getProject()).getCachedValue(dir, key, new CachedValueProvider<Collection<String>>() {
      @Nullable
      @Override
      public Result<Collection<String>> compute() {
        Collection<String> set = ContainerUtil.newLinkedHashSet();
        for (PsiFile file : dir.getFiles()) {
          if (file instanceof GoFile && !directoryToIgnore(file.getName())) {
            String name = ((GoFile)file).getPackageName();
            if (StringUtil.isNotEmpty(name)) {
              set.add(trimTestSuffices && GoTestFinder.isTestFile(file) ? StringUtil.trimEnd(name, GoConstants.TEST_SUFFIX) : name);
            }
          }
        }
        return Result.create(set, dir);
      }
    }, false);
  }

  @NotNull
  public static GlobalSearchScope moduleScopeWithoutTests(@NotNull PsiElement context) {
    return new ExceptTestsScope(moduleScope(context));
  }

  private static class ExceptTestsScope extends DelegatingGlobalSearchScope {
    public ExceptTestsScope(@NotNull GlobalSearchScope baseScope) {
      super(baseScope);
    }

    @Override
    public boolean contains(@NotNull VirtualFile file) {
      return !GoTestFinder.isTestFile(file) && super.contains(file);
    }
  }
  
  public static class ExceptChildOfDirectory extends DelegatingGlobalSearchScope {
    @NotNull private final VirtualFile myParent;

    public ExceptChildOfDirectory(@NotNull VirtualFile parent, @NotNull GlobalSearchScope baseScope) {
      super(baseScope);
      myParent = parent;
    }

    @Override
    public boolean contains(@NotNull VirtualFile file) {
      return !myParent.equals(file.getParent()) && super.contains(file);
    }
  }

}