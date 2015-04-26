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

package com.goide.generate;

import com.goide.psi.GoInterfaceType;
import com.goide.psi.GoMethodSpec;
import com.goide.psi.GoStructType;
import com.goide.psi.GoTypeReferenceExpression;
import com.goide.stubs.GoTypeStub;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class GenerateInterfaceImplementationDialog extends DialogWrapper {
  private CollectionListModel<GoInterfaceType> mySelectedInterfaces;
  private final JBPanel myPanel;
  private final JCheckBox myPointerReference;

  protected GenerateInterfaceImplementationDialog(GoStructType struct) {
    super(struct.getProject());
    setTitle("Select the Interfaces You Want to Implement");

    myPanel = new JBPanel();
    myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.Y_AXIS));

    JBTextField searchBox = new JBTextField();
    searchBox.addActionListener(getSearchListener());
    myPanel.add(LabeledComponent.create(searchBox, "Interface name"));

    // TODO better interface here
    // We should have a search box and when a user types something in
    // completion should kick in and present the user a list of interfaces that match
    // After the desired interface is found, it should be added to the list
    mySelectedInterfaces = new CollectionListModel<GoInterfaceType>(new GoInterfaceType() {
      @NotNull
      @Override
      public List<GoMethodSpec> getMethodSpecList() {
        return null;
      }

      @Nullable
      @Override
      public PsiElement getLbrace() {
        return null;
      }

      @Nullable
      @Override
      public PsiElement getRbrace() {
        return null;
      }

      @NotNull
      @Override
      public PsiElement getInterface() {
        return null;
      }

      @NotNull
      @Override
      public List<GoMethodSpec> getMethods() {
        return null;
      }

      @NotNull
      @Override
      public List<GoTypeReferenceExpression> getBaseTypesReferences() {
        return null;
      }

      @Nullable
      @Override
      public GoTypeReferenceExpression getTypeReferenceExpression() {
        return null;
      }

      @Override
      public boolean shouldGoDeeper() {
        return false;
      }

      @Override
      public IStubElementType getElementType() {
        return null;
      }

      @Override
      public GoTypeStub getStub() {
        return null;
      }

      @NotNull
      @Override
      public Project getProject() throws PsiInvalidElementAccessException {
        return null;
      }

      @NotNull
      @Override
      public Language getLanguage() {
        return null;
      }

      @Override
      public PsiManager getManager() {
        return null;
      }

      @NotNull
      @Override
      public PsiElement[] getChildren() {
        return new PsiElement[0];
      }

      @Override
      public PsiElement getParent() {
        return null;
      }

      @Override
      public PsiElement getFirstChild() {
        return null;
      }

      @Override
      public PsiElement getLastChild() {
        return null;
      }

      @Override
      public PsiElement getNextSibling() {
        return null;
      }

      @Override
      public PsiElement getPrevSibling() {
        return null;
      }

      @Override
      public PsiFile getContainingFile() throws PsiInvalidElementAccessException {
        return null;
      }

      @Override
      public TextRange getTextRange() {
        return null;
      }

      @Override
      public int getStartOffsetInParent() {
        return 0;
      }

      @Override
      public int getTextLength() {
        return 0;
      }

      @Nullable
      @Override
      public PsiElement findElementAt(int offset) {
        return null;
      }

      @Nullable
      @Override
      public PsiReference findReferenceAt(int offset) {
        return null;
      }

      @Override
      public int getTextOffset() {
        return 0;
      }

      @Override
      public String getText() {
        return null;
      }

      @NotNull
      @Override
      public char[] textToCharArray() {
        return new char[0];
      }

      @Override
      public PsiElement getNavigationElement() {
        return null;
      }

      @Override
      public PsiElement getOriginalElement() {
        return null;
      }

      @Override
      public boolean textMatches(CharSequence text) {
        return false;
      }

      @Override
      public boolean textMatches(PsiElement element) {
        return false;
      }

      @Override
      public boolean textContains(char c) {
        return false;
      }

      @Override
      public void accept(PsiElementVisitor visitor) {

      }

      @Override
      public void acceptChildren(PsiElementVisitor visitor) {

      }

      @Override
      public PsiElement copy() {
        return null;
      }

      @Override
      public PsiElement add(PsiElement element) throws IncorrectOperationException {
        return null;
      }

      @Override
      public PsiElement addBefore(PsiElement element, @Nullable PsiElement anchor) throws IncorrectOperationException {
        return null;
      }

      @Override
      public PsiElement addAfter(PsiElement element, @Nullable PsiElement anchor) throws IncorrectOperationException {
        return null;
      }

      @Override
      public void checkAdd(PsiElement element) throws IncorrectOperationException {

      }

      @Override
      public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        return null;
      }

      @Override
      public PsiElement addRangeBefore(PsiElement first, PsiElement last, PsiElement anchor) throws IncorrectOperationException {
        return null;
      }

      @Override
      public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor) throws IncorrectOperationException {
        return null;
      }

      @Override
      public void delete() throws IncorrectOperationException {

      }

      @Override
      public void checkDelete() throws IncorrectOperationException {

      }

      @Override
      public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {

      }

      @Override
      public PsiElement replace(PsiElement newElement) throws IncorrectOperationException {
        return null;
      }

      @Override
      public boolean isValid() {
        return false;
      }

      @Override
      public boolean isWritable() {
        return false;
      }

      @Nullable
      @Override
      public PsiReference getReference() {
        return null;
      }

      @NotNull
      @Override
      public PsiReference[] getReferences() {
        return new PsiReference[0];
      }

      @Nullable
      @Override
      public <T> T getCopyableUserData(Key<T> key) {
        return null;
      }

      @Override
      public <T> void putCopyableUserData(Key<T> key, @Nullable T value) {

      }

      @Override
      public boolean processDeclarations(PsiScopeProcessor processor,
                                         ResolveState state,
                                         @Nullable PsiElement lastParent,
                                         PsiElement place) {
        return false;
      }

      @Nullable
      @Override
      public PsiElement getContext() {
        return null;
      }

      @Override
      public boolean isPhysical() {
        return false;
      }

      @NotNull
      @Override
      public GlobalSearchScope getResolveScope() {
        return null;
      }

      @NotNull
      @Override
      public SearchScope getUseScope() {
        return null;
      }

      @Override
      public ASTNode getNode() {
        return null;
      }

      @Override
      public boolean isEquivalentTo(PsiElement another) {
        return false;
      }

      @Override
      public Icon getIcon(int flags) {
        return null;
      }

      @Nullable
      @Override
      public <T> T getUserData(Key<T> key) {
        return null;
      }

      @Override
      public <T> void putUserData(Key<T> key, @Nullable T value) {

      }
    });
    JBList myList = new JBList(mySelectedInterfaces);
    myList.setCellRenderer(new DefaultListCellRenderer());
    ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myList);
    decorator.disableAddAction();
    myPanel.add(LabeledComponent.create(decorator.createPanel(), "Interfaces to implement"));

    myPointerReference = new JCheckBox();
    myPointerReference.setText("With pointer reference");
    myPanel.add(myPointerReference);

    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myPanel;
  }

  public List<GoInterfaceType> getSelectedInterfaces() {
    return mySelectedInterfaces.getItems();
  }

  public boolean withPointerReference() {
    return myPointerReference.isSelected();
  }

  private static ActionListener getSearchListener() {
    return new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // TODO here we should perform the search for interfaces with auto-completion
      }
    };
  }
}
