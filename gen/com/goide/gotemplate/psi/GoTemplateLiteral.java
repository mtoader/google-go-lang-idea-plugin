// This is a generated file. Not intended for manual editing.
package com.goide.gotemplate.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface GoTemplateLiteral extends PsiElement {

  @Nullable
  GoTemplateIdentLiteral getIdentLiteral();

  @Nullable
  GoTemplateLocalIdentLiteral getLocalIdentLiteral();

  @Nullable
  GoTemplateNumberLiteral getNumberLiteral();

  @Nullable
  GoTemplateStringLiteral getStringLiteral();

}
