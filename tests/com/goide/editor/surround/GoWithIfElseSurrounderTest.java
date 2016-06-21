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

package com.goide.editor.surround;

public class GoWithIfElseSurrounderTest extends GoSurrounderTestBase {
  private static final String SURROUNDER_DESCRIPTION = new GoWithIfElseSurrounder().getTemplateDescription();

  public void testWithOneStatement() {
    doTest("<selection>var b bool</selection>", "if <caret>{\n\tvar b bool\n} else {\n\n}\n", SURROUNDER_DESCRIPTION, true);
  }

  public void testWithThreeStatements() {
    doTest("<selection>var b int = 1\nb = true\ntype Type int</selection>",
           "if <caret>{\n\tvar b int = 1\n\tb = true\n\ttype Type int\n} else {\n\n}\n", SURROUNDER_DESCRIPTION, true);
  }

  public void testNoIf() {
    doTest("var b, c bool = true, <selection>false</selection>", "", SURROUNDER_DESCRIPTION, false);
  }
}
