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

package com.goide.psi.impl;

import com.goide.GoConstants;
import com.goide.psi.*;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.SyntaxTraverser;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class GoTypeUtil {
  private static final Comparator<GoNamedElement> BY_NAME = new Comparator<GoNamedElement>() {
    @Override
    public int compare(@NotNull GoNamedElement o1, @NotNull GoNamedElement o2) {
      return Comparing.compare(o1.getName(), o2.getName());
    }
  };
  public static final Function<Pair<GoType, Boolean>, String> TO_STRING = new Function<Pair<GoType, Boolean>, String>() {
    @Override
    public String fun(@NotNull Pair<GoType, Boolean> t) {
      return t.first.getText();
    }
  };
  public static final Function<Pair<GoType, Boolean>, String> TO_STRING_WITH_VARIADIC =  new Function<Pair<GoType, Boolean>, String>() {
    @NotNull
    @Override
    public String fun(@NotNull Pair<GoType, Boolean> pair) {
      return pair.first.getText() + " " + pair.second;
    }
  };

  private static final Set NUMERIC_TYPES = ContainerUtil.newHashSet("int", "int8", "int16", "int32", "int64", "uint", "uint8", "uint16",
                                                                    "uint32", "uint64", "float32", "float64", "complex64", "complex128",
                                                                    "rune", "byte");

  /**
   * https://golang.org/ref/spec#For_statements
   * The expression on the right in the "range" clause is called the range expression,
   * which may be an array, pointer to an array, slice, string, map, or channel permitting receive operations.
   */
  public static boolean isIterable(@Nullable GoType type) {
    type = type != null ? type.getUnderlyingType() : null;
    return type instanceof GoArrayOrSliceType ||
           type instanceof GoPointerType && isArray(((GoPointerType)type).getType()) ||
           type instanceof GoMapType ||
           type instanceof GoChannelType ||
           isString(type);
  }

  private static boolean isArray(@Nullable GoType type) {
    type = type != null ? type.getUnderlyingType() : null;
    return type instanceof GoArrayOrSliceType && ((GoArrayOrSliceType)type).getExpression() != null;
  }

  public static boolean isString(@Nullable GoType type) {
    return isBuiltinType(type, "string");
  }

  public static boolean isBoolean(@Nullable GoType type) {
    return isBuiltinType(type, "bool");
  }

  private static boolean isBuiltinType(@Nullable GoType type, @Nullable String builtinTypeName) {
    if (builtinTypeName == null || type == null) return false;
    return type.textMatches(builtinTypeName) && isBuiltinType(type);
  }

  private static boolean isBuiltinType(@Nullable GoType type) {
    type = type != null ? type.getUnderlyingType() : null;
    return type != null && GoPsiImplUtil.builtin(type);
  }

  public static boolean isNil(@Nullable GoExpression o) {
    return o instanceof GoReferenceExpression && o.textMatches("nil") && GoPsiImplUtil.builtin(((GoReferenceExpression)o).resolve());
  }

  private static boolean isNumericType(@Nullable GoType type) {
    return type != null && NUMERIC_TYPES.contains(type.getText()) && isBuiltinType(type);
  }

  @NotNull
  public static List<Pair<GoType, Boolean>> getExpectedTypesWithVariadic(@NotNull GoExpression expression) {
    PsiElement parent = expression.getParent();
    if (parent == null) return Collections.emptyList();
    if (parent instanceof GoAssignmentStatement) {
      return createListWithFalseVariadic(getExpectedTypesFromAssignmentStatement(expression, (GoAssignmentStatement)parent));
    }
    if (parent instanceof GoRangeClause) {
      return createListWithFalseVariadic(Collections.singletonList(getGoType(null, parent)));
    }
    if (parent instanceof GoRecvStatement) {
      return createListWithFalseVariadic(getExpectedTypesFromRecvStatement((GoRecvStatement)parent));
    }
    if (parent instanceof GoVarSpec) {
      return createListWithFalseVariadic(getExpectedTypesFromVarSpec(expression, (GoVarSpec)parent));
    }
    if (parent instanceof GoArgumentList) {
      return getExpectedTypesFromArgumentList(expression, (GoArgumentList)parent);
    }
    if (parent instanceof GoUnaryExpr) {
      GoUnaryExpr unaryExpr = (GoUnaryExpr)parent;
      if (unaryExpr.getSendChannel() != null) {
        Pair<GoType, Boolean> item = ContainerUtil.getFirstItem(getExpectedTypesWithVariadic(unaryExpr));
        GoType type = item != null ? item.first : null;
        GoType chanType = GoElementFactory.createType(parent.getProject(), "chan " + getInterfaceIfNull(type, parent).getText());
        return createListWithFalseVariadic(Collections.singletonList(chanType));
      }
      else {
        return createListWithFalseVariadic(Collections.singletonList(getGoType(null, parent)));
      }
    }
    if (parent instanceof GoSendStatement || parent instanceof GoLeftHandExprList && parent.getParent() instanceof GoSendStatement) {
      GoSendStatement sendStatement = (GoSendStatement)(parent instanceof GoSendStatement ? parent : parent.getParent());
      return createListWithFalseVariadic(getExpectedTypesFromGoSendStatement(expression, sendStatement));
    }
    if (parent instanceof GoExprCaseClause) {
      return createListWithFalseVariadic(getExpectedTypesFromExprCaseClause((GoExprCaseClause)parent));
    }
    return Collections.emptyList();
  }

  @NotNull
  private static List<Pair<GoType, Boolean>> createListWithFalseVariadic(@NotNull List<GoType> list) {
    return ContainerUtil.map(list, new Function<GoType, Pair<GoType, Boolean>>() {
      @NotNull
      @Override
      public Pair<GoType, Boolean> fun(GoType type) {
        return Pair.pair(type, false);
      }
    });
  }

  @NotNull
  private static List<GoType> getExpectedTypesFromExprCaseClause(@NotNull GoExprCaseClause exprCaseClause) {
    GoExprSwitchStatement switchStatement = PsiTreeUtil.getParentOfType(exprCaseClause, GoExprSwitchStatement.class);
    assert switchStatement != null;

    GoExpression switchExpr = switchStatement.getExpression();
    if (switchExpr != null) {
      return Collections.singletonList(getGoType(switchExpr, exprCaseClause));
    }

    GoStatement statement = switchStatement.getStatement();
    if (statement == null) {
      return Collections.singletonList(getInterfaceIfNull(GoPsiImplUtil.getBuiltinType("bool", exprCaseClause), exprCaseClause));
    }
    
    GoLeftHandExprList leftHandExprList = statement instanceof GoSimpleStatement ? ((GoSimpleStatement)statement).getLeftHandExprList() : null;
    GoExpression expr = leftHandExprList != null ? ContainerUtil.getFirstItem(leftHandExprList.getExpressionList()) : null;
    return Collections.singletonList(getGoType(expr, exprCaseClause));
  }

  @NotNull
  private static List<GoType> getExpectedTypesFromGoSendStatement(@NotNull GoExpression expression, @NotNull GoSendStatement statement) {
    GoLeftHandExprList leftHandExprList = statement.getLeftHandExprList();
    GoExpression channel = ContainerUtil.getFirstItem(leftHandExprList != null ? leftHandExprList.getExpressionList() : statement.getExpressionList());
    GoExpression sendExpr = statement.getSendExpression();
    assert channel != null;
    if (expression.isEquivalentTo(sendExpr)) {
      GoType chanType = channel.getGoType(null);
      if (chanType instanceof GoChannelType) {
        return Collections.singletonList(getInterfaceIfNull(((GoChannelType)chanType).getType(), statement));
      }
    }
    if (expression.isEquivalentTo(channel)) {
      GoType type = sendExpr != null ? sendExpr.getGoType(null) : null;
      GoType chanType = GoElementFactory.createType(statement.getProject(), "chan " + getInterfaceIfNull(type, statement).getText());
      return Collections.singletonList(chanType);
    }
    return Collections.singletonList(getInterfaceIfNull(null, statement));
  }

  @NotNull
  private static List<Pair<GoType, Boolean>> getExpectedTypesFromArgumentList(@NotNull GoExpression expression,
                                                                              @NotNull GoArgumentList argumentList) {
    PsiElement parentOfParent = argumentList.getParent();
    assert parentOfParent instanceof GoCallExpr;
    PsiReference reference = ((GoCallExpr)parentOfParent).getExpression().getReference();
    if (reference != null) {
      PsiElement resolve = reference.resolve();
      if (resolve instanceof GoFunctionOrMethodDeclaration) {
        GoSignature signature = ((GoFunctionOrMethodDeclaration)resolve).getSignature();
        if (signature != null) {
          List<GoExpression> exprList = argumentList.getExpressionList();
          List<GoParameterDeclaration> paramsList = signature.getParameters().getParameterDeclarationList();
          if (exprList.size() == 1) {
            List<GoType> typeList = ContainerUtil.newSmartList();
            for (GoParameterDeclaration parameterDecl : paramsList) {
              for (GoParamDefinition parameter : parameterDecl.getParamDefinitionList()) {
                typeList.add(getGoType(parameter, argumentList));
              }
              if (parameterDecl.getParamDefinitionList().isEmpty()) {
                typeList.add(getInterfaceIfNull(parameterDecl.getType(), argumentList));
              }
            }
            GoParameterDeclaration lastItem = ContainerUtil.getLastItem(paramsList);
            boolean variadic = lastItem != null && lastItem.isVariadic();
            List<Pair<GoType, Boolean>> result =
              ContainerUtil.newSmartList(Pair.pair(createGoTypeListOrGoType(typeList, argumentList), variadic));
            if (paramsList.size() > 1) {
              assert paramsList.get(0) != null;
              result.add(Pair.pair(getInterfaceIfNull(paramsList.get(0).getType(), argumentList), false));
            }
            return result;
          }
          else {
            int position = exprList.indexOf(expression);
            if (position >= 0) {
              int i = 0;
              GoParameterDeclaration lastParameter = ContainerUtil.getLastItem(paramsList);
              boolean variadic = lastParameter != null && lastParameter.isVariadic();
              for (GoParameterDeclaration parameterDecl : paramsList) {
                int paramDeclSize = Math.max(1, parameterDecl.getParamDefinitionList().size());
                if (i + paramDeclSize > position) {
                  return Collections.singletonList(Pair.pair(getInterfaceIfNull(parameterDecl.getType(), argumentList),
                                                             variadic && parameterDecl.isEquivalentTo(lastParameter)));
                }
                i += paramDeclSize;
              }
              if (variadic) {
                return Collections.singletonList(Pair.pair(getInterfaceIfNull(lastParameter.getType(), argumentList), false));
              }
            }
          }
        }
      }
    }
    return Collections.singletonList(Pair.pair(getInterfaceIfNull(null, argumentList), false));
  }

  @NotNull
  private static List<GoType> getExpectedTypesFromRecvStatement(@NotNull GoRecvStatement recvStatement) {
    List<GoType> typeList = ContainerUtil.newSmartList();
    for (GoExpression expr : recvStatement.getLeftExpressionsList()) {
      typeList.add(getGoType(expr, recvStatement));
    }
    return Collections.singletonList(createGoTypeListOrGoType(typeList, recvStatement));
  }

  @NotNull
  private static List<GoType> getExpectedTypesFromVarSpec(@NotNull GoExpression expression, @NotNull GoVarSpec varSpec) {
    List<GoType> result = ContainerUtil.newSmartList();
    GoType type = getInterfaceIfNull(varSpec.getType(), varSpec);
    if (varSpec.getRightExpressionsList().size() == 1) {
      List<GoType> typeList = ContainerUtil.newSmartList();
      int defListSize = varSpec.getVarDefinitionList().size();
      for (int i = 0; i < defListSize; i++) {
        typeList.add(type);
      }
      result.add(createGoTypeListOrGoType(typeList, expression));
      if (defListSize > 1) {
        result.add(getInterfaceIfNull(type, varSpec));
      }
      return result;
    }
    result.add(type);
    return result;
  }

  @NotNull
  private static List<GoType> getExpectedTypesFromAssignmentStatement(@NotNull GoExpression expression,
                                                                      @NotNull GoAssignmentStatement assignment) {
    List<GoExpression> leftExpressions = assignment.getLeftHandExprList().getExpressionList();
    if (assignment.getExpressionList().size() == 1) {
      List<GoType> typeList = ContainerUtil.newSmartList();
      for (GoExpression expr : leftExpressions) {
        GoType type = expr.getGoType(null);
        typeList.add(type);
      }
      List<GoType> result = ContainerUtil.newSmartList(createGoTypeListOrGoType(typeList, expression));
      if (leftExpressions.size() > 1) {
        result.add(getGoType(leftExpressions.get(0), assignment));
      }
      return result;
    }
    
    int position = assignment.getExpressionList().indexOf(expression);
    GoType leftExpression = leftExpressions.size() > position ? leftExpressions.get(position).getGoType(null) : null;
    return Collections.singletonList(getInterfaceIfNull(leftExpression, assignment));
  }

  @NotNull
  private static GoType createGoTypeListOrGoType(@NotNull List<GoType> types, @NotNull PsiElement context) {
    if (types.size() < 2) {
      return getInterfaceIfNull(ContainerUtil.getFirstItem(types), context);
    }
    return GoElementFactory.createTypeList(context.getProject(), StringUtil.join(types, new Function<GoType, String>() {
      @Override
      public String fun(@Nullable GoType type) {
        return type == null ? GoConstants.INTERFACE_TYPE : type.getText();
      }
    }, ", "));
  }

  @NotNull
  private static GoType getInterfaceIfNull(@Nullable GoType type, @NotNull PsiElement context) {
    return type == null ? GoElementFactory.createType(context.getProject(), GoConstants.INTERFACE_TYPE) : type;
  }

  @NotNull
  private static GoType getGoType(@Nullable GoTypeOwner element, @NotNull PsiElement context) {
    return getInterfaceIfNull(element != null ? element.getGoType(null) : null, context);
  }

  @Nullable
  public static GoType getDefaultType(@Nullable GoType type) {
    if (type == null) return null;
    if (GoPsiImplUtil.builtin(type)) {
      if ("ComplexType".equals(type.getText())) return GoPsiImplUtil.getBuiltinType("complex128", type);
      if ("FloatType".equals(type.getText())) return GoPsiImplUtil.getBuiltinType("float64", type);
    }
    if (type instanceof GoLightType.LightUntypedNumericType) {
      return ((GoLightType.LightUntypedNumericType)type).getDefaultType();
    }
    return type;
  }

  public static boolean isAssignable(@NotNull GoType left, @Nullable GoType right) {
    if (right == null) return false;
    if (left == right || left.equals(right)) return true;

    // A value x is assignable to a variable of type T ("x is assignable to T") in any of these cases:
    // 1) x's type is identical to T.
    if (identical(left, right)) return true;

    GoType underlyingLeft = left.getUnderlyingType();
    if (right instanceof GoLightType.LightUntypedNumericType && isNumericType(underlyingLeft)) {
      return true;
    }

    // 2) x's type V and T have identical underlying types and at least one of V or T is not a named type.
    GoType underlyingRight = right.getUnderlyingType();
    if (identical(underlyingLeft, underlyingRight) && (!isNamedType(left) || !isNamedType(right))) {
      return true;
    }

    // 3) T is an interface type and x implements T.
    if (underlyingLeft instanceof GoInterfaceType) {
      return isImplementsInterface((GoInterfaceType)underlyingLeft, right);
    }

    // 4) x is a bidirectional channel value, T is a channel type, x's type V and T have identical element types,
    //    and at least one of V or T is not a named type.
    if (underlyingLeft instanceof GoChannelType) {
      if (!(underlyingRight instanceof GoChannelType) || isNamedType(left) && isNamedType(right)) return false;
      GoChannelType rightChannel = (GoChannelType)underlyingRight;
      if (rightChannel.getSendChannel() != null) return false;
      GoType leftChannelType = ((GoChannelType)underlyingLeft).getType();
      return identical(leftChannelType, rightChannel.getType());
    }

    // 5) x is an untyped constant representable by a value of type T
    //todo add const type conversion
    //todo: fix calculating type of untyped const expression (GoConstsAssignableTest)
    return false;
  }

  public static boolean identical(@Nullable GoType left, @Nullable GoType right) {
    if (left == null || right == null) return false;
    if (left instanceof GoSpecType) {
      return right instanceof GoSpecType && left.isEquivalentTo(right);
    }
    if (left instanceof GoArrayOrSliceType) {
      if (!(right instanceof GoArrayOrSliceType)) return false;
      GoArrayOrSliceType l = (GoArrayOrSliceType)left;
      GoArrayOrSliceType r = (GoArrayOrSliceType)right;
      return identical(l.getType(), r.getType()) && getLength(l) == getLength(r);
    }
    if (left instanceof GoStructType) {
      return right instanceof GoStructType && identicalStructs((GoStructType)left, (GoStructType)right);
    }
    if (left instanceof GoPointerType) {
      return right instanceof GoPointerType && identical(((GoPointerType)left).getType(), ((GoPointerType)right).getType());
    }
    if (left instanceof GoFunctionType) {
      // Two function types are identical if they have the same number of parameters and result values,
      // corresponding parameter and result types are identical, and either both functions are variadic or neither is.
      // Parameter and result names are not required to match.
      return right instanceof GoFunctionType &&
             isSignaturesIdentical(((GoFunctionType)left).getSignature(), ((GoFunctionType)right).getSignature());
    }
    if (left instanceof GoInterfaceType) {
      // Two interface types are identical if they have the same set of methods with the same names and identical function types.
      // Lower-case method names from different packages are always different. The order of the methods is irrelevant.
      return right instanceof GoInterfaceType && isInterfacesIdentical((GoInterfaceType)left, (GoInterfaceType)right);
    }
    if (left instanceof GoMapType) {
      return right instanceof GoMapType
             && identical(((GoMapType)left).getKeyType(), ((GoMapType)right).getKeyType())
             && identical(((GoMapType)left).getValueType(), ((GoMapType)right).getValueType());
    }
    if (left instanceof GoChannelType) {
      if (!(right instanceof GoChannelType)) return false;
      GoChannelType l = (GoChannelType)left;
      GoChannelType r = (GoChannelType)right;
      return l.getDirection() == r.getDirection() && identical(((GoChannelType)left).getType(), ((GoChannelType)right).getType());
    }
    if (left instanceof GoTypeList) {
      return right instanceof GoTypeList && isListsOfGoTypeIdentical(((GoTypeList)left).getTypeList(), ((GoTypeList)right).getTypeList());
    }
    if (left instanceof GoCType) {
      return right instanceof GoCType;
    }

    if (isNamedType(left) != isNamedType(right)) return false;
    if (isAliases(left, right)) return true;
    GoTypeReferenceExpression l = left.getTypeReferenceExpression(); // todo: stubs?
    GoTypeReferenceExpression r = right.getTypeReferenceExpression();
    if (l == null || r == null) return false;
    PsiElement lResolve = l.resolve();
    return lResolve != null && lResolve.isEquivalentTo(r.resolve());
  }

  private static Set INT32_ALIAS = ContainerUtil.newTreeSet("int32", "rune");
  private static Set UINT8_ALIAS = ContainerUtil.newTreeSet("uint8", "byte");
  
  private static boolean isAliases(@NotNull GoType left, @NotNull GoType right) {
    if (!(isBuiltinType(left) && isBuiltinType(right))) return false;
    String l = left.getText();
    String r = right.getText();
    return INT32_ALIAS.contains(l) && INT32_ALIAS.contains(r) || 
           UINT8_ALIAS.contains(l) && UINT8_ALIAS.contains(r);
  }

  private static int getLength(@NotNull GoArrayOrSliceType slice) { // todo: stubs
    if (slice.getTripleDot() == null && slice.getExpression() == null) return -1;
    if (slice.getTripleDot() != null) {
      GoCompositeLit compositeLit = ObjectUtils.tryCast(slice.getParent(), GoCompositeLit.class);
      if (compositeLit == null) return 0;
      GoLiteralValue literal = compositeLit.getLiteralValue();
      return literal != null ? literal.getElementList().size() : 0;
    }
    // todo: length
    return -2;
  }

  private static boolean isInterfacesIdentical(@NotNull GoInterfaceType left, @NotNull GoInterfaceType right) {
    List<GoMethodSpec> leftMethods = left.getAllMethods();
    List<GoMethodSpec> rightMethods = right.getAllMethods();
    if (leftMethods.size() != rightMethods.size()) return false;
    ContainerUtil.sort(leftMethods, BY_NAME);
    ContainerUtil.sort(rightMethods, BY_NAME);
    if (!isNameListsIdentical(leftMethods, rightMethods)) return false;
    for (int i = 0; i < leftMethods.size(); i++) {
      if (!isSignaturesIdentical(leftMethods.get(i).getSignature(), rightMethods.get(i).getSignature())) return false;
    }
    return true;
  }

  private static boolean isNamesIdentical(@NotNull GoNamedElement left, @NotNull GoNamedElement right) {
    String name = left.getName();
    if (!Comparing.equal(name, right.getName())) return false;
    if (name == null) return false;
    return !(!name.isEmpty() &&
             Character.isLowerCase(name.charAt(0)) &&
             !Comparing.equal(left.getContainingFile().getPackageName(), right.getContainingFile().getPackageName()));
  }

  private static boolean identicalStructs(@NotNull GoStructType left, @NotNull GoStructType right) {
    List<GoNamedElement> l = SyntaxTraverser.psiTraverser(left).filter(GoNamedElement.class).toList();
    List<GoNamedElement> r = SyntaxTraverser.psiTraverser(right).filter(GoNamedElement.class).toList();
    if (!isNameListsIdentical(l, r)) return false;

    for (int i = 0; i < l.size(); i++) {
      GoNamedElement f = l.get(i);
      GoNamedElement s = r.get(i);
      if (f instanceof GoFieldDefinition) {
        if (!(s instanceof GoFieldDefinition)) return false;
        if (!identical(f.getGoType(null), s.getGoType(null))) return false;
      }
      if (f instanceof GoAnonymousFieldDefinition) {
        if (!(s instanceof GoAnonymousFieldDefinition)) return false;
        GoTypeReferenceExpression fe = ((GoAnonymousFieldDefinition)f).getTypeReferenceExpression();
        GoTypeReferenceExpression se = ((GoAnonymousFieldDefinition)s).getTypeReferenceExpression();
        if (!identical(fe != null ? fe.resolveType() : null, se != null ? se.resolveType() : null)) return false;
      }
      if (!Comparing.equal(getTagForFieldDefinition(f), getTagForFieldDefinition(s))) return false;
    }
    return true;
  }

  // todo: stubs
  private static String getTagForFieldDefinition(GoNamedElement definition) {
    GoFieldDeclaration declaration = PsiTreeUtil.getStubOrPsiParentOfType(definition, GoFieldDeclaration.class);
    if (declaration == null) return null;
    GoTag tag = declaration.getTag();
    if (tag == null) return null;
    GoStringLiteral literal = tag.getStringLiteral();
    String string = literal.getText();
    if (string == null || string.isEmpty()) return null;
    StringBuilder builder = new StringBuilder();
    literal.createLiteralTextEscaper().decode(new TextRange(1, string.length() - 1) , builder);
    return builder.toString();
  }

  private static boolean isNameListsIdentical(@NotNull List<? extends GoNamedElement> l, @NotNull List<? extends GoNamedElement> r) {
    if (l.size() != r.size()) return false;
    for (int i = 0; i < l.size(); i++) {
      if (!isNamesIdentical(l.get(i), r.get(i))) return false;
    }
    return true;
  }

  private static boolean isNamedType(@Nullable GoType type) {
    if (type == null ||
        type instanceof GoArrayOrSliceType ||
        type instanceof GoStructType ||
        type instanceof GoPointerType ||
        type instanceof GoFunctionType ||
        type instanceof GoInterfaceType ||
        type instanceof GoMapType ||
        type instanceof GoChannelType ||
        type instanceof GoTypeList ||
        type instanceof GoCType ||
        type instanceof GoSpecType) {
      return false;
    }
    GoTypeReferenceExpression reference = type.getTypeReferenceExpression();
    PsiElement resolve = reference != null ? reference.resolve() : null;
    return resolve != null && !GoPsiImplUtil.builtin(resolve);
  }

  private static boolean isImplementsInterface(@NotNull GoInterfaceType interfaceType, @NotNull GoType type) {
    List<GoMethodSpec> interfaceMethods = interfaceType.getAllMethods();
    if (interfaceMethods.isEmpty()) return true;
    List<? extends GoNamedSignatureOwner> methodsForType = getOwners(type);
    if (methodsForType == null) return false;
    ContainerUtil.sort(methodsForType, BY_NAME);
    ContainerUtil.sort(interfaceMethods, BY_NAME);
    int j = 0;
    for (int i = 0; i < interfaceMethods.size(); i++) {
      GoMethodSpec interfaceMethod = interfaceMethods.get(i);
      while (j < methodsForType.size() && !isNamesIdentical(interfaceMethod, methodsForType.get(j))) {
        j++;
      }
      if (j == methodsForType.size() || !isSignaturesIdentical(interfaceMethod.getSignature(), methodsForType.get(j).getSignature())) {
        return false;
      }
    }
    return true;
  }

  @Nullable
  private static List<? extends GoNamedSignatureOwner> getOwners(@NotNull GoType o) {
    GoType underlyingType = o.getUnderlyingType();
    if (underlyingType instanceof GoInterfaceType) {
      return ((GoInterfaceType)underlyingType).getAllMethods();
    }
    GoType type = GoPsiImplUtil.unwrapPointerIfNeeded(o);
    if (type == null) return null;
    GoTypeReferenceExpression reference = GoPsiImplUtil.getTypeReference(type);
    GoTypeSpec spec = ObjectUtils.tryCast(type instanceof GoSpecType ? GoPsiImplUtil.getTypeSpecSafe(type) : // todo
                                              reference != null ? reference.resolve() : null, GoTypeSpec.class);
    if (spec == null) return null;
    return ContainerUtil.newArrayList(spec.getMethods());
  }

  private static boolean isSignaturesIdentical(@Nullable GoSignature left, @Nullable GoSignature right) {
    if (right == null || left == null) {
      return right == left;
    }

    List<GoType> leftResultTypeList = getTypesFromResult(left.getResult());
    List<GoType> rightResultTypeList = getTypesFromResult(right.getResult());
    if (!isListsOfGoTypeIdentical(leftResultTypeList, rightResultTypeList)) return false;

    Pair<List<GoType>, Boolean> leftParamsTypeList = getTypesAndIsVariadicFromParameters(left.getParameters());
    Pair<List<GoType>, Boolean> rightParamsTypeList = getTypesAndIsVariadicFromParameters(right.getParameters());
    return leftParamsTypeList.second.compareTo(rightParamsTypeList.second) == 0 &&
           isListsOfGoTypeIdentical(leftParamsTypeList.first, rightParamsTypeList.first);
  }

  @Nullable
  private static List<GoType> getTypesFromResult(@Nullable GoResult result) {
    if (result == null) return null;
    GoType type = result.getType();
    if (type != null) {
      return type instanceof GoTypeList ? ((GoTypeList)type).getTypeList() : Collections.singletonList(type);
    }
    return getTypesAndIsVariadicFromParameters(result.getParameters()).first;
  }

  @NotNull
  private static Pair<List<GoType>, Boolean> getTypesAndIsVariadicFromParameters(@Nullable GoParameters parameters) {
    List<GoType> result = ContainerUtil.newSmartList();
    if (parameters == null) return Pair.create(result, false);
    for (GoParameterDeclaration parameterDecl : parameters.getParameterDeclarationList()) {
      for (GoParamDefinition parameter : parameterDecl.getParamDefinitionList()) {
        result.add(parameter.getGoType(null));
      }
      if (parameterDecl.getParamDefinitionList().isEmpty()) {
        result.add(parameterDecl.getType());
      }
    }
    GoParameterDeclaration last = ContainerUtil.getLastItem(parameters.getParameterDeclarationList());
    return Pair.create(result, last != null && last.isVariadic());
  }

  private static boolean isListsOfGoTypeIdentical(@Nullable List<GoType> left, @Nullable List<GoType> right) {
    if (left == null || right == null) return left == right;
    if (left.size() != right.size()) return false;
    for (int i = 0; i < left.size(); i++) {
      if (!identical(left.get(i), right.get(i))) return false;
    }
    return true;
  }
}
