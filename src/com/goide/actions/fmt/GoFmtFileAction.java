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

package com.goide.actions.fmt;

import com.goide.GoEnvironmentUtil;
import com.goide.sdk.GoSdkService;
import com.goide.util.GoExecutor;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class GoFmtFileAction extends GoExternalToolsAction {
  public boolean doSomething(@NotNull PsiFile file,
                             @NotNull Project project,
                             @Nullable final VirtualFile virtualFile,
                             @NotNull String groupId) throws ExecutionException {
    if (virtualFile == null || !virtualFile.isInLocalFileSystem()) return true;
    Document document = PsiDocumentManager.getInstance(project).getDocument(file);
    assert document != null;
    String filePath = virtualFile.getCanonicalPath();
    assert filePath != null;

    File executable = GoEnvironmentUtil.getBinaryFilePathFromEnvironment("gofmt");

    if (executable == null) {
      warning(project, groupId, "Looks like you haven't any gofmt executable in your PATH");
      return true;
    }

    FileDocumentManager.getInstance().saveDocument(document);

    GoExecutor.
      in(project).
      addParameters("fmt", filePath).
      showOutputOnError().
      execute();
    return false;
  }

  @Override
  @NotNull
  protected String getWarningTitle() {
    return "Reformat code with go gmt";
  }

  @Override
  @NotNull
  protected String getErrorTitle(@NotNull String fileName) {
    return fileName + " formatting with go fmt failed";
  }
}
