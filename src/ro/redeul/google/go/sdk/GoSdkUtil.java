package ro.redeul.google.go.sdk;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.SystemProperties;
import org.jetbrains.annotations.NotNull;
import ro.redeul.google.go.config.sdk.*;
import ro.redeul.google.go.util.GoUtil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class GoSdkUtil {

    public static final String PACKAGES = "src/pkg";

    private static final Logger LOG = Logger.getInstance(
        "ro.redeul.google.go.sdk.GoSdkUtil");
    private static final String TEST_SDK_PATH = "go.test.sdk.home";

    private static final String DEFAULT_MOCK_PATH = "go/default";

    private static final String ENV_GO_ROOT = "GOROOT";

    // release: "xx"
    private static final Pattern RE_APP_ENGINE_VERSION_MATCHER =
        Pattern.compile("^release: \"([^\"]+)\"$", Pattern.MULTILINE);

    private static final Pattern RE_APP_ENGINE_TIMESTAMP_MATCHER =
        Pattern.compile("^timestamp: ([0-9]+)$", Pattern.MULTILINE);

    private static final Pattern RE_APP_ENGINE_API_VERSIONS_MATCHER =
        Pattern.compile("^api_versions: \\[([^\\]]+)\\]$", Pattern.MULTILINE);

    private static final Pattern RE_OS_MATCHER =
        Pattern.compile("^(?:set )?GOOS=\"?(darwin|freebsd|linux|windows)\"?$",
                        Pattern.MULTILINE);

    private static final Pattern RE_ARCH_MATCHER =
        Pattern.compile("^(?:set )?GOARCH=\"?(386|amd64|arm)\"?$",
                        Pattern.MULTILINE);

    private static final Pattern RE_HOSTOS_MATCHER =
        Pattern.compile("^(?:set )?GOHOSTOS=\"?(darwin|freebsd|linux|windows)\"?$",
                        Pattern.MULTILINE);

    private static final Pattern RE_HOSTARCH_MATCHER =
        Pattern.compile("^(?:set )?GOHOSTARCH=\"?(386|amd64|arm)\"?$",
                        Pattern.MULTILINE);

    private static final Pattern RE_ROOT_MATCHER =
        Pattern.compile("^(?:set )?GOROOT=\"?([^\"]+)\"?$", Pattern.MULTILINE);

    public static GoSdkData testGoogleGoSdk(String path) {

        if (!checkFolderExists(path))
            return null;

        // 1.1.2 moves src into go/version/libexec
        if (!checkFolderExists(path, "src") && !checkFolderExists(path, "libexec", "src"))
            return null;

        // 1.1.2 moves pkg into go/version/libexec
        if (!checkFolderExists(path, "pkg") && !checkFolderExists(path, "libexec", "src"))
            return null;

        String goCommand = findGoExecutable(path);
        if (goCommand.equals("")) {
            return null;
        }

        GoSdkData data = findHostOsAndArch(path, goCommand, new GoSdkData());

        data = findVersion(path, goCommand, data);

        if (data != null ) {
            data.GO_GOROOT_PATH = path;
            data.GO_BIN_PATH = goCommand;
            data.GO_GOPATH_PATH = getSysGoPathPath();
            data.version = GoSdkData.LATEST_VERSION;
        }

        return data;
    }

    public static String findGoExecutable(String path) {
        String goCommand = path + "/bin/go";
        if (checkFileExists(goCommand)) {
            return goCommand;
        }

        // Perhaps we're on Windows?
        goCommand = path + "/bin/go.exe";
        if (checkFileExists(goCommand)) {
            return goCommand;
        }

        // Perhaps we are not on Windows but the packers have a different idea
        // on how this should work...
        goCommand = "/usr/bin/go";
        if (checkFileExists(goCommand)) {
            return goCommand;
        }

        // Well then no go executable for us :(
        return "";
    }

    public static VirtualFile getSdkSourcesRoot(Sdk sdk) {
        final VirtualFile homeDirectory = sdk.getHomeDirectory();

        if (homeDirectory == null) {
            return null;
        }

        if (checkFolderExists(homeDirectory.getPath(), "src")) {
            return homeDirectory.findFileByRelativePath("src/pkg");
        } else if (checkFolderExists(homeDirectory.getPath(), "libexec", "src")) {
            return homeDirectory.findFileByRelativePath("libexec/src/pkg");
        }

        return null;
    }

    private static GoSdkData findVersion(final String path, String goCommand, GoSdkData data) {

        if (data == null)
            return null;

        try {
            GeneralCommandLine command = new GeneralCommandLine();
            command.setExePath(goCommand);
            command.addParameter("version");
            command.setWorkDirectory(path);
            command.getEnvironment().put("GOROOT", path);

            ProcessOutput output = new CapturingProcessHandler(
                command.createProcess(),
                Charset.defaultCharset(),
                command.getCommandLineString()).runProcess();

            if (output.getExitCode() != 0) {
                LOG.error("Go compiler exited with invalid exit code: " +
                              output.getExitCode());
                return null;
            }

            data.VERSION_MAJOR = output.getStdout().replaceAll("go version", "").trim();
            return data;
        } catch (Exception e) {
            LOG.error("Exception while executing the process:", e);
            return null;
        }
    }

    private static GoSdkData findHostOsAndArch(final String path, String goCommand, GoSdkData data) {

        if (data == null)
            return null;

        try {
            GeneralCommandLine command = new GeneralCommandLine();
            command.setExePath(goCommand);
            command.addParameter("env");
            command.setWorkDirectory(path);
            command.getEnvironment().put("GOROOT", path);

            ProcessOutput output = new CapturingProcessHandler(
                command.createProcess(),
                Charset.defaultCharset(),
                command.getCommandLineString()).runProcess();

            if (output.getExitCode() != 0) {
                LOG.error(
                    format(
                        "%s env command exited with invalid exit code: %d",
                        goCommand, output.getExitCode()));
                return null;
            }

            String outputString = output.getStdout();

            Matcher matcher;
            matcher = RE_HOSTOS_MATCHER.matcher(outputString);
            if (matcher.find()) {
                data.TARGET_OS = GoTargetOs.fromString(matcher.group(1));
            }

            matcher = RE_HOSTARCH_MATCHER.matcher(outputString);
            if (matcher.find()) {
                data.TARGET_ARCH = GoTargetArch.fromString(matcher.group(1));
            }
        } catch (ExecutionException e) {
            LOG.error("Exception while executing the process:", e);
            return null;
        }

        if (data.TARGET_ARCH != null && data.TARGET_OS != null)
            return data;

        return null;
    }

    public static GoAppEngineSdkData testGoAppEngineSdk(String path) {

        if (!checkFolderExists(path) || !checkFileExists(path,
                                                         "dev_appserver.py")
            || !checkFolderExists(path, "goroot") || !checkFolderExists(path,
                                                                        "goroot",
                                                                        "pkg"))
            return null;

        if (!checkFileExists(path, "VERSION"))
            return null;


        GoAppEngineSdkData sdkData = new GoAppEngineSdkData();

        sdkData.GO_HOME_PATH = format("%s/goroot", path);

        GeneralCommandLine command = new GeneralCommandLine();
        if(checkFileExists(sdkData.GO_HOME_PATH + "/bin/go")) {
            command.setExePath(sdkData.GO_HOME_PATH + "/bin/go");
        } else {
            command.setExePath(sdkData.GO_HOME_PATH + "/bin/goapp");
        }
        command.addParameter("env");
        command.setWorkDirectory(sdkData.GO_HOME_PATH + "/bin");

        sdkData.TARGET_ARCH = GoTargetArch._amd64;
        sdkData.TARGET_OS = GoTargetOs.Linux;

        try {
            ProcessOutput output = new CapturingProcessHandler(
                command.createProcess(),
                Charset.defaultCharset(),
                command.getCommandLineString()).runProcess();

            if (output.getExitCode() != 0) {
                LOG.error("Go command exited with invalid exit code: " +
                              output.getExitCode());
                return null;
            }

            String outputString = output.getStdout();

            Matcher matcher = RE_OS_MATCHER.matcher(outputString);
            if (matcher.find()) {
                sdkData.TARGET_OS = GoTargetOs.fromString(matcher.group(1));
            }

            matcher = RE_ARCH_MATCHER.matcher(outputString);
            if (matcher.find()) {
                sdkData.TARGET_ARCH = GoTargetArch.fromString(matcher.group(1));
            }
        } catch (ExecutionException e) {
            LOG.error("Exception while executing the process:", e);
        }

        try {
            String fileContent =
                VfsUtil.loadText(VfsUtil.findFileByIoFile(
                        new File(format("%s/VERSION", path)), true));

            Matcher matcher = RE_APP_ENGINE_VERSION_MATCHER.matcher(
                fileContent);

            if (!matcher.find())
                return null;
            sdkData.VERSION_MAJOR = matcher.group(1);

            matcher = RE_APP_ENGINE_TIMESTAMP_MATCHER.matcher(fileContent);
            if (!matcher.find())
                return null;
            sdkData.VERSION_MINOR = matcher.group(1);

            matcher = RE_APP_ENGINE_API_VERSIONS_MATCHER.matcher(fileContent);
            if (!matcher.find())
                return null;

            sdkData.API_VERSIONS = matcher.group(1);

        } catch (IOException e) {
            return null;
        }

        return sdkData;
    }

    private static boolean checkFileExists(String path, String child) {
        return checkFileExists(new File(path, child));
    }

    private static boolean checkFileExists(String path) {
        return checkFileExists(new File(path));
    }

    private static boolean checkFileExists(File file) {
        return file.exists() && file.isFile();
    }

    private static boolean checkFolderExists(String path) {
        return checkFolderExists(new File(path));
    }

    private static boolean checkFolderExists(File file) {
        return file.exists() && file.isDirectory();
    }

    private static boolean checkFolderExists(String path, String child) {
        return checkFolderExists(new File(path, child));
    }

    private static boolean checkFolderExists(String path, String child, String child2) {
        return checkFolderExists(new File(new File(path, child), child2));
    }


    /**
     * Uses the following to get the go sdk for tests:
     * 1. Uses the path given by the system property go.test.sdk.home, if given
     * 2. Uses the path given by the GOROOT environment variable, if available
     * 3. Uses HOMEPATH/go/default
     *
     * @return the go sdk parameters or array of zero elements if error
     */
    public static GoSdkData getMockGoogleSdk() {
        // Fallback to default home path / default mock path
        String sdkPath = PathManager.getHomePath() + "/" + DEFAULT_MOCK_PATH;

        String testSdkHome = System.getProperty(TEST_SDK_PATH);
        String goRoot = resolvePotentialGoogleGoHomePath();

        // Use the test sdk path before anything else, if available
        if (testSdkHome != null) {
            sdkPath = testSdkHome;
        } else if (goRoot != null) {
            sdkPath = goRoot;
        }

        return getMockGoogleSdk(sdkPath);
    }

    private static GoSdkData getMockGoogleSdk(String path) {
        GoSdkData sdkData = testGoogleGoSdk(path);
        /* TODO: I'm not really sure why we need this.
        if (sdkData != null) {
            new File(
                sdkData.GO_BIN_PATH,
                getCompilerName(sdkData.TARGET_ARCH)
            ).setExecutable(true);

            new File(
                sdkData.GO_BIN_PATH,
                getLinkerName(sdkData.TARGET_ARCH)
            ).setExecutable(true);

            new File(
                sdkData.GO_BIN_PATH,
                getArchivePackerName()
            ).setExecutable(true);
        }
        */

        return sdkData;
    }

    private static String getArchivePackerName() {
        return "gopack";
    }

    public static String getCompilerName(GoTargetArch arch) {
        return getBinariesDesignation(arch) + "g";
    }

    public static String getLinkerName(GoTargetArch arch) {
        return getBinariesDesignation(arch) + "l";
    }

    public static String getBinariesDesignation(GoTargetArch arch) {

        switch (arch) {
            case _386:
                return "8";

            case _amd64:
                return "6";

            case _arm:
                return "5";
        }

        return "unknown";
    }

    public static Sdk getGoogleGoSdkForModule(Module module) {

        ModuleRootModel moduleRootModel = ModuleRootManager.getInstance(module);

        Sdk sdk;
        if (!moduleRootModel.isSdkInherited()) {
            sdk = moduleRootModel.getSdk();
        } else {
            sdk = ProjectRootManager.getInstance(module.getProject())
                                    .getProjectSdk();
        }

        if (GoSdkType.isInstance(sdk)) {
            return sdk;
        }

        return null;
    }

    public static Sdk getGoogleGoSdkForProject(Project project) {

        Sdk sdk = ProjectRootManager.getInstance(project).getProjectSdk();

        if (GoSdkType.isInstance(sdk)) {
            return sdk;
        }

        return null;
    }

    public static Sdk getGoogleGoSdkForFile(PsiFile file) {
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(
            file.getProject()).getFileIndex();
        Module module = projectFileIndex.getModuleForFile(
            file.getVirtualFile());

        return getGoogleGoSdkForModule(module);
    }

    public static String getToolName(GoTargetArch arch, GoSdkTool tool) {

        String binariesDesignation = getBinariesDesignation(arch);

        switch (tool) {
            case GoCompiler:
                return binariesDesignation + "g";
            case GoLinker:
                return binariesDesignation + "l";
            case GoArchivePacker:
                return "pack";
            case GoMake:
                return "gomake";
            case GoFmt:
                return "gofmt";
        }

        return "";
    }

    public static String resolvePotentialGoogleGoAppEngineHomePath() {

        if (!isSdkRegistered(
            PathManager.getHomePath() + "/bundled/go-appengine-sdk",
            GoAppEngineSdkType
                .getInstance())) {
            return PathManager.getHomePath() + "/bundled/go-appengine-sdk";
        }

        String path = System.getenv("PATH");
        if (path == null) {
            return null;
        }

        String[] parts = path.split("[:;]+");
        for (String part : parts) {
            if (!isSdkRegistered(part, GoAppEngineSdkType.getInstance())) {
                return part;
            }
        }

        return SystemProperties.getUserHome();
    }


    public static String resolvePotentialGoogleGoHomePath() {

        if (!isSdkRegistered(PathManager.getHomePath() + "/bundled/go-sdk",
                             GoSdkType.getInstance())) {
            return PathManager.getHomePath() + "/bundled/go-sdk";
        }

        String goRoot = System.getenv(ENV_GO_ROOT);
        if (goRoot != null && !isSdkRegistered(goRoot,
                                               GoSdkType.getInstance())) {
            return goRoot;
        }

        String command = "go";
        if (GoUtil.testGoHomeFolder("/usr/lib/go") &&
            new File("/usr/lib/go/bin/go").canExecute() ) {
            command = "/usr/lib/go/bin/go";
        } else if (GoUtil.testGoHomeFolder("/usr/local/go") &&
            new File("/usr/local/go/bin/go").canExecute() ) {
            command = "/usr/local/go/bin/go";
        }

        GeneralCommandLine goCommandLine = new GeneralCommandLine();

        goCommandLine.setExePath(command);
        goCommandLine.addParameter("env");

        LOG.info("command: " + command);

        try {
            ProcessOutput output = new CapturingProcessHandler(
                goCommandLine.createProcess(),
                Charset.defaultCharset(),
                goCommandLine.getCommandLineString()).runProcess();

            if (output.getExitCode() == 0) {
                String outputString = output.getStdout();

                LOG.info("Output:\n" + outputString);
                Matcher matcher = RE_ROOT_MATCHER.matcher(outputString);
                if (matcher.find()) {
                    LOG.info("Matched: " + matcher.group(1));
                    return matcher.group(1);
                }
            }
        } catch (ExecutionException e) {
            //
        }

        return SystemProperties.getUserHome();
    }

    private static boolean isSdkRegistered(String homePath, SdkType sdkType) {

        VirtualFile homePathAsVirtualFile;
        try {
            homePathAsVirtualFile = VfsUtil.findFileByURL(
                new URL(VfsUtil.pathToUrl(homePath)));
        } catch (MalformedURLException e) {
            return true;
        }

        if (homePathAsVirtualFile == null || !homePathAsVirtualFile.isDirectory()) {
            return true;
        }

        List<Sdk> registeredSdks = GoSdkUtil.getSdkOfType(sdkType);

        for (Sdk registeredSdk : registeredSdks) {
            if (homePathAsVirtualFile.equals(
                registeredSdk.getHomeDirectory())) {
                return true;
            }
        }

        return false;
    }


    public static List<Sdk> getSdkOfType(SdkType sdkType) {
        return getSdkOfType(sdkType, ProjectJdkTable.getInstance());
    }

    public static List<Sdk> getSdkOfType(SdkType sdkType, ProjectJdkTable table) {
        Sdk[] sdks = table.getAllJdks();

        List<Sdk> goSdks = new LinkedList<Sdk>();
        for (Sdk sdk : sdks) {
            if (sdk.getSdkType() == sdkType) {
                goSdks.add(sdk);
            }
        }

        return goSdks;
    }

    public static String getGoPath(GoSdkData sdkData) {
        if (!sdkData.GO_GOROOT_PATH.isEmpty()) {
            return sdkData.GO_GOROOT_PATH;

        }

        return getSysGoPathPath();
    }

    public static String prependToGoPath(GoSdkData sdkData, String prependedPath) {
        String goPath = getGoPath(sdkData);

        return format("%s%s%s", prependedPath, File.pathSeparator, goPath);
    }

    public static String appendToGoPath(GoSdkData sdkData, String prependedPath) {
        String goPath = getGoPath(sdkData);

        return format("%s%s%s", goPath, File.pathSeparator, prependedPath);
    }

    public static String getSysGoRootPath() {
        try {
            return System.getenv("GOROOT");
        } catch (NullPointerException ignored) {
            return "";
        } catch (SecurityException ignored) {
            return "";
        }
    }

    public static String getSysGoPathPath() {
        try {
            return System.getenv("GOPATH");
        } catch (NullPointerException ignored) {
            return "";
        } catch (SecurityException ignored) {
            return "";
        }
    }

    @NotNull
    public static String getPathToDisplay(final VirtualFile file) {
        if (file == null) {
            return "";
        }
        return FileUtil.toSystemDependentName(file.getPath());
    }

    public static VirtualFile getVirtualFile(String path) {
        File pluginPath = new File(path);

        if (!pluginPath.exists()) {
            return null;
        }

        String url = VfsUtil.pathToUrl(pluginPath.getAbsolutePath());

        return VirtualFileManager.getInstance().findFileByUrl(url);
    }

    public static Map<String, String> getExtendedSysEnv(GoSdkData sdkData, String projectDir, String envVars) {
        Map<String,String> sysEnv = new HashMap<String, String>(System.getenv());
        sysEnv.put("GOROOT", getSdkRootPath(sdkData));
        sysEnv.put("GOPATH", GoSdkUtil.appendToGoPath(sdkData, projectDir));

        if (envVars.length() > 0) {
            String[] envVarsArray = envVars.split(";");
            for(String envVar: envVarsArray) {
                if (!envVar.contains("=")) {
                    continue;
                }
                String[] splitEnvVars = envVar.split("=");
                if (splitEnvVars.length != 2) {
                    continue;
                }
                sysEnv.put(splitEnvVars[0], splitEnvVars[1]);
            }
        }

        return sysEnv;
    }

    public static String[] convertEnvMapToArray(Map<String, String> envMap) {
        String[] goEnv = new String[envMap.size()];

        Iterator it = envMap.entrySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            goEnv[i] = pairs.getKey() + "=" + pairs.getValue();
            i++;
        }

        return goEnv;
    }

    public static String[] getExtendedGoEnv(GoSdkData sdkData, String projectDir, String envVars) {
        return convertEnvMapToArray(getExtendedSysEnv(sdkData, projectDir, envVars));
    }

    public static String getSdkRootPath(GoSdkData sdkData) {
        if (!sdkData.GO_GOROOT_PATH.isEmpty()) {
            return sdkData.GO_GOROOT_PATH;
        }

        File possibleRoot = new File(sdkData.GO_BIN_PATH).getParentFile();
        try {
            if (new File(possibleRoot.getCanonicalPath().concat("/src")).exists()) {
                return possibleRoot.getCanonicalPath();
            }

            if (new File(possibleRoot.getCanonicalPath().concat("/libexec/src")).exists()) {
                return possibleRoot.getCanonicalPath();
            }
        } catch (IOException ignored) {
        }

        try {
            return possibleRoot.getParentFile().getCanonicalPath();
        } catch (IOException ignored) {
        }

        // If anything else failed try to get it from the system
        return getSysGoRootPath();
    }
}
