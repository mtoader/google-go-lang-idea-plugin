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

public class GoTemplateStatementListImpl extends ASTWrapperPsiElement implements GoTemplateStatementList {

  public GoTemplateStatementListImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof GoTemplateVisitor) ((GoTemplateVisitor)visitor).visitStatementList(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<GoTemplateStatement> getStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, GoTemplateStatement.class);
  }

}
