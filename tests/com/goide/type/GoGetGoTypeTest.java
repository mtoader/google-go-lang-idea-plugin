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
import com.goide.psi.GoVarDefinition;
import com.goide.psi.impl.GoTypeUtil;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class GoGetGoTypeTest extends GoTypesIdenticalTestCase {
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
      {"","1","int"},
      {"","'s'","rune"},
      {"","\"s\"","string"},
      {"","2.0","float64"},
      {"const c = complex(1,1)","real(c)","float64"},
      {"const c = complex(1,1)","imag(c)","float64"},
      //{"","2.0 + 6.0i","complex64"},
      {"","complex(1,2)","complex128"},
      {"var arr = [...]int{1,2,3}", " arr[0:1]", "[]int"},
      {"var arr[]int", " arr[0:1]", "[]int"},
      {"var arr[]int", " arr", "[]int"},
      {"type T int; func(t T) f(i int, s string){}; var t T", " T.f", "func(T, int, string)"},
      {"type T int; func(t T) f(i int, s string){}; var t T", " t.f", "func(int, string)"},
      {"type T int; func(t T) f() T {return nil}; var t T", "t.f()", "T"},

      {"import \"C\"",  " C.gid_t(ug)", "interface{}"},
      {"import \"C\"",  "C.CString(u.Username)", "interface{}"},

    });
  }

  @Override
  void doTest() {
    myFixture.configureByText("a.go", "package main;" + typesAndFuncs + "\n var x = " + left + "\n var y " + right);
    myFixture.checkHighlighting();
    List<GoVarDefinition> vars = ((GoFile)myFixture.getFile()).getVars();
    int varSize = vars.size();
    GoType left = vars.get(varSize - 2).getGoType(null);
    GoType right = vars.get(varSize - 1).getGoType(null);
    String leftText = left == null ? null : left.getText();
    String rightText = right == null ? null : right.getText();
    assertTrue(leftText + " should" + (!ok ? " not " : " ") + "equal " + rightText, ok == GoTypeUtil.identical(left, right));
  }

  @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
  public GoGetGoTypeTest(String consts, String left, String right) {
    super(consts, left, right, true);
  }
}
