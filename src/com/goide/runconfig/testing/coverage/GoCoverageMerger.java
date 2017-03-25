package com.goide.runconfig.testing.coverage;

import com.intellij.openapi.diagnostic.Logger;

import java.io.*;

public class GoCoverageMerger {
  protected static final Logger LOG = Logger.getInstance("#com.goide.runconfig.testing.coverage.GoCoverageMerger");

  public static void MergeCoverage(String fromPath, String toFile) {
    try {
      try (
          FileWriter stream = new FileWriter(toFile, false);
          BufferedWriter writer = new BufferedWriter(stream)
      ) {
        writer.write("mode: set");
        writer.newLine();
        mergeCoverage(writer, new File(fromPath));
      }
    } catch (IOException e) {
      LOG.error(e.getMessage());
    }
  }

  private static void mergeCoverage(BufferedWriter writer, File directory) throws IOException {
    for (File file : directory.listFiles()) {
      if (file.isDirectory()) {
        mergeCoverage(writer, file);
      } else if ("profile.cov".equals(file.getName())) {
        try (
            FileReader stream = new FileReader(file);
            BufferedReader reader = new BufferedReader(stream)
        ) {
          copyCoverage(writer, reader);
        }

        file.delete();
      }
    }
  }

  private static void copyCoverage(BufferedWriter writer, BufferedReader reader) throws IOException {
    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
      writer.write(line);
      writer.newLine();
    }
  }
}
