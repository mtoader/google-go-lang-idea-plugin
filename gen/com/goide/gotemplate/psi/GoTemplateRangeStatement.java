// This is a generated file. Not intended for manual editing.
package com.goide.gotemplate.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface GoTemplateRangeStatement extends GoTemplateStatement {

  @Nullable
  GoTemplatePipeline getPipeline();

  @Nullable
  GoTemplateRangeAssign getRangeAssign();

  @Nullable
  GoTemplateStatement getStatement();

  @Nullable
  GoTemplateStatementList getStatementList();

}
