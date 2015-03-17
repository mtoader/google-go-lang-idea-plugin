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

import com.intellij.openapi.components.*;
import org.jetbrains.annotations.NotNull;

@State(
  name = "GoSettings",
  storages = {
    @Storage(id = "other", file = StoragePathMacros.APP_CONFIG + "/go.xml")
  }
)
public class GoApplicationSettings implements PersistentStateComponent<GoApplicationSettings.GoApplicationSettingsBean> {

  public static class GoApplicationSettingsBean {
    public boolean goFmtOnSave = true;
    public boolean goimportsOnSave = false;
  }

  private GoApplicationSettingsBean bean;

  @Override
  @NotNull
  public GoApplicationSettingsBean getState() {
    return bean != null ? bean : new GoApplicationSettingsBean();
  }

  @Override
  public void loadState(GoApplicationSettingsBean settingsBean) {
    this.bean = settingsBean;
  }

  @NotNull
  public static GoApplicationSettings getInstance() {
    return ServiceManager.getService(GoApplicationSettings.class);
  }
}
