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
import com.goide.quickfix.GoDeleteQuickFix;
import com.intellij.codeInspection.*;
import com.intellij.openapi.util.Trinity;
import org.jetbrains.annotations.NotNull;

public class GoRedundantSecondIndexInSlicesInspection extends GoInspectionBase implements CleanupLocalInspectionTool {

  public final static String DELETE_REDUNDANT_INDEX_QUICK_FIX_NAME = "Delete redundant index";

  private static final GoDeleteQuickFix DELETE_REDUNDANT_INDEX_QUICK_FIX =
    new GoDeleteQuickFix(DELETE_REDUNDANT_INDEX_QUICK_FIX_NAME, GoCallExpr.class);

  @NotNull
  @Override
  protected GoVisitor buildGoVisitor(@NotNull final ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new GoVisitor() {
      @Override
      public void visitIndexOrSliceExpr(@NotNull GoIndexOrSliceExpr o) {
        Trinity<GoExpression, GoExpression, GoExpression> indexes = o.getIndices();
        if (indexes.third == null && indexes.second instanceof GoCallExpr && o.getExpression() != null &&
            o.getExpression().getReference() != null) {
          if (o.getExpression() instanceof GoReferenceExpression && ((GoReferenceExpression)o.getExpression()).getQualifier() != null) {
            return;
          }
          GoCallExpr callExpr = (GoCallExpr)indexes.second;
          if (callExpr.getExpression().textMatches("len") &&
              callExpr.getArgumentList().getExpressionList().size() == 1) {
            GoExpression secondRightExpr = callExpr.getArgumentList().getExpressionList().get(0);
            if (secondRightExpr.getReference() != null &&
                o.getExpression().getReference().resolve() == secondRightExpr.getReference().resolve()) {
              holder.registerProblem(callExpr, "Redundant index", ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                     DELETE_REDUNDANT_INDEX_QUICK_FIX);
            }
          }
        }
      }
    };
  }
}



