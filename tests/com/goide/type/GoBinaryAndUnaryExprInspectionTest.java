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
import com.goide.inspections.GoBinaryAndUnaryExprInspection;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;

public class GoBinaryAndUnaryExprInspectionTest extends GoCodeInsightFixtureTestCase {
  @Override
  public void setUp() throws Exception {
    super.setUp();
    setUpProjectSdk();
    myFixture.enableInspections(GoBinaryAndUnaryExprInspection.class);
  }

  @Override
  protected LightProjectDescriptor getProjectDescriptor() {
    return createMockProjectDescriptor();
  }

  private void doTest() {
    myFixture.testHighlighting(true, false, true, getTestName(true) + ".go");
  }

  @NotNull
  @Override
  protected String getBasePath() {
    return "typeHighlighting/binaryAndUnaryExpr";
  }

  public void testBinary() {
    doTest();
  }

  public void testUnary() {
    doTest();
  }
}
