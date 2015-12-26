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

package com.goide.gotemplate.highlighting;

import com.goide.gotemplate.GoTemplateLanguage;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.util.LayerDescriptor;
import com.intellij.openapi.editor.ex.util.LayeredLexerEditorHighlighter;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.templateLanguages.TemplateDataLanguageMappings;

import static com.goide.gotemplate.GoTemplateTypes.TEXT;

public class GoTemplateLayeredSyntaxHighlighter extends LayeredLexerEditorHighlighter {
  public GoTemplateLayeredSyntaxHighlighter(Project project, EditorColorsScheme scheme, FileType ptype, VirtualFile virtualFile) {
    super(new GoTemplateSyntaxHighlighter(), scheme);

    // highlighter for outer lang
    FileType type = null;
    if (project == null || virtualFile == null) {
      type = StdFileTypes.PLAIN_TEXT;
    }
    else {
      Language language = TemplateDataLanguageMappings.getInstance(project).getMapping(virtualFile);
      if (language != null) type = language.getAssociatedFileType();
      if (type == null) type = GoTemplateLanguage.getDefaultTemplateLang();
    }

    SyntaxHighlighter outerHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(type, project, virtualFile);
    registerLayer(TEXT, new LayerDescriptor(outerHighlighter, ""));
  }
}
