package com.goide.runconfig.testing.coverage;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.util.Key;

import java.io.File;

class GoCoverageProcessListener implements ProcessListener {
  private String myCoverageFilePath;
  private String myCoverageDir;

  public GoCoverageProcessListener(String fromDir, String toFile) {
    myCoverageFilePath = toFile;
    myCoverageDir = fromDir;
  }

  @Override
  public void startNotified(ProcessEvent event) {}

  @Override
  public void processTerminated(ProcessEvent event) {
    if (event.getExitCode() != 0 || new File(myCoverageFilePath).exists()) {
      return;
    }

    GoCoverageMerger.MergeCoverage(myCoverageDir, myCoverageFilePath);
  }

  @Override
  public void processWillTerminate(ProcessEvent event, boolean willBeDestroyed) {}

  @Override
  public void onTextAvailable(ProcessEvent event, Key outputType) {}
}
