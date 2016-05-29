/*
 * Copyright 2013-2016 Sergey Ignatov, Alexander Zolotov, Florin Patan
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

import com.goide.psi.GoFile;
import com.goide.psi.GoImportDeclaration;
import com.goide.psi.GoImportList;
import com.goide.psi.GoTopLevelDeclaration;
import com.intellij.codeInsight.editorActions.moveUpDown.LineMover;
import com.intellij.codeInsight.editorActions.moveUpDown.LineRange;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class GoStatementMover extends LineMover {
  @Override
  public boolean checkAvailable(@NotNull Editor editor, @NotNull PsiFile file, @NotNull MoveInfo info, boolean down) {
    if (!(file instanceof GoFile || super.checkAvailable(editor, file, info, down))) return false;

    Pair<PsiElement, PsiElement> primeElementRange = getElementRange(editor, file, getLineRangeFromSelection(editor));
    if (primeElementRange == null) return false;
    PsiElement commonParent =
      primeElementRange.first.isEquivalentTo(primeElementRange.second) ? primeElementRange.first.getParent() :
      PsiTreeUtil.findCommonParent(primeElementRange.first, primeElementRange.second);
    if (commonParent == null) return false;

    Pair<PsiElement, PsiElement> elementRange = getLogicElementRange(primeElementRange, commonParent);
    if (elementRange == null) return false;
    if (commonParent == elementRange.first) commonParent = commonParent.getParent();
    info.toMove = new LineRange(elementRange.first, elementRange.second);
    if (elementRange.first instanceof GoTopLevelDeclaration) {
      if (!file.isValid()) {
        return true;
      }
      GoTopLevelDeclaration toMove2 =
        (GoTopLevelDeclaration)getElementOfTypeAfter(commonParent, elementRange, GoTopLevelDeclaration.class, down);
      info.toMove2 = toMove2 == null ? null : new LineRange(toMove2);
      return true;
    }
    if (commonParent instanceof GoImportList) {
      GoImportDeclaration toMove2 =
        (GoImportDeclaration)getElementOfTypeAfter(commonParent, elementRange, GoImportDeclaration.class, down);
      info.toMove2 = toMove2 == null ? null : new LineRange(toMove2);
      return true;
    }
    setUpInfo(info, elementRange, commonParent, down);
    return true;
  }

  @Nullable
  private static Pair<PsiElement, PsiElement> getLogicElementRange(@NotNull Pair<PsiElement, PsiElement> elementRange,
                                                                   @NotNull PsiElement parent) {
    if (elementRange.first == null || elementRange.second == null) return null;
    int start = elementRange.first.getTextOffset();
    int end = elementRange.second.getTextRange().getEndOffset();

    TextRange range = parent.getTextRange();
    PsiElement[] children = parent.getChildren();
    if (range.getStartOffset() == start && (children.length == 0 || children[0].getTextOffset() > start) ||
        range.getEndOffset() == end && (children.length == 0 || children[children.length - 1].getTextRange().getEndOffset() < end)) {
      return new Pair<>(parent, parent);
    }

    PsiElement startElement = elementRange.first;
    PsiElement endElement = elementRange.second;
    for (PsiElement element : children) {
      range = element.getTextRange();
      if (range.getStartOffset() <= start && range.getEndOffset() <= end && range.getEndOffset() > start) {
        startElement = element;
      }
      if (range.getStartOffset() >= start && range.getStartOffset() < end && range.getEndOffset() >= end) {
        endElement = element;
      }
    }
    if (!startElement.getParent().isEquivalentTo(endElement.getParent())) {
      return null;
    }

    return new Pair<>(startElement, endElement);
  }

  private static void setUpInfo(@NotNull MoveInfo info,
                                @NotNull Pair<PsiElement, PsiElement> range,
                                @NotNull PsiElement parent,
                                boolean down) {
    info.toMove = new LineRange(range.first, range.second);
    info.toMove2 = null;
    GoTopLevelDeclaration block = PsiTreeUtil.getParentOfType(parent, GoTopLevelDeclaration.class);
    if (block == null) return;

    int nearLine = down ? info.toMove.endLine : info.toMove.startLine - 1;
    LineRange blockLineRange = new LineRange(block);
    if (!blockLineRange.containsLine(down ? info.toMove.endLine + 1 : info.toMove.startLine - 2)) {
      info.toMove2 = null;
      return;
    }

    info.toMove2 =
      blockLineRange.containsLine(down ? info.toMove.endLine + 1 : info.toMove.startLine - 2)
      ? new LineRange(nearLine, nearLine + 1)
      : null;
  }

  @Nullable
  private static PsiElement getElementOfTypeAfter(@NotNull PsiElement parent,
                                                  @NotNull Pair<PsiElement, PsiElement> range,
                                                  @NotNull Class<? extends PsiElement> clazz,
                                                  boolean after) {
    PsiElement[] children = parent.getChildren();
    for (int i = 0; i < children.length; i++) {
      if (children[i].isEquivalentTo(after ? range.second : range.first)) {
        int j = after ? i + 1 : i - 1;
        while (j >= 0 && j < children.length) {
          if (clazz.isInstance(children[j])) {
            return children[j];
          }
          j = after ? j + 1 : j - 1;
        }
      }
    }
    return null;
  }
}

