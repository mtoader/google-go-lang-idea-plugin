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

import com.goide.psi.*;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

public class GoAddressOfLoopVariableInspection extends GoInspectionBase {
  @NotNull
  @Override
  protected GoVisitor buildGoVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new GoVisitor() {
      @Override
      public void visitUnaryExpr(@NotNull GoUnaryExpr o) {
        if (o.getBitAnd() == null) {
          return;
        }

        //TODO improve by supporting references to fields of struct variables
        GoReferenceExpression refExpr = ObjectUtils.tryCast(o.getExpression(), GoReferenceExpression.class);
        if (refExpr == null || refExpr.getQualifier() != null) {
          return;
        }

        GoVarDefinition varDefinition = ObjectUtils.tryCast(refExpr.getReference().resolve(), GoVarDefinition.class);
        PsiElement parent = varDefinition != null ? varDefinition.getParent() : null;
        if (!(parent instanceof GoRangeClause)) {
          return;
        }

        holder.registerProblem(o, "Suspicious: obtaining address of a for loop variable");
      }
    };
  }
}
