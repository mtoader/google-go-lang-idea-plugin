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
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GoRedundantTypeDeclInCompositeLit extends GoInspectionBase implements CleanupLocalInspectionTool {
  public final static String DELETE_REDUNDANT_TYPE_DECLARATION_QUICK_FIX_NAME = "Delete redundant type declaration";
  private static final GoDeleteQuickFix DELETE_REDUNDANT_TYPE_DECLARATION_QUICK_FIX =
    new GoDeleteQuickFix(DELETE_REDUNDANT_TYPE_DECLARATION_QUICK_FIX_NAME, GoTypeReferenceExpression.class);


  @NotNull
  @Override
  protected GoVisitor buildGoVisitor(@NotNull final ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new GoVisitor() {
      @Override
      public void visitCompositeLit(@NotNull GoCompositeLit o) {
        GoLiteralValue literalValue = o.getLiteralValue();
        GoType expectedType = null;
        boolean isPointer = false;
        if (o.getType() instanceof GoArrayOrSliceType && ((GoArrayOrSliceType)o.getType()).getType() != null) {
          expectedType = ((GoArrayOrSliceType)o.getType()).getType();
        }
        if (o.getType() instanceof GoMapType && ((GoMapType)o.getType()).getValueType() != null) {
          expectedType = ((GoMapType)o.getType()).getValueType();
        }
        if (expectedType instanceof GoPointerType) {
          expectedType = ((GoPointerType)expectedType).getType();
          isPointer = true;
        }

        // TODO o.getType() instanceof GoStruct (struct or T[][])

        if (literalValue != null && (expectedType != null || isPointer)) {
          for (GoElement element : literalValue.getElementList()) {
            if (element.getValue() != null) {
              GoExpression expr = element.getValue().getExpression();
              if (isPointer && expr instanceof GoUnaryExpr) {
                GoUnaryExpr unaryExpr = (GoUnaryExpr)expr;
                PsiElement bitAnd = unaryExpr.getBitAnd();
                if (bitAnd != null && unaryExpr.getExpression() instanceof GoCompositeLit) {
                  if (isTypeReferencesEquals(expectedType, (GoCompositeLit)unaryExpr)) {
                    registerRedundantTypeDeclarationProblem(holder, bitAnd, ((GoCompositeLit)unaryExpr).getTypeReferenceExpression());
                  }
                }
              }
              else if (expr instanceof GoCompositeLit && isTypeReferencesEquals(expectedType, (GoCompositeLit)expr)) {
                registerRedundantTypeDeclarationProblem(holder, ((GoCompositeLit)expr).getTypeReferenceExpression(),
                                                        ((GoCompositeLit)expr).getTypeReferenceExpression());
              }
            }
          }
        }
      }
    };
  }

  // TODO o to concrete type
  private static void registerRedundantTypeDeclarationProblem
  (@NotNull final ProblemsHolder holder, @Nullable PsiElement start, @Nullable GoTypeReferenceExpression end) {
    if (start == null || end == null) {
      return;
    }
    ProblemDescriptor descriptor = holder.getManager().createProblemDescriptor(start, end, "Redundant type declaration",
                                                                               ProblemHighlightType.LIKE_UNUSED_SYMBOL, true,
                                                                               DELETE_REDUNDANT_TYPE_DECLARATION_QUICK_FIX);
    holder.registerProblem(descriptor);
  }

  private static boolean isTypeReferencesEquals(GoType pattern, GoCompositeLit value) {
    if (!pattern.isValid() || !value.isValid()) {
      return false;
    }

    if (pattern.getTypeReferenceExpression() == null || value.getTypeReferenceExpression() == null) {
      return false;
    }

    if (pattern.getTypeReferenceExpression().resolve() != value.getTypeReferenceExpression().resolve()) {
      return false;
    }
    //TODO Complex type comparison
    return true;
  }
}