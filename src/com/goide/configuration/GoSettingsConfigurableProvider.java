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

package com.goide.configuration;

import com.goide.ui.GoSettingsConfigurable;
import com.intellij.openapi.options.CompositeConfigurable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class GoSettingsConfigurableProvider extends ConfigurableProvider {

  @Nullable
  @Override
  public Configurable createConfigurable() {
    return goConfigurable();
  }

  private static Configurable goConfigurable() {
    return new CompositeConfigurable<UnnamedConfigurable>() {

      @Nullable
      @Override
      public JComponent createComponent() {
        final List<UnnamedConfigurable> configurables = getConfigurables();
        final GridLayoutManager layoutManager = new GridLayoutManager(configurables.size() + 1, 1, new Insets(0, 0, 0, 0), -1, -1);
        final JPanel rootPanel = new JPanel(layoutManager);
        final Spacer spacer = new Spacer();
        rootPanel.add(spacer, new GridConstraints(configurables.size(), 0, 1, 1, GridConstraints.ANCHOR_SOUTH,
                                                  GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED,
                                                  GridConstraints.SIZEPOLICY_FIXED, null, null, null));

        for (int i = 0; i < configurables.size(); i++) {
          UnnamedConfigurable configurable = configurables.get(i);
          final JComponent configurableComponent = configurable.createComponent();
          assert configurableComponent != null;
          rootPanel.add(configurableComponent, configurableConstrains(i));
        }

        rootPanel.revalidate();
        return rootPanel;
      }

      @Override
      protected List<UnnamedConfigurable> createConfigurables() {
        final List<UnnamedConfigurable> result = ContainerUtil.newArrayList();
        result.add(new GoSettingsConfigurable());
        return result;
      }

      @Nls
      @Override
      public String getDisplayName() {
        return "Go Settings";
      }

      @Nullable
      @Override
      public String getHelpTopic() {
        return null;
      }

      @NotNull
      private GridConstraints configurableConstrains(int i) {
        return new GridConstraints(i, 0, 1, 1, GridConstraints.ANCHOR_NORTHEAST, GridConstraints.FILL_BOTH,
                                   GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_WANT_GROW |
                                   GridConstraints.SIZEPOLICY_CAN_SHRINK,
                                   GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK,
                                   null, null, null);
      }
    };
  }
}
