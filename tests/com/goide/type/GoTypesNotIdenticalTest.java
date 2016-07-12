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
public class GoTypesNotIdenticalTest extends GoTypesIdenticalTestCase {
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
      {"int", "string"},
      {"int32", "byte"},
      {"int8", "rune"},
      {"uint8", "rune"},
      {"int8", "byte"},

      {"int", "*int"},
      {"**int", "***int"},
      {"*[]string", "*string"},
      {"*string", "*int"},

      {"string", "[]string"},
      {"string", "map[string]string"},
      {"string", "[3]string"},
      {"string", "= [...]string{}"},
      {"string", "chan string"},

      {"map[int]string", "map[int]float32"},
      {"map[string]float32", "map[int]float32"},
      {"map[*int]string", "map[int]string"},
      {"map[*int]struct{}", "map[int]struct{}"},
      {"map[*int]struct{}", "map[*int]struct{int}"},
      {"map[int]string", "map[int]*string"},

      {"struct{}", "struct{a int}"},
      //{"struct{ *int }", "struct{ int }"},
      {"struct{ a *int }", "struct{ a int }"},
      {"struct{_ int}", "struct{a int}"},
      {"struct{_ int}", "struct{ int }"},
      {"struct{a int}", "struct{b int}"},
      {"struct{a int \"tag\"}", "struct{a int}"},
      {"struct{a int \"tag\"}", "struct{a int \"t2\"}"},
      {"struct{a, b int \"tag\"}", "struct{a int \"tag\"; int b}"},
      {"struct{a, b int \"tag\"}", "struct{a int; int b \"tag\"}"},
      {"struct{a int \"\\\"\"}", "struct{a int `\\\"`}"},
      {"struct{int \"\\U000065e5\\U0000672c\\U00008a9e\"}", "struct { int `U000065e5\\U0000672c\\U00008a9e`}"},
      {"struct{a int}", "struct{a string; b int}"},
      {"struct{a int; int}", "struct{a int; int string}"},

      {"chan int", "chan string"},
      {"chan<- int", "chan int"},
      {"chan int", "<-chan int"},
      {"chan<- int", "<-chan int"},
      {"chan<- string", "chan<- float64"},
      {"<-chan <-chan int", "<-chan <-chan string"},
      {"chan<- <-chan int", "<-chan chan int"},
      {"chan chan chan int", "chan chan chan chan int"},

      {"func(int)", "func(string)"},
      {"func(int)", "func(i string)"},
      {"func(i int)", "func(i string)"},
      {"func(... int)", "func(int)"},
      {"func(i ... int)", "func()"},
      {"func(... int)", "func()"},
      {"func(i ... int)", "func(int, int)"},
      {"func(... int)", "func(k ...string)"},
      {"func(... int)", "func( ...string)"},
      {"func(j ... int)", "func(i int, j ...int)"},
      {"func(i, j int)", "func(i int)"},
      {"func(f func() int)", "func(func() string)"},
      {"func(f func(j, i int) int)", "func(func(int) int)"},
      {"func() int", "func() string"},
      {"func() int", "func() []int"},
      {"func() int", "func() *int"},
      {"func() int", "func() (*int)"},
      {"func() (int)", "func() (string)"},
      {"func() (string, int)", "func() (int, string)"},
      {"func() (i, s string)", "func() (i int, s string)"},
      {"func() (int, int)", "func() (int)"},
      {"func() (int, int, int)", "func() (int, int)"},
      {"func() (i int)", "func() (i string)"},
      {"func() (s string, i int)", "func() (s int, i string)"},
      {"func() (i int, j int)", "func() (i int)"},
      {"func() (i int, j int, k int)", "func() (i int, j int)"},

      {"[]int", "[]string"},
      {"[4]int;", "[]int;"},
      //{"[][3]int", "[][2]int"},
      //{"[4]int", "[5]string"},
      {"[][3]int", "[3][]int"},
      {"[4][4]int", "[4][]int"},
      {"[][4]int", "[4][]int"},
      {"[4][4]*int", "[4][4]int"},
      {"[4][4][4]int", "[4][4]int"},
      {"[4]struct{ i int }", "[]struct{ i int }"},
      {"[4]struct{ i int }", "[4]struct{ j int }"},
      {" = [...]int{1, 2, 3}", "[]int"},
      //{" = [...]int{1, 2, 3}", " = [...]int{3, 4}"},
      //{" = [...]int{3, 4, 6}", "[2]int"},
      {" = [...]int{}", "[]int"},
    });
  }

  @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
  public GoTypesNotIdenticalTest(String left, String right) {
    super(left, right, false);
  }
}
