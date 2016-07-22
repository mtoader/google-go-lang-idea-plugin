/*
 * Copyright 2013-2016 Sergey Ignatov, Alexander Zolotov, Florin Patan
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

package com.goide.intentions;

import com.goide.psi.GoImportSpec;
import com.goide.psi.GoImportString;
import com.goide.psi.impl.imports.GoImportReferenceSet;
import com.goide.util.GoExecutor;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiElement;
import com.intellij.util.Consumer;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GoUpdatePackage extends PsiElementBaseIntentionAction {
  @Nls
  @NotNull
  @Override
  public String getText() {
    return "Update package";
  }

  @Nls
  @NotNull
  @Override
  public String getFamilyName() {
    return getText();
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
    return getPackageName(element) != null;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
    String packageName = getPackageName(element);
    if (packageName == null) return;
    CommandProcessor.getInstance().runUndoTransparentAction(new Runnable() {
      @Override
      public void run() {
        Consumer<Boolean> consumer = new Consumer<Boolean>() {
          @Override
          public void consume(Boolean aBoolean) {
            VirtualFileManager.getInstance().asyncRefresh(null);
          }
        };
        GoExecutor.in(project, null).withPresentableName("go get -u -t " + packageName + "/...")
          .withParameters("get", "-u", "-t", packageName + "/...").showNotifications(false, true).showOutputOnError()
          .executeWithProgress(false, consumer);
      }
    });
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Nullable
  private static String getPackageName(@Nullable PsiElement element) {
    if (element == null) return null;
    if (!(element.getParent() instanceof GoImportString)) return null;

    GoImportString importString = (GoImportString)element.getParent();
    if (!(importString.getParent() instanceof GoImportSpec)) return null;
    if (importString.resolve() == null) return null;

    GoImportSpec importSpec = (GoImportSpec)importString.getParent();
    if (importSpec.isCImport()) return null;
    if (GoImportReferenceSet.isRelativeImport(importSpec.getPath()) || importSpec.isSDKImport()) return null;

    return importString.getPath();
  }
}
