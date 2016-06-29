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

package com.goide.quickfix;

import com.goide.psi.*;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateBuilderImpl;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.ConstantNode;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.diagnostic.AttachmentFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GoRenameReceiverQuickFix extends LocalQuickFixAndIntentionActionOnPsiElement {
  private static final String INPUT_RECEIVER_NAME = "INPUTVAR";
  private static final String OTHER_RECEIVER_NAME = "OTHERVAR";

  public static final String RENAME_ONE_METHOD_QUICKFIX_NAME = "Rename receiver";
  public static final String RENAME_ALL_METHODS_QUICKFIX_NAME = "Rename all receivers";


  private final boolean renameInOneMethod;

  public GoRenameReceiverQuickFix(PsiElement element, boolean renameInOneMethod) {
    super(element);
    this.renameInOneMethod = renameInOneMethod;
  }

  @NotNull
  @Override
  public String getText() {
    return renameInOneMethod ? RENAME_ONE_METHOD_QUICKFIX_NAME : RENAME_ALL_METHODS_QUICKFIX_NAME;
  }

  @Nls
  @NotNull
  @Override
  public String getFamilyName() {
    return "Rename receivers";
  }


  @Override
  public void invoke(@NotNull Project project,
                     @NotNull PsiFile file,
                     @Nullable("is null when called from inspection") Editor editor,
                     @NotNull PsiElement startElement,
                     @NotNull PsiElement endElement) {
    if (editor == null) {
      LOG.error("Cannot run quick fix without editor: " + getClass().getSimpleName(),
                AttachmentFactory.createAttachment(file.getVirtualFile()));
      return;
    }

    GoReceiver receiver = PsiTreeUtil.getParentOfType(startElement, GoReceiver.class);
    if (receiver == null) return;
    GoType type = receiver.getType();
    if (type == null) return;
    String typeName = type.getText();
    if (typeName == null || typeName.isEmpty()) return;
    String name = StringUtil.decapitalize(typeName.substring(0, 1));

    GoTypeReferenceExpression typeReference = type.getTypeReferenceExpression();
    GoTypeSpec typeSpec = typeReference != null ? ObjectUtils.tryCast(typeReference.resolve(), GoTypeSpec.class) : null;
    if (typeSpec == null) return;

    TemplateBuilderImpl builder = new TemplateBuilderImpl(file);
    List<PsiElement> elementsToRename = renameInOneMethod ? visitMethod(PsiTreeUtil.getParentOfType(receiver, GoMethodDeclaration.class))
                                                          : collectElements(file, typeSpec);
    for (PsiElement expr : elementsToRename) {
      if (!expr.equals(startElement)) {
        builder.replaceElement(expr, OTHER_RECEIVER_NAME, INPUT_RECEIVER_NAME, false);
      }
      else {
        builder.replaceElement(expr, INPUT_RECEIVER_NAME, new ConstantNode(name), true);
      }
    }

    editor.getCaretModel().moveToOffset(file.getTextRange().getStartOffset());
    Template template = builder.buildInlineTemplate();
    editor.getCaretModel().moveToOffset(file.getTextRange().getStartOffset());
    TemplateManager.getInstance(project).startTemplate(editor, template);
  }

  private static List<PsiElement> collectElements(PsiFile file, GoTypeSpec typeSpec) {
    List<PsiElement> result = ContainerUtil.newSmartList();
    for (GoMethodDeclaration method : typeSpec.getMethods()) {
      if (!file.isEquivalentTo(method.getContainingFile())) continue;
      result.addAll(visitMethod(method));
    }
    return result;
  }

  private static List<PsiElement> visitMethod(@Nullable GoMethodDeclaration method) {
    List<PsiElement> result = ContainerUtil.newSmartList();
    if (method == null) return result;
    GoReceiver methodReceiver = method.getReceiver();
    if (methodReceiver == null) return result;
    PsiElement identifier = methodReceiver.getIdentifier();
    if (identifier == null) return result;
    result.add(identifier);

    GoRecursiveVisitor visitor = new GoRecursiveVisitor() {
      @Override
      public void visitReferenceExpression(@NotNull GoReferenceExpression o) {
        if (o.textMatches(identifier)) {
          result.add(o);
        }
        visitElement(o);
      }
    };
    method.accept(visitor);

    return result;
  }
}


