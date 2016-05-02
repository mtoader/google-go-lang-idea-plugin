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

package com.goide.inspections.unresolved;

import com.goide.inspections.GoInspectionBase;
import com.goide.psi.*;
import com.goide.quickfix.GoRenameToBlankQuickFix;
import com.goide.runconfig.testing.GoTestFinder;
import com.goide.runconfig.testing.GoTestFunctionType;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GoUnusedParameterInspection extends GoInspectionBase {
  @NotNull
  @Override
  protected GoVisitor buildGoVisitor(@NotNull final ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new GoVisitor() {
      @Override
      public void visitMethodDeclaration(@NotNull GoMethodDeclaration o) {
        super.visitMethodDeclaration(o);
        visitDeclaration(holder, o.getSignature(), false);
      }

      @Override
      public void visitFunctionDeclaration(@NotNull GoFunctionDeclaration o) {
        super.visitFunctionDeclaration(o);
        if (GoTestFinder.isTestFile(o.getContainingFile()) && GoTestFunctionType.fromName(o.getName()) != null) {
          return;
        }
        visitDeclaration(holder, o.getSignature(), true);
      }

      @Override
      public void visitFunctionLit(@NotNull GoFunctionLit o) {
        super.visitFunctionLit(o);
        visitDeclaration(holder, o.getSignature(), true);
      }
    };
  }

  private static void visitDeclaration(@NotNull ProblemsHolder holder, @Nullable GoSignature signature, boolean checkParameters) {
    if (signature == null) return;
    if (checkParameters) {
      GoParameters parameters = signature.getParameters();
      visitParameterList(holder, parameters.getParameterDeclarationList(), "parameter");
    }

    GoResult result = signature.getResult();
    GoParameters returnParameters = result != null ? result.getParameters() : null;
    if (returnParameters != null) {
      visitParameterList(holder, returnParameters.getParameterDeclarationList(), "named return parameter");
    }
  }

  private static void visitParameterList(@NotNull ProblemsHolder holder, @NotNull List<GoParameterDeclaration> parameters, String what) {
    Query<PsiReference> search;
    for (GoParameterDeclaration parameterDeclaration : parameters) {
      if (parameterDeclaration.getParamDefinitionList().isEmpty()) {
        continue;
      }
      for (GoParamDefinition parameter : parameterDeclaration.getParamDefinitionList()) {
        ProgressManager.checkCanceled();
        if (parameter.isBlank()) continue;

        search = ReferencesSearch.search(parameter, parameter.getUseScope());
        if (search.findFirst() != null) continue;

        holder.registerProblem(
          parameter,
          "Unused " + what + " <code>#ref</code> #loc",
          ProblemHighlightType.LIKE_UNUSED_SYMBOL,
          new GoRenameToBlankQuickFix(parameter)
        );
      }
    }
  }
}
