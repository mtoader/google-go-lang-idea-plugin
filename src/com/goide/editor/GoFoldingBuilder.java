/*
 * Copyright 2013-2014 Sergey Ignatov, Alexander Zolotov
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

package com.goide.editor;

import com.goide.GoParserDefinition;
import com.goide.GoTypes;
import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mtoader
 * Date: Aug 17, 2010
 * Time: 11:21:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class GoFoldingBuilder implements FoldingBuilder, DumbAware, GoTypes {
  @NotNull
  public FoldingDescriptor[] buildFoldRegions(@NotNull ASTNode node, @NotNull Document document) {
    List<FoldingDescriptor> descriptors = new ArrayList<FoldingDescriptor>();
    appendDescriptors(node.getPsi(), document, descriptors);
    return descriptors.toArray(new FoldingDescriptor[descriptors.size()]);
  }

  private void appendDescriptors(PsiElement psi, Document document, List<FoldingDescriptor> descriptors) {
    ASTNode node = psi.getNode();
    if (node == null || !isMultiline(psi)) return;
    IElementType type = node.getElementType();

    if (GoParserDefinition.MULTILINE_COMMENT == type) {
      descriptors.add(new FoldingDescriptor(node, node.getTextRange()));
      return;
    }

    if (TYPE_DECLARATION == type) {
      addDescriptorStartFromChildNode(descriptors, node, "{");
      return;
    }

    if (CONST_DECLARATION == type || VAR_DECLARATION == type || IMPORT_DECLARATION == type) {
      addDescriptorStartFromChildNode(descriptors, node, "(");
      return;
    }

    if (BLOCK == type) {
      descriptors.add(new FoldingDescriptor(node, node.getTextRange()));
    }

    PsiElement child = psi.getFirstChild();
    while (child != null) {
      appendDescriptors(child, document, descriptors);
      child = child.getNextSibling();
    }
  }

  private void addDescriptorStartFromChildNode(List<FoldingDescriptor> descriptors, ASTNode node, String childText) {
    ASTNode startNode = findChildOfText(node, childText);
    if (startNode != null) {
      int end = node.getStartOffset() + node.getTextLength();
      descriptors.add(new FoldingDescriptor(node, new TextRange(startNode.getStartOffset(), end)));
    }
  }

  private static ASTNode findChildOfText(ASTNode parent, String text) {
    ASTNode child = parent.getFirstChildNode();
    while (child != null) {
      if (child.getText().equals(text)) {
        break;
      }
      ASTNode sub = findChildOfText(child, text);
      if (sub != null) {
        return sub;
      }
      child = child.getTreeNext();
    }
    return child;
  }

  public String getPlaceholderText(@NotNull ASTNode node) {
    return "{ ... }";
  }

  public boolean isCollapsedByDefault(@NotNull ASTNode node) {
    return false;
  }

  private static boolean isMultiline(PsiElement element) {
    String text = element.getText();
    return text.contains("\n") || text.contains("\r") || text.contains("\r\n");
  }
}
