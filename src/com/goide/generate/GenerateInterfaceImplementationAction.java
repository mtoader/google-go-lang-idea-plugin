/*
 * Copyright 2013-2015 Sergey Ignatov, Alexander Zolotov, Mihai Toader, Florin Patan
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

package com.goide.generate;

import com.goide.psi.GoInterfaceType;
import com.goide.psi.GoMethodSpec;
import com.goide.psi.GoStructType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GenerateInterfaceImplementationAction extends AnAction {
  public void actionPerformed(@NotNull AnActionEvent e) {
    GoStructType struct = getPsiStructFromContext(e);
    if (struct == null) {
      return;
    }
    GenerateInterfaceImplementationDialog dialog = new GenerateInterfaceImplementationDialog(struct);
    dialog.show();
    if (!dialog.isOK()) {
      return;
    }

    generateInterfaceImplementation(struct, dialog.getSelectedInterfaces(), dialog.withPointerReference());
  }

  private static void generateInterfaceImplementation(@NotNull final GoStructType struct,
                                                      @NotNull final List<GoInterfaceType> interfaces,
                                                      final boolean withPointerReference) {
    if (interfaces.size() == 0) {
      return;
    }

    new WriteCommandAction.Simple(struct.getProject(), struct.getContainingFile()) {
      @Override
      protected void run() throws Throwable {

        StringBuilder prototypeBuffer = new StringBuilder("func (");
        prototypeBuffer.append("func (")
          .append(struct.getStruct().getText().substring(0, 1).toLowerCase());
        if (withPointerReference) {
          prototypeBuffer.append("*");
        }
        prototypeBuffer.append(struct.getStruct().getText()).append(") ");

        String prototype = prototypeBuffer.toString();

        StringBuilder builder = new StringBuilder("");
        for (GoInterfaceType goInterface : interfaces) {
          for (GoMethodSpec method : goInterface.getMethods()) {
            builder.append(prototype)
              .append(method.getSignature())
              .append(" {\n\t// TODO implement this\n}\n");
          }
        }

        builder.toString();
      }
    };
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    GoStructType struct = getPsiStructFromContext(e);
    e.getPresentation().setEnabled(struct != null);
  }

  private static GoStructType getPsiStructFromContext(@NotNull AnActionEvent e) {
    PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
    Editor editor = e.getData(CommonDataKeys.EDITOR);
    if (psiFile == null || editor == null) {
      return null;
    }

    int offset = editor.getCaretModel().getOffset();
    PsiElement elementAt = psiFile.findElementAt(offset);
    return PsiTreeUtil.getParentOfType(elementAt, GoStructType.class);
  }
}
