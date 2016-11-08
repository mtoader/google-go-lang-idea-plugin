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

package com.goide.intentions;

import com.goide.psi.*;
import com.goide.psi.impl.GoElementFactory;
import com.goide.psi.impl.GoPsiImplUtil;
import com.intellij.codeInsight.intention.BaseElementAtCaretIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

import static com.intellij.util.containers.ContainerUtil.*;

public class GoMoveToStructInitializationIntention extends BaseElementAtCaretIntentionAction {
  public static final String NAME = "Move field assignment to struct initialization";

  public GoMoveToStructInitializationIntention() {
    setText(NAME);
  }

  @Nls
  @NotNull
  @Override
  public String getFamilyName() {
    return NAME;
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
    return getData(element) != null;
  }

  @Nullable
  private static Data getData(@NotNull PsiElement element) {
    if (!element.isValid() || !element.isWritable()) return null;
    GoAssignmentStatement assignment = getValidAssignmentParent(element);
    GoStatement prevStatement = assignment != null ? PsiTreeUtil.getPrevSiblingOfType(assignment, GoStatement.class) : null;
    GoReferenceExpression selectedExpression = prevStatement != null
                                               ? getFieldReferenceExpression(element, assignment, prevStatement) : null;
    if (selectedExpression == null) return null;

    GoVarDefinition definition = getDefinition(selectedExpression);
    GoType type = definition != null ? definition.getGoType(null) : null;
    GoStructType structType = type != null ? ObjectUtils.tryCast(type.getUnderlyingType(), GoStructType.class) : null;
    if (structType == null) return null;

    boolean needReplaceDeclaration = isUnassigned(getSingleVarSpec(prevStatement, definition));
    GoCompositeLit compositeLit = !needReplaceDeclaration ? getStructLiteralByReference(selectedExpression, prevStatement) : null;
    if (compositeLit == null && !needReplaceDeclaration) return null;

    List<GoReferenceExpression> expressions = getUninitializedReferenceExpressions(assignment, prevStatement, definition, compositeLit,
                                                                                   structType);
    return !expressions.isEmpty() ? new Data(assignment, compositeLit, expressions, prevStatement, definition) : null;
  }

  @Nullable
  private static GoCompositeLit createStructLiteral(@NotNull GoVarDefinition definition, @NotNull Project project) {
    GoType type = definition.getGoType(null);
    return type != null ? GoElementFactory.createCompositeLit(project, type) : null;
  }

  @Nullable
  private static GoVarDefinition getDefinition(@NotNull GoReferenceExpression referenceExpressions) {
    return ObjectUtils.tryCast(resolveQualifier(referenceExpressions), GoVarDefinition.class);
  }

  @Nullable
  private static GoAssignmentStatement getValidAssignmentParent(@Nullable PsiElement element) {
    GoAssignmentStatement assignment = PsiTreeUtil.getNonStrictParentOfType(element, GoAssignmentStatement.class);
    return assignment != null && assignment.isValid() && getLeftHandElements(assignment).size() == assignment.getExpressionList().size()
           ? assignment : null;
  }

  @Nullable
  private static GoReferenceExpression getFieldReferenceExpression(@NotNull PsiElement selectedElement,
                                                                   @NotNull GoAssignmentStatement assignment,
                                                                   @NotNull GoStatement prevStatement) {
    GoReferenceExpression selectedExpression = PsiTreeUtil.getTopmostParentOfType(selectedElement, GoReferenceExpression.class);
    if (isFieldReferenceExpression(selectedExpression)) {
      return !isAssignedInStatement(getRightExpression(selectedExpression, assignment), prevStatement) ? selectedExpression : null;
    }

    List<GoReferenceExpression> fieldReferenceExpressions = getFieldReferenceExpressions(assignment);
    if (exists(fieldReferenceExpressions, expression ->
      isAssignedInStatement(getRightExpression(expression, assignment), prevStatement))) {
      return null;
    }

    Set<PsiElement> resolvedQualifiers = map2Set(fieldReferenceExpressions, GoMoveToStructInitializationIntention::resolveQualifier);
    return resolvedQualifiers.size() == 1 ? getFirstItem(fieldReferenceExpressions) : null;
  }

