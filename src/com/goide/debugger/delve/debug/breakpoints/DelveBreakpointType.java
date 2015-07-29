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

package com.goide.debugger.delve.debug.breakpoints;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DelveBreakpointType extends XLineBreakpointType<DelveBreakpointProperties> {
  private static final Logger LOG = Logger.getInstance(DelveBreakpointType.class);

  protected DelveBreakpointType() {
    super("delve", "Delve Breakpoint");
  }

  @Nullable
  @Override
  public DelveBreakpointProperties createBreakpointProperties(VirtualFile file, int line) {
    LOG.warn("createBreakpointProperties: stub");
    return null;
  }

  @Override
  public boolean canPutAt(@NotNull VirtualFile file, int line, @NotNull Project project) {
    String extension = file.getExtension();
    return (extension != null && extension.equals("go"));
  }
}
