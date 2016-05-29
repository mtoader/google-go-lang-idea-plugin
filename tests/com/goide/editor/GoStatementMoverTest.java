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

package com.goide.editor;

import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

public class GoStatementMoverTest extends LightCodeInsightFixtureTestCase {


  public void testSimpleStatement() {
    doTest(false);
    doTest(true);
  }

  public void testStatementInEndOfBlock() {
    doTest(true);
    doTest(false);
  }

  public void testInsertedStatement() {
    doTest(false);
  }

  public void testInsertedStatementDown() {
    doTest(true);
  }

  public void testFuncDecl() {
    doTest(true);
    doTest(false);
  }

  public void testFuncDeclWithFewTopLvlDecl() {
    doTest(false);
  }

  public void testFuncDeclWithFewTopLvlDeclDown() {
    doTest(true);
  }

  public void testImport() {
    doTest(false);
  }

  public void testImportDown() {
    doTest(true);
  }

  public void testPackage() {
    doTest(true);
  }

  public void testTwoFunc() {
    doTest(true);
    doTest(false);
  }

  public void testTwoStatements() {
    doTest(false);
  }

  public void testTwoStatementsDown() {
    doTest(true);
  }

  private void doTest(boolean down) {
    final String testName = getTestName(false);

    myFixture.configureByFile(getBasePath() + "/" + testName + ".go");
    if (down) {
      myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION);
      myFixture.checkResultByFile(getBasePath() + "/" + testName + "-afterDown.go", true);
    }
    else {
      myFixture.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_UP_ACTION);
      myFixture.checkResultByFile(getBasePath() + "/" + testName + "-afterUp.go", true);
    }
  }

  @NotNull
  @Override
  public String getTestDataPath() {
    return "testData/";
  }

  @NotNull
  @Override
  protected String getBasePath() {
    return "editor/statement-mover";
  }
}
