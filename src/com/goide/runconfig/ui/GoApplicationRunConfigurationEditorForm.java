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

import com.goide.runconfig.application.GoApplicationRunConfiguration;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class GoApplicationRunConfigurationEditorForm extends SettingsEditor<GoApplicationRunConfiguration> {
  private JPanel myComponent;
  private GoCommonSettingsPanel myCommonSettingsPanel;


  public GoApplicationRunConfigurationEditorForm(@NotNull final Project project) {
    super(null);
    myCommonSettingsPanel.init(project);
  }

  @Override
  protected void resetEditorFrom(@NotNull GoApplicationRunConfiguration configuration) {
    myCommonSettingsPanel.resetEditorFrom(configuration);
  }

  @Override
  protected void applyEditorTo(@NotNull GoApplicationRunConfiguration configuration) throws ConfigurationException {
    myCommonSettingsPanel.applyEditorTo(configuration);
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
