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

package com.goide.psi.impl;

import com.goide.GoConstants;
import com.goide.GoTypes;
import com.goide.psi.*;
import com.goide.psi.impl.imports.GoImportReferenceSet;
import com.goide.runconfig.testing.GoTestFinder;
import com.goide.sdk.GoSdkUtil;
import com.goide.stubs.*;
import com.goide.stubs.index.GoMethodIndex;
import com.goide.util.GoStringLiteralEscaper;
import com.goide.util.GoUtil;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceOwner;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiFileReference;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.NotNullFunction;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.goide.psi.impl.GoLightType.*;

public class GoPsiImplUtil {
  public static final Key<SmartPsiElementPointer<GoReferenceExpressionBase>> CONTEXT = Key.create("CONTEXT");
  public static final NotNullFunction<PsiElement, String> GET_TEXT_FUNCTION = new NotNullFunction<PsiElement, String>() {
    @NotNull
    @Override
    public String fun(@NotNull PsiElement element) {
      return element.getText();
    }
  };

  public static boolean builtin(@Nullable PsiElement resolve) {
    if (resolve == null) return false;
    PsiFile file = resolve.getContainingFile();
    if (!(file instanceof GoFile)) return false;
    return isBuiltinFile(file);
  }

  public static boolean isPanic(@NotNull GoCallExpr o) {
    GoExpression e = o.getExpression();
    if (StringUtil.equals("panic", e.getText()) && e instanceof GoReferenceExpression) {
      PsiReference reference = e.getReference();
      PsiElement resolve = reference != null ? reference.resolve() : null;
      if (!(resolve instanceof GoFunctionDeclaration)) return false;
      GoFile file = ((GoFunctionDeclaration)resolve).getContainingFile();
      return isBuiltinFile(file);
    }
    return false;
  }

  private static boolean isBuiltinFile(@NotNull PsiFile file) {
    return StringUtil.equals(((GoFile)file).getPackageName(), GoConstants.BUILTIN_PACKAGE_NAME)
           && StringUtil.equals(file.getName(), GoConstants.BUILTIN_FILE_NAME);
  }

  @NotNull
  public static GlobalSearchScope packageScope(@NotNull GoFile file) {
    List<GoFile> files = getAllPackageFiles(file);
    return GlobalSearchScope.filesScope(file.getProject(), ContainerUtil.map(files, new Function<GoFile, VirtualFile>() {
      @Override
      public VirtualFile fun(GoFile file) {
        return file.getVirtualFile();
      }
    }));
  }

  @NotNull
  public static List<GoFile> getAllPackageFiles(@NotNull GoFile file) {
    String name = file.getPackageName();
    PsiDirectory parent = file.getParent();
    if (parent == null || StringUtil.isEmpty(name)) return ContainerUtil.list(file);
    PsiElement[] children = parent.getChildren();
    List<GoFile> files = ContainerUtil.newArrayListWithCapacity(children.length);
    for (PsiElement element : children) {
      if (element instanceof GoFile && Comparing.equal(((GoFile)element).getPackageName(), name)) {
        files.add(((GoFile)element));
      }
    }
    return files;
  }

  @Nullable
  public static GoTopLevelDeclaration getTopLevelDeclaration(@Nullable PsiElement startElement) {
    GoTopLevelDeclaration declaration = PsiTreeUtil.getTopmostParentOfType(startElement, GoTopLevelDeclaration.class);
    if (declaration == null || !(declaration.getParent() instanceof GoFile)) return null;
    return declaration;
  }

  @Nullable
  public static GoTypeReferenceExpression getQualifier(@NotNull GoTypeReferenceExpression o) {
    return PsiTreeUtil.getChildOfType(o, GoTypeReferenceExpression.class);
  }

  @Nullable
  public static PsiDirectory resolve(@NotNull GoImportString importString) {
    PsiReference[] references = importString.getReferences();
    for (PsiReference reference : references) {
      if (reference instanceof FileReferenceOwner) {
        PsiFileReference lastFileReference = ((FileReferenceOwner)reference).getLastFileReference();
        PsiElement result = lastFileReference != null ? lastFileReference.resolve() : null;
        return result instanceof PsiDirectory ? (PsiDirectory)result : null;
      }
    }
    return null;
  }

  @NotNull
  public static PsiReference getReference(@NotNull GoTypeReferenceExpression o) {
    return new GoTypeReference(o);
  }

  @NotNull
  public static PsiReference getReference(@NotNull GoLabelRef o) {
    return new GoLabelReference(o);
  }

  @Nullable
  public static PsiReference getReference(@NotNull GoVarDefinition o) {
    GoShortVarDeclaration shortDeclaration = PsiTreeUtil.getParentOfType(o, GoShortVarDeclaration.class);
    boolean createRef = PsiTreeUtil.getParentOfType(shortDeclaration, GoBlock.class, GoForStatement.class, GoIfStatement.class, GoSwitchStatement.class, GoSelectStatement.class) instanceof GoBlock;
    return createRef ? new GoVarReference(o) : null;
  }

  @NotNull
  public static GoReference getReference(@NotNull GoReferenceExpression o) {
    return new GoReference(o);
  }

  @NotNull
  public static PsiReference getReference(@NotNull GoFieldName o) {
    return new PsiMultiReference(new PsiReference[]{new GoFieldNameReference(o), new GoReference(o)}, o) {
      @Override
      public PsiElement resolve() {
        PsiReference[] refs = getReferences();
        PsiElement resolve = refs.length > 0 ? refs[0].resolve() : null;
        if (resolve != null) return resolve;
        return refs.length > 1 ? refs[1].resolve() : null;
      }

      public boolean isReferenceTo(PsiElement element) {
        return GoUtil.couldBeReferenceTo(element, getElement()) && getElement().getManager().areElementsEquivalent(resolve(), element);
      }
    };
  }

  @Nullable
  public static GoReferenceExpression getQualifier(@SuppressWarnings("UnusedParameters") @NotNull GoFieldName o) {
    return null;
  }

  @NotNull
  public static PsiReference[] getReferences(@NotNull GoImportString o) {
    if (o.getTextLength() < 2) return PsiReference.EMPTY_ARRAY;
    return new GoImportReferenceSet(o).getAllReferences();
  }

  @Nullable
  public static GoReferenceExpression getQualifier(@NotNull GoReferenceExpression o) {
    return PsiTreeUtil.getChildOfType(o, GoReferenceExpression.class);
  }

  public static boolean processDeclarations(@NotNull GoCompositeElement o, @NotNull PsiScopeProcessor processor, @NotNull ResolveState state, PsiElement lastParent, @NotNull PsiElement place) {
    boolean isAncestor = PsiTreeUtil.isAncestor(o, place, false);
    if (o instanceof GoVarSpec) return isAncestor || GoCompositeElementImpl.processDeclarationsDefault(o, processor, state, lastParent, place);

    if (isAncestor) return GoCompositeElementImpl.processDeclarationsDefault(o, processor, state, lastParent, place);

    if (o instanceof GoBlock ||
        o instanceof GoIfStatement ||
        o instanceof GoForStatement ||
        o instanceof GoCommClause ||
        o instanceof GoFunctionLit ||
        o instanceof GoTypeCaseClause ||
        o instanceof GoExprCaseClause) {
      return processor.execute(o, state);
    }
    return GoCompositeElementImpl.processDeclarationsDefault(o, processor, state, lastParent, place);
  }

  @Nullable
  public static GoType getGoTypeInner(@NotNull GoReceiver o, @SuppressWarnings("UnusedParameters") @Nullable ResolveState context) {
    return o.getType();
  }

  @Nullable
  public static PsiElement getIdentifier(@SuppressWarnings("UnusedParameters") @NotNull GoAnonymousFieldDefinition o) {
    return null;
  }

  @Nullable
  public static String getName(@NotNull GoPackageClause packageClause) {
    PsiElement packageIdentifier = packageClause.getIdentifier();
    if (packageIdentifier != null) {
      return packageIdentifier.getText().trim();
    }
    return null;
  }

