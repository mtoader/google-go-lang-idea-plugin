/*
 * Copyright 2013-2014 Sergey Ignatov, Alexander Zolotov
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

package com.goide.runconfig.ui;

import com.goide.GoConstants;
import com.goide.GoFileType;
import com.goide.completion.GoImportPathsCompletionProvider;
import com.goide.psi.GoFile;
import com.goide.runconfig.GoRunConfigurationBase;
import com.goide.runconfig.GoRunConfigurationWithMain;
import com.goide.util.GoUtil;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.util.TextFieldCompletionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GoRunConfigurationEditorForm extends SettingsEditor<GoRunConfigurationWithMain<?>> {
  @NotNull private final Project myProject;
  private JPanel myComponent;
  private TextFieldWithBrowseButton myFileField;
  private GoCommonSettingsPanel myCommonSettingsPanel;
  private EditorTextField myPackageField;
  private JComboBox myRunKindComboBox;
  private JLabel myPackageLabel;
  private JLabel myFileLabel;


  public GoRunConfigurationEditorForm(@NotNull final Project project) {
    super(null);
    myProject = project;
    myCommonSettingsPanel.init(project);

    installRunKindComboBox();
    installFileChoosers();
  }

  private void onRunKindChanged() {
    GoRunConfigurationBase.Kind selectedKind = (GoRunConfigurationBase.Kind) myRunKindComboBox.getSelectedItem();
    if (selectedKind == null) {
      selectedKind = GoRunConfigurationBase.Kind.PACKAGE;
    }
    boolean thePackage = selectedKind == GoRunConfigurationBase.Kind.PACKAGE;
    boolean file = selectedKind == GoRunConfigurationBase.Kind.FILE;

    myPackageField.setVisible(thePackage);
    myPackageLabel.setVisible(thePackage);
    myFileField.setVisible(file);
    myFileLabel.setVisible(file);
  }

  @Override
  protected void resetEditorFrom(@NotNull GoRunConfigurationWithMain<?> configuration) {
    myFileField.setText(configuration.getFilePath());
    myPackageField.setText(configuration.getPackage());
    myRunKindComboBox.setSelectedItem(configuration.getKind());
    myCommonSettingsPanel.resetEditorFrom(configuration);
  }

  @Override
  protected void applyEditorTo(@NotNull GoRunConfigurationWithMain<?> configuration) throws ConfigurationException {
    configuration.setFilePath(myFileField.getText());
    configuration.setPackage(myPackageField.getText());
    configuration.setKind((GoRunConfigurationBase.Kind)myRunKindComboBox.getSelectedItem());
    myCommonSettingsPanel.applyEditorTo(configuration);
  }

  private void createUIComponents() {
    myPackageField = new TextFieldCompletionProvider() {
      @Override
      protected void addCompletionVariants(@NotNull String text,
                                           int offset,
                                           @NotNull String prefix,
                                           @NotNull final CompletionResultSet result) {
        GoImportPathsCompletionProvider.addCompletions(result, myCommonSettingsPanel.getSelectedModule() ,null);
      }
    }.createEditor(myProject);
  }

  @Nullable
  private static ListCellRendererWrapper<GoRunConfigurationBase.Kind> getRunKindListCellRendererWrapper() {
    return new ListCellRendererWrapper<GoRunConfigurationBase.Kind>() {
      @Override
      public void customize(JList list, @Nullable GoRunConfigurationBase.Kind kind, int index, boolean selected, boolean hasFocus) {
        if (kind != null) {
          String kindName = StringUtil.capitalize(kind.toString().toLowerCase());
          setText(kindName);
        }
      }
    };
  }

  private void installRunKindComboBox() {
    myRunKindComboBox.removeAllItems();
    myRunKindComboBox.setRenderer(getRunKindListCellRendererWrapper());
    // TODO this should be present only for "go build" configurations not for "go run" configurations
    myRunKindComboBox.addItem(GoRunConfigurationBase.Kind.PACKAGE);
    myRunKindComboBox.addItem(GoRunConfigurationBase.Kind.FILE);
    myRunKindComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onRunKindChanged();
      }
    });
  }

  private void installFileChoosers() {
    GoUtil.installFileChooser(myProject, myFileField, false, new Condition<VirtualFile>() {
      @Override
      public boolean value(VirtualFile file) {
        if (file.getFileType() != GoFileType.INSTANCE) {
          return false;
        }
        final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
        if (psiFile != null && psiFile instanceof GoFile) {
          return GoConstants.MAIN.equals(((GoFile)psiFile).getPackageName()) && ((GoFile)psiFile).findMainFunction() != null;
        }
        return false;
      }
    });
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return myComponent;
  }

  @Override
  protected void disposeEditor() {
    myComponent.setVisible(false);
  }
}
