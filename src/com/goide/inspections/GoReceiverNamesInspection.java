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
import com.goide.quickfix.GoRenameReceiverQuickFix;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class GoReceiverNamesInspection extends GoInspectionBase {
  private static final Set<String> genericNamesSet = ContainerUtil.newHashSet("me", "this", "self");

  @NotNull
  @Override
  protected GoVisitor buildGoVisitor(@NotNull final ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new GoVisitor() {
      @Override
      public void visitReceiver(@NotNull GoReceiver o) {
        if (genericNamesSet.contains(o.getName())) {
          PsiElement identifier = o.getIdentifier();
          if (identifier == null) return;
          holder.registerProblem(identifier, "Receiver has generic name", new GoRenameReceiverQuickFix(identifier, false),
                                 new GoRenameReceiverQuickFix(identifier, true));
        }
      }

      @Override

      public void visitMethodDeclaration(@NotNull GoMethodDeclaration o) {
        GoFile file = o.getContainingFile();
        GoReceiver methodReceiver = o.getReceiver();
        if (methodReceiver == null) return;
        String name = methodReceiver.getName();
        if (name == null) return;
        GoType type = methodReceiver.getType();
        type = type instanceof GoPointerType ? ((GoPointerType)type).getType() : type;
        if (type == null) return;
        GoTypeReferenceExpression reference = type.getTypeReferenceExpression();
        if (reference == null) return;
        GoTypeSpec typeSpec = ObjectUtils.tryCast(reference.resolve(), GoTypeSpec.class);
        if (typeSpec == null) return;
        List<GoMethodDeclaration> methods = typeSpec.getMethods();
        for (GoMethodDeclaration method : methods) {
          if (!file.isEquivalentTo(method.getContainingFile())) continue;
          GoReceiver receiver = method.getReceiver();
          if (receiver != null && !name.equals(receiver.getName())) {
            PsiElement identifier = methodReceiver.getIdentifier();
            if (identifier == null) continue;
            holder.registerProblem(identifier, "Receiver names are different", new GoRenameReceiverQuickFix(identifier, true),
                                   new GoRenameReceiverQuickFix(identifier, false));
            return;
          }
        }
      }
    };
  }
}
