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

import com.goide.psi.*;
import com.goide.stubs.GoTypeStub;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.light.LightElement;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class GoLightType<E extends GoCompositeElement> extends LightElement implements GoType {
  @NotNull protected final E myElement;

  protected GoLightType(@NotNull E e) {
    super(e.getManager(), e.getLanguage());
    myElement = e;
    setNavigationElement(e);
  }

  @Nullable
  @Override
  public GoTypeReferenceExpression getTypeReferenceExpression() {
    return null;
  }

  @Override
  public boolean shouldGoDeeper() {
    return false;
  }

  @Override
  public boolean isAssignableFrom(GoType right) {
    return GoPsiImplUtil.isAssignableFrom(this, right);
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    return GoPsiImplUtil.resolve(this);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + myElement + "}";
  }

  @Override
  public IStubElementType getElementType() {
    return null;
  }

  @Override
  public GoTypeStub getStub() {
    return null;
  }

  @NotNull
  @Override
  public GoType getUnderlyingType() {
    return GoPsiImplUtil.getUnderlyingType(this);
  }

  static class LightPointerType extends GoLightType<GoType> implements GoPointerType {
    protected LightPointerType(@NotNull GoType o) {
      super(o);
    }

    @Override
    public String getText() {
      return "*" + myElement.getText();
    }

    @Nullable
    @Override
    public GoType getType() {
      return myElement;
    }

    @NotNull
    @Override
    public PsiElement getMul() {
      return myElement; // todo: mock it?
    }
  }

  static class LightTypeList extends GoLightType<GoCompositeElement> implements GoTypeList {
    @NotNull private final List<GoType> myTypes;

    public LightTypeList(@NotNull GoCompositeElement o, @NotNull List<GoType> types) {
      super(o);
      myTypes = types;
    }

    @NotNull
    @Override
    public List<GoType> getTypeList() {
      return myTypes;
    }

    @Override
    public String toString() {
      return "MyGoTypeList{myTypes=" + myTypes + '}';
    }

    @Override
    public String getText() {
      return StringUtil.join(getTypeList(), GoPsiImplUtil.GET_TEXT_FUNCTION, ", ");
    }
  }

  static class LightFunctionType extends GoLightType<GoSignatureOwner> implements GoFunctionType {
    private final GoSignature mySignature;

    public LightFunctionType(@NotNull GoSignatureOwner o, @Nullable String receiver) {
      super(o);
      mySignature = calcSignature(o, receiver);
    }

    @Nullable
    private static GoSignature calcSignature(@NotNull GoSignatureOwner o, @Nullable String receiver) {
      if (receiver != null && o instanceof GoMethodDeclaration) {
        GoMethodDeclaration method = (GoMethodDeclaration)o;
        GoSignature signature = method.getSignature();
        if (signature != null) {
          String params = StringUtil.join(GoTypeUtil.getTypesAndIsVariadicFromParameters(
            signature.getParameters()).first, GoPsiImplUtil.GET_TEXT_FUNCTION, ", ");
          String receiverWithParams = receiver + (params.isEmpty() ? "" : ", " + params);
          List<GoType> resultTypes = GoTypeUtil.getTypesFromResult(signature.getResult());
          String result = resultTypes != null ? StringUtil.join(resultTypes, GoPsiImplUtil.GET_TEXT_FUNCTION, ", ") : "";
          return GoElementFactory.createFunctionSignatureFromText(o.getProject(), receiverWithParams, result, signature);
        }
      }
      return o.getSignature();
    }

    @Nullable
    @Override
    public GoSignature getSignature() {
      return mySignature;
    }

    @NotNull
    @Override
    public PsiElement getFunc() {
      return myElement instanceof GoFunctionOrMethodDeclaration ? ((GoFunctionOrMethodDeclaration)myElement).getFunc() : myElement;
    }

    @Override
    public String getText() {
      return "func " + (mySignature != null ? mySignature.getText() : "<null>");
    }
  }

  static class LightSliceType extends GoLightType<GoType> implements GoArrayOrSliceType {
    protected LightSliceType(GoType type) {
      super(type);
    }

    @Override
    public String getText() {
      return "[]" + myElement.getText();
    }

    @Nullable
    @Override
    public GoExpression getExpression() {
      return null;
    }

    @Nullable
    @Override
    public GoType getType() {
      return myElement;
    }

    @NotNull
    @Override
    public PsiElement getLbrack() {
      //noinspection ConstantConditions
      return null; // todo: mock?
    }

    @Nullable
    @Override
    public PsiElement getRbrack() {
      return null;
    }

    @Nullable
    @Override
    public PsiElement getTripleDot() {
      return null;
    }

    @Override
    public int getLength() {
      return -1;
    }

    @Override
    public boolean isArray() {
      return false;
    }
  }

  public static abstract class LightUntypedNumericType extends GoLightType<GoCompositeElement> {
    protected LightUntypedNumericType(@NotNull GoCompositeElement o) {
      super(o);
    }

    @Override
    public String getText() {
      return "untyped " + getDefaultTypeName();
    }

    @Nullable
    public GoType getDefaultType() {
      return GoPsiImplUtil.getBuiltinType(getDefaultTypeName(), myElement);
    }

    abstract public String getDefaultTypeName();
  }

  public static class LightUntypedIntType extends LightUntypedNumericType {
    protected LightUntypedIntType(@NotNull GoCompositeElement o) {
      super(o);
    }

    @Override
    public String getDefaultTypeName() {
      return "int";
    }
  }

  public static class LightUntypedFloatType extends LightUntypedNumericType {
    protected LightUntypedFloatType(@NotNull GoCompositeElement o) {
      super(o);
    }

    @Override
    public String getDefaultTypeName() {
      return "float64";
    }
  }

  public static class LightUntypedRuneType extends LightUntypedNumericType {
    protected LightUntypedRuneType(@NotNull GoCompositeElement o) {
      super(o);
    }

    @Override
    public String getDefaultTypeName() {
      return "rune";
    }
  }

  public static class LightUntypedComplexType extends LightUntypedNumericType {
    protected LightUntypedComplexType(@NotNull GoCompositeElement o) {
      super(o);
    }

    @Override
    public String getDefaultTypeName() {
      return "complex128";
    }
  }
}
