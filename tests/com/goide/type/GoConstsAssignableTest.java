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

import com.goide.psi.GoFile;
import com.goide.psi.GoType;
import com.goide.psi.impl.GoTypeUtil;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class GoConstsAssignableTest extends GoTypesIdenticalTestCase {
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
      {"int", " = 3", true},
      {"float32", " = 3", true},
      {"int", " = 3.0", true},
      {"int", " int = 3", true},
      {"int", " = 2 + 3.0", true},
      {"float32", " = 2.0", true},
      {"float64", " = 2.0", true},
      {"int", " = 15 / 4 ", true},
      {"int", " = 15 / 4.0 ", true},
      {"float64", " float64 = 3/2 ", true},
      {"float32", " float64 = 3/2 ", false},
      {"int", " = 1.0 << 3", true},
      {"float32", " = 1.0 << 3", true},
      {"bool", " = true ", true},
      {"bool", " bool = true ", true},
      {"string", " = \"hi\"", true},
      {"complex64", " = 1 - 0.707i ", true},

      {"string", " = 1 + 2", false},
      {"string", " rune = 'a'", false},
    });
  }


  @Override
  void doTest() {
    GoFile file = (GoFile)myFixture.configureByText("a.go", "package main;" + typesAndFuncs + "var x " + left + "; const c " + right);
    myFixture.checkHighlighting();
    GoType left = file.getVars().get(0).getGoType(null);
    assert left != null;
    GoType right = file.getConstants().get(0).getGoType(null);
    String leftText = left.getText();
    String rightText = right == null ? null : right.getText();
    assertTrue(leftText + " should" + (!ok ? " not " : " ") + "assign " + rightText, ok == GoTypeUtil.isAssignable(left, right));
  }

  @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
  public GoConstsAssignableTest(String left, String right, boolean ok) {
    super(left, right, ok);
  }
}
