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

public class GoTemplateElseIfStatementImpl extends GoTemplateStatementImpl implements GoTemplateElseIfStatement {

  public GoTemplateElseIfStatementImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof GoTemplateVisitor) ((GoTemplateVisitor)visitor).visitElseIfStatement(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public GoTemplatePipeline getPipeline() {
    return findNotNullChildByClass(GoTemplatePipeline.class);
  }

  @Override
  @NotNull
  public GoTemplateStatement getStatement() {
    return findNotNullChildByClass(GoTemplateStatement.class);
  }

  @Override
  @NotNull
  public GoTemplateStatementList getStatementList() {
    return findNotNullChildByClass(GoTemplateStatementList.class);
  }

}
