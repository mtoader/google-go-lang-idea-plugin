/*
 * Copyright 2013-2015 Sergey Ignatov, Alexander Zolotov, Florin Patan
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
import com.goide.psi.impl.GoPsiImplUtil;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GoInspectionUtil {
  public static final int UNKNOWN_COUNT = -1;

  public static int getExpressionResultCount(GoExpression call) {
    if (call instanceof GoLiteral || call instanceof GoStringLiteral || call instanceof GoBinaryExpr || call instanceof GoParenthesesExpr ||
        (call instanceof GoUnaryExpr && ((GoUnaryExpr)call).getSendChannel() == null) || call instanceof GoBuiltinCallExpr) {
      return 1;
    }
    else if (call instanceof GoTypeAssertionExpr) {
      return getTypeAssertionResultCount((GoTypeAssertionExpr)call);
    }
    else if (call instanceof GoCallExpr) {
      return getFunctionResultCount((GoCallExpr)call);
    }
    else if (call instanceof GoReferenceExpression) {
      if (((GoReferenceExpression)call).getReference().resolve() instanceof GoVarDefinition) return 1;
    }
    return UNKNOWN_COUNT;
  }

  private static int getTypeAssertionResultCount(@NotNull GoTypeAssertionExpr expression) { // todo: ???
    PsiElement parent = expression.getParent();
    if (parent instanceof GoAssignmentStatement) {
      // TODO: get expressions and identifiers of assign statement
      return UNKNOWN_COUNT;
    }

    if (!(parent instanceof GoVarSpec)) {
      return 1;
    }

    List<GoVarDefinition> identifiers = ((GoVarSpec)parent).getVarDefinitionList();
    List<GoExpression> expressions = ((GoVarSpec)parent).getExpressionList();
    // if the type assertion is the only expression, and there are two variables.
    // The result of the type assertion is a pair of values with types (T, bool)
    if (identifiers.size() == 2 && expressions.size() == 1) {
      return 2;
    }

    return 1;
  }

  private static int getFunctionResultCount(@NotNull GoCallExpr call) {
    GoSignatureOwner signatureOwner = GoPsiImplUtil.resolveCall(call);
    return signatureOwner == null ? UNKNOWN_COUNT : getFunctionResultCount(signatureOwner);
  }
  
  public static int getFunctionResultCount(@NotNull GoSignatureOwner function) {
    int count = 0;
    GoSignature signature = function.getSignature();
    GoResult result = signature != null ? signature.getResult() : null;
    GoParameters parameters = result != null ? result.getParameters() : null;
    if (parameters != null) {
      for (GoParameterDeclaration p : parameters.getParameterDeclarationList()) {
        count += Math.max(p.getParamDefinitionList().size(), 1);
      }
      return count;
    }
    else if (result != null) {
      GoType type = result.getType();
      if (type instanceof GoTypeList) return ((GoTypeList)type).getTypeList().size();
      if (type != null) return 1;
    }
    return count;
  }
}
