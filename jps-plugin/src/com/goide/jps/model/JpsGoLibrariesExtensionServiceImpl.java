/*
 * Copyright 2013-2015 Sergey Ignatov, Alexander Zolotov, Florin Patan
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

package com.goide.jps.model;

import com.goide.GoEnvironmentUtil;
import com.goide.GoPathState;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.JpsGlobal;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.JpsSimpleElement;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.module.JpsTypedModule;

import java.io.File;
import java.util.Collection;

public class JpsGoLibrariesExtensionServiceImpl extends JpsGoLibrariesExtensionService {
  private static final JpsElementChildRole<JpsGoLibraries> LIBRARIES_ROLE = JpsElementChildRoleBase.create("go.libraries.role");

  @Override
  public void setModuleLibrariesState(@NotNull JpsGoModuleProperties properties, @Nullable GoPathState state) {
    properties.setLibrariesState(state);
  }

  @NotNull
  @Override
  public GoPathState getModuleLibrariesState(@NotNull JpsSimpleElement<JpsGoModuleProperties> properties) {
    return properties.getData().getLibrariesState();
  }

  @Override
  public void setProjectLibrariesState(@NotNull JpsProject project, @Nullable GoPathState state) {
    project.getContainer().setChild(LIBRARIES_ROLE, new JpsGoLibraries(state));
  }

  @NotNull
  @Override
  public GoPathState getProjectLibrariesState(@NotNull JpsProject project) {
    final JpsGoLibraries child = project.getContainer().getChild(LIBRARIES_ROLE);
    return child != null ? child.getState() : new GoPathState();
  }

  @Override
  public void setApplicationLibrariesState(@NotNull JpsGlobal global, @Nullable GoPathState state) {
    global.getContainer().setChild(LIBRARIES_ROLE, new JpsGoLibraries(state));
  }

  @NotNull
  @Override
  public GoPathState getApplicationLibrariesState(@NotNull JpsGlobal global) {
    final JpsGoLibraries child = global.getContainer().getChild(LIBRARIES_ROLE);
    return child != null ? child.getState() : new GoPathState();
  }

  @NotNull
  @Override
  public String retrieveGoPath(@NotNull JpsTypedModule<JpsSimpleElement<JpsGoModuleProperties>> module) {
    Collection<String> parts = ContainerUtil.newLinkedHashSet();
    ContainerUtil.addIfNotNull(parts, GoEnvironmentUtil.retrieveGoPathFromEnvironment());
    ContainerUtil.addAllNotNull(parts, urlsToPaths(getModuleLibrariesState(module.getProperties()).getUrls()));
    ContainerUtil.addAllNotNull(parts, urlsToPaths(getProjectLibrariesState(module.getProject()).getUrls()));
    ContainerUtil.addAllNotNull(parts, urlsToPaths(getApplicationLibrariesState(module.getProject().getModel().getGlobal()).getUrls()));
    return StringUtil.join(parts, File.pathSeparator);
  }

  @NotNull
  private static Collection<String> urlsToPaths(@NotNull Collection<String> urls) {
    return ContainerUtil.map(urls, new Function<String, String>() {
      @Override
      public String fun(String s) {
        return VfsUtilCore.urlToPath(String.valueOf(s));
      }
    });
  }
}
