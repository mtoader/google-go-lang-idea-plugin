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

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class GoTypesIdenticalTest extends GoTypesIdenticalTestCase {
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
      {"[]string", "[]string"},
      {"map[int]string", "map[int]string"},
      {"int", "int"},
      {"string", "string"},
      {"*string", "*string"},
      {"struct{}", "struct{}"},
      {"struct{a int}", "struct{a int}"},
      {"struct{a int; b int}", "struct{a, b int}"},
      {"struct{a string; string}", "struct{a string; string}"},
      {"struct{a string; s struct {} }", "struct{a string; s struct {} }"},
      {"struct{a string; s struct {a int} }", "struct{a string; s struct {a int} }"},
      {"chan int", "chan int"},
      {"chan<- int", "chan<- int"},
      {"<-chan int", "<-chan int"},
      {"chan<- float64", "chan<- float64"},
      {"<-chan <-chan int", "<-chan <-chan int"},
      {"func()", "func()"},
      {"func(int)", "func(int)"},
      {"func(i, j, k int)", "func(int, int, int)"},
      {"func(f func() int)", "func(func() int)"},
      {"func(i string)", "func(j string)"},
      {"func(i, j string)", "func(j string, i string)"},
      {"func(i, j string, r ... string)", "func(j string, i string, ... string)"},
      {"func() (int)", "func() int"},
      {"func() (int, string)", "func() (int, string)"},
      {"func() (i int, s string)", "func() (int, string)"},
      {"interface{}", "interface{}"},
      {"interface{ a(int) }", "interface{ a(int) }"},
      {"interface{b(); a()}", "interface {a(); b()}"},
      {"interface{a() string}", "interface {a() string}"},
      {"interface{a() (string, int)}", "interface {a() (s string, i int)}"},
      {"[]int", "[]int"},
      {" = [...]int{1, 2, 3};", " = [...]int{3, 4, 5};"},
      {"[4]int;", "[4]int;"},
      //{" = [...]int{3, 4, 6};", "[3]int;"},
    });
  }

  @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
  public GoTypesIdenticalTest(String left, String right) {
    super(left, right, true);
  }
}