  @NotNull
  public static String getName(@NotNull GoAnonymousFieldDefinition o) {
    return o.getTypeReferenceExpression().getIdentifier().getText();
  }

  public static int getTextOffset(@NotNull GoAnonymousFieldDefinition o) {
    return o.getTypeReferenceExpression().getIdentifier().getTextOffset();
  }

  @Nullable
  public static String getName(@NotNull GoMethodSpec o) {
    GoNamedStub<GoMethodSpec> stub = o.getStub();
    if (stub != null) {
      return stub.getName();
    }
    PsiElement identifier = o.getIdentifier();
    if (identifier != null) return identifier.getText();
    GoTypeReferenceExpression typeRef = o.getTypeReferenceExpression();
    return typeRef != null ? typeRef.getIdentifier().getText() : null;
  }

  @Nullable
  public static GoTypeReferenceExpression getTypeReference(@Nullable GoType o) {
    if (o == null) return null;
    if (o instanceof GoReceiverType) {
      return PsiTreeUtil.findChildOfAnyType(o, GoTypeReferenceExpression.class);
    }
    return o.getTypeReferenceExpression();
  }

  @Nullable
  public static GoType getGoTypeInner(@NotNull final GoConstDefinition o, @Nullable final ResolveState context) {
    GoType fromSpec = findTypeInConstSpec(o);
    if (fromSpec != null) return fromSpec;
    // todo: stubs
    return RecursionManager.doPreventingRecursion(o, true, new NullableComputable<GoType>() {
      @Nullable
      @Override
      public GoType compute() {
        GoConstSpec prev = PsiTreeUtil.getPrevSiblingOfType(o.getParent(), GoConstSpec.class);
        while (prev != null) {
          GoType type = prev.getType();
          if (type != null) return type;
          GoExpression expr = ContainerUtil.getFirstItem(prev.getExpressionList()); // not sure about first
          if (expr != null) return expr.getGoType(context);
          prev = PsiTreeUtil.getPrevSiblingOfType(prev, GoConstSpec.class);
        }
        return null;
      }
    });
  }

  @Nullable
  private static GoType findTypeInConstSpec(@NotNull GoConstDefinition o) {
    GoConstDefinitionStub stub = o.getStub();
    PsiElement parent = PsiTreeUtil.getStubOrPsiParent(o);
    if (!(parent instanceof GoConstSpec)) return null;
    GoConstSpec spec = (GoConstSpec)parent;
    GoType commonType = spec.getType();
    if (commonType != null) return commonType;
    List<GoConstDefinition> varList = spec.getConstDefinitionList();
    int i = Math.max(varList.indexOf(o), 0);
    if (stub != null) return null;
    GoConstSpecStub specStub = spec.getStub();
    List<GoExpression> es = specStub != null ? specStub.getExpressionList() : spec.getExpressionList(); // todo: move to constant spec
    if (es.size() <= i) return null;
    return es.get(i).getGoType(null);
  }

  @Nullable
  public static GoType getGoType(@NotNull final GoExpression o, @Nullable final ResolveState context) {
    return RecursionManager.doPreventingRecursion(o, true, new Computable<GoType>() {
      @Override
      public GoType compute() {
        if (context != null) return getGoTypeInner(o, context);
        return CachedValuesManager.getCachedValue(o, new CachedValueProvider<GoType>() {
          @Nullable
          @Override
          public Result<GoType> compute() {
            return Result.create(getGoTypeInner(o, null), PsiModificationTracker.MODIFICATION_COUNT);
          }
        });
      }
    });
  }

  @Nullable
  public static GoType getGoTypeInner(@NotNull final GoExpression o, @Nullable ResolveState context) {
    if (o instanceof GoUnaryExpr) {
      GoExpression expression = ((GoUnaryExpr)o).getExpression();
      if (expression != null) {
        GoType type = expression.getGoType(context);
        if (type instanceof GoChannelType && ((GoUnaryExpr)o).getSendChannel() != null) return ((GoChannelType)type).getType();
        if (type instanceof GoPointerType && ((GoUnaryExpr)o).getMul() != null) return ((GoPointerType)type).getType();
        return type;
      }
      return null;
    }
    else if (o instanceof GoAddExpr) {
      return ((GoAddExpr)o).getLeft().getGoType(context);
    }
    else if (o instanceof GoMulExpr) {
      GoExpression left = ((GoMulExpr)o).getLeft();
      if (!(left instanceof GoLiteral)) return left.getGoType(context);
      GoExpression right = ((GoBinaryExpr)o).getRight();
      if (right != null) return right.getGoType(context);
    }
    else if (o instanceof GoCompositeLit) {
      GoType type = ((GoCompositeLit)o).getType();
      if (type != null) return type;
      GoTypeReferenceExpression expression = ((GoCompositeLit)o).getTypeReferenceExpression();
      return findTypeFromTypeRef(expression);
    }
    else if (o instanceof GoFunctionLit) {
      return new MyFunctionType((GoFunctionLit)o);
    }
    else if (o instanceof GoBuiltinCallExpr) {
      String text = ((GoBuiltinCallExpr)o).getReferenceExpression().getText();
      boolean isNew = "new".equals(text);
      boolean isMake = "make".equals(text);
      if (isNew || isMake) {
        GoBuiltinArgs args = ((GoBuiltinCallExpr)o).getBuiltinArgs();
        GoType type = args != null ? args.getType() : null;
        return isNew ? type == null ? null : new MyPointerType(type) : type;
      }
    }
    else if (o instanceof GoCallExpr) {
      GoExpression e = ((GoCallExpr)o).getExpression();
      if (e instanceof GoReferenceExpression) { // todo: unify Type processing
        PsiReference ref = e.getReference();
        PsiElement resolve = ref != null ? ref.resolve() : null;
        if (((GoReferenceExpression)e).getQualifier() == null && "append".equals(((GoReferenceExpression)e).getIdentifier().getText())) {
          if (resolve instanceof GoFunctionDeclaration && isBuiltinFile(resolve.getContainingFile())) {
            List<GoExpression> l = ((GoCallExpr)o).getArgumentList().getExpressionList();
            GoExpression f = ContainerUtil.getFirstItem(l);
            return f == null ? null : getGoType(f, context);
          }
        }
        else if (resolve == e) { // C.call()
          return new MyCType(e);
        }
      }
      GoType type = ((GoCallExpr)o).getExpression().getGoType(context);
      if (type instanceof GoFunctionType) {
        return funcType(type);
      }
      GoType byRef = type == null ? null : findBaseTypeFromRef(type.getTypeReferenceExpression());
      if (byRef instanceof GoSpecType && ((GoSpecType)byRef).getType() instanceof GoFunctionType) {
        return funcType(((GoSpecType)byRef).getType());
      }
      return type;
    }
    else if (o instanceof GoReferenceExpression) {
      PsiReference reference = o.getReference();
      PsiElement resolve = reference != null ? reference.resolve() : null;
      if (resolve instanceof GoTypeOwner) return typeOrParameterType((GoTypeOwner)resolve, context);
    }
    else if (o instanceof GoParenthesesExpr) {
      GoExpression expression = ((GoParenthesesExpr)o).getExpression();
      return expression != null ? expression.getGoType(context) : null;
    }
    else if (o instanceof GoSelectorExpr) {
      GoExpression item = ContainerUtil.getLastItem(((GoSelectorExpr)o).getExpressionList());
      return item != null ? item.getGoType(context) : null;
    }
    else if (o instanceof GoIndexOrSliceExpr) {
      GoExpression first = ContainerUtil.getFirstItem(((GoIndexOrSliceExpr)o).getExpressionList());
      GoType type = first == null ? null : first.getGoType(context);
      if (o.getNode().findChildByType(GoTypes.COLON) != null) return type; // means slice expression, todo: extract if needed
      GoTypeReferenceExpression typeRef = getTypeReference(type);
      if (typeRef != null) {
        type = findTypeFromTypeRef(typeRef);
      }
      if (type instanceof GoSpecType) type = ((GoSpecType)type).getType();
      if (type instanceof GoMapType) {
        List<GoType> list = ((GoMapType)type).getTypeList();
        if (list.size() == 2) {
          return list.get(1);
        }
      }
      else if (type instanceof GoArrayOrSliceType) {
        GoType tt = ((GoArrayOrSliceType)type).getType();
        return typeFromRefOrType(tt);
      }
    }
    else if (o instanceof GoTypeAssertionExpr) {
      return ((GoTypeAssertionExpr)o).getType();
    }
    else if (o instanceof GoConversionExpr) {
      return ((GoConversionExpr)o).getType();
    }
    else if (o instanceof GoStringLiteral) {
      return getBuiltinType(o, "string");
    }
    else if (o instanceof GoLiteral) {
      GoLiteral l = (GoLiteral)o;
      if (l.getChar() != null) return getBuiltinType(o, "rune");
      if (l.getInt() != null || l.getHex() != null || ((GoLiteral)o).getOct() != null) return getBuiltinType(o, "int");
      if (l.getFloat() != null) return getBuiltinType(o, "float64");
      if (l.getFloati() != null) return getBuiltinType(o, "complex64");
      if (l.getDecimali() != null) return getBuiltinType(o, "complex128");
    }
    return null;
  }

