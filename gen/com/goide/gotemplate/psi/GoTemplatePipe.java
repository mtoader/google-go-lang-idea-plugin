// This is a generated file. Not intended for manual editing.
package com.goide.gotemplate.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface GoTemplatePipe extends PsiElement {

  @NotNull
  List<GoTemplateExpression> getExpressionList();

  @Nullable
  GoTemplatePipe getPipe();

}
