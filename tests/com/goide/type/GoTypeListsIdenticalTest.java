/*
 * Copyright 2013-2016 Sergey Ignatov, Alexander Zolotov, Florin Patan
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

package com.goide.type;

import com.goide.psi.*;
import com.goide.psi.impl.GoTypeUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nullable;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class GoTypeListsIdenticalTest extends GoTypesIdenticalTestCase {
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
      {"(int)", "(int)", true},
      {"(string)", "(int)", false},
      {"(int)", "(int, int)", false},
      {"(int, int)", "(int, int)", true},
    });
  }

  @Override
  void doTest() {
    PsiFile lFile = myFixture.addFileToProject("l.go", "package l; func _() " + left + "{}");
    PsiFile rFile = myFixture.addFileToProject("r.go", "package r; func _() " + right + "{}");
    myFixture.testHighlighting("l.go", "r.go");
    GoType left = getResultType((GoFile)lFile);
    GoType right = getResultType((GoFile)rFile);
    assert left != null && right != null;
    String leftText = left.getText();
    String rightText = right.getText();
    assertTrue(leftText + " should" + (!ok ? " not " : " ") + "equal " + rightText, ok == GoTypeUtil.identical(left, right));
  }

  @Nullable
  private static GoType getResultType(GoFile file) {
    GoFunctionDeclaration functionDeclaration = ContainerUtil.getFirstItem(file.getFunctions());
    GoSignature signature = functionDeclaration != null ? functionDeclaration.getSignature() : null;
    GoResult result = signature != null ? signature.getResult() : null;
    return result != null ? result.getType() : null;
  }

  @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
  public GoTypeListsIdenticalTest(String left, String right, boolean ok) {
    super(left, right, ok);
  }
}