  @Nullable
  private static GoType getBuiltinType(@NotNull GoExpression o, @NotNull final String name) {
    GoFile builtin = GoSdkUtil.findBuiltinFile(o);
    if (builtin != null) {
      GoTypeSpec spec = ContainerUtil.find(builtin.getTypes(), new Condition<GoTypeSpec>() {
        @Override
        public boolean value(GoTypeSpec spec) {
          return name.equals(spec.getName());
        }
      });
      if (spec != null) {
        return spec.getSpecType().getType(); // todo
      }
    }
    return null;
  }

  @Nullable
  private static GoType typeFromRefOrType(@Nullable GoType t) {
    if (t == null) return null;
    GoTypeReferenceExpression tr = getTypeReference(t);
    return tr != null ? findTypeFromTypeRef(tr) : t;
  }

  @Nullable
  public static GoType typeOrParameterType(@NotNull final GoTypeOwner resolve, @Nullable ResolveState context) {
    GoType type = resolve.getGoType(context);
    if (resolve instanceof GoParamDefinition && ((GoParamDefinition)resolve).isVariadic()) {
      return type == null ? null : new MyArrayType(type);
    }
    if (resolve instanceof GoSignatureOwner) {
      return new MyFunctionType((GoSignatureOwner)resolve);
    }
    return type;
  }

  @Nullable
  private static GoType findTypeFromTypeRef(@Nullable GoTypeReferenceExpression expression) {
    PsiReference reference = expression != null ? expression.getReference() : null;
    PsiElement resolve = reference != null ? reference.resolve() : null;
    if (resolve instanceof GoTypeSpec) return ((GoTypeSpec)resolve).getSpecType();
    if (resolve == expression && expression != null) {  // hacky C resolve
      return new MyCType(expression);
    }
    return null;
  }

  public static boolean isVariadic(@NotNull GoParamDefinition o) {
    PsiElement parent = o.getParent();
    return parent instanceof GoParameterDeclaration && ((GoParameterDeclaration)parent).isVariadic();
  }

  public static boolean isVariadic(@NotNull GoParameterDeclaration o) {
    GoParameterDeclarationStub stub = o.getStub();
    return stub != null ? stub.isVariadic() : o.getTripleDot() != null;
  }

  @Nullable
  public static GoType getGoTypeInner(@NotNull GoTypeSpec o, @SuppressWarnings("UnusedParameters") @Nullable ResolveState context) {
    return o.getSpecType();
  }

  @Nullable
  public static GoType getGoTypeInner(@NotNull GoVarDefinition o, @Nullable ResolveState context) {
    // see http://golang.org/ref/spec#RangeClause
    PsiElement parent = PsiTreeUtil.getStubOrPsiParent(o);
    if (parent instanceof GoRangeClause) {
      return processRangeClause(o, (GoRangeClause)parent, context);
    }
    if (parent instanceof GoVarSpec) {
      return findTypeInVarSpec(o, context);
    }
    GoCompositeLit literal = PsiTreeUtil.getNextSiblingOfType(o, GoCompositeLit.class);
    if (literal != null) {
      return literal.getType();
    }
    GoType siblingType = o.findSiblingType();
    if (siblingType != null) return siblingType;

    if (parent instanceof GoTypeSwitchGuard) {
      SmartPsiElementPointer<GoReferenceExpressionBase> pointer = context == null ? null : context.get(CONTEXT);
      GoTypeCaseClause typeCase = PsiTreeUtil.getParentOfType(pointer != null ? pointer.getElement() : null, GoTypeCaseClause.class);
      if (typeCase != null && typeCase.getDefault() != null) {
        return ((GoTypeSwitchGuard)parent).getExpression().getGoType(context);
      }
      return typeCase != null ? typeCase.getType() : null;
    }
    return null;
  }

  @Nullable
  private static GoType findTypeInVarSpec(@NotNull GoVarDefinition o, @Nullable ResolveState context) {
    GoVarSpec parent = (GoVarSpec)PsiTreeUtil.getStubOrPsiParent(o);
    if (parent == null) return null;
    GoType commonType = parent.getType();
    if (commonType != null) return commonType;
    List<GoVarDefinition> varList = parent.getVarDefinitionList();
    int i = varList.indexOf(o);
    i = i == -1 ? 0 : i;
    List<GoExpression> exprs = parent.getExpressionList();
    if (exprs.size() == 1 && exprs.get(0) instanceof GoCallExpr) {
      GoExpression call = exprs.get(0);
      GoType fromCall = call.getGoType(context);
      boolean canDecouple = varList.size() > 1;
      GoType underlyingType = canDecouple && fromCall instanceof GoSpecType ? ((GoSpecType)fromCall).getType() : fromCall;
      GoType byRef = typeFromRefOrType(underlyingType);
      GoType type = funcType(canDecouple && byRef instanceof GoSpecType ? ((GoSpecType)byRef).getType() : byRef);
      if (type == null) return fromCall;
      if (type instanceof GoTypeList) {
        if (((GoTypeList)type).getTypeList().size() > i &&
            !isFuncInFuncSignature(underlyingType)) {
          return ((GoTypeList)type).getTypeList().get(i);
        }
      }
      return type;
    }
    if (exprs.size() <= i) return null;
    return exprs.get(i).getGoType(context);
  }

  private static boolean isFuncInFuncSignature(@Nullable GoType type) {
    // TODO improve ugly hack to see if we are called from within a GoSignature
    // We don't want to split up the list of variables if the parent is a result type and the parent of that parent is a function
    // because we've essentially bumped into a function signature that's returned by a function
    // e.g. func demo() func() (res1 int, res2 error)

    if (type == null) return false;

    PsiElement parentFunc = type;

    if (type.getParent() instanceof GoParameterDeclaration &&
        type.getParent().getParent() instanceof GoParameters) {
      parentFunc = type.getParent().getParent().getParent();
    }
    else if (type.getParent() instanceof GoResult) {
      parentFunc = type.getParent();
    }

    if (parentFunc.getParent() instanceof GoResult) {
      parentFunc = type.getParent();
    }
    else if (!(parentFunc instanceof GoResult)) {
      return false;
    }

    return parentFunc.getParent() instanceof GoSignature &&
           (type instanceof GoFunctionType ||
            parentFunc.getParent().getParent().getParent().getParent() instanceof GoSignature);
  }

  @Nullable
  private static GoType funcType(@Nullable GoType type) {
    if (type instanceof GoFunctionType) {
      GoSignature signature = ((GoFunctionType)type).getSignature();
      GoResult result = signature != null ? signature.getResult() : null;
      if (result != null) {
        GoType rt = result.getType();
        if (rt != null) return rt;
        GoParameters parameters = result.getParameters();
        if (parameters != null) {
          List<GoParameterDeclaration> list = parameters.getParameterDeclarationList();
          List<GoType> types = ContainerUtil.newArrayListWithCapacity(list.size());
          for (GoParameterDeclaration declaration : list) {
            GoType declarationType = declaration.getType();
            for (GoParamDefinition ignored : declaration.getParamDefinitionList()) {
              types.add(declarationType);
            }
          }
          if (types.size() == 1) return types.get(0);
          return new MyGoTypeList(parameters, types);
        }
      }
    }
    return type;
  }

