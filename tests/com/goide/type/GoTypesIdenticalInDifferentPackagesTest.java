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
import com.intellij.psi.PsiFile;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class GoTypesIdenticalInDifferentPackagesTest extends GoTypesIdenticalTestCase {
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
      {"type MyType int; var m MyType;", "type MyType int; var m MyType;", false},
      {"type MyType int; var m MyType;", "var m l.MyType;", true},
      {"var m a.MyType;", "var m a.MyType;", true},
      {"type MyType int; var m a.MyType;", "var m l.MyType;", false},
      {"var i int;", "var j int;", true},
      {"var i int;", "var s string;", false},
      {"var m interface{};", "var m interface{};", true},
      {"var m interface{ MyFunc() };", "var m interface{ MyFunc() };", true},
      {"var m interface{ myFunc() };", "var m interface{ myFunc() };", false},
      {"var m struct{};", "var m struct{};", true},
      {"var m struct{ i int };", "var m struct{ i int };", false},
      {"var m struct{ Int int };", "var m struct{ Int int };", true},
      {"var m struct{ Int int };", "var m struct{ String int };", false},
      {"var m struct{ MyStruct struct { i int } };", "var m struct{ MyStruct struct { j int } };", false},
      {"var m struct{ MyStruct struct { i int } };", "var m struct{ MyStruct struct { i int } };", false},
      {"var m struct{ MyStruct struct { I int } };", "var m struct{ MyStruct struct { I int } };", true},
      {"var m struct{ myStruct struct { i int } };", "var m struct{ myStruct struct { i int } };", false},
      {"type MyType int; var m struct{ MyType };", "type MyType int; var m struct{ MyType };", false},
    });
  }


  @Override
  void doTest() {
    myFixture.addFileToProject("a/a.go", "package a; type MyType string;");
    PsiFile lFile = myFixture.addFileToProject("l/l.go", "package l\n import (\"r\" ; \"a\") \n" + left);
    PsiFile rFile = myFixture.addFileToProject("r/r.go", "package r\n import (\"l\" ; \"a\") \n" + right);
    GoType left = ((GoFile)lFile).getVars().get(0).getGoType(null);
    GoType right = ((GoFile)rFile).getVars().get(0).getGoType(null);
    String leftText = left == null ? null : left.getText();
    String rightText = right == null ? null : right.getText();
    assertTrue(leftText + " should" + (!ok ? " not " : " ") + "equal " + rightText, ok == GoTypeUtil.identical(left, right));
  }

  @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
  public GoTypesIdenticalInDifferentPackagesTest(String left, String right, boolean ok) {super(left, right, ok);}
}
