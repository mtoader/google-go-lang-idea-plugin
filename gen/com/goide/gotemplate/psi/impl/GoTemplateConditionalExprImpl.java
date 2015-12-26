// This is a generated file. Not intended for manual editing.
package com.goide.gotemplate.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.goide.gotemplate.GoTemplateTypes.*;
import com.goide.gotemplate.psi.*;

public class GoTemplateConditionalExprImpl extends GoTemplateExpressionImpl implements GoTemplateConditionalExpr {

  public GoTemplateConditionalExprImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof GoTemplateVisitor) ((GoTemplateVisitor)visitor).visitConditionalExpr(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<GoTemplateExpression> getExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, GoTemplateExpression.class);
  }

}