  @Nullable
  private static GoType processRangeClause(@NotNull GoVarDefinition o, @NotNull GoRangeClause parent, @Nullable ResolveState context) {
    List<GoExpression> exprs = parent.getExpressionList();
    GoExpression last = ContainerUtil.getLastItem(exprs);
    int rangeOffset = parent.getRange().getTextOffset();
    last = last != null && last.getTextOffset() > rangeOffset ? last : null;

    if (last != null) {
      List<GoVarDefinition> varList = parent.getVarDefinitionList();
      int i = varList.indexOf(o);
      i = i == -1 ? 0 : i;
      GoType type = last.getGoType(context);
      if (type instanceof GoChannelType) {
        return ((GoChannelType)type).getType();
      }
      if (type instanceof GoPointerType) {
        type = ((GoPointerType)type).getType();
      }
      else if (type instanceof GoParType) {
        type = ((GoParType)type).getType();
      }
      else if (type instanceof GoSpecType) {
        type = ((GoSpecType)type).getType();
      }
      GoTypeReferenceExpression typeRef = type != null ? type.getTypeReferenceExpression() : null;
      if (typeRef != null) {
        PsiElement resolve = typeRef.getReference().resolve();
        if (resolve instanceof GoTypeSpec) {
          type = ((GoTypeSpec)resolve).getSpecType().getType();
          if (type instanceof GoChannelType) {
            return ((GoChannelType)type).getType();
          }
        }
      }
      if (type instanceof GoArrayOrSliceType && i == 1) return ((GoArrayOrSliceType)type).getType();
      if (type instanceof GoMapType) {
        List<GoType> list = ((GoMapType)type).getTypeList();
        if (i == 0) return ContainerUtil.getFirstItem(list);
        if (i == 1) return ContainerUtil.getLastItem(list);
      }
    }
    return null;
  }

  @NotNull
  public static String getText(@Nullable GoType o) {
    if (o == null) return "";
    if (o instanceof GoSpecType) {
      String fqn = getFqn(getTypeSpecSafe(o));
      if (fqn != null) {
        return fqn;
      }
    }
    else if (o instanceof GoStructType) {
      return ((GoStructType)o).getFieldDeclarationList().isEmpty() ? "struct{}" : "struct {...}";
    }
    else if (o instanceof GoInterfaceType) {
      return ((GoInterfaceType)o).getMethodSpecList().isEmpty() ? "interface{}" : "interface {...}";
    }
    String text = o.getText();
    if (text == null) return "";
    return text.replaceAll("\\s+", " ");
  }

  @Nullable
  private static String getFqn(@Nullable GoTypeSpec typeSpec) {
    if (typeSpec == null) return null;
    String name = typeSpec.getName();
    GoFile file = typeSpec.getContainingFile();
    String packageName = file.getPackageName();
    if (name != null) {
      return packageName != null && !isBuiltinFile(file) ? packageName + "." + name : name;
    }
    return null;
  }

  @NotNull
  public static List<GoMethodSpec> getMethods(@NotNull GoInterfaceType o) {
    return ContainerUtil.filter(o.getMethodSpecList(), new Condition<GoMethodSpec>() {
      @Override
      public boolean value(@NotNull GoMethodSpec spec) {
        return spec.getIdentifier() != null;
      }
    });
  }

  @NotNull
  public static List<GoTypeReferenceExpression> getBaseTypesReferences(@NotNull GoInterfaceType o) {
    final List<GoTypeReferenceExpression> refs = ContainerUtil.newArrayList();
    o.accept(new GoRecursiveVisitor() {
      @Override
      public void visitMethodSpec(@NotNull GoMethodSpec o) {
        ContainerUtil.addIfNotNull(refs, o.getTypeReferenceExpression());
      }
    });
    return refs;
  }

  @NotNull
  public static List<GoMethodDeclaration> getMethods(@NotNull final GoTypeSpec o) {
    final PsiDirectory dir = o.getContainingFile().getOriginalFile().getParent();
    if (dir != null) {
      return CachedValuesManager.getCachedValue(o, new CachedValueProvider<List<GoMethodDeclaration>>() {
        @Nullable
        @Override
        public Result<List<GoMethodDeclaration>> compute() {
          return Result.create(calcMethods(o), dir);
        }
      });
    }
    return calcMethods(o);
  }

  static boolean allowed(@NotNull PsiFile file, boolean isTesting) {
    return file instanceof GoFile && (!GoTestFinder.isTestFile(file) || isTesting) && GoUtil.allowed(file);
  }

  public static boolean allowed(@NotNull PsiFile file, @Nullable PsiFile contextFile) {
    if (contextFile == null || !(contextFile instanceof GoFile)) return true;
    return allowed(file, GoTestFinder.isTestFile(contextFile));
  }

  static boolean processNamedElements(@NotNull PsiScopeProcessor processor,
                                      @NotNull ResolveState state,
                                      @NotNull Collection<? extends GoNamedElement> elements,
                                      boolean localResolve) {
    return processNamedElements(processor, state, elements, localResolve, false);
  }

  static boolean processNamedElements(@NotNull PsiScopeProcessor processor,
                                      @NotNull ResolveState state,
                                      @NotNull Collection<? extends GoNamedElement> elements,
                                      boolean localResolve,
                                      boolean checkContainingFile) {
    PsiFile contextFile = checkContainingFile ? getContextFile(state) : null;
    for (GoNamedElement definition : elements) {
      if (!definition.isValid() || checkContainingFile && !allowed(definition.getContainingFile(), contextFile)) continue;
      if ((localResolve || definition.isPublic()) && !processor.execute(definition, state)) return false;
    }
    return true;
  }

  @Nullable
  static PsiFile getContextFile(@NotNull ResolveState state) {
    SmartPsiElementPointer<GoReferenceExpressionBase> context = state.get(CONTEXT);
    return context != null ? context.getContainingFile() : null;
  }

  public static boolean processSignatureOwner(@NotNull GoSignatureOwner o, @NotNull GoScopeProcessorBase processor) {
    GoSignature signature = o.getSignature();
    if (signature == null) return true;
    if (!processParameters(processor, signature.getParameters())) return false;
    GoResult result = signature.getResult();
    GoParameters resultParameters = result != null ? result.getParameters() : null;
    return resultParameters == null || processParameters(processor, resultParameters);
  }

  private static boolean processParameters(@NotNull GoScopeProcessorBase processor, @NotNull GoParameters parameters) {
    for (GoParameterDeclaration declaration : parameters.getParameterDeclarationList()) {
      if (!processNamedElements(processor, ResolveState.initial(), declaration.getParamDefinitionList(), true)) return false;
    }
    return true;
  }

  @NotNull
  public static String joinPsiElementText(List<? extends PsiElement> items) {
    return StringUtil.join(items, GET_TEXT_FUNCTION, ", ");
  }

  @Nullable
  public static PsiElement getBreakStatementOwner(@NotNull PsiElement breakStatement) {
    GoCompositeElement owner = PsiTreeUtil.getParentOfType(breakStatement, GoSwitchStatement.class, GoForStatement.class,
                                                           GoSelectStatement.class, GoFunctionLit.class);
    return owner instanceof GoFunctionLit ? null : owner;
  }

  @NotNull
  private static List<GoMethodDeclaration> calcMethods(@NotNull GoTypeSpec o) {
    PsiFile file = o.getContainingFile().getOriginalFile();
    if (file instanceof GoFile) {
      String packageName = ((GoFile)file).getPackageName();
      String typeName = o.getName();
      if (StringUtil.isEmpty(packageName) || StringUtil.isEmpty(typeName)) return Collections.emptyList();
      String key = packageName + "." + typeName;
      Project project = ((GoFile)file).getProject();
      PsiDirectory parent = file.getParent();
      GlobalSearchScope scope = parent == null ? GlobalSearchScope.allScope(project) : GlobalSearchScopesCore.directoryScope(parent, false);
      Collection<GoMethodDeclaration> declarations = GoMethodIndex.find(key, project, scope);
      return ContainerUtil.newArrayList(declarations);
    }
    return Collections.emptyList();
  }

