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

package com.goide.inspections.external.golint;

import com.goide.GoEnvironmentUtil;
import com.goide.GoFileType;
import com.goide.util.GoExecutor;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ex.DisableInspectionToolAction;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GoLintExternalAnnotator extends ExternalAnnotator<GoLintExternalAnnotator.State, GoLintExternalAnnotator.State> {
  private final static Logger LOG = Logger.getInstance(GoLintExternalAnnotator.class);

  @Nullable
  private static Problem parseProblem(@NotNull String input) {
    List<String> split = StringUtil.split(input, ":");
    if (split.size() < 4) return null;
    int line = StringUtil.parseInt(split.get(1), 0);
    return new Problem(line, StringUtil.join(split.subList(3, split.size()), ":"));
  }

  @Nullable
  @Override
  public State collectInformation(@NotNull PsiFile file) {
    VirtualFile vFile = file.getVirtualFile();
    if (vFile == null || vFile.getFileType() != GoFileType.INSTANCE) return null;
    String canonicalPath = vFile.getCanonicalPath();
    if (canonicalPath == null) return null;
    Module module = ModuleUtilCore.findModuleForPsiElement(file);
    if (module == null) return null;
    Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
    if (sdk == null) return null;
    String homePath = sdk.getHomePath();
    if (homePath == null) return null;

    InspectionProfile profile = InspectionProjectProfileManager.getInstance(file.getProject()).getInspectionProfile();
    HighlightDisplayKey key = HighlightDisplayKey.find(GoLintInspection.INSPECTION_SHORT_NAME);
    if (!profile.isToolEnabled(key)) return null;

    return new State(module, canonicalPath);
  }

  @Nullable
  @Override
  public State doAnnotate(@Nullable State state) {
    if (state == null) return null;

    ProcessOutput output = null;
    try {
      output = getExecutorOutput(state);
    } catch (ExecutionException e) {
      LOG.debug(e);
    }
    if (output != null) {
      if (output.getStderrLines().isEmpty()) {
        for (String line : output.getStdoutLines()) {
          LOG.debug(line);
          Problem problem = parseProblem(line);
          LOG.debug(problem != null ? problem.toString() : null);
          ContainerUtil.addAllNotNull(state.problems, problem);
        }
      }
    }
    return state;
  }

  @Override
  public void apply(@NotNull PsiFile file, @Nullable State annotationResult, @NotNull AnnotationHolder holder) {
    if (annotationResult == null || !file.isValid()) return;
    String text = file.getText();
    for (Problem problem : annotationResult.problems) {
      int offset = StringUtil.lineColToOffset(text, problem.myLine - 1, 0);

      if (offset == -1) continue;

      int width = 0;
      while (offset + width < text.length() && !StringUtil.isLineBreak(text.charAt(offset + width))) width++;

      TextRange problemRange = TextRange.create(offset, offset + width);
      String message = "golint: " + problem.myDescription;
      Annotation annotation = holder.createWarningAnnotation(problemRange, message);
      HighlightDisplayKey key = HighlightDisplayKey.find(GoLintInspection.INSPECTION_SHORT_NAME);
      annotation.registerFix(new DisableInspectionToolAction(key) {
        @NotNull
        @Override
        public String getName() {
          return "Disable 'golint-based inspections'";
        }
      });
    }
  }

  @Nullable
  public File getInspectionTool() {
    return PathEnvironmentVariableUtil.findInPath(GoEnvironmentUtil.getBinaryFileNameForPath("golint"));
  }

  private ProcessOutput getExecutorOutput(@NotNull State state) throws ExecutionException {
    File goToolPath = getInspectionTool();
    if (goToolPath == null) {
      return null;
    }

    return GoExecutor.in(state.myModule)
      .withExePath(goToolPath.getAbsolutePath())
      .withParameters(state.myFilePath)
      .withTimeout(2000)
      .runWithProcessOutput();
  }

  public static class Problem {
    private final int myLine;
    private final String myDescription;

    public Problem(int line, String description) {
      myLine = line;
      myDescription = description;
    }

    @NotNull
    @Override
    public String toString() {
      return "Problem{" +
             "myLine=" + myLine +
             ", myDescription='" + myDescription + '\'' +
             '}';
    }
  }

  public static class State {
    public final List<Problem> problems = new ArrayList<Problem>();
    private final String myFilePath;
    private final Module myModule;

    public State(Module module, String filePath) {
      myModule = module;
      myFilePath = filePath;
    }
  }
}
