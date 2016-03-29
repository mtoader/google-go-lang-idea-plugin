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

import com.goide.psi.GoTypeSpec;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.psi.PsiElement;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ToolbarDecorator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class GoTypeSpecListDialog extends DialogWrapper {
  @NotNull private CollectionListModel<GoTypeSpec> myFields;
  @NotNull private final LabeledComponent<JPanel> myComponent;

  public GoTypeSpecListDialog(@NotNull PsiElement psiElement,
                              @NotNull ArrayList<GoTypeSpec> interfaces,
                              @NotNull String windowTitle,
                              @NotNull String labelText) {
    super(psiElement.getProject());

    setTitle(windowTitle);
    myFields = new CollectionListModel<GoTypeSpec>(interfaces);
    JList fieldList = new JList(myFields);
    fieldList.setCellRenderer(new DefaultPsiElementCellRenderer());
    ToolbarDecorator decorator = ToolbarDecorator.createDecorator(fieldList);
    decorator.disableAddAction();
    JPanel panel = decorator.createPanel();
    myComponent = LabeledComponent.create(panel, labelText);

    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myComponent;
  }

  @NotNull
  public List<GoTypeSpec> getInterfaces() {
    return myFields.getItems();
  }
}
