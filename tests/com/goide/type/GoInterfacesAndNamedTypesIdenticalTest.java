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
public class GoInterfacesAndNamedTypesIdenticalTest extends GoTypesIdenticalTestCase {
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
      {"", "interface{}", "interface{}", true},
      {"", "interface{ a(int) }", "interface{ a(int) }", true},
      {"", "interface{b(); a()}", "interface {a(); b()}", true},
      {"", "interface{a() string}", "interface {a() string}", true},
      {"", "interface{ a(int) }", "interface{ b(int) }", false},
      {"", "interface{ a(int) }", "interface{}", false},
      {"", "interface{a() (string, int)}", "interface {a() (i int, s string)}", false},
      {"", "interface{a() (string, int)}", "interface {a() (s string, i int)}", true},
      {"type T interface{ f1() }", "interface{ T }", "interface { T }", true},
      {"type T interface{ f1() }", "interface{ T }", "interface { f1() }", true},
      {"type T interface{ f1() }", "interface{ T }", "interface { f1(int) }", false},
      {"type T interface{ f1(i int) }", "interface{ T }", "interface { f1(int) }", true},
      {"type (T interface{ f1(i int) }; T2 interface{ T ; f2() int})", "interface{ T2 }", "interface { T; f2() int }", true},
      {"type (T interface{ f1(i int) }; T2 interface{ T ; f2() int})", "interface{ T }", "interface { T; f2() int }", false},
      {"type (T interface{ f1(i int) }; T2 interface{ T ; f2() int})", "interface{ T2 }", "interface { f1(int); f2() int }", true},
      {"type (T interface{ f1(i int) }; T2 interface{ f2() int})", "interface{ T; T2 }", "interface { T2; T }", true},
      {"type (T interface{ f1(i int) }; T2 interface{ f2() int})", "interface{ T; T2 }", "interface { T2; f1(int) }", true},
      {"type (T interface{ f1(i int) }; T2 interface{ f2() int}; T3 interface {})", "interface{ T3 }", "interface {}", true},
      {"type (T interface{ f1(i int) }; T2 interface{ f2() int}; T3 interface {})", "interface{ T3; T }", "interface {T}", true},
      {"type (T interface{ f1(i int) }; T2 interface{ f2() int}; T3 interface { a() })", "interface{ T3; T }", "interface {T}", false},
      {"type (T interface{ f1(i int) }; T2 interface{ f2() int}; T3 interface { T2; T })", "interface{ T3 }", "interface {T2; T}", true},
      {"type (T interface{ f1(i int) }; T2 interface{ f2() int}; T3 interface { T2; T })", "interface{ T3 }", "interface {T2}", false},
      {"type (T interface{ f1(i int) }; T2 interface{ f2() int}; T3 interface { T2; T })", "interface{ T }", "interface {T2}", false},
      {"type (T interface{ f1(i int) }; T2 interface{ f1(int)})", "interface{ T }", "interface {T2}", true},

      {"type MyType int", "MyType", "MyType", true},
      {"type MyType int", "MyType", "int", false},
      {"type (MyType int; myType int)", "MyType", "myType", false},
      {"type (MyType int; myType int)", "MyType", "myType", false},
      {"type (MyType struct{}; myType int)", "MyType", "struct{}", false},
    });
  }


  @Override
  void doTest() {
    myFixture.configureByText("a.go", "package main;"  + typesAndFuncs +"; var x " + left + "; var y " + right);
    List<GoVarDefinition> vars = ((GoFile)myFixture.getFile()).getVars();
    GoType left = vars.get(0).getGoType(null);
    GoType right = vars.get(1).getGoType(null);
    String leftText = left == null ? null : left.getText();
    String rightText = right == null ? null : right.getText();
    assertTrue(leftText + " should" + (!ok ? " not " : " ") + "equal " + rightText, ok == GoTypeUtil.identical(left, right));
  }

  @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
  public GoInterfacesAndNamedTypesIdenticalTest(String typesAndFuncs, String left, String right, boolean ok) {
    super(typesAndFuncs, left, right, ok);
  }
}
