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

import com.goide.psi.GoFieldDeclaration;
import com.goide.psi.GoFieldDefinition;
import com.goide.psi.GoVisitor;
import com.goide.quickfix.GoConvertToExportedDefinitionQuickFix;
import com.goide.quickfix.GoConvertToMultipleFieldsDeclarationsQuickFix;
import com.goide.quickfix.GoDeleteTagQuickFix;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import org.jetbrains.annotations.NotNull;

public class GoTaggedFieldInspection extends GoInspectionBase {
  @NotNull
  @Override
  protected GoVisitor buildGoVisitor(@NotNull final ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new GoVisitor() {
      @Override
      public void visitFieldDeclaration(@NotNull GoFieldDeclaration o) {
        super.visitFieldDeclaration(o);
        if (o.getTag() == null) return;
        if (o.getFieldDefinitionList().size() > 1) {
          holder.registerProblem(o,
                                 "Cannot use tag with multiple fields in declaration",
                                 ProblemHighlightType.WEAK_WARNING,
                                 new GoConvertToMultipleFieldsDeclarationsQuickFix(o),
                                 new GoDeleteTagQuickFix(o));
          return;
        }
        GoFieldDefinition field = o.getFieldDefinitionList().get(0);
        if (field.isBlank()) return;
        if (!field.isPublic()) {
          holder.registerProblem(o,
                                 "Field \"" + field.getName() + "\" is not exported but has a tag attached to it",
                                 ProblemHighlightType.WEAK_WARNING,
                                 new GoConvertToExportedDefinitionQuickFix(field),
                                 new GoDeleteTagQuickFix(o));
        }
      }
    };
  }
}
