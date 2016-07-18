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
import com.goide.psi.impl.GoCType;
import com.goide.psi.impl.GoLightType;
import com.goide.psi.impl.GoPsiImplUtil;
import com.goide.psi.impl.GoTypeUtil;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GoBinaryAndUnaryExprInspection extends GoInspectionBase {
  @NotNull
  @Override
  protected GoVisitor buildGoVisitor(@NotNull final ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new GoVisitor() {

      @Override
      public void visitUnaryExpr(@NotNull GoUnaryExpr o) {
        GoExpression expr = o.getExpression();
        GoType type = expr != null ? expr.getGoType(null) : null;
        if (type == null) return;
        GoType t = type.getUnderlyingType();
        if (o.getPlus() != null || o.getMinus() != null) checkNumeric(o, t);
        if (o.getNot() != null) checkBool(o, t);
        if (o.getBitXor() != null) checkInteger(o, t);
        if (o.getSendChannel() != null) {
          if (!(t instanceof GoChannelType)) {
            registerProblem(o, "(receive from non-chan type " + t.getText() + ")");
          }
          else {
            if (((GoChannelType)t).getDirection() == GoPsiImplUtil.ChannelDirection.RECEIVER) {
              registerProblem(o, "(receive from send-only type " + t.getText() + ")");
            }
          }
        }
        if (o.getMul() != null) {
          if (!(type instanceof GoSpecType || t instanceof GoPointerType || t instanceof GoCType)) {
            holder.registerProblem(o, "invalid indirect of " + expr.getText() + " (type " + t.getText() +")");
          }
        }
      }

      @Override
      public void visitBinaryExpr(@NotNull GoBinaryExpr o) {
        if (o instanceof GoSelectorExpr || o instanceof GoConversionExpr || o instanceof GoConditionalExpr) return;
        GoExpression left = o.getLeft();
        GoType leftType = left.getGoType(null);
        GoExpression right = o.getRight();
        GoType rightType = right != null ? right.getGoType(null) : null;
        if (mustBeEquals(o)) {
          if (!identical(leftType, rightType)) {
            holder.registerProblem(o, "invalid operation: " + o.getText() + " (mismatched types "  + leftType.getText() + " and "
                                      + rightType.getText() + ")");
            return;
          }
        }
        else {
          if (checkExpr(o, right, rightType, false)) return;
        }
        checkExpr(o, left, leftType, true);
      }

      private boolean identical (@Nullable GoType left, @Nullable GoType right) {
        if (left == null || right == null) return true;
        GoType l = left.getUnderlyingType();
        GoType r = right.getUnderlyingType();
        if (l instanceof GoLightType.LightUntypedNumericType && GoTypeUtil.isNumericType(r) ||
            r instanceof GoLightType.LightUntypedNumericType && GoTypeUtil.isNumericType(l)) return true;
        return GoTypeUtil.identical(l, r);
      }


      private boolean checkExpr(@NotNull GoBinaryExpr o, @Nullable GoExpression expr, @Nullable GoType type, boolean left) {
        if (expr == null || type == null) return false;
        GoType t = type.getUnderlyingType();
        if (o instanceof GoAndExpr || o instanceof GoOrExpr) {
          return checkBool(o, t);
        }
        if (o instanceof GoAddExpr) {
          GoAddExpr add = (GoAddExpr)o;
          if (add.getBitOr() != null || add.getBitXor() != null) {
            return checkInteger(o, t);
          }
          if (add.getMinus() != null) {
            return checkNumeric(o, t);
          }
          if (add.getPlus() != null) {
            if (!(GoTypeUtil.isNumericType(t) || GoTypeUtil.isString(t))) {
              registerProblem(o, t);
              return true;
            }
            return false;
          }
        }
        if (o instanceof GoMulExpr) {
          GoMulExpr mul = (GoMulExpr)o;
          if (mul.getBitClear() != null || mul.getRemainder() != null || mul.getBitAnd() != null) {
            return checkInteger(o, t);
          }

          if (mul.getShiftRight() != null || mul.getShiftLeft() != null) {
            if (left) {
              return checkInteger(o, t);
            }
            else {
              if (!(GoTypeUtil.isUintType(t) || GoTypeUtil.isUntypedOrCType(t))) {
                registerProblem(expr, "(shift count type " + t.getText() + ", must be unsigned integer)" );
                return true;
              }
              return false;
            }
          }

          if (mul.getMul() != null || mul.getQuotient() != null) {
            return checkNumeric(o, t);
          }
        }
        return false;
      }

      private boolean checkBool(@NotNull GoExpression expr, GoType t) {
        if (!GoTypeUtil.isBoolean(t)) {
          registerProblem(expr, t);
          return true;
        }
        return false;
      }

      private boolean mustBeEquals(GoBinaryExpr o) {
        return !(o instanceof GoMulExpr && (((GoMulExpr)o).getShiftLeft() != null || ((GoMulExpr)o).getShiftRight() != null));
      }

      private boolean checkInteger(GoExpression expr, GoType type) {
        if (!(GoTypeUtil.isIntegerType(type) || GoTypeUtil.isUntypedOrCType(type))) {
          registerProblem(expr, type);
          return true;
        }
        return false;
      }

      private boolean checkNumeric(GoExpression expr, GoType type) {
        if (!GoTypeUtil.isNumericType(type)) {
          registerProblem(expr, type);
          return true;
        }
        return false;
      }

      private void registerProblem(@NotNull GoExpression expr, @NotNull PsiElement type) {
        PsiElement operator = getOperator(expr);
        String operatorText = operator != null ? operator.getText() : "";
        if (expr instanceof GoBinaryExpr) {
          registerProblem(expr, "(operator " + operatorText + " not defined on " + type.getText() + ")");
        }
        if (expr instanceof GoUnaryExpr) {
          holder.registerProblem(expr, "invalid operation: " + operatorText + " " + type.getText());
        }
      }

      private void registerProblem(@NotNull GoExpression expr, @NotNull String message) {
        holder.registerProblem(expr, "invalid operation: " + expr.getText() + " " + message);
      }

      @Nullable
      private PsiElement getOperator(@NotNull GoExpression o) {
        if (o instanceof GoMulExpr) {
          GoMulExpr m = (GoMulExpr)o;
          PsiElement[] elements = {m.getRemainder(), m.getShiftRight(), m.getShiftLeft(), m.getQuotient(), m.getMul(), m.getBitAnd(), m.getBitClear()};
          return getNotNull(elements);
        }
        if (o instanceof GoAddExpr) {
          GoAddExpr a = (GoAddExpr)o;
          PsiElement[] elements = {a.getMinus(), a.getPlus(), a.getBitOr(), a.getBitXor()};
          return getNotNull(elements);
        }
        if (o instanceof GoAndExpr) return ((GoAndExpr)o).getCondAnd();
        if (o instanceof GoOrExpr) return ((GoOrExpr)o).getCondOr();
        if (o instanceof GoUnaryExpr) {
          GoUnaryExpr u = (GoUnaryExpr)o;
          PsiElement[] elements = {u.getBitAnd(), u.getNot(), u.getSendChannel(), u.getBitXor(), u.getMul(), u.getMinus(), u.getPlus()};
          return getNotNull(elements);
        }
        return null;
      }

      @Nullable
      private PsiElement getNotNull(@NotNull PsiElement[] elements) {
        for (int i = 0; i < elements.length; i++) {
          if (elements[i] != null) return elements[i];
        }
        return null;
      }
    };
  }
}
