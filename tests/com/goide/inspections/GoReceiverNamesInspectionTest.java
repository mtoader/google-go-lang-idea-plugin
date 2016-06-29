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

package com.goide.inspections;

import com.goide.quickfix.GoQuickFixTestBase;
import com.goide.quickfix.GoRenameReceiverQuickFix;

public class GoReceiverNamesInspectionTest extends GoQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(GoReceiverNamesInspection.class);
  }

  public void testThis() {
    doTest(false);
  }

  public void testMeRenameAll() {
    doTest(true);
  }

  public void testNoFix() {
    doTestNoFix(false);
  }

  public void testDifferentNames() {
    doTest(false);
  }

  public void testDifferentNamesRenameAll() {
    doTest(true);
  }


  public void doTest(boolean renameAll) {
    super.doTest(getQuickFixName(renameAll), true);
  }

  public void doTestNoFix(boolean renameAll) {
    super.doTestNoFix(getQuickFixName(renameAll), true);
  }

  private static String getQuickFixName(boolean renameAll) {
    return renameAll ? GoRenameReceiverQuickFix.RENAME_ALL_METHODS_QUICKFIX_NAME :
           GoRenameReceiverQuickFix.RENAME_ONE_METHOD_QUICKFIX_NAME;
  }

  @Override
  public String getBasePath() {
    return "inspections/receiver-names";
  }


}
