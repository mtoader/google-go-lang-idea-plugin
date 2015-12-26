// This is a generated file. Not intended for manual editing.
package com.goide.gotemplate.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiElement;

public class GoTemplateVisitor extends PsiElementVisitor {

  public void visitAndExpr(@NotNull GoTemplateAndExpr o) {
    visitExpression(o);
  }

  public void visitBlockStatement(@NotNull GoTemplateBlockStatement o) {
    visitStatement(o);
  }

  public void visitConditionalExpr(@NotNull GoTemplateConditionalExpr o) {
    visitExpression(o);
  }

  public void visitElseStatement(@NotNull GoTemplateElseStatement o) {
    visitStatement(o);
  }

  public void visitEndStatement(@NotNull GoTemplateEndStatement o) {
    visitStatement(o);
  }

  public void visitExpression(@NotNull GoTemplateExpression o) {
    visitPsiElement(o);
  }

  public void visitFieldChain(@NotNull GoTemplateFieldChain o) {
    visitPsiElement(o);
  }

  public void visitFieldChainExpr(@NotNull GoTemplateFieldChainExpr o) {
    visitExpression(o);
  }

  public void visitIdentLiteral(@NotNull GoTemplateIdentLiteral o) {
    visitPsiElement(o);
  }

  public void visitIfStatement(@NotNull GoTemplateIfStatement o) {
    visitStatement(o);
  }

  public void visitLiteral(@NotNull GoTemplateLiteral o) {
    visitPsiElement(o);
  }

  public void visitLiteralExpr(@NotNull GoTemplateLiteralExpr o) {
    visitExpression(o);
  }

  public void visitLocalIdentLiteral(@NotNull GoTemplateLocalIdentLiteral o) {
    visitPsiElement(o);
  }

  public void visitNumberLiteral(@NotNull GoTemplateNumberLiteral o) {
    visitPsiElement(o);
  }

  public void visitOrExpr(@NotNull GoTemplateOrExpr o) {
    visitExpression(o);
  }

  public void visitParenthesesExpr(@NotNull GoTemplateParenthesesExpr o) {
    visitExpression(o);
  }

  public void visitPipe(@NotNull GoTemplatePipe o) {
    visitPsiElement(o);
  }

  public void visitPipeline(@NotNull GoTemplatePipeline o) {
    visitPsiElement(o);
  }

  public void visitPipelineStatement(@NotNull GoTemplatePipelineStatement o) {
    visitStatement(o);
  }

  public void visitRangeAssign(@NotNull GoTemplateRangeAssign o) {
    visitPsiElement(o);
  }

  public void visitRangeStatement(@NotNull GoTemplateRangeStatement o) {
    visitStatement(o);
  }

  public void visitStatement(@NotNull GoTemplateStatement o) {
    visitPsiElement(o);
  }

  public void visitStatementList(@NotNull GoTemplateStatementList o) {
    visitPsiElement(o);
  }

  public void visitStringLiteral(@NotNull GoTemplateStringLiteral o) {
    visitPsiElement(o);
  }

  public void visitTemplateStatement(@NotNull GoTemplateTemplateStatement o) {
    visitStatement(o);
  }

  public void visitWithStatement(@NotNull GoTemplateWithStatement o) {
    visitStatement(o);
  }

  public void visitPsiElement(@NotNull PsiElement o) {
    visitElement(o);
  }

}
