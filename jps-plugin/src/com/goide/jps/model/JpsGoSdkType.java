package com.goide.jps.model;

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsDummyElement;
import org.jetbrains.jps.model.JpsElementFactory;
import org.jetbrains.jps.model.JpsElementTypeWithDefaultProperties;
import org.jetbrains.jps.model.library.sdk.JpsSdkType;

import java.io.File;

public class JpsGoSdkType extends JpsSdkType<JpsDummyElement> implements JpsElementTypeWithDefaultProperties<JpsDummyElement> {
  public static final JpsGoSdkType INSTANCE = new JpsGoSdkType();

  private static final String goExecName = SystemInfo.isWindows ? "go.exe" : "go";

  @NotNull
  public static File getGoExecutableFile(@NotNull String sdkHome) {
    File fromSdkPath = getExecutable(new File(sdkHome, "bin").getAbsolutePath(), goExecName);
    File fromEnvironment = PathEnvironmentVariableUtil.findInPath(goExecName);
    return fromSdkPath.canExecute() || fromEnvironment == null ? fromSdkPath : fromEnvironment;
  }

  @NotNull
  public static String getBinaryPathByModulePath(@NotNull String modulePath, @NotNull String outputDirectory) {
    return outputDirectory + File.separatorChar + getBinaryFileNameForPath(modulePath);
  }

  @NotNull
  public static String getBinaryFileNameForPath(@NotNull String path) {
    String resultBinaryName = FileUtil.getNameWithoutExtension(PathUtil.getFileName(path));
    return SystemInfo.isWindows ? resultBinaryName + ".exe" : resultBinaryName;
  }

  @NotNull
  @Override
  public JpsDummyElement createDefaultProperties() {
    return JpsElementFactory.getInstance().createDummyElement();
  }

  @NotNull
  private static File getExecutable(@NotNull String path, @NotNull String command) {
    return new File(path, getBinaryFileNameForPath(command));
  }
}
