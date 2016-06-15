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

package com.goide.refactor;

import com.goide.psi.*;
import com.goide.psi.impl.GoPsiImplUtil;
import com.goide.psi.impl.GoTypeUtil;
import com.intellij.codeInsight.PsiEquivalenceUtil;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GoRefactoringUtil {

  private GoRefactoringUtil() {}

  @NotNull
  public static List<PsiElement> getLocalOccurrences(@NotNull PsiElement element) {
    return getOccurrences(element, PsiTreeUtil.getTopmostParentOfType(element, GoBlock.class));
  }

  @NotNull
  public static List<PsiElement> getOccurrences(@NotNull final PsiElement pattern, @Nullable PsiElement context) {
    if (context == null) return Collections.emptyList();
    final List<PsiElement> occurrences = ContainerUtil.newArrayList();
    PsiRecursiveElementVisitor visitor = new PsiRecursiveElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (PsiEquivalenceUtil.areElementsEquivalent(element, pattern)) {
          occurrences.add(element);
          return;
        }
        super.visitElement(element);
      }
    };
    context.acceptChildren(visitor);
    return occurrences;
  }

  @Nullable
  public static PsiElement findLocalAnchor(@NotNull List<PsiElement> occurrences) {
    return findAnchor(occurrences, PsiTreeUtil.getNonStrictParentOfType(PsiTreeUtil.findCommonParent(occurrences), GoBlock.class));
  }

  @Nullable
  public static PsiElement findAnchor(@NotNull List<PsiElement> occurrences, @Nullable PsiElement context) {
    PsiElement first = ContainerUtil.getFirstItem(occurrences);
    PsiElement statement = PsiTreeUtil.getNonStrictParentOfType(first, GoStatement.class);
    while (statement != null && statement.getParent() != context) {
      statement = statement.getParent();
    }
    return statement == null ? GoPsiImplUtil.getTopLevelDeclaration(first) : statement;
  }

  @NotNull
  public static Expression getParameterNameSuggestedExpression(GoExpression expression) {
    GoTopLevelDeclaration topLevelDecl = PsiTreeUtil.getParentOfType(expression, GoTopLevelDeclaration.class);
    return new ParameterNameExpression(getSuggestedNames(expression, topLevelDecl == null ? null : topLevelDecl.getNextSibling()));
  }

  private static class ParameterNameExpression extends Expression {
    private final Set<String> myNames;

    public ParameterNameExpression(@NotNull Set<String> namesSet) {
      myNames = namesSet;
    }

    @Nullable
    @Override
    public Result calculateResult(ExpressionContext context) {
      LookupElement[] lookupItems = calculateLookupItems(context);
      if (lookupItems.length == 0) return new TextResult("");

      return new TextResult(lookupItems[0].getLookupString());
    }

    @Nullable
    @Override
    public Result calculateQuickResult(ExpressionContext context) {
      return null;
    }

    @NotNull
    @Override
    public LookupElement[] calculateLookupItems(ExpressionContext context) {
      LookupElement[] lookupItems;

      PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();

      int offset = context.getStartOffset();

      Project project = context.getProject();
      PsiDocumentManager.getInstance(project).commitAllDocuments();
      assert context.getEditor() != null;
      PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(context.getEditor().getDocument());
      assert file != null;
      PsiElement elementAt = file.findElementAt(offset);

      GoParameters parameters = PsiTreeUtil.getParentOfType(elementAt, GoParameters.class);

      Set<LookupElement> set = new LinkedHashSet<LookupElement>();
      if (parameters == null) {
        for (String name : myNames) {
          set.add(LookupElementBuilder.create(name));
        }

        lookupItems = set.toArray(new LookupElement[set.size()]);
        return lookupItems;
      }

      GoParamDefinition parameter = PsiTreeUtil.getParentOfType(elementAt, GoParamDefinition.class);
      Set<String> parameterNames = new HashSet<String>();
      for (GoParameterDeclaration paramDecl : parameters.getParameterDeclarationList()) {
        for (GoParamDefinition paramDef : paramDecl.getParamDefinitionList()) {
          if (parameter == paramDef) continue;
          parameterNames.add(paramDef.getName());
        }
      }

      for (String name : myNames) {
        if (parameterNames.contains(name)) {
          int j = 1;
          while (parameterNames.contains(name + j)) j++;
          //noinspection StringConcatenationInLoop
          name += j;
        }

        set.add(LookupElementBuilder.create(name));
      }

      lookupItems = set.toArray(new LookupElement[set.size()]);
      return lookupItems;
    }
  }

  public static LinkedHashSet<String> getSuggestedNames(GoExpression expression) {
    return getSuggestedNames(expression, expression);
  }

  @NotNull
  private static LinkedHashSet<String> getSuggestedNames(GoExpression expression, PsiElement context) {
    // todo rewrite with names resolve; check occurrences contexts
    if (expression.isEquivalentTo(context)) {
      context = PsiTreeUtil.getParentOfType(context, GoBlock.class);
    }
    LinkedHashSet<String> usedNames = getNamesInContext(context);
    LinkedHashSet<String> names = ContainerUtil.newLinkedHashSet();

    if (expression instanceof GoCallExpr) {
      GoReferenceExpression callReference = PsiTreeUtil.getChildOfType(expression, GoReferenceExpression.class);
      if (callReference != null) {
        String name = StringUtil.decapitalize(callReference.getIdentifier().getText());
        for (String candidate : NameUtil.getSuggestionsByName(name, "", "", false, false, false)) {
          if (!usedNames.contains(candidate)) names.add(candidate);
        }
      }
    }

    GoType type = expression.getGoType(null);
    String typeText = GoPsiImplUtil.getText(type);
    if (StringUtil.isNotEmpty(typeText)) {
      boolean array = GoTypeUtil.isIterable(type) && !GoTypeUtil.isString(type);
      for (String candidate : NameUtil.getSuggestionsByName(typeText, "", "", false, false, array)) {
        if (!usedNames.contains(candidate) && !typeText.equals(candidate)) names.add(candidate);
      }
    }

    if (names.isEmpty()) {
      if (usedNames.contains("i")) {
        int counter = 1;
        while (usedNames.contains("i" + counter)) counter++;
        names.add("i" + counter);
      }
      else {
        names.add("i");
      }
    }
    return names;
  }

  @NotNull
  private static LinkedHashSet<String> getNamesInContext(PsiElement context) {
    if (context == null) return ContainerUtil.newLinkedHashSet();
    LinkedHashSet<String> names = ContainerUtil.newLinkedHashSet();

    for (GoNamedElement namedElement : PsiTreeUtil.findChildrenOfType(context, GoNamedElement.class)) {
      names.add(namedElement.getName());
    }
    names.addAll(((GoFile)context.getContainingFile()).getImportMap().keySet());

    GoFunctionDeclaration functionDeclaration = PsiTreeUtil.getParentOfType(context, GoFunctionDeclaration.class);
    GoSignature signature = PsiTreeUtil.getChildOfType(functionDeclaration, GoSignature.class);
    for (GoParamDefinition param : PsiTreeUtil.findChildrenOfType(signature, GoParamDefinition.class)) {
      names.add(param.getName());
    }
    return names;
  }
}
