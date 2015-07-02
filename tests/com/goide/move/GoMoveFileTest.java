/*
 * Copyright 2013-2015 Sergey Ignatov, Alexander Zolotov, Mihai Toader, Florin Patan
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

package com.goide.move;

import com.goide.GoCodeInsightFixtureTestCase;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl;

public class GoMoveFileTest extends GoCodeInsightFixtureTestCase {
  public void testMoveFilePackageNameNotDirectoryName() {
    String testName = getTestName(true);
    myFixture.copyFileToProject(testName + "/from/a.go");
    myFixture.copyFileToProject(testName + "/to/b.go");
    myFixture.moveFile(testName + "/from/a.go", testName + "/to");
    myFixture.checkResultByFile(testName + "/to/a.go", testName + "/to/a_after.go", true);
  }
  public void testMoveFile() throws Throwable {
    /*myFixture.configureByFiles(
      "/from/a.go",
      "/to/b.go",
      "/usage/c.go"
    );*/
    String testName = getTestName(true);
    myFixture.copyFileToProject(testName + "/from/a.go");
    myFixture.copyFileToProject(testName + "/to/b.go");
    myFixture.copyFileToProject(testName + "/usage/c.go");
    myFixture.moveFile(testName + "/from/a.go", testName + "/to", testName + "/to/b.go", testName + "/usage/c.go");
    /*myFixture.testRename("/from/a.go", "/to/a_after.go", "/to/a.go",
                         "/to/b.go", "/usage/c.go");*/
    myFixture.checkResultByFile(testName + "/to/a.go", testName + "/to/a_after.go", true);
    myFixture.checkResultByFile(testName + "/to/b.go", testName + "/to/b_after.go", true);
    myFixture.checkResultByFile(testName + "/usage/c.go", testName + "/usage/c_after.go", true);
  }

  /**
   * Needed? not sure
   * @throws Exception
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();
    ((CodeInsightTestFixtureImpl)myFixture).canChangeDocumentDuringHighlighting(true);
    setUpProjectSdk();
  }

  /**
   * Needed? not sure
   */
  @Override
  protected LightProjectDescriptor getProjectDescriptor() {
    return createMockProjectDescriptor();
  }


  @Override
  protected String getBasePath() {
    return "refactoring/move";
  }
}
