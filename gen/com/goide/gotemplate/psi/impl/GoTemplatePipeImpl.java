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

public class GoTemplatePipeImpl extends ASTWrapperPsiElement implements GoTemplatePipe {

  public GoTemplatePipeImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof GoTemplateVisitor) ((GoTemplateVisitor)visitor).visitPipe(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<GoTemplateExpression> getExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, GoTemplateExpression.class);
  }

  @Override
  @Nullable
  public GoTemplatePipe getPipe() {
    return findChildByClass(GoTemplatePipe.class);
  }

}
