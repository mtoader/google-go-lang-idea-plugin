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

package com.goide.actions;

import com.goide.psi.*;
import com.goide.psi.impl.GoPsiImplUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GoImplementInterfaceAction extends AnAction {
  private final static String windowTitle = "Select Interfaces to Implement";
  private final static String labelText = "Interfaces to be implemented:";

  @Override
  public void update(@NotNull AnActionEvent e) {
    PsiElement psiElement = getPsiElementFromContext(e);
    e.getPresentation().setEnabled(psiElement != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    PsiElement psiElement = getPsiElementFromContext(e);
    if (psiElement == null || !inCurrentProject(e, psiElement)) return;

    ArrayList<GoTypeSpec> interfaces = getInterfaceList(psiElement);
    if (interfaces == null || interfaces.size() == 0) return;

    GoTypeSpecListDialog dlg = new GoTypeSpecListDialog(psiElement, interfaces, windowTitle, labelText);
    dlg.show();
    if (dlg.isOK()) {
      implementInterface(psiElement, dlg.getInterfaces());
    }
  }

  private static void implementInterface(@NotNull final PsiElement element, @NotNull final List<GoTypeSpec> interfaces) {
    Project project = element.getProject();
    WriteCommandAction.runWriteCommandAction(project, new Runnable() {
      @Override
      public void run() {
        GoStructType struct = ((GoStructType)((GoSpecType)element).getType());
        for (GoTypeSpec goInterface : interfaces) {
          for (GoMethodSpec method : ((GoInterfaceType)goInterface.getSpecType().getType()).getMethods()) {
            GoPsiImplUtil.addSpec(struct, method);
          }
        }
      }
    });
  }

  @Nullable
  private static PsiElement getPsiElementFromContext(@NotNull AnActionEvent e) {
    PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
    Editor editor = e.getData(CommonDataKeys.EDITOR);
    if (psiFile == null || editor == null) return null;

    int offset = editor.getCaretModel().getOffset();
    PsiElement elementAt = psiFile.findElementAt(offset);
    if (elementAt == null) return null;

    GoType parentElement = PsiTreeUtil.getParentOfType(elementAt, GoSpecType.class, GoStructType.class);
    if (parentElement == null) {
      if (elementAt.getParent() instanceof GoReferenceExpression) {
        parentElement = GoPsiImplUtil.getGoType((GoReferenceExpression)elementAt.getParent(), null);
      }
      else if (elementAt.getParent() instanceof GoTypeReferenceExpression) {
        parentElement = GoPsiImplUtil.findTypeFromTypeRef((GoTypeReferenceExpression)elementAt.getParent());
      }
      else {
        return null;
      }
    }

    if (parentElement instanceof GoStructType) return parentElement;
    if (parentElement instanceof GoSpecType) {
      return ((GoSpecType)parentElement).getType() instanceof GoStructType ? parentElement : null;
    }

    return null;
  }

  private static boolean inCurrentProject(@NotNull AnActionEvent e, @NotNull PsiElement element) {
    return e.getProject() != null && e.getProject().equals(element.getProject());
  }

  @Nullable
  private static ArrayList<GoTypeSpec> getInterfaceList(@NotNull PsiElement element) {
    PsiFile file = element.getContainingFile();
    if (!(file instanceof GoFile)) return null;
    
    // TODO Get all the private interfaces from the package the struct is defined
    // TODO Get all the public interfaces from the package the struct is defined
    // TODO Get all the public interfaces from the project
    // TODO Get all the public interfaces from the GOPATH
    // TODO Get all the public interfaces from the Go SDK
    List<GoTypeSpec> types = ((GoFile)file).getTypes();
    ArrayList<GoTypeSpec> results = new ArrayList<GoTypeSpec>();
    for (GoTypeSpec type : types) {
      if (type.getSpecType().getType() instanceof GoInterfaceType) {
        results.add(type);
      }
    }
    return results;
  }
}