  @Nullable
  public static GoType getGoTypeInner(@NotNull GoAnonymousFieldDefinition o, @SuppressWarnings("UnusedParameters") @Nullable ResolveState context) {
    return findBaseTypeFromRef(o.getTypeReferenceExpression());
  }

  @Nullable
  public static GoType findBaseSpecType(@Nullable GoType type) {
    while (type instanceof GoSpecType && ((GoSpecType)type).getType().getTypeReferenceExpression() != null) {
      GoType inner = findTypeFromTypeRef(((GoSpecType)type).getType().getTypeReferenceExpression());
      if (inner == null || type.isEquivalentTo(inner) || builtin(inner)) return type;
      type = inner;
    }
    return type;
  }

  /**
   * Finds the "base" type of {@code expression}, resolving type references iteratively until a type spec is found.
   */  
  @Nullable
  public static GoType findBaseTypeFromRef(@Nullable GoTypeReferenceExpression expression) {
    return findBaseSpecType(findTypeFromTypeRef(expression));
  }

  @Nullable
  public static GoType getGoTypeInner(@NotNull GoSignatureOwner o, @SuppressWarnings("UnusedParameters") @Nullable ResolveState context) {
    GoSignature signature = o.getSignature();
    GoResult result = signature != null ? signature.getResult() : null;
    if (result != null) {
      GoType type = result.getType();
      if (type instanceof GoTypeList && ((GoTypeList)type).getTypeList().size() == 1) {
        return ((GoTypeList)type).getTypeList().get(0);
      }
      if (type != null) return type;
      GoParameters parameters = result.getParameters();
      if (parameters != null) {
        GoType parametersType = parameters.getType();
        if (parametersType != null) return parametersType;
        List<GoType> composite = ContainerUtil.newArrayList();
        for (GoParameterDeclaration p : parameters.getParameterDeclarationList()) {
          for (GoParamDefinition definition : p.getParamDefinitionList()) {
            composite.add(definition.getGoType(context));
          }
        }
        if (composite.size() == 1) return composite.get(0);
        return new MyGoTypeList(parameters, composite);
      }
    }
    return null;
  }

  @NotNull
  public static GoImportSpec addImport(@NotNull GoImportList importList, @NotNull String packagePath, @Nullable String alias) {
    Project project = importList.getProject();
    GoImportDeclaration newDeclaration = GoElementFactory.createImportDeclaration(project, packagePath, alias, false);
    List<GoImportDeclaration> existingImports = importList.getImportDeclarationList();
    for (int i = existingImports.size() - 1; i >= 0; i--) {
      GoImportDeclaration existingImport = existingImports.get(i);
      List<GoImportSpec> importSpecList = existingImport.getImportSpecList();
      if (existingImport.getRparen() == null && importSpecList.size() == 1) {
        GoImportSpec firstItem = ContainerUtil.getFirstItem(importSpecList);
        assert firstItem != null;
        if (firstItem.isCImport()) continue;
        String path = firstItem.getPath();
        String oldAlias = firstItem.getAlias();

        GoImportDeclaration importWithParentheses = GoElementFactory.createImportDeclaration(project, path, oldAlias, true);
        existingImport = (GoImportDeclaration)existingImport.replace(importWithParentheses);
      }
      return existingImport.addImportSpec(packagePath, alias);
    }
    return addImportDeclaration(importList, newDeclaration);
  }

  @NotNull
  private static GoImportSpec addImportDeclaration(@NotNull GoImportList importList, @NotNull GoImportDeclaration newImportDeclaration) {
    GoImportDeclaration lastImport = ContainerUtil.getLastItem(importList.getImportDeclarationList());
    GoImportDeclaration importDeclaration = (GoImportDeclaration)importList.addAfter(newImportDeclaration, lastImport);
    PsiElement importListNextSibling = importList.getNextSibling();
    if (!(importListNextSibling instanceof PsiWhiteSpace)) {
      importList.addAfter(GoElementFactory.createNewLine(importList.getProject()), importDeclaration);
      if (importListNextSibling != null) {
        // double new line if there is something valuable after import list
        importList.addAfter(GoElementFactory.createNewLine(importList.getProject()), importDeclaration);
      }
    }
    importList.addBefore(GoElementFactory.createNewLine(importList.getProject()), importDeclaration);
    GoImportSpec result = ContainerUtil.getFirstItem(importDeclaration.getImportSpecList());
    assert result != null;
    return result;
  }

  @NotNull
  public static GoImportSpec addImportSpec(@NotNull GoImportDeclaration declaration, @NotNull String packagePath, @Nullable String alias) {
    PsiElement rParen = declaration.getRparen();
    assert rParen != null;
    declaration.addBefore(GoElementFactory.createNewLine(declaration.getProject()), rParen);
    GoImportSpec spec = (GoImportSpec)declaration.addBefore(GoElementFactory.createImportSpec(declaration.getProject(), packagePath, alias), rParen);
    declaration.addBefore(GoElementFactory.createNewLine(declaration.getProject()), rParen);
    return spec;
  }

  public static String getLocalPackageName(@NotNull String importPath) {
    StringBuilder name = null;
    for (int i = 0; i < importPath.length(); i++) {
      char c = importPath.charAt(i);
      if (!(Character.isLetter(c) || c == '_' || (i != 0 && Character.isDigit(c)))) {
        if (name == null) {
          name = new StringBuilder(importPath.length());
          name.append(importPath, 0, i);
        }
        name.append('_');
      }
      else if (name != null) {
        name.append(c);
      }
    }
    return name == null ? importPath : name.toString();
  }

  public static String getLocalPackageName(@NotNull GoImportSpec importSpec) {
    return getLocalPackageName(importSpec.getPath());
  }

  public static boolean isCImport(@NotNull GoImportSpec importSpec) {
    return GoConstants.C_PATH.equals(importSpec.getPath());
  }

  public static boolean isDot(@NotNull GoImportSpec importSpec) {
    GoImportSpecStub stub = importSpec.getStub();
    return stub != null ? stub.isDot() : importSpec.getDot() != null;
  }

  @NotNull
  public static String getPath(@NotNull GoImportSpec importSpec) {
    GoImportSpecStub stub = importSpec.getStub();
    return stub != null ? stub.getPath() : importSpec.getImportString().getPath();
  }

  public static String getName(@NotNull GoImportSpec importSpec) {
    return getAlias(importSpec);
  }

  public static String getAlias(@NotNull GoImportSpec importSpec) {
    GoImportSpecStub stub = importSpec.getStub();
    if (stub != null) {
      return stub.getAlias();
    }

    PsiElement identifier = importSpec.getIdentifier();
    if (identifier != null) {
      return identifier.getText();
    }
    return importSpec.isDot() ? "." : null;
  }

  public static boolean shouldGoDeeper(@SuppressWarnings("UnusedParameters") GoImportSpec o) {
    return false;
  }

  public static boolean shouldGoDeeper(@SuppressWarnings("UnusedParameters") GoTypeSpec o) {
    return false;
  }

  public static boolean isForSideEffects(@NotNull GoImportSpec o) {
    return "_".equals(o.getAlias());
  }

  @NotNull
  public static String getPath(@NotNull GoImportString o) {
    return unquote(o.getText());
  }

  @NotNull
  public static String unquote(@Nullable String s) {
    if (StringUtil.isEmpty(s)) return "";
    char quote = s.charAt(0);
    int startOffset = isQuote(quote) ? 1 : 0;
    int endOffset = s.length();
    if (s.length() > 1) {
      char lastChar = s.charAt(s.length() - 1);
      if (isQuote(quote) && lastChar == quote) {
        endOffset = s.length() - 1;
      }
      if (!isQuote(quote) && isQuote(lastChar)){
        endOffset = s.length() - 1;
      }
    }
    return s.substring(startOffset, endOffset);
  }

