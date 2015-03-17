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

package com.goide.ui;

import com.goide.GoIcons;
import com.goide.settings.GoApplicationSettings;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class GoSettingsConfigurable implements SearchableConfigurable {
  private JRadioButton doNothingOnSave;
  private JRadioButton goFmtOnSave;
  private JRadioButton goimportsOnSave;
  private ButtonGroup myButtonGroup = new ButtonGroup();
  private final JPanel myPanel;

  public GoSettingsConfigurable() {
    doNothingOnSave = new JRadioButton("Do nothing");
    doNothingOnSave.setMnemonic('n');
    goFmtOnSave = new JRadioButton("go fmt", true);
    goFmtOnSave.setMnemonic('f');
    goimportsOnSave = new JRadioButton("goimports");
    goimportsOnSave.setMnemonic('i');

    myButtonGroup.add(doNothingOnSave);
    myButtonGroup.add(goFmtOnSave);
    myButtonGroup.add(goimportsOnSave);

    final GridLayoutManager layoutManager = new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1);
    myPanel = new JPanel(layoutManager);
    final Spacer spacer = new Spacer();
    myPanel.add(spacer, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST,
                                              GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                              GridConstraints.SIZEPOLICY_FIXED, null, null, null));

    myPanel.add(doNothingOnSave, buttonConstrains(0));
    myPanel.add(goFmtOnSave, buttonConstrains(1));
    myPanel.add(goimportsOnSave, buttonConstrains(2));
  }

  @NotNull
  @Override
  public String getId() {
    return "go";
  }

  @Override
  public Runnable enableSearch(String option) {
    return null;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Go Settings";
  }

  public Icon getIcon() {
    return GoIcons.ICON;
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }

  @Override
  public JComponent createComponent() {
    return myPanel;
  }

  @Override
  public boolean isModified() {
    GoApplicationSettings.GoApplicationSettingsBean settingsBean = GoApplicationSettings.getInstance().getState();

    if (settingsBean.goFmtOnSave != goFmtOnSave.isSelected()) {
      return true;
    }

    if (settingsBean.goimportsOnSave != goimportsOnSave.isSelected()) {
      return true;
    }

    return false;
  }

  @Override
  public void apply() throws ConfigurationException {
    GoApplicationSettings.GoApplicationSettingsBean settingsBean = GoApplicationSettings.getInstance().getState();

    settingsBean.goFmtOnSave = goFmtOnSave.isSelected();
    settingsBean.goimportsOnSave = goimportsOnSave.isSelected();

    GoApplicationSettings.getInstance().loadState(GoApplicationSettings.getInstance().getState());
  }

  @Override
  public void reset() {
    GoApplicationSettings.GoApplicationSettingsBean settingsBean = GoApplicationSettings.getInstance().getState();

    doNothingOnSave.setSelected(!settingsBean.goFmtOnSave && !settingsBean.goimportsOnSave);
    goFmtOnSave.setSelected(settingsBean.goFmtOnSave);
    goimportsOnSave.setSelected(settingsBean.goimportsOnSave);
  }

  @Override
  public void disposeUIResources() {

  }

  @NotNull
  private static GridConstraints buttonConstrains(int i) {
    return new GridConstraints(i, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE,
                               GridConstraints.SIZEPOLICY_FIXED,
                               GridConstraints.SIZEPOLICY_FIXED,
                               null, null, null);
  }
}
