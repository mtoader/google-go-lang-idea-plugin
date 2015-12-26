/*
 * Copyright 2013-2015 Sergey Ignatov, Alexander Zolotov, Florin Patan, José Santos
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

package com.goide.gotemplate.file;

import com.goide.GoIcons;
import com.goide.gotemplate.GoTemplateLanguage;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.TemplateLanguageFileType;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class GoTemplateFileType extends LanguageFileType implements TemplateLanguageFileType {
  public static final LanguageFileType INSTANCE = new GoTemplateFileType();

  protected GoTemplateFileType() {
    super(GoTemplateLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public String getName() {
    return "Go template";
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Go template files";
  }

  @NotNull
  @Override
  public String getDefaultExtension() {
    return "gohtml";
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return GoIcons.ICON;
  }

  @Override
  public String getCharset(@NotNull VirtualFile file, @NotNull byte[] content) {
    return CharsetToolkit.UTF8;
  }
}