  @NotNull
  public static TextRange getPathTextRange(@NotNull GoImportString importString) {
    String text = importString.getText();
    return !text.isEmpty() && isQuote(text.charAt(0)) ? TextRange.create(1, text.length() - 1) : TextRange.EMPTY_RANGE;
  }

  public static boolean isQuotedImportString(@NotNull String s) {
    return s.length() > 1 && isQuote(s.charAt(0)) && s.charAt(0) == s.charAt(s.length() - 1);
  }

  private static boolean isQuote(char ch) {
    return ch == '"' || ch == '\'' || ch == '`';
  }

  public static boolean isValidHost(@SuppressWarnings("UnusedParameters") @NotNull GoStringLiteral o) {
      return true;
  }

  @NotNull
  public static GoStringLiteralImpl updateText(@NotNull GoStringLiteral o, @NotNull String text) {
    if (text.length() > 2) {
      if (o.getString() != null) {
        StringBuilder outChars = new StringBuilder();
        GoStringLiteralEscaper.escapeString(text.substring(1, text.length()-1), outChars);
        outChars.insert(0, '"');
        outChars.append('"');
        text = outChars.toString();
      }
    }

    ASTNode valueNode = o.getNode().getFirstChildNode();
    assert valueNode instanceof LeafElement;

    ((LeafElement)valueNode).replaceWithText(text);
    return (GoStringLiteralImpl)o;
  }

  @NotNull
  public static GoStringLiteralEscaper createLiteralTextEscaper(@NotNull GoStringLiteral o) {
    return new GoStringLiteralEscaper(o);
  }

  public static boolean prevDot(@Nullable PsiElement e) {
    PsiElement prev = e == null ? null : PsiTreeUtil.prevVisibleLeaf(e);
    return prev instanceof LeafElement && ((LeafElement)prev).getElementType() == GoTypes.DOT;
  }

  @Nullable
  public static GoSignatureOwner resolveCall(@Nullable GoExpression call) {
    return ObjectUtils.tryCast(resolveCallRaw(call), GoSignatureOwner.class);
  }

  public static PsiElement resolveCallRaw(@Nullable GoExpression call) {
    if (!(call instanceof GoCallExpr)) return null;
    GoExpression e = ((GoCallExpr)call).getExpression();
    if (e instanceof GoSelectorExpr) {
      GoExpression right = ((GoSelectorExpr)e).getRight();
      PsiReference reference = right instanceof GoReferenceExpression ? right.getReference() : null;
      return reference != null ? reference.resolve() : null;
    }
    else if (e instanceof GoCallExpr) {
      GoSignatureOwner resolve = resolveCall(e);
      if (resolve != null) {
        GoSignature signature = resolve.getSignature();
        GoResult result = signature != null ? signature.getResult() : null;
        return result != null ? result.getType() : null;
      }
      return null;
    }
    GoReferenceExpression r = e instanceof GoReferenceExpression ? ((GoReferenceExpression)e) : PsiTreeUtil.getChildOfType(e, GoReferenceExpression.class);
    PsiReference reference = (r != null ? r : e).getReference();
    return reference != null ? reference.resolve() : null;
  }

  public static boolean isUnaryBitAndExpression(@Nullable PsiElement parent) {
    PsiElement grandParent = parent != null ? parent.getParent() : null;
    return grandParent instanceof GoUnaryExpr && ((GoUnaryExpr)grandParent).getBitAnd() != null;
  }

  @NotNull
  public static GoVarSpec addSpec(@NotNull GoVarDeclaration declaration,
                                  @NotNull String name,
                                  @Nullable String type,
                                  @Nullable String value,
                                  @Nullable GoVarSpec specAnchor) {
    Project project = declaration.getProject();
    GoVarSpec newSpec = GoElementFactory.createVarSpec(project, name, type, value);
    PsiElement rParen = declaration.getRparen();
    if (rParen == null) {
      GoVarSpec item = ContainerUtil.getFirstItem(declaration.getVarSpecList());
      assert item != null;
      boolean updateAnchor = specAnchor == item;
      declaration = (GoVarDeclaration)declaration.replace(GoElementFactory.createVarDeclaration(project, "(" + item.getText() + ")"));
      rParen = declaration.getRparen();
      if (updateAnchor) {
        specAnchor = ContainerUtil.getFirstItem(declaration.getVarSpecList());
      }
    }

    assert rParen != null;
    PsiElement anchor = ObjectUtils.notNull(specAnchor, rParen);
    if (!hasNewLineBefore(anchor)) {
      declaration.addBefore(GoElementFactory.createNewLine(declaration.getProject()), anchor);
    }
    GoVarSpec spec = (GoVarSpec)declaration.addBefore(newSpec, anchor);
    declaration.addBefore(GoElementFactory.createNewLine(declaration.getProject()), anchor);
    return spec;
  }

  @NotNull
  public static GoConstSpec addSpec(@NotNull GoConstDeclaration declaration,
                                    @NotNull String name,
                                    @Nullable String type,
                                    @Nullable String value,
                                    @Nullable GoConstSpec specAnchor) {
    Project project = declaration.getProject();
    GoConstSpec newSpec = GoElementFactory.createConstSpec(project, name, type, value);
    PsiElement rParen = declaration.getRparen();
    if (rParen == null) {
      GoConstSpec item = ContainerUtil.getFirstItem(declaration.getConstSpecList());
      assert item != null;
      boolean updateAnchor = specAnchor == item;
      declaration = (GoConstDeclaration)declaration.replace(GoElementFactory.createConstDeclaration(project, "(" + item.getText() + ")"));
      rParen = declaration.getRparen();
      if (updateAnchor) {
        specAnchor = ContainerUtil.getFirstItem(declaration.getConstSpecList());
      }
    }

    assert rParen != null;
    PsiElement anchor = ObjectUtils.notNull(specAnchor, rParen);
    if (!hasNewLineBefore(anchor)) {
      declaration.addBefore(GoElementFactory.createNewLine(declaration.getProject()), anchor);
    }
    GoConstSpec spec = (GoConstSpec)declaration.addBefore(newSpec, anchor);
    declaration.addBefore(GoElementFactory.createNewLine(declaration.getProject()), anchor);
    return spec;
  }

  public static void deleteSpec(@NotNull GoVarDeclaration declaration, @NotNull GoVarSpec specToDelete) {
    List<GoVarSpec> specList = declaration.getVarSpecList();
    int index = specList.indexOf(specToDelete);
    assert index >= 0;
    if (specList.size() == 1) {
      declaration.delete();
      return;
    }
    specToDelete.delete();
  }

  public static void deleteSpec(@NotNull GoConstDeclaration declaration, @NotNull GoConstSpec specToDelete) {
    List<GoConstSpec> specList = declaration.getConstSpecList();
    int index = specList.indexOf(specToDelete);
    assert index >= 0;
    if (specList.size() == 1) {
      declaration.delete();
      return;
    }
    specToDelete.delete();
  }

  public static void deleteDefinition(@NotNull GoVarSpec spec, @NotNull GoVarDefinition definitionToDelete) {
    List<GoVarDefinition> definitionList = spec.getVarDefinitionList();
    int index = definitionList.indexOf(definitionToDelete);
    assert index >= 0;
    if (definitionList.size() == 1) {
      PsiElement parent = spec.getParent();
      if (parent instanceof GoVarDeclaration) {
        ((GoVarDeclaration)parent).deleteSpec(spec);
      }
      else {
        spec.delete();
      }
      return;
    }

    GoExpression value = definitionToDelete.getValue();
    if (value != null && spec.getExpressionList().size() <= 1) {
      PsiElement assign = spec.getAssign();
      if (assign != null) {
        assign.delete();
      }
    }
    deleteElementFromCommaSeparatedList(value);
    deleteElementFromCommaSeparatedList(definitionToDelete);
  }

