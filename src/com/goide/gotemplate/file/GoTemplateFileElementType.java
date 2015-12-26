/*
 * Copyright 2013-2015 Sergey Ignatov, Alexander Zolotov, Florin Patan, José Santos
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

package com.goide.gotemplate.file;

import com.goide.gotemplate.GoTemplateLanguage;
import com.goide.gotemplate.stub.GoTemplateFileStub;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.IStubFileElementType;

public class GoTemplateFileElementType extends IStubFileElementType<GoTemplateFileStub> {
  public static IFileElementType INSTANCE = new GoTemplateFileElementType();

  public GoTemplateFileElementType() {
    super(GoTemplateLanguage.INSTANCE);
  }
}