  @NotNull
  private static List<GoReferenceExpression> getFieldReferenceExpressions(@NotNull GoAssignmentStatement assignment) {
    return filter(map(getLeftHandElements(assignment), GoMoveToStructInitializationIntention::unwrapParensAndCast),
                  GoMoveToStructInitializationIntention::isFieldReferenceExpression);
  }

  @Nullable
  private static GoReferenceExpression unwrapParensAndCast(@Nullable PsiElement element) {
    while (element instanceof GoParenthesesExpr) {
      element = ((GoParenthesesExpr)element).getExpression();
    }
    return ObjectUtils.tryCast(element, GoReferenceExpression.class);
  }

  @Nullable
  @Contract("_, null -> null; null, _ -> null")
  private static GoVarSpec getSingleVarSpec(@Nullable GoStatement statement, @Nullable GoVarDefinition definition) {
    GoVarDeclaration declaration = statement != null ? statement.getVarDeclaration() : null;
    List<GoVarSpec> varSpecs = declaration != null ? declaration.getVarSpecList() : emptyList();
    GoVarSpec singleVarSpec = varSpecs.size() == 1 ? getFirstItem(varSpecs) : null;
    List<GoVarDefinition> varDefinitions = singleVarSpec != null ? singleVarSpec.getVarDefinitionList() : emptyList();
    return varDefinitions.size() == 1 && definition == getFirstItem(varDefinitions) ? singleVarSpec : null;
  }

  @Contract("null -> false")
  private static boolean isUnassigned(@Nullable GoVarSpec varSpec) {
    return varSpec != null && varSpec.getExpressionList().isEmpty();
  }

  @Contract("null -> false")
  private static boolean isFieldReferenceExpression(@Nullable GoReferenceExpression element) {
    return element != null && isFieldDefinition(element.resolve());
  }

  @Contract("null -> false")
  private static boolean isFieldDefinition(@Nullable PsiElement element) {
    return element instanceof GoFieldDefinition || element instanceof GoAnonymousFieldDefinition;
  }

  private static boolean isAssignedInStatement(@Nullable GoReferenceExpression referenceExpression, @NotNull GoStatement statement) {
    PsiElement resolve = referenceExpression != null ? referenceExpression.resolve() : null;
    return resolve != null && exists(getLeftHandElements(statement), element -> isResolvedTo(element, resolve));
  }

  @Nullable
  private static GoReferenceExpression getRightExpression(@NotNull GoExpression expression, @NotNull GoAssignmentStatement assignment) {
    return unwrapParensAndCast(GoPsiImplUtil.getRightExpression(assignment, getTopmostExpression(expression)));
  }

  @NotNull
  private static GoExpression getTopmostExpression(@NotNull GoExpression expression) {
    return ObjectUtils.notNull(PsiTreeUtil.getTopmostParentOfType(expression, GoExpression.class), expression);
  }

  private static boolean isResolvedTo(@Nullable PsiElement element, @NotNull PsiElement resolve) {
    if (element instanceof GoVarDefinition) return resolve == element;

    GoReferenceExpression refExpression = unwrapParensAndCast(element);
    return refExpression != null && refExpression.resolve() == resolve;
  }

  @NotNull
  private static List<GoReferenceExpression> getUninitializedReferenceExpressions(@NotNull GoAssignmentStatement assignment,
                                                                                  @NotNull GoStatement prevStatement,
                                                                                  @NotNull GoVarDefinition definition,
                                                                                  @Nullable GoCompositeLit compositeLit,
                                                                                  @NotNull GoStructType type) {
    List<GoReferenceExpression> uninitializedFieldReferences = filter(
      getUninitializedFieldReferenceExpressions(assignment, prevStatement, compositeLit, type),
      element -> isResolvedTo(element.getQualifier(), definition));
    MultiMap<PsiElement, GoReferenceExpression> resolved = groupBy(uninitializedFieldReferences, GoReferenceExpression::resolve);
    return map(filter(resolved.entrySet(), set -> set.getValue().size() == 1), set -> getFirstItem(set.getValue()));
  }

