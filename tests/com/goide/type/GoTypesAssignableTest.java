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
public class GoTypesAssignableTest extends GoTypesIdenticalTestCase {
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
      {"", "int", "int", true},
      {"type MyType int", "MyType", "MyType", true},
      {"type MyType int", "MyType", "int", true},

      {"type (T1 int; T2 T1; T3 T1);", "T2", "T3", false},
      {"type (T1 int; T2 T1; T3 T1);", "T1", "int", true},
      {"type (T1 int; T2 T1; T3 T1);", "int", "T2", true},
      {"type (T1 int; T2 T1; T3 T1);", "T3", "int", true},
      {"type (T1 int; T2 T1; T3 T1);", "T2", "T1", false},
      {"type (T1 int; T2 T1; T3 T1);", "T2", "T2", true},

      {"", "interface{}", "int", true},
      {"type (T1 int; T2 T1);", "interface{}", "T2", true},
      {"type (I interface { fun()}; MyType int); func (m MyType) fun(){};", "I", "MyType", true},
      {"type (I interface { fun()}; MyType int); func (m MyType) fun(){};", "I", "int", false},
      {"type (I interface { fun()}; T1 int; T2 T1); func (T1) fun(){};", "I", "T2", false},
      {"type (I2 interface { fun(); I}; I interface { f2()}; MyType int); func (m MyType) fun(){}; func (m MyType) f2(){};",
        "I2", "MyType", true},
      {"type (I2 interface { fun(); I}; I interface { f2()}; MyType int); func (m MyType) fun(){};",
        "I2", "MyType", false},

      {"type (T1 int; T2 T1);", "interface{}", "T2", true},
      {"type (I interface { fun()}; MyType int); func (m *MyType) fun(){};", "I", "*MyType", true},
      {"type (I interface { fun()}; MyType int); func (m *MyType) fun(){};", "I", "int", false},
      {"type (I interface { fun()}; T1 int; T2 T1); func (T1) fun(){};", "I", "T2", false},
      {"type (I2 interface { fun(); I}; I interface { f2()}; MyType int); func (m *MyType) fun(){}; func (m *MyType) f2(){};",
        "I2", "*MyType", true},
      {"type (I2 interface { fun(); I}; I interface { f2()}; MyType int); func (m *MyType) fun(){};",
        "I2", "*MyType", false},

      {"type (I interface { f1() }; I2 interface{ f1() })", "I", "I2", true},
      {"type (I interface { f1() }; I2 interface{})", "I2", "I", true},
      {"type (I interface { I2 }; I2 interface{ f1() })", "I2", "I", true},
      {"type (I interface { I2 }; I2 interface{ f1() })", "I", "I2", true},
      {"type (I interface { I2; f() }; I2 interface{ f1() })", "I2", "I", true},
      {"type (I interface { I2 }; I2 interface{ f1() }; I3 interface{ I2; f()})", "I2", "I3", true},
      {"type (I interface { I2 }; I2 interface{ f1() }; I3 interface{ I2; f()})", "I3", "I2", false},

      {"type (C1 chan int);", "C1", "chan int", true},
      {"type (C1 chan<- int);", "C1", "chan int", true},
      {"type (C1 chan<- int);", "chan int", "C1", false},
      {"type (C1 <-chan int);", "C1", "chan int", true},
      {"type (C1 <-chan int);", "chan int", "C1", false},
      {"", "chan<- int", "<-chan int", false},
      {"type (C1 chan string);", "chan int", "C1", false},
      {"type (C1 <-chan string);", "chan int", "C1", false},
      {"", "chan int", "chan chan int", false},
      {"", "chan int", "chan int", true},
      {"type (C1 chan int; C2 chan int);", "C2", "C1", false},
      {"type (C1 chan int32);", "chan int", "C1", false},
    });
  }

  @Override
  void doTest() {
    myFixture.configureByText("a.go", "package main\n" + typesAndFuncs + "\n var x " + left + "\n var y " + right);
    myFixture.checkHighlighting();
    List<GoVarDefinition> vars = ((GoFile)myFixture.getFile()).getVars();
    GoType left = vars.get(0).getGoType(null);
    GoType right = vars.get(1).getGoType(null);
    assertTrue(left != null);
    String leftText = left.getText();
    String rightText = right == null ? null : right.getText();
    assertTrue(leftText + " should" + (!ok ? " not " : " ") + "assignable " + rightText, ok == GoTypeUtil.isAssignable(left, right));
  }

  @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
  public GoTypesAssignableTest(String typesAndFuncs, String left, String right, boolean ok) {
    super(typesAndFuncs, left, right, ok);
  }
}
