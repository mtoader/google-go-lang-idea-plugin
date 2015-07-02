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

package com.goide;

import com.goide.psi.GoPackageClause;
import com.goide.psi.impl.GoElementFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Helps with Go directory related functions
 */
public class GoDirectoryService {
  public static String getPackageName(@NotNull final PsiDirectory dir) {
    return ContainerUtil.getLastItem(StringUtil.split(dir.getName(), "-"));
  }
  public static GoPackageClause getPackage(@NotNull final PsiDirectory dir) {
    return GoElementFactory.createPackageClause(dir.getProject(), getPackageName(dir));
  }
}