  @Nullable
  private static GoCompositeLit getStructLiteralByReference(@NotNull GoReferenceExpression fieldReferenceExpression,
                                                            @NotNull GoStatement statement) {
    if (statement instanceof GoSimpleStatement) {
      return getStructLiteral(fieldReferenceExpression, (GoSimpleStatement)statement);
    }
    if (statement instanceof GoAssignmentStatement) {
      return getStructLiteral(fieldReferenceExpression, (GoAssignmentStatement)statement);
    }
    return getStructLiteral(getDefinition(fieldReferenceExpression), statement);
  }

  @Nullable
  private static GoCompositeLit getStructLiteral(@NotNull GoReferenceExpression fieldReferenceExpression,
                                                 @NotNull GoSimpleStatement structDeclaration) {
    GoShortVarDeclaration varDeclaration = structDeclaration.getShortVarDeclaration();
    if (varDeclaration == null) return null;

    PsiElement resolve = resolveQualifier(fieldReferenceExpression);
    GoVarDefinition structVarDefinition = find(varDeclaration.getVarDefinitionList(), definition -> resolve == definition);
    return structVarDefinition != null ? ObjectUtils.tryCast(structVarDefinition.getValue(), GoCompositeLit.class) : null;
  }

  @Nullable
  @Contract("null -> null")
  private static PsiElement resolveQualifier(@NotNull GoReferenceExpression fieldReferenceExpression) {
    GoReferenceExpression qualifier = fieldReferenceExpression.getQualifier();
    return qualifier != null ? qualifier.resolve() : null;
  }

  @Nullable
  private static GoCompositeLit getStructLiteral(@NotNull GoReferenceExpression fieldReferenceExpression,
                                                 @NotNull GoAssignmentStatement structAssignment) {
    GoVarDefinition varDefinition = getDefinition(fieldReferenceExpression);
    PsiElement field = fieldReferenceExpression.resolve();
    if (varDefinition == null || !isFieldDefinition(field) || !hasStructTypeWithField(varDefinition, (GoNamedElement)field)) {
      return null;
    }

    GoExpression structReferenceExpression = find(structAssignment.getLeftHandExprList().getExpressionList(),
                                                  expression -> isResolvedTo(expression, varDefinition));
    if (structReferenceExpression == null) return null;
    GoExpression compositeLit = GoPsiImplUtil.getRightExpression(structAssignment, structReferenceExpression);
    return ObjectUtils.tryCast(compositeLit, GoCompositeLit.class);
  }

  @Nullable
  @Contract("null, _ -> null")
  private static GoCompositeLit getStructLiteral(@Nullable GoVarDefinition definition, @NotNull GoStatement statement) {
    GoVarSpec varSpec = definition != null ? getSingleVarSpec(statement, definition) : null;
    return varSpec != null ? ObjectUtils.tryCast(getFirstItem(varSpec.getRightExpressionsList()), GoCompositeLit.class) : null;
  }

  private static boolean hasStructTypeWithField(@NotNull GoVarDefinition structVarDefinition, @NotNull GoNamedElement field) {
    GoType type = structVarDefinition.getGoType(null);
    GoStructType structType = type != null ? ObjectUtils.tryCast(type.getUnderlyingType(), GoStructType.class) : null;
    return structType != null && PsiTreeUtil.isAncestor(structType, field, true);
  }

  private static boolean isFieldInitialization(@NotNull GoElement element, @NotNull PsiElement field) {
    GoKey key = element.getKey();
    GoFieldName fieldName = key != null ? key.getFieldName() : null;
    return fieldName != null && fieldName.resolve() == field;
  }

  @NotNull
  private static List<GoReferenceExpression> getUninitializedFieldReferenceExpressions(@NotNull GoAssignmentStatement assignment,
                                                                                       @NotNull GoStatement prevStatement,
                                                                                       @Nullable GoCompositeLit compositeLit,
                                                                                       @NotNull GoStructType type) {
    return filter(getFieldReferenceExpressions(assignment), expression ->
      isUninitializedFieldReferenceExpression(expression, compositeLit, type) &&
      !isAssignedInStatement(getRightExpression(expression, assignment), prevStatement));
  }

