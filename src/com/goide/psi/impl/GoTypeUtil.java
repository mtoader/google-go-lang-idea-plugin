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

package com.goide.psi.impl;

import com.goide.psi.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class GoTypeUtil {
  /**
   * https://golang.org/ref/spec#For_statements
   * The expression on the right in the "range" clause is called the range expression,
   * which may be an array, pointer to an array, slice, string, map, or channel permitting receive operations.
   */
  public static boolean isIterable(@Nullable GoType type) {
    type = type != null ? type.getUnderlyingType() : null;
    return type instanceof GoArrayOrSliceType ||
           type instanceof GoPointerType && isArray(((GoPointerType)type).getType()) ||
           type instanceof GoMapType ||
           type instanceof GoChannelType ||
           isString(type);
  }

  private static boolean isArray(@Nullable GoType type) {
    type = type != null ? type.getUnderlyingType() : null;
    return type instanceof GoArrayOrSliceType && ((GoArrayOrSliceType)type).getExpression() != null;
  }

  public static boolean isString(@Nullable GoType type) {
    GoType underlyingType = type != null ? type.getUnderlyingType() : null;
    return underlyingType != null && underlyingType.textMatches("string") && GoPsiImplUtil.builtin(underlyingType);
  }

  @NotNull
  public static List<GoType> getExpectedTypes(@NotNull GoExpression expression) {
    PsiElement parent = expression.getParent();
    if (parent == null) return Collections.emptyList();

    List<GoType> result = ContainerUtil.newSmartList();
    if (parent instanceof GoAssignmentStatement) {
      GoAssignmentStatement assignment = (GoAssignmentStatement)parent;
      List<GoExpression> leftExpressions = assignment.getLeftHandExprList().getExpressionList();
      if (assignment.getExpressionList().size() == 1) {
        List<GoType> typeList = ContainerUtil.newSmartList();
        for (GoExpression expr : leftExpressions) {
          GoType type = expr.getGoType(null);
          typeList.add(type);
        }
        result.add(createGoTypeListOrGoType(typeList, expression));
        if (leftExpressions.size() > 1) {
          result.add(getType(ContainerUtil.getFirstItem(leftExpressions), parent));
        }
        return result;
      }

      int position = assignment.getExpressionList().indexOf(expression);
      result.add(leftExpressions.size() > position
                 ? assignment.getLeftHandExprList().getExpressionList().get(position).getGoType(null)
                 : null);
    }

    if (parent instanceof GoRangeClause) {
      result.add(getType(null, parent));
      return result;
    }

    if (parent instanceof GoVarSpec) {
      GoVarSpec varSpec = (GoVarSpec)parent;
      GoType type = getType(varSpec.getType(), parent);
      if (parent instanceof GoRecvStatement) {
        GoRecvStatement recvStatement = (GoRecvStatement)parent;
        type =
          recvStatement.getAssign() != null ? getType(ContainerUtil.getFirstItem(recvStatement.getLeftExpressionsList()), parent) : type;
      }
      if (varSpec.getRightExpressionsList().size() == 1) {
        List<GoType> typeList = ContainerUtil.newSmartList();
        int defListSize = varSpec.getVarDefinitionList().size();
        for (int i = 0; i < defListSize; i++) {
          typeList.add(type);
        }
        if (parent instanceof GoRecvStatement) {
          for (GoExpression expr : ((GoRecvStatement)parent).getLeftExpressionsList()) {
            typeList.add(getType(expr, parent));
          }
        }
        result.add(createGoTypeListOrGoType(typeList, expression));
        if (defListSize > 1) {
          result.add(getType(type, parent));
        }
        return result;
      }

      result.add(type);
      return result;
    }
    if (parent instanceof GoArgumentList) {
      PsiElement parentOfParent = parent.getParent();
      if (parentOfParent instanceof GoCallExpr) {
        PsiReference reference = ((GoCallExpr)parentOfParent).getExpression().getReference();
        if (reference != null) {
          PsiElement resolve = reference.resolve();
          if (resolve instanceof GoFunctionOrMethodDeclaration) {
            GoSignature signature = ((GoFunctionOrMethodDeclaration)resolve).getSignature();
            if (signature != null) {
              List<GoExpression> exprList = ((GoArgumentList)parent).getExpressionList();
              List<GoParameterDeclaration> paramsList = signature.getParameters().getParameterDeclarationList();
              if (exprList.size() == 1) {
                List<GoType> typeList = ContainerUtil.newSmartList();
                for (GoParameterDeclaration parameter : paramsList) {
                  typeList.add(parameter.getType());
                }
                result.add(createGoTypeListOrGoType(typeList, parent));
                if (paramsList.size() > 1) {
                  result.add(getType(ContainerUtil.getFirstItem(paramsList), parent));
                }
              }
              else {
                int position = exprList.indexOf(expression);
                if (position >= 0 && paramsList.size() > position) {
                  result.add(getType(paramsList.get(position), parent));
                }
                else {
                  result.add(getType(null, parent));
                }
              }
              return result;
            }
          }
        }
        result.add(getType(null, parent));
        return result;
      }
    }


    if (parent instanceof GoUnaryExpr) {
      GoUnaryExpr unaryExpr = (GoUnaryExpr)parent;
      if (unaryExpr.getSendChannel() != null) {
        GoType type = ContainerUtil.getFirstItem(getExpectedTypes(unaryExpr));
        result.add(GoElementFactory.createType(parent.getProject(), "chan " + getType(type, parent).getText()));
      }
      else {
        result.add(getType(null, parent));
      }
    }

    if (parent instanceof GoSendStatement || parent instanceof GoLeftHandExprList && parent.getParent() instanceof GoSendStatement) {
      GoSendStatement sendStatement = (GoSendStatement)(parent instanceof GoSendStatement ? parent : parent.getParent());
      GoLeftHandExprList leftHandExprList = sendStatement.getLeftHandExprList();
      GoExpression channel =
        ContainerUtil.getFirstItem(leftHandExprList != null ? leftHandExprList.getExpressionList() : sendStatement.getExpressionList());
      GoExpression sendExpr = sendStatement.getSendExpression();
      assert channel != null;
      if (expression.isEquivalentTo(sendExpr)) {
        GoType chanType = channel.getGoType(null);
        if (chanType instanceof GoChannelType) {
          result.add(getType(((GoChannelType)chanType).getType(), parent));
          return result;
        }
      }

      if (expression.isEquivalentTo(channel)) {
        GoType type = sendExpr != null ? sendExpr.getGoType(null) : null;
        result.add(GoElementFactory.createType(parent.getProject(), "chan " + getType(type, parent).getText()));
        return result;
      }

      result.add(getType(null, parent));
      return result;
    }

    if (parent instanceof GoExprCaseClause) {
      GoExprSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(parent, GoExprSwitchStatement.class);
      if (switchStatement == null) return result;

      GoType type;
      GoExpression switchExpr = switchStatement.getExpression();
      if (switchExpr != null) {
        type = getType(switchExpr, parent);
      }
      else {
        GoStatement statement = switchStatement.getStatement();
        if (statement == null) {
          type = GoElementFactory.createType(parent.getProject(), "bool");
        }
        else {
          GoLeftHandExprList leftHandExprList = ((GoSimpleStatement)statement).getLeftHandExprList();
          GoExpression expr = leftHandExprList != null ? ContainerUtil.getFirstItem(leftHandExprList.getExpressionList()) : null;
          type = getType(expr, parent);
        }
      }
      result.add(type);
    }

    return result;
  }

  @NotNull
  private static GoType createGoTypeListOrGoType(@NotNull List<GoType> types, @NotNull PsiElement context) {
    if (types.size() < 2) {
      return getType(ContainerUtil.getFirstItem(types), context);
    }
    return GoElementFactory.createTypeList(context.getProject(), StringUtil.join(types, new Function<GoType, String>() {
      @Override
      public String fun(GoType type) {
        return type == null ? "interface{}" : type.getText();
      }
    }, ", "));
  }

  @NotNull
  private static GoType getType(@Nullable PsiElement element, @NotNull PsiElement context) {
    if (element == null) {
      return GoElementFactory.createType(context.getProject(), "interface{}");
    }

    GoType type = null;
    if (element instanceof GoType) {
      type = (GoType)element;
    }
    if (element instanceof GoExpression) {
      type = ((GoExpression)element).getGoType(null);
    }
    if (element instanceof GoParameterDeclaration) {
      type = ((GoParameterDeclaration)element).getType();
    }

    assert type != null;
    return type;
  }
}
