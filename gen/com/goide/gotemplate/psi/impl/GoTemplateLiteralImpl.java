// This is a generated file. Not intended for manual editing.
package com.goide.gotemplate.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.goide.gotemplate.GoTemplateTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.goide.gotemplate.psi.*;

public class GoTemplateLiteralImpl extends ASTWrapperPsiElement implements GoTemplateLiteral {

  public GoTemplateLiteralImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof GoTemplateVisitor) ((GoTemplateVisitor)visitor).visitLiteral(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public GoTemplateIdentLiteral getIdentLiteral() {
    return findChildByClass(GoTemplateIdentLiteral.class);
  }

  @Override
  @Nullable
  public GoTemplateLocalIdentLiteral getLocalIdentLiteral() {
    return findChildByClass(GoTemplateLocalIdentLiteral.class);
  }

  @Override
  @Nullable
  public GoTemplateNumberLiteral getNumberLiteral() {
    return findChildByClass(GoTemplateNumberLiteral.class);
  }

  @Override
  @Nullable
  public GoTemplateStringLiteral getStringLiteral() {
    return findChildByClass(GoTemplateStringLiteral.class);
  }

}
