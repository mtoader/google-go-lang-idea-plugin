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

package com.goide.quickfix;

import com.goide.psi.GoNamedElement;
import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class GoConvertToExportedDefinitionQuickFix extends LocalQuickFixOnPsiElement {
  public static final String NAME = "Change to exported definition";

  public GoConvertToExportedDefinitionQuickFix(GoNamedElement o) {
    super(o);
  }

  @NotNull
  @Override
  public String getText() {
    return NAME;
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull PsiFile file, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
    if (startElement.isValid() && startElement instanceof GoNamedElement) {
      String name = ((GoNamedElement)startElement).getName();
      if (name == null || "_".equals(name)) return;
      WriteCommandAction.runWriteCommandAction(project, new Runnable() {
        @Override
        public void run() {
          ((GoNamedElement)startElement).setName(StringUtil.capitalize(name));
        }
      });
    }
  }

  @NotNull
  @Override
  public String getFamilyName() {
    return getName();
  }
}
