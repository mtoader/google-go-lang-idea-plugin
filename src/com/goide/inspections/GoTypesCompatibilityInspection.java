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
import com.goide.psi.impl.GoPsiImplUtil;
import com.goide.psi.impl.GoTypeUtil;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GoTypesCompatibilityInspection extends GoInspectionBase {
  @NotNull
  @Override
  protected GoVisitor buildGoVisitor(@NotNull final ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new GoVisitor() {
      private boolean builtin(@NotNull GoCallExpr callExpr) {
        GoExpression functionReferenceExpression = callExpr.getExpression();
        if (!(functionReferenceExpression instanceof GoReferenceExpression)) return false;
        return GoPsiImplUtil.builtin(((GoReferenceExpression)functionReferenceExpression).resolve());
      }
      
      @Override
      public void visitCallExpr(@NotNull GoCallExpr o) {
        if (builtin(o)) return;
        GoArgumentList argumentList = o.getArgumentList();
        List<GoExpression> expressionList = argumentList.getExpressionList();
        boolean variadic = argumentList.getTripleDot() != null;
        for (int i = 0; i < expressionList.size(); i++) {
          checkExpression(expressionList.get(i), variadic && i + 1 == expressionList.size());
        }
        super.visitCallExpr(o);
      }

      private void checkExpression(@NotNull GoExpression e, boolean variadic) {
        if (GoTypeUtil.isNil(e)) {
          // todo: check nil
          return;
        }
        
        GoType type = e.getGoType(null);
        if (type == null) return;
        List<Pair<GoType, Boolean>> types = GoTypeUtil.getExpectedTypesWithVariadic(e);
        for (Pair<GoType, Boolean> exp : types) {
          if (variadic) {
            GoArrayOrSliceType typeSlice = ObjectUtils.tryCast(type, GoArrayOrSliceType.class);
            if (exp.second && typeSlice != null && exp.first.isAssignableFrom(typeSlice.getType())) return;
          }
          else {
            if (exp.first.isAssignableFrom(type)) return;
          }
        }
        holder.registerProblem(e, "Cannot use " + e.getText() + " (type " + type.getText() + ") as type " +
                                  StringUtil.join(ContainerUtil.map(types, GoTypeUtil.TO_STRING), ","));
      }
    };
  }
}
