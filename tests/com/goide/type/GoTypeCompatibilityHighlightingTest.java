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

import com.goide.GoCodeInsightFixtureTestCase;
import com.goide.inspections.GoTypesCompatibilityInspection;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;

public class GoTypeCompatibilityHighlightingTest extends GoCodeInsightFixtureTestCase {
  @Override
  public void setUp() throws Exception {
    super.setUp();
    setUpProjectSdk();
    myFixture.enableInspections(
      GoTypesCompatibilityInspection.class
    );
  }

  private void doTest() {
    myFixture.testHighlighting(true, false, true, getTestName(true) + ".go");
  }

  @NotNull
  @Override
  protected String getBasePath() {
    return "typeHighlighting";
  }

  public void testSimple()                   { doTest(); }
  public void testVariadic()                 { doTest(); }
  public void testNilAndVoidInterface()      { doTest(); }
  public void testConstants()                { doTest(); }
  public void testLiterals()                 { doTest(); }
  public void testCompositeLit()             { doTest(); }
  public void testRange()                    { doTest(); }
  public void testInterfaces()               { doTest(); }
  public void testMethods()                  { doTest(); }
  public void testUnsafe()                   { doTest(); }
  public void testCType()                    { doTest(); }
  public void testUnresolved()               { doTest(); }
  public void testBuiltin()                  { doTest(); }

  public void testMethodsWithSelector() {
    myFixture.addFileToProject("a/a.go", "package a; type T []int; const C = T(3); func (a.T) F (int){};");
    PsiFile file =  myFixture.addFileToProject("b/b.go", "package b; import \"a\";   func foo(func(int)){}; func main(){ foo(a.C.F)}");
    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
  }


  @Override
  protected LightProjectDescriptor getProjectDescriptor() {
    return createMockProjectDescriptor();
  }
}