  private static boolean isUninitializedFieldReferenceExpression(@NotNull GoReferenceExpression fieldReferenceExpression,
                                                                 @Nullable GoCompositeLit compositeLit,
                                                                 @NotNull GoStructType type) {
    PsiElement resolve = fieldReferenceExpression.resolve();
    if (resolve == null || !PsiTreeUtil.isAncestor(type, resolve, true)) return false;

    GoLiteralValue literalValue = compositeLit != null ? compositeLit.getLiteralValue() : null;
    if (literalValue == null) return true;

    return isFieldDefinition(resolve) && !exists(literalValue.getElementList(), element -> isFieldInitialization(element, resolve));
  }

  @NotNull
  private static List<? extends PsiElement> getLeftHandElements(@NotNull GoStatement statement) {
    if (statement instanceof GoSimpleStatement) {
      GoShortVarDeclaration varDeclaration = ((GoSimpleStatement)statement).getShortVarDeclaration();
      return varDeclaration != null ? varDeclaration.getVarDefinitionList() : emptyList();
    }
    if (statement instanceof GoAssignmentStatement) {
      return ((GoAssignmentStatement)statement).getLeftHandExprList().getExpressionList();
    }
    return emptyList();
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
    Data data = getData(element);
    if (data == null) return;

    boolean needReplaceDeclaration = data.myCompositeLit == null;
    GoCompositeLit compositeLit = needReplaceDeclaration ? createStructLiteral(data.myStructDefinition, project) : data.myCompositeLit;
    if (compositeLit == null) return;

    moveFieldReferenceExpressions(data.myReferenceExpressions, compositeLit.getLiteralValue(), data.myAssignment);
    if (needReplaceDeclaration) {
      String definitionText = data.myStructDefinition.getText();
      GoStatement shortVarStatement = GoElementFactory.createShortVarDeclarationStatement(project, definitionText, compositeLit.getText());
      data.myStructDeclaration.replace(shortVarStatement);
    }
  }

  private static void moveFieldReferenceExpressions(@NotNull List<GoReferenceExpression> referenceExpressions,
                                                    @Nullable GoLiteralValue literalValue,
                                                    @NotNull GoAssignmentStatement parentAssignment) {
    if (literalValue == null) return;
    for (GoReferenceExpression expression : referenceExpressions) {
      GoExpression anchor = getTopmostExpression(expression);
      GoExpression fieldValue = GoPsiImplUtil.getRightExpression(parentAssignment, anchor);
      if (fieldValue == null) continue;

      GoPsiImplUtil.deleteExpressionFromAssignment(parentAssignment, anchor);
      addFieldDefinition(literalValue, expression.getIdentifier().getText(), fieldValue.getText());
    }
  }

  private static void addFieldDefinition(@NotNull GoLiteralValue literalValue, @NotNull String name, @NotNull String value) {
    Project project = literalValue.getProject();
    PsiElement newField = GoElementFactory.createLiteralValueElement(project, name, value);
    PsiElement rbrace = literalValue.getRbrace();
    if (!literalValue.getElementList().isEmpty()) {
      literalValue.addBefore(GoElementFactory.createComma(project), rbrace);
    }
    literalValue.addBefore(newField, rbrace);
  }

  private static class Data {
    @Nullable private final GoCompositeLit myCompositeLit;
    @NotNull private final GoAssignmentStatement myAssignment;
    @NotNull private final List<GoReferenceExpression> myReferenceExpressions;
    @NotNull private final GoStatement myStructDeclaration;
    @NotNull private final GoVarDefinition myStructDefinition;

    public Data(@NotNull GoAssignmentStatement assignment,
                @Nullable GoCompositeLit compositeLit,
                @NotNull List<GoReferenceExpression> referenceExpressions,
                @NotNull GoStatement structDeclaration,
                @NotNull GoVarDefinition structDefinition) {
      myCompositeLit = compositeLit;
      myAssignment = assignment;
      myReferenceExpressions = referenceExpressions;
      myStructDeclaration = structDeclaration;
      myStructDefinition = structDefinition;
    }
  }
}


