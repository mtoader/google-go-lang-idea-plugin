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

package com.goide.settings;

import com.goide.GoConstants;
import com.goide.util.GoUtil;
import com.intellij.openapi.components.*;
import com.intellij.openapi.roots.impl.ModuleRootManagerComponent;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

@State(
  name = GoConstants.GO_SERVICE_NAME,
  reloadable = true,
  storages = {
    @Storage(id = "other", file = StoragePathMacros.APP_CONFIG + "/" + GoConstants.GO_CONFIG_FILE, roamingType = RoamingType.PER_PLATFORM)
  }
)
public class GoPluginSettings implements PersistentStateComponent<Element> {

  private static final String PLUGIN_VERSION = GoUtil.getPlugin().getVersion();
  private String projectTutorialShown = "";

  public static GoPluginSettings getInstance() {
    return ServiceManager.getService(GoPluginSettings.class);
  }

  @Nullable
  @Override
  public Element getState() {
    final Element element = new Element("GoPluginSettings");
    element.setAttribute("projectTutorialShown", projectTutorialShown);

    return element;
  }

  @Override
  public void loadState(Element element) {
    String value = element.getAttributeValue("projectTutorialShown");
    if (value != null) projectTutorialShown = value;
  }

  public boolean isProjectTutorialShown() {
    return projectTutorialShown != null && projectTutorialShown.equals(PLUGIN_VERSION);
  }

  public void setProjectTutorialShown() {
    projectTutorialShown = PLUGIN_VERSION;
  }
}
