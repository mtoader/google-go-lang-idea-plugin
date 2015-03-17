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

package com.goide.formatter;

import com.goide.GoFileType;
import com.goide.actions.fmt.GoFmtFileAction;
import com.goide.actions.fmt.GoImportsFileAction;
import com.goide.settings.GoApplicationSettings;
import com.intellij.execution.ExecutionException;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerAdapter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;


public class GoOnSaveFormatter extends FileDocumentManagerAdapter {
  @Override
  public void beforeDocumentSaving(@NotNull final Document document) {

    if (!document.isWritable()) {
      return;
    }

    final VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (file == null || file.getFileType() != GoFileType.INSTANCE) {
      return;
    }

    Project project = ProjectUtil.guessProjectForContentFile(file, GoFileType.INSTANCE);
    if (project == null) {
      return;
    }

    final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
    if (psiFile == null) {
      return;
    }

    GoApplicationSettings.GoApplicationSettingsBean settings = GoApplicationSettings.getInstance().getState();

    final Project finalProject = project;
    if (settings.goimportsOnSave) {
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          if (finalProject.isDisposed()) {
            return;
          }

          try {
            new GoImportsFileAction().doSomething(psiFile, finalProject, file, "");
          }
          catch (ExecutionException ignored) {
          }
        }
      });
    }
    else if (settings.goFmtOnSave) {
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          if (finalProject.isDisposed()) {
            return;
          }

          try {
            new GoFmtFileAction().doSomething(psiFile, finalProject, file, "");
          }
          catch (ExecutionException ignored) {
          }
        }
      });
    }
  }
}
