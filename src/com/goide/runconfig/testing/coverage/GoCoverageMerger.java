package com.goide.runconfig.testing.coverage;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.*;

public class GoCoverageMerger {
  protected static final Logger LOG = Logger.getInstance("#com.goide.runconfig.testing.coverage.GoCoverageMerger");

  public static void MergeCoverage(String fromPath, String toFile) {
    try (
        FileWriter stream = new FileWriter(toFile, false);
        BufferedWriter writer = new BufferedWriter(stream)
    ) {
      writer.write("mode: set");
      writer.newLine();
      VirtualFile fromFile = LocalFileSystem.getInstance().findFileByPath(fromPath);
      VfsUtil.markDirtyAndRefresh(false, true, true, fromFile);
      mergeCoverage(writer, fromFile);
    } catch (IOException e) {
      LOG.error(e.getMessage());
    }
  }

  private static void mergeCoverage(BufferedWriter writer, VirtualFile directory) {
    VfsUtil.iterateChildrenRecursively(directory,
        null,
        virtualFile -> {
          if (!"profile.cov".equals(virtualFile.getName())) {
            return true;
          }

          try {
            try (
                InputStream stream = virtualFile.getInputStream();
                InputStreamReader streamReader = new InputStreamReader(stream);
                BufferedReader reader = new BufferedReader(streamReader)
            ) {
              copyCoverage(writer, reader);
            }
            virtualFile.delete(null);
            return true;
          }
          catch (IOException e) {
            LOG.error(e.getMessage());
            return false;
          }
        });
  }

  private static void copyCoverage(BufferedWriter writer, BufferedReader reader) throws IOException {
    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
      writer.write(line);
      writer.newLine();
    }
  }
}