  public static void deleteDefinition(@NotNull GoConstSpec spec, @NotNull GoConstDefinition definitionToDelete) {
    List<GoConstDefinition> definitionList = spec.getConstDefinitionList();
    int index = definitionList.indexOf(definitionToDelete);
    assert index >= 0;
    if (definitionList.size() == 1) {
      PsiElement parent = spec.getParent();
      if (parent instanceof GoConstDeclaration) {
        ((GoConstDeclaration)parent).deleteSpec(spec);
      }
      else {
        spec.delete();
      }
      return;
    }
    GoExpression value = definitionToDelete.getValue();
    if (value != null && spec.getExpressionList().size() <= 1) {
      PsiElement assign = spec.getAssign();
      if (assign != null) {
        assign.delete();
      }
    }
    deleteElementFromCommaSeparatedList(value);
    deleteElementFromCommaSeparatedList(definitionToDelete);
  }

  private static void deleteElementFromCommaSeparatedList(@Nullable PsiElement element) {
    if (element == null) {
      return;
    }
    PsiElement prevVisibleLeaf = PsiTreeUtil.prevVisibleLeaf(element);
    PsiElement nextVisibleLeaf = PsiTreeUtil.nextVisibleLeaf(element);
    if (prevVisibleLeaf != null && prevVisibleLeaf.textMatches(",")) {
      prevVisibleLeaf.delete();
    }
    else if (nextVisibleLeaf != null && nextVisibleLeaf.textMatches(",")) {
      nextVisibleLeaf.delete();
    }
    element.delete();
  }

  private static boolean hasNewLineBefore(@NotNull PsiElement anchor) {
    PsiElement prevSibling = anchor.getPrevSibling();
    while (prevSibling != null && prevSibling instanceof PsiWhiteSpace) {
      if (prevSibling.textContains('\n')) {
        return true;
      }
      prevSibling = prevSibling.getPrevSibling();
    }
    return false;
  }

  @Nullable
  public static GoExpression getValue(@NotNull GoVarDefinition definition) {
    PsiElement parent = definition.getParent();
    assert parent instanceof GoVarSpec;
    int index = ((GoVarSpec)parent).getVarDefinitionList().indexOf(definition);
    return getByIndex(((GoVarSpec)parent).getExpressionList(), index);
  }

  @Nullable
  public static GoExpression getValue(@NotNull GoConstDefinition definition) {
    PsiElement parent = definition.getParent();
    assert parent instanceof GoConstSpec;
    int index = ((GoConstSpec)parent).getConstDefinitionList().indexOf(definition);
    return getByIndex(((GoConstSpec)parent).getExpressionList(), index);
  }

  private static <T> T getByIndex(@NotNull List<T> list, int index) {
    return 0 <= index && index < list.size()? list.get(index) : null;
  }

  @Nullable
  public static GoTypeSpec getTypeSpecSafe(@NotNull GoType type) {
    GoTypeStub stub = type.getStub();
    PsiElement parent = stub == null ? type.getParent() : stub.getParentStub().getPsi();
    return ObjectUtils.tryCast(parent, GoTypeSpec.class);
  }

  @NotNull
  public PsiElement getType(@NotNull GoTypeSpec o) {
    return o.getSpecType();
  }

  /**
   * Returns {@code true} if the given element is in an invalid location for a type literal or type reference.
   */
  public static boolean isIllegalUseOfTypeAsExpression(@NotNull PsiElement e) {
    PsiElement parent = PsiTreeUtil.skipParentsOfType(e, GoParenthesesExpr.class, GoUnaryExpr.class);
    // Part of a selector such as T.method
    if (parent instanceof GoReferenceExpression || parent instanceof GoSelectorExpr) return false;
    // A situation like T("foo").
    return !(parent instanceof GoCallExpr);
  }

  public static boolean hasUnresolvedReferences(@NotNull List<GoExpression> expressionList) {
    for (GoExpression expression : expressionList) {
      if (expression instanceof GoReferenceExpression) {
        if (expression.getReference() == null ||
            expression.getReference().resolve() == null) {
          return true;
        }

        if (isIllegalUseOfTypeAsExpression(expression)) {
          return true;
        }
      }
    }
    return false;
  }

  public static boolean hasUnresolvedCalls(@NotNull List<GoExpression> expressionList) {
    for (GoExpression expression : expressionList) {
      if (expression instanceof GoCallExpr) {
        expression = ((GoCallExpr)expression).getExpression();
        if (expression.getGoType(null) == null) {
          if (expression.getReference() != null) {
            if (expression.getReference().resolve() == null) {
              return true;
            }
            continue;
          }
          return true;
        }
      }
    }
    return false;
  }

  public static boolean isTypeAssertion(@NotNull List<GoExpression> expressionList) {
    if (expressionList.size() != 1) {
      return false;
    }

    GoExpression expr = expressionList.get(0);
    if (expr instanceof GoParenthesesExpr) {
      expr = ((GoParenthesesExpr)expr).getExpression();
    }

    return expr instanceof GoTypeAssertionExpr;
  }

  public static boolean isMapRead(@NotNull List<GoExpression> expressionList) {
    if (expressionList.size() != 1) {
      return false;
    }

    GoExpression expr = expressionList.get(0);
    return expr instanceof GoIndexOrSliceExpr;
  }

  public static boolean isChannelReceiver(@NotNull List<GoExpression> expressionList) {
    if (expressionList.size() != 1) {
      return false;
    }

    GoExpression expr = expressionList.get(0);
    if (expr instanceof GoParenthesesExpr) {
      expr = ((GoParenthesesExpr)expr).getExpression();
    }
    return (expr instanceof GoUnaryExpr &&
            ((GoUnaryExpr)expr).getSendChannel() != null);
  }

  public static boolean hasAtLeastOneCall(@NotNull List<GoExpression> expressionList) {
    for (GoExpression expression : expressionList) {
      if (expression instanceof GoCallExpr) return true;
    }
    return false;
  }

  public static boolean hasCGOImport(@NotNull GoExpression expression) {
    List<GoImportSpec> imports = ((GoFile)expression.getContainingFile()).getImports();
    for (GoImportSpec goImport : imports) {
      if (goImport.getPath().equals("C")) {
        return true;
      }
    }

    return false;
  }

  public static boolean hasCGOCall(@NotNull List<GoExpression> expressionList) {
    if (expressionList.size() < 1) return false;

    if (!hasCGOImport(expressionList.get(0))) return false;

    for (GoExpression expression : expressionList) {
      if (expression instanceof GoCallExpr) {
        if (expression.getText().startsWith("C.")) {
          return true;
        }
      }
    }
    return false;
  }

  public static int getArity(@NotNull List<GoExpression> expressionList) {
    ProgressIndicatorProvider.checkCanceled();

    int result = 0;
    for (GoExpression expression : expressionList) {
      result += getArity(expression);
    }
    return result;
  }

  public static int getArity(@Nullable GoExpression expression) {
    ProgressIndicatorProvider.checkCanceled();

    if (expression == null) return 0;

    if (expression instanceof GoCallExpr) {
      GoCallExpr callExpr = (GoCallExpr)expression;
      if (callExpr.getExpression() instanceof GoCallExpr) {
        GoType goType = callExpr.getExpression().getGoType(null);
        if (goType == null) return 0;

        GoType myGoType = callExpr.getExpression().getGoType(null);
        if (myGoType != null) {
          GoType baseRefType = findBaseTypeFromRef(myGoType.getTypeReferenceExpression());

          if (baseRefType == null) {
            return getArity(goType);
          }
          else if (baseRefType instanceof GoSpecType) {
            return getArity(((GoSpecType)baseRefType).getType());
          }
        }
        myGoType = ((GoCallExpr)callExpr.getExpression()).getExpression().getGoType(null);
        if (myGoType instanceof GoFunctionType) {
          return getArity((GoFunctionType)myGoType);
        }
        return getArity(findBaseTypeFromRef(goType.getTypeReferenceExpression()));
      }
      return getArity(callExpr);
    }
    else if (expression instanceof GoReferenceExpression) {
      return getArity((GoReferenceExpression)expression);
    }
    else if (expression instanceof GoBuiltinCallExpr) {
      return getArity(((GoBuiltinCallExpr)expression).getReferenceExpression());
    }
    else if (expression instanceof GoParenthesesExpr) {
      return getArity(((GoParenthesesExpr)expression).getExpression());
    }

    return 1;
  }

