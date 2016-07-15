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
      {"", "1", "int"},
      {"", "'s'", "rune"},
      {"", "\"s\"", "string"},
      {"", "2.0", "float64"},

      {"", "uint(2.0)", "uint"},
      {"type T func(int)", "T(1)", "T"},
      {"type (T int32; T2 T; T3 T2)", "T3(3)", "T3"},

      {"type T struct{}" , "&T{}", "*T"},

      {"", "2 + 2.0 + 2", "float64"},
      {"", "2 / 23", "int"},
      {"", "2 / 23.", "float64"},
      {"", "1.0 << 3", "int"},
      {"", "1 << 3.0", "int"},

      {"", "uint(2) << uint64(3)", "uint"},
      {"", "uint64(3) >> uint(2)", "uint64"},

      {"const c = complex(1,1)", "real(c)", "float64"},
      {"const c = complex(1,1)", "imag(c)", "float64"},
      {"const c = complex(1,1)", "1 + (imag(c) + 3i) * 2", "complex128"},
      {"", "2.0 + 6.0i", "complex128"},
      {"", "complex(1,2)", "complex128"},
      {"const c = 2.0", "c * complex(1,2)", "complex128"},

      {"", "true", "bool"},
      {"", "false", "bool"},
      {"", "true || false", "bool"},
      {"", "true && false", "bool"},
      {"", "1 && 3", "bool"},
      {"", "1 > 2", "bool"},
      {"", "2 == 2", "bool"},
      {"", "true == false", "bool"},
      {"", "3 != 2", "bool"},

      {"", "'w' + 1 ", "rune"},
      {"", "'w' * 23 ", "rune"},
      {"", "'w' >> uint8(23) ", "rune"},

      {"", "3.0 >> (4.0 + 0i) ", "int"},
      {"", "3.0 >> uint(4.0 + 0i) ", "int"},
      {"", "3.0 | 5.5", "int"},
      {"", "3.0 ^ 5.5", "int"},
      {"", "-3.0 ^ 5.5", "int"},

      {"var c int64", "(c | 123)  + 12", "int64"},
      {"type T int64; var c T", "c + 2.0", "T"},
      {"type T int64; var c T", "c * 2.0", "T"},
      {"const c1 int64 = 1", "(c1 | 4) + 123", "int64"},
      {"const c1 = 1; const c2 = c1 + 2", "c2 + uint(0)", "uint"},
      {"const c1 = 1; const c2 = c1 + 2", "(c2 * 2.0) << uint(0)", "int"},

      {"var arr = [...]int{1,2,3}", " arr[0:1]", "[]int"},
      {"var arr[]int", " arr[0:1]", "[]int"},
      {"var arr[]int", " arr", "[]int"},

      {"type T int; func(t T) f(i int, s string){}; var t T", " T.f", "func(T, int, string)"},
      {"type T int; func(t T) f(i int, s string){}; var t T", " t.f", "func(int, string)"},
      {"type T int; func(t T) f() T {return nil}; var t T", "t.f()", "T"},

      {"import \"C\"",  " C.gid_t(ug)", "interface{}"},
      {"import \"C\"",  "C.CString(u.Username)", "interface{}"},

      {"", "T(3)", "null"},
      {"var t T = 3", "3 + t", "null"},

      {"var m map[int]string; var a,b = m[3]", "b", "bool"},
      {"type T int; var a interface{}; var b, c= a.(T)", "c", "bool"},
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
    assertTrue(leftText + " should" + (!ok ? " not " : " ") + "equal " + rightText,
               left == null && "null".equals(rightText) || ok == GoTypeUtil.identical(left, right));
  }

  @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
  public GoGetGoTypeTest(String consts, String left, String right) {
    super(consts, left, right, true);
  }
}
