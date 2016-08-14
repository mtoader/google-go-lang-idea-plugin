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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.codeInspection.ProblemHighlightType.WEAK_WARNING;

public class GoAssignmentToReceiverInspection extends GoInspectionBase {
  @NotNull
  @Override
  protected GoVisitor buildGoVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new GoVisitor() {
      @Override
      public void visitAssignmentStatement(@NotNull GoAssignmentStatement o) {
        super.visitAssignmentStatement(o);
        for (GoExpression expr : o.getLeftHandExprList().getExpressionList()) {
          if (expr instanceof GoReferenceExpression) {
            GoReferenceExpression refExpr = (GoReferenceExpression)expr;
            PsiElement resolve = refExpr.resolve();
            if (resolve instanceof GoReceiver) {
              if (!canInspect(refExpr, (GoReceiver)resolve)) return;
              String message = "Assignment to method receiver doesn't propagate to other calls";
              if (((GoReceiver)resolve).getType() instanceof GoPointerType) {
                message = "Assignment to method receiver propagates only to callees but not to callers";
              }
              holder.registerProblem(expr, message, WEAK_WARNING);
            }
          }
        }
      }
    };
  }

  private static boolean canInspect(GoReferenceExpression o, @Nullable GoReceiver receiver) {
    if (receiver == null) return false;
    GoType resolveType = receiver.getType();
    if (resolveType == null) return false;
    PsiElement parent = o.getParent();

    if (resolveType instanceof GoPointerType) {
      if (parent instanceof GoUnaryExpr) {
        if (((GoUnaryExpr)parent).getMul() != null) return false;
      }
    }

    GoType underlyingType = resolveType.getUnderlyingType();
    return !(underlyingType instanceof GoChannelType ||
             underlyingType instanceof GoArrayOrSliceType ||
             underlyingType instanceof GoMapType);
  }
}
