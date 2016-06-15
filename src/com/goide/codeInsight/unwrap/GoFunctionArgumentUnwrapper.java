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

package com.goide.codeInsight.unwrap;

import com.goide.psi.GoArgumentList;
import com.goide.psi.GoCallExpr;
import com.goide.psi.GoExpression;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;

import java.util.List;

public class GoFunctionArgumentUnwrapper extends GoUnwrapper {
  public GoFunctionArgumentUnwrapper() {
    super("Unwrap argument");
  }

  @Override
  public boolean isApplicableTo(PsiElement e) {
    return e instanceof GoExpression && e.getParent() instanceof GoArgumentList;
  }

  @Override
  protected void doUnwrap(PsiElement element, Context context) throws IncorrectOperationException {
    GoCallExpr call = (GoCallExpr)element.getParent().getParent();
    context.extractElement(element, call);
    context.delete(call);
  }

  @Override
  public String getDescription(PsiElement e) {
    String text = e.getText();
    if (text.length() > 20) text = text.substring(0, 17) + "...";
    return "Unwrap " + text;
  }

  @Override
  public PsiElement collectAffectedElements(PsiElement e, List<PsiElement> toExtract) {
    super.collectAffectedElements(e, toExtract);
    return e.getParent().getParent();
  }
}
