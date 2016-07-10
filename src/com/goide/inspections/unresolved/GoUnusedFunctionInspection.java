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

package com.goide.inspections.unresolved;

import com.goide.GoConstants;
import com.goide.inspections.GoInspectionBase;
import com.goide.psi.GoFile;
import com.goide.psi.GoFunctionDeclaration;
import com.goide.psi.GoVisitor;
import com.goide.quickfix.GoDeleteQuickFix;
import com.goide.quickfix.GoRenameToBlankQuickFix;
import com.goide.runconfig.testing.GoTestFinder;
import com.goide.runconfig.testing.GoTestFunctionType;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.SearchRequestCollector;
import com.intellij.psi.search.SearchSession;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class GoUnusedFunctionInspection extends GoInspectionBase {
  @NotNull
  @Override
  protected GoVisitor buildGoVisitor(@NotNull final ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new GoVisitor() {
      @Override
      public void visitFunctionDeclaration(@NotNull GoFunctionDeclaration o) {
        if (o.isBlank()) return;
        GoFile file = o.getContainingFile();
        String name = o.getName();
        if (!canRun(name)) return;
        if (GoConstants.MAIN.equals(file.getPackageName()) && GoConstants.MAIN.equals(name)) return;
        if (GoConstants.INIT.equals(name)) return;
        if (GoTestFinder.isTestFile(file) && GoTestFunctionType.fromName(name) != null) return;
        if (isUnused(o)) {
          PsiElement id = o.getIdentifier();
          TextRange range = TextRange.from(id.getStartOffsetInParent(), id.getTextLength());
          holder.registerProblem(o, "Unused function <code>#ref</code> #loc", ProblemHighlightType.LIKE_UNUSED_SYMBOL, range,
                                 new GoDeleteQuickFix("Delete function", GoFunctionDeclaration.class), new GoRenameToBlankQuickFix(o));
        }
      }
    };
  }

  protected boolean canRun(String name) {
    return !StringUtil.isCapitalized(name);
  }

  private static boolean isUnused(@NotNull GoFunctionDeclaration o) {
    PsiReference ref = ReferencesSearch.search(o, o.getUseScope()).findFirst();
    if (ref == null) return true;
    PsiElement elem = ref.getElement();
    GoFunctionDeclaration parent;
    while ((parent = PsiTreeUtil.getParentOfType(elem, GoFunctionDeclaration.class)) != null) {
      if (o.equals(parent)) return complexFind(o);
      elem = parent;
    }
    return false;
  }

  private static boolean complexFind(@NotNull GoFunctionDeclaration o) {
    Collection<PsiReference> results = ReferencesSearch.search(o, o.getUseScope()).findAll();
    for (PsiReference result : results) {
      ProgressManager.checkCanceled();
      PsiElement elem = result.getElement();
      GoFunctionDeclaration parent;
      while ((parent = PsiTreeUtil.getParentOfType(elem, GoFunctionDeclaration.class)) != null) {
        if (o.equals(parent)) break;
        elem = parent;
      }
    }
    return true;
  }
}
