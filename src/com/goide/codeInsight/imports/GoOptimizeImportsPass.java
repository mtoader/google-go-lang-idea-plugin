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

package com.goide.codeInsight.imports;

import com.intellij.codeInsight.daemon.impl.DefaultHighlightInfoProcessor;
import com.intellij.codeInsight.daemon.impl.ProgressableTextEditorHighlightingPass;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.DocumentUtil;
import org.jetbrains.annotations.NotNull;

public class GoOptimizeImportsPass extends ProgressableTextEditorHighlightingPass {
  @NotNull private final PsiFile myFile;
  private Runnable myRunnableFix;

  public GoOptimizeImportsPass(@NotNull Project project, @NotNull PsiFile file, @NotNull Editor editor) {
    super(project, editor.getDocument(), "Go Optimize Imports Pass", file, editor, file.getTextRange(), false,
          new DefaultHighlightInfoProcessor());
    myFile = file;
  }

  @Override
  protected void collectInformationWithProgress(@NotNull ProgressIndicator progress) {
    myRunnableFix = new GoImportOptimizer().processFile(myFile);
    progress.checkCanceled();
  }

  @Override
  protected void applyInformationWithProgress() {
    final Project project = myFile.getProject();
    UndoManager undoManager = UndoManager.getInstance(project);
    if (undoManager.isUndoInProgress() || undoManager.isRedoInProgress()) return;
    DocumentUtil.writeInRunUndoTransparentAction(myRunnableFix);
  }
}