  public static int getArity(@NotNull GoCallExpr expression) {
    ProgressIndicatorProvider.checkCanceled();

    if (expression.getExpression() instanceof GoFunctionLit) {
      GoFunctionLit func = ((GoFunctionLit)expression.getExpression());
      if (func.getSignature() == null) return 0;

      return getArity(func.getSignature());
    }

    if (expression.getExpression() instanceof GoIndexOrSliceExpr) {
      return getArity(
        ((GoIndexOrSliceExpr)expression.getExpression())
          .getExpressionList()
          .get(0)
          .getGoType(null));
    }
    else if (expression.getExpression() instanceof GoSelectorExpr) {
      if (isFuncInFuncSignature(expression.getExpression().getGoType(null))) {
        return 1;
      }
      return getArity(expression.getExpression().getGoType(null));
    }

    PsiReference reference = expression.getExpression().getReference();
    PsiElement resolve = reference != null ? reference.resolve() : null;

    if (resolve == null) {
      GoType expressionGoType = expression.getExpression().getGoType(null);
      if (expressionGoType != null) {
        return getArity(expressionGoType);
      }
      return 0;
    }

    if (resolve instanceof GoParamDefinition) {
      PsiElement parent = resolve.getParent();
      if (parent instanceof GoParameterDeclaration) {
        return getArity(((GoParameterDeclaration)parent).getType());
      }
    }

    if (resolve instanceof GoVarDefinition) {
      GoType goType = ((GoVarDefinition)resolve).getGoType(null);
      if (goType == null) {
        PsiElement parent = resolve.getParent();
        if (parent instanceof GoVarSpec) {
          return getArity(((GoVarDefinition)resolve).getValue());
        }
        else if (parent instanceof GoTypeSwitchGuard) {
          return 1;
        }

        return 0;
      }
      return getArity(goType);
    }
    else if (resolve instanceof GoFieldDefinition) {
      GoType innerType = ((GoFieldDefinition)resolve).getGoType(null);
      return getArity(innerType);
    }
    else if (resolve instanceof GoAnonymousFieldDefinition) {
      GoType innerType = ((GoAnonymousFieldDefinition)resolve).getGoType(null);
      return getArity(innerType);
    }
    else if (resolve instanceof GoFunctionDeclaration &&
             !(resolve.getParent() instanceof GoFile)) {
      return 1;
    }
    else if (resolve instanceof GoSignatureOwner) {
      GoSignature signature = ((GoSignatureOwner)resolve).getSignature();
      return getArity(signature);
    }
    else if (resolve instanceof GoSpecType) {
      return getArity((GoSpecType)resolve);
    }
    else if (resolve instanceof GoType) {
      return getArity((GoType)resolve);
    }
    else if (resolve instanceof GoTypeSpec) {
      return 1;
    }
    else if (resolve instanceof GoReceiver) {
      return getArity(((GoReceiver)resolve).getType());
    }

    return 0;
  }

  public static int getArity(@NotNull GoReferenceExpression expression) {
    ProgressIndicatorProvider.checkCanceled();

    PsiElement resolve = expression.getReference().resolve();
    if (resolve == null) return 0;

    if (resolve instanceof GoFunctionOrMethodDeclaration &&
        !(expression.getParent() instanceof GoCallExpr) &&
        !(expression.getParent() instanceof GoBuiltinCallExpr)) {
      return 1;
    }

    GoType type = null;
    if (resolve instanceof GoVarDefinition) {
      type = ((GoVarDefinition)resolve).getGoType(null);
    }
    else if (resolve instanceof GoConstDefinition) {
      type = ((GoConstDefinition)resolve).getGoType(null);
    }
    else if (resolve instanceof GoFieldDefinition) {
      type = ((GoFieldDefinition)resolve).getGoType(null);
    }
    else if (resolve instanceof GoAnonymousFieldDefinition) {
      type = ((GoAnonymousFieldDefinition)resolve).getGoType(null);
    }
    else if (resolve instanceof GoFunctionDeclaration) {
      return getArity(((GoFunctionDeclaration)resolve).getSignature());
    }

    if (type == null ||
        type.getTypeReferenceExpression() == null) {
      return 1;
    }

    PsiReference reference = type.getTypeReferenceExpression().getReference();
    if (reference instanceof GoExpression) {
      return getArity((GoExpression)reference);
    }

    return 1;
  }

  public static int getArity(@Nullable GoSignature signature) {
    ProgressIndicatorProvider.checkCanceled();

    if (signature == null) return 0;

    GoResult result = signature.getResult();
    if (result == null) return 0;

    GoType signatureType = signature.getResult().getType();
    if (signatureType == null) {
      int arity = 0;
      GoParameters parameters = result.getParameters();
      if (parameters != null) {
        for (GoParameterDeclaration declaration : parameters.getParameterDeclarationList()) {
          arity += declaration.getParamDefinitionList().size();
        }
      }
      return arity;
    }

    if (signatureType instanceof GoTypeList) {
      return ((GoTypeList)signature.getResult().getType()).getTypeList().size();
    }
    else if (signatureType instanceof GoPointerType ||
             signatureType instanceof GoArrayOrSliceType ||
             signatureType instanceof GoChannelType ||
             signatureType instanceof GoMapType ||
             signatureType instanceof GoInterfaceType ||
             signatureType instanceof GoFunctionType ||
             signatureType.getTypeReferenceExpression() != null) {
      return 1;
    }

    return 0;
  }

  public static int getArity(@Nullable GoFunctionType function) {
    ProgressIndicatorProvider.checkCanceled();
    if (function == null) return 0;

    return getArity(function.getSignature());
  }

  public static int getArity(@Nullable GoType type) {
    ProgressIndicatorProvider.checkCanceled();

    if (type == null) return 0;

    if (type instanceof GoArrayOrSliceType ||
        type instanceof GoMapType) {
      if (type instanceof GoArrayOrSliceType) {
        type = ((GoArrayOrSliceType)type).getType();
      }
      else {
        type = ((GoMapType)type).getValueType();
      }
      if (type == null) return 0;

      if (type.getTypeReferenceExpression() != null) {
        return getArity(findBaseTypeFromRef(type.getTypeReferenceExpression()));
      }
      else if (type instanceof GoFunctionType) {
        return getArity((GoFunctionType)type);
      }

      return 1;
    }

    if (type instanceof MyGoTypeList) {
      return ((MyGoTypeList)type).getTypeList().size();
    }
    else if (type instanceof GoTypeList) {
      return ((GoTypeList)type).getTypeList().size();
    }
    else if (type instanceof GoSpecType) {
      return getArity((GoSpecType)type);
    }
    else if (type instanceof GoExpression) {
      return getArity((GoExpression)type);
    }
    else if (type instanceof GoFunctionType) {
      return getArity((GoFunctionType)type);
    }
    else if (type instanceof GoChannelType ||
             type instanceof GoInterfaceType ||
             type instanceof GoPointerType ||
             type.getParent() instanceof GoResult) {
      return 1;
    }
    else if (type instanceof GoParType) {
      GoType goPar = ((GoParType)type).getType();
      if (goPar instanceof GoPointerType) {
        return getArity(((GoPointerType)goPar).getType());
      }
      else if (goPar instanceof GoFunctionType) {
        return getArity(goPar);
      }
    }

    return getArity(findBaseTypeFromRef(type.getTypeReferenceExpression()));
  }

  public static int getArity(@NotNull GoSpecType type) {
    ProgressIndicatorProvider.checkCanceled();

    GoType myType = type.getType();
    if (myType instanceof GoFunctionType) {
      GoSignature signature = ((GoFunctionType)myType).getSignature();
      return getArity(signature);
    }

    return 1;
  }
}
