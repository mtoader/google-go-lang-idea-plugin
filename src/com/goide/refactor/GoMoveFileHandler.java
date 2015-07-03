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

package com.goide.refactor;

import com.goide.GoDirectoryService;
import com.goide.psi.GoFile;
import com.goide.psi.GoFunctionDeclaration;
import com.goide.psi.GoPackageClause;
import com.goide.psi.impl.GoElementFactory;
import com.goide.psi.impl.GoPackageClauseImpl;
import com.goide.util.GoUtil;
import com.intellij.codeInsight.ChangeContextUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.file.PsiDirectoryImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFileHandler;
import com.intellij.refactoring.util.MoveRenameUsageInfo;
import com.intellij.refactoring.util.TextOccurrencesUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.tools.shell.Global;

import java.io.File;
import java.util.*;

public class GoMoveFileHandler extends MoveFileHandler {
  private static final Logger LOG = Logger.getInstance(
    "#com.goide.refactor.GoMoveFileHandler");

  @Override
  public boolean canProcessElement(PsiFile file) {
    return file instanceof GoFile;
  }

  @Override
  public void prepareMovedFile(PsiFile file, PsiDirectory directory, Map<PsiElement, PsiElement> map) {
  }

  public static UsageInfo[] findUsages(final PsiElement element,
                                       boolean searchInStringsAndComments,
                                       boolean searchInNonGoFiles,
                                       final String newQName) {
    PsiManager manager = element.getManager();
    ArrayList<UsageInfo> results = new ArrayList<UsageInfo>();
    Set<PsiReference> foundReferences = new HashSet<PsiReference>();
    GlobalSearchScope projectsScope = GlobalSearchScope.projectScope(manager.getProject());
    for(PsiReference reference : ReferencesSearch.search(element, projectsScope, false)) {
      if(foundReferences.contains(reference)) {
        continue;
      }
      TextRange range = reference.getRangeInElement();
      results.add(new MoveRenameUsageInfo(reference.getElement(), reference, range.getStartOffset(), range.getEndOffset(), element, false));
      foundReferences.add(reference);
    }
    findNonCodeUsages(searchInStringsAndComments, searchInNonGoFiles, element, newQName, results);
    return results.toArray(new UsageInfo[results.size()]);
  }

  public static void findNonCodeUsages(boolean searchInStringsAndComments,
                                       boolean searchInNonGoFiles,
                                       final PsiElement element,
                                       final String newQName,
                                       ArrayList<UsageInfo> results) {
    final String stringToSearch = getStringToSearch(element);
    if (stringToSearch == null) return;
    TextOccurrencesUtil.findNonCodeUsages(element, stringToSearch, searchInStringsAndComments, searchInNonGoFiles, newQName, results);
  }

  private static String getStringToSearch(PsiElement element) {
    if (element instanceof PsiPackage) {
      return ((PsiPackage)element).getQualifiedName();
    }
    else if (element instanceof PsiClass) {
      return ((PsiClass)element).getQualifiedName();
    }
    else if (element instanceof PsiDirectory) {
      return getStringToSearch(GoDirectoryService.getPackage((PsiDirectory)element));
    }
    else {
      LOG.error("Unknown element type");
      return null;
    }
  }


  @Nullable
  @Override
  public List<UsageInfo> findUsages(PsiFile file, PsiDirectory newParent, boolean searchInComments, boolean searchInNonGoFiles) {
    final List<UsageInfo> result = new ArrayList<UsageInfo>();
    final GoPackageClause newParentPackage = GoDirectoryService.getPackage(newParent);
    final String qualifiedName = newParentPackage.getText();
    for (GoFunctionDeclaration func : ((GoFile)file).getFunctions()) {
      Collections.addAll(result, findUsages(func, searchInComments, searchInNonGoFiles,
                                            StringUtil.getQualifiedName(qualifiedName, func.getName())));
    }
    return result.isEmpty() ? null : result;
  }

  @Override
  public void retargetUsages(List<UsageInfo> list, Map<PsiElement, PsiElement> map) {

  }

  @Override
  public void updateMovedFile(PsiFile file) throws IncorrectOperationException {
    GoFile goFile = (GoFile)file;
    String packageName = goFile.getPackageName();
    final PsiDirectory containingDirectory = file.getContainingDirectory();
    if (containingDirectory != null) {
      Collection<String> packages = GoUtil.getAllPackagesInDirectory(containingDirectory);
      GoPackageClause newPackage = null;
      if(packages.size() > 0) {
        for(String pName : packages) {
          // simply take first package non equal package in the containing directory because we're not sure
          // that the package name equals the directory name.
          if(packageName != pName) {
            newPackage = GoElementFactory.createPackageClause(file.getProject(), pName);
            break;
          }
        }
      }
      if(newPackage == null) {
        newPackage = GoDirectoryService.getPackage(containingDirectory);
      }
      GoPackageClause p = goFile.getPackage();
      if (p != null) {
        p.replace(newPackage);
      }
    }
  }
}
