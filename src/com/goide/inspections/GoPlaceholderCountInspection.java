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

package com.goide.inspections;

import com.goide.psi.GoCallExpr;
import com.goide.psi.GoFunctionOrMethodDeclaration;
import com.goide.psi.GoVisitor;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;

public class GoPlaceholderCountInspection extends GoInspectionBase {

  @NotNull
  @Override
  protected GoVisitor buildGoVisitor(@NotNull final ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new GoVisitor() {
      @Override
      public void visitCallExpr(@NotNull GoCallExpr o) {
        PsiReference psiReference = o.getExpression().getReference();
        PsiElement resolved = psiReference != null ? psiReference.resolve() : null;
        if (!(resolved instanceof GoFunctionOrMethodDeclaration)) return;

        String functionName = StringUtil.toLowerCase(((GoFunctionOrMethodDeclaration)resolved).getName());
        if (functionName == null) return;

        GoPlaceholderChecker checker = new GoPlaceholderChecker(holder, o, (GoFunctionOrMethodDeclaration)resolved);
        if (GoPlaceholderChecker.isFormattingFunction(functionName)) {
          checker.checkPrintf();
        }
        else if (GoPlaceholderChecker.isPrintingFunction(functionName)) {
          checker.checkPrint();
        }
      }
    };
  }
}
