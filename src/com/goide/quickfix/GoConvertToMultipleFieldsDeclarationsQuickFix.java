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

import com.goide.psi.GoFieldDeclaration;
import com.goide.psi.GoNamedElement;
import com.goide.psi.GoStructType;
import com.goide.psi.impl.GoElementFactory;
import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

public class GoConvertToMultipleFieldsDeclarationsQuickFix extends LocalQuickFixOnPsiElement {
  public static final String NAME = "Split into multiple fields";

  public GoConvertToMultipleFieldsDeclarationsQuickFix(GoFieldDeclaration o) {
    super(o);
  }

  @NotNull
  @Override
  public String getText() {
    return NAME;
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull PsiFile file, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
    PsiElement parent = startElement.getParent();
    if (startElement.isValid() && startElement instanceof GoFieldDeclaration && parent instanceof GoStructType) {
      int idx = ContainerUtil.indexOf(((GoStructType)parent).getFieldDeclarationList(), (GoFieldDeclaration)startElement);
      WriteCommandAction.runWriteCommandAction(project, new Runnable() {
        @Override
        public void run() {
          GoStructType structType = GoElementFactory.createMultipleFieldsFromSingleDeclaration(project, (GoStructType)parent, idx);
          parent.replace(structType);
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
