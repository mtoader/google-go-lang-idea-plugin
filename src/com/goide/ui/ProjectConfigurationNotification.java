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

package com.goide.ui;

import com.goide.GoFileType;
import com.goide.GoLanguage;
import com.goide.settings.GoPluginSettings;
import com.intellij.ProjectTopics;
import com.intellij.notification.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootAdapter;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class ProjectConfigurationNotification extends EditorNotifications.Provider<EditorNotificationPanel> {
  private static final Key<EditorNotificationPanel> KEY = Key.create("Project config title");
  private final GoPluginSettings mySettings;
  private final Project myProject;

  public ProjectConfigurationNotification(Project project, @NotNull final EditorNotifications notifications) {
    mySettings = GoPluginSettings.getInstance();
    myProject = project;

    myProject.getMessageBus().connect(project).subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootAdapter() {
      @Override
      public void rootsChanged(ModuleRootEvent event) {
        notifications.updateAllNotifications();
      }
    });
  }

  @NotNull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }

  @Nullable
  @Override
  public EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull FileEditor fileEditor) {
    if (mySettings == null || mySettings.isProjectTutorialShown()) {
      return null;
    }

    if (file.getFileType() != GoFileType.INSTANCE) return null;

    PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
    if (psiFile == null) return null;

    if (psiFile.getLanguage() != GoLanguage.INSTANCE) return null;

    NotificationGroup group = new NotificationGroup("GO_PLUGIN", NotificationDisplayType.STICKY_BALLOON, true);
    Notification notification = group.createNotification(
      "Learn how to setup a new Go project",
      "Please visit our " +
      "<a href=\"https://github.com/go-lang-plugin-org/go-lang-idea-plugin/wiki/v1.0.0-Setup-initial-project\">wiki page<a/>" +
      " to learn how to setup a new Go project",
      NotificationType.INFORMATION,
      NotificationListener.URL_OPENING_LISTENER
    );
    Notifications.Bus.notify(notification);
    mySettings.setProjectTutorialShown();

    return null;
  }
}