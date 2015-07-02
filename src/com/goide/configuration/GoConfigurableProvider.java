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

import com.goide.sdk.GoSdkService;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.labels.LinkLabel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class GoConfigurableProvider extends ConfigurableProvider {
  @NotNull private final Project myProject;

  public GoConfigurableProvider(@NotNull Project project) {
    myProject = project;
  }

  @Nullable
  @Override
  public Configurable createConfigurable() {
    Configurable librariesConfigurable = new GoLibrariesConfigurableProvider(myProject).createConfigurable();
    Configurable sdkConfigurable = GoSdkService.getInstance(myProject).createSdkConfigurable();
    Configurable buildFlagsConfigurable = new GoBuildTargetConfigurable(myProject, false);
    if (sdkConfigurable != null) {
      return new GoCompositeConfigurable(myProject, sdkConfigurable, buildFlagsConfigurable, librariesConfigurable);
    }
    else {
      return new GoCompositeConfigurable(myProject, buildFlagsConfigurable, librariesConfigurable);
    }
  }

  private static class GoCompositeConfigurable extends SearchableConfigurable.Parent.Abstract {
    private Configurable[] myConfigurables;
    @NotNull private final Project myProject;

    public GoCompositeConfigurable(@NotNull Project project, Configurable... configurables) {
      myProject = project;
      myConfigurables = configurables;
    }

    @Override
    public JComponent createComponent() {
      JComponent content = new JPanel(new BorderLayout());
      content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      content.add(BorderLayout.NORTH, new JLabel("Go settings"));

      JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
      content.add(BorderLayout.CENTER, panel);
      panel.add(Box.createVerticalStrut(10));
      for (final Configurable current : myConfigurables) {
        LinkLabel label = new LinkLabel(current.getDisplayName(), null) {
          @Override
          public void doClick() {
            openLink(current);
          }
        };
        label.setBorder(BorderFactory.createEmptyBorder(1, 17, 3, 1));
        panel.add(label);
      }
      return content;
    }

    void openLink(Configurable configurable) {
      ShowSettingsUtil.getInstance().editConfigurable(myProject, configurable);
    }

    @Override
    protected Configurable[] buildConfigurables() {
      return myConfigurables;
    }

    @NotNull
    @Override
    public String getId() {
      return "go";
    }

    @Nls
    @Override
    public String getDisplayName() {
      return "Go";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
      return null;
    }

    @Override
    public void disposeUIResources() {
      super.disposeUIResources();
      myConfigurables = null;
    }
  }
}
