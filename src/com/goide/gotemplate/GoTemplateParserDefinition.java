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

package com.goide.gotemplate;

import com.goide.gotemplate.file.GoTemplateFileElementType;
import com.goide.gotemplate.lexer.GoTemplateLexer;
import com.goide.gotemplate.parser.GoTemplateParser;
import com.goide.gotemplate.psi.GoTemplateFile;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

import static com.goide.gotemplate.GoTemplateTypes.Factory;

public class GoTemplateParserDefinition implements ParserDefinition {

  @NotNull
  @Override
  public Lexer createLexer(Project project) {
    return new GoTemplateLexer();
  }

  @Override
  public PsiParser createParser(Project project) {
    return new GoTemplateParser();
  }

  @Override
  public IFileElementType getFileNodeType() {
    return GoTemplateFileElementType.INSTANCE;
  }

  @NotNull
  @Override
  public TokenSet getWhitespaceTokens() {
    return GoTemplateTokens.tsWHITESPACE;
  }

  @NotNull
  @Override
  public TokenSet getCommentTokens() {
    return GoTemplateTokens.tsCOMMENTS;
  }

  @NotNull
  @Override
  public TokenSet getStringLiteralElements() {
    return GoTemplateTokens.tsSTRINGS;
  }

  @NotNull
  @Override
  public PsiElement createElement(ASTNode node) {
    return Factory.createElement(node);
  }

  @Override
  public PsiFile createFile(FileViewProvider provider) {
    return new GoTemplateFile(provider);
  }

  @Override
  public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode node, ASTNode node1) {
    return SpaceRequirements.MAY;
  }
}
