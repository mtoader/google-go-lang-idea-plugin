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

package com.goide.debugger.delve.debug;

import com.goide.debugger.delve.dlv.Delve;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DelveConsoleView {
  private JPanel myContentPanel;
  private JTextField myPrompt;
  private JPanel myConsoleContainer;

  private final Delve myDelve;

  // The actual console
  private final ConsoleViewImpl mConsole;

  // The last command that was sent
  private String myLastCommand;

  public DelveConsoleView(Delve delve, @NotNull Project project) {
    myDelve = delve;
    mConsole = new ConsoleViewImpl(project, true);
    myConsoleContainer.add(mConsole.getComponent(), BorderLayout.CENTER);
    myPrompt.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@NotNull ActionEvent event) {
        String command = event.getActionCommand();
        if (command.isEmpty() && myLastCommand != null) {
          // Resend the last command
          myDelve.sendCommand(myLastCommand);
        }
        else if (!command.isEmpty()) {
          // Send the command to Delve
          myLastCommand = command;
          myPrompt.setText("");
          myDelve.sendCommand(command);
        }
      }
    });
  }

  public ConsoleViewImpl getConsole() {
    return mConsole;
  }

  public JComponent getComponent() {
    return myContentPanel;
  }

  public JComponent getPreferredFocusableComponent() {
    return myPrompt;
  }
}
