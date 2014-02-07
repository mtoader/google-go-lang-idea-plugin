package uk.co.cwspencer.gdb.gdbmi;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for GDB/MI output version 2.
 *
 * @author Florin Patan <florinpatan@gmail.com>
 */
public class GdbMiParser2 {

    private static final Set<String> START_TOKENS = new HashSet<String>(Arrays.asList(
            new String[]{"^", "*", "+", "=", "~", "@", "&", ",", "\"", "{", "}", "[", "]"}
    ));

    // Partially processed record
    private GdbMiResultRecord m_resultRecord;
    private GdbMiStreamRecord m_streamRecord;

    private ConsoleView console;

    // List of unprocessed records
    private List<GdbMiRecord> m_records = new ArrayList<GdbMiRecord>();
    private Long currentToken;

    public GdbMiParser2(ConsoleView console) {
        this.console = console;
    }

    /**
     * Returns a list of unprocessed records. The caller should erase items from this list as they
     * are processed.
     *
     * @return A list of unprocessed records.
     */
    public List<GdbMiRecord> getRecords() {
        return m_records;
    }

    /**
     * Processes the given data.
     *
     * @param data Data read from the GDB process.
     */
    public void process(byte[] data) {
        process(data, data.length);
    }

    /**
     * Processes the given data.
     *
     * @param data   Data read from the GDB process.
     * @param length Number of bytes from data to process.
     */
    public void process(byte[] data, int length) {
        // Run the data through the lexer first
        String[] buffer = convertGoOutput(data);

        for (String line : buffer) {
            if (line.isEmpty() ||
                    line.matches("@\u0000*")) {
                continue;
            }

            GdbMiRecord parsedLine = parseLine(line);
            if (parsedLine == null) {
                continue;
            }

            m_records.add(parsedLine);
        }
    }

    private String[] convertGoOutput(byte[] data) {
        String buff;

        try {
            buff = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            return new String[]{};
        }

        String[] lines = buff.split("\n");

        int i;
        for (i = 0; i < lines.length; i++) {
            if (isGdbMiLine(lines[i])) {
                continue;
            }

            lines[i] = "@" + lines[i];
        }

        return lines;
    }

    private Boolean isGdbMiLine(String line) {
        if (START_TOKENS.contains(line.substring(0, 1))) {
            return true;
        }

        if (line.matches("\\d+\\^.*")) {
            return true;
        }

        if (line.startsWith("(gdb)")) {
            return true;
        }

        return false;
    }

    @Nullable
    private GdbMiRecord parseLine(String line) {
        if (System.getProperty("go.gdb.dev.debug", "false").equals("true")) {
            console.print("{[Parsing line]} ", ConsoleViewContentType.NORMAL_OUTPUT);
            console.print(line + "\n", ConsoleViewContentType.SYSTEM_OUTPUT);
        }

        GdbMiRecord result;

        if (line.matches("\\d+\\^.*")) {
            currentToken = Long.parseLong(line.substring(0, line.indexOf('^')), 10);

            result = new GdbMiResultRecord(GdbMiRecord.Type.Immediate, currentToken);
            result = parseImmediateLine(line, (GdbMiResultRecord) result);
            return result;
        }

        if (line.startsWith("(gdb)")) {
            return null;
        }

        switch (line.charAt(0)) {
            case '*':
                result = new GdbMiResultRecord(GdbMiRecord.Type.Exec, currentToken);
                result = parseExecLine(line, (GdbMiResultRecord) result);
                currentToken = null;
                break;

            case '+':
                result = new GdbMiStreamRecord(GdbMiRecord.Type.Log, currentToken);
                ((GdbMiStreamRecord) result).message = line.concat("\n");
                currentToken = null;
                break;

            case '=':
                result = new GdbMiResultRecord(GdbMiRecord.Type.Notify, currentToken);
                result = parseNotifyLine(line, (GdbMiResultRecord) result);
                currentToken = null;
                break;

            case '~':
                result = new GdbMiStreamRecord(GdbMiRecord.Type.Console, currentToken);
                line = line.substring(2, line.length() - 1)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replaceAll("<http(.*)>", "http$1");

                ((GdbMiStreamRecord) result).message = line;
                currentToken = null;
                break;

            case '@':
                result = new GdbMiStreamRecord(GdbMiRecord.Type.Target, currentToken);
                ((GdbMiStreamRecord) result).message = line.substring(1).concat("\n");
                currentToken = null;
                break;

            case '&':
                result = new GdbMiStreamRecord(GdbMiRecord.Type.Log, currentToken);
                line = line.substring(2, line.length() - 1)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replaceAll("<http(.*)>", "http$1");

                ((GdbMiStreamRecord) result).message = line;
                currentToken = null;
                break;

            default:
                result = new GdbMiStreamRecord(GdbMiRecord.Type.Log, currentToken);
                ((GdbMiStreamRecord) result).message = line.concat("\n");
        }

        return result;
    }

    private GdbMiResultRecord parseNotifyLine(String line, GdbMiResultRecord result) {
        result.className = line.substring(1, line.indexOf(','));

        line = line.substring(line.indexOf(',') + 1);
        if (line.startsWith("bkpt")) {
            result.results.add(parseBreakpointLine(line));
            return result;
        }

        Pattern p = Pattern.compile("([a-z-]+)=(?:\"([^\"]+?)\")+");
        Matcher m = p.matcher(line);
        while (m.find()) {
            GdbMiResult subRes = new GdbMiResult(m.group(1));
            subRes.value = new GdbMiValue(GdbMiValue.Type.String);
            subRes.value.string = m.group(2).replace("\\\\t", "    ");
            result.results.add(subRes);
        }

        return result;
    }

    private GdbMiResultRecord parseExecLine(String line, GdbMiResultRecord result) {
        if (line.indexOf(',') < 0) {
            result.className = line.substring(1);
            return result;
        }

        result.className = line.substring(1, line.indexOf(','));

        line = line.substring(line.indexOf(',') + 1);
        if (result.className.equals("stopped")) {
            if (line.startsWith("reason=\"breakpoint-hit\"")) {
                result.results.addAll(parseBreakpointHitLine(line));
            } else if (line.startsWith("reason=\"end-stepping-range\"")) {
                result.results.addAll(parseEndSteppingRangeLine(line));
            } else if (line.startsWith("reason=\"signal-received\"")) {
                result.results.addAll(parseSignalReceivedLine(line));
            } else if (line.startsWith("reason=\"function-finished\"")) {
                result.results.addAll(parseFunctionFinishedLine(line));
            } else if (line.startsWith("reason=\"location-reached\"")) {
                result.results.addAll(parseLocationReachedLine(line));
            } else if (line.startsWith("reason=\"exited\"")) {
                result.results.addAll(parseStoppedExitedLine(line));
            } else if (line.startsWith("reason=\"exited-normally\"")) {
                GdbMiResult reasonVal = new GdbMiResult("reason");
                reasonVal.value = new GdbMiValue(GdbMiValue.Type.String);
                reasonVal.value.string = "exited-normally";
                result.results.add(reasonVal);
            } else if (line.startsWith("frame=")) {
                result.results.addAll(parseStoppedFrameLine(line));
            } else {
                console.print("[[[ go.gdb.internal ]]] " + line + "\n", ConsoleViewContentType.ERROR_OUTPUT);
            }

            return result;
        }

        if (result.className.equals("running")) {
            if (line.startsWith("thread-id")) {
                result.results.add(parseRunningThreadId(line));
            } else {
                console.print("[[[ go.gdb.internal ]]] " + line + "\n", ConsoleViewContentType.ERROR_OUTPUT);
            }

            return result;
        }

        console.print("[[[ go.gdb.internal ]]] " + line + "\n", ConsoleViewContentType.ERROR_OUTPUT);
        return result;
    }

    private GdbMiResultRecord parseImmediateLine(String line, GdbMiResultRecord result) {
        if (line.indexOf(',') < 0) {
            result.className = line.substring(line.indexOf('^') + 1);
            return result;
        }

        result.className = line.substring(line.indexOf('^') + 1, line.indexOf(','));
        line = line.substring(line.indexOf(',') + 1);

        // Check for breakpoint
        if (line.startsWith("bkpt=")) {
            result.results.add(parseBreakpointLine(line));
        } else if (line.startsWith("stack=")) {
            result.results.add(parseStackListLine(line));
        } else if (line.startsWith("variables=")) {
            result.results.add(parseStackListVariablesLine(line));
        } else if (line.startsWith("name=\"var")) {
            result.results.addAll(parseVarCreateLine(line));
        } else if (line.startsWith("changelist=")) {
            result.results.add(parseChangelistLine(line));
        } else if (line.startsWith("msg=")) {
            result.results.add(parseMsgLine(line));
        } else if (line.startsWith("numchild=")) {
            result.results.addAll(parseNumChildLine(line));
        }

        return result;
    }

    private static GdbMiResult parseBreakpointLine(String line) {
        GdbMiResult subRes = new GdbMiResult("bkpt");
        GdbMiValue bkptVal = new GdbMiValue(GdbMiValue.Type.Tuple);

        Pattern p = Pattern.compile(
                "(?:number=\"(\\d+)\")," +
                        "(?:type=\"([^\"]+)\")," +
                        "(?:disp=\"([^\"]+)\")," +
                        "(?:enabled=\"([^\"]+)\")," +
                        "(?:addr=\"([^\"]+)\")," +
                        "(?:func=\"([^\"]+)\")," +
                        "(?:file=\"([^\"]+)\")," +
                        "(?:fullname=\"([^\"]+)\")," +
                        "(?:line=\"([^\"]+)\")," +
                        "(?:thread-groups=\\[\"([^\"]+)\"\\])," +
                        "(?:times=\"(\\d+)\")," +
                        "(?:original-location=\"([^\"]+)\")"
        );
        Matcher m = p.matcher(line);

        if (!m.find()) {
            return subRes;
        }

        // number="1"
        GdbMiResult numVal = new GdbMiResult("number");
        numVal.value.type = GdbMiValue.Type.String;
        numVal.value.string = m.group(1);
        bkptVal.tuple.add(numVal);

        // type="breakpoint"
        GdbMiResult typeVal = new GdbMiResult("type");
        typeVal.value.type = GdbMiValue.Type.String;
        typeVal.value.string = m.group(2);
        bkptVal.tuple.add(typeVal);

        // disp="keep"
        GdbMiResult dispVal = new GdbMiResult("disp");
        dispVal.value.type = GdbMiValue.Type.String;
        dispVal.value.string = m.group(3);
        bkptVal.tuple.add(dispVal);

        // enabled="y"
        GdbMiResult enabledVal = new GdbMiResult("enabled");
        enabledVal.value.type = GdbMiValue.Type.String;
        enabledVal.value.string = m.group(4);
        bkptVal.tuple.add(enabledVal);

        // addr="0x0000000000400c57"
        GdbMiResult addrVal = new GdbMiResult("addr");
        addrVal.value.type = GdbMiValue.Type.String;
        addrVal.value.string = m.group(5);
        bkptVal.tuple.add(addrVal);

        // func="main.main"
        GdbMiResult funcVal = new GdbMiResult("func");
        funcVal.value.type = GdbMiValue.Type.String;
        funcVal.value.string = m.group(6);
        bkptVal.tuple.add(funcVal);

        // file="/var/www/personal/untitled4/src/untitled4.go"
        GdbMiResult fileVal = new GdbMiResult("file");
        fileVal.value.type = GdbMiValue.Type.String;
        fileVal.value.string = m.group(7);
        bkptVal.tuple.add(fileVal);

        // fullname="/var/www/personal/untitled4/src/untitled4.go"
        GdbMiResult fullnameVal = new GdbMiResult("fullname");
        fullnameVal.value.type = GdbMiValue.Type.String;
        fullnameVal.value.string = m.group(8);
        bkptVal.tuple.add(fullnameVal);

        // line="17"
        GdbMiResult lineVal = new GdbMiResult("line");
        lineVal.value.type = GdbMiValue.Type.String;
        lineVal.value.string = m.group(9);
        bkptVal.tuple.add(lineVal);

        // thread-groups=["i1"]
        GdbMiResult threadGroupVal = new GdbMiResult("thread-groups");
        threadGroupVal.value.type = GdbMiValue.Type.List;
        threadGroupVal.value.list = new GdbMiList();
        threadGroupVal.value.list.type = GdbMiList.Type.Values;
        threadGroupVal.value.list.values = new ArrayList<GdbMiValue>();

        String[] threadGroupIds = m.group(10).split(",");
        for (String threadGroupId : threadGroupIds) {
            GdbMiValue tgiVal = new GdbMiValue(GdbMiValue.Type.String);
            tgiVal.string = threadGroupId;
            threadGroupVal.value.list.values.add(tgiVal);
        }
        bkptVal.tuple.add(threadGroupVal);

        // times="0"
        GdbMiResult timesVal = new GdbMiResult("times");
        timesVal.value.type = GdbMiValue.Type.String;
        timesVal.value.string = m.group(11);
        bkptVal.tuple.add(timesVal);

        // original-location="/var/www/personal/untitled4/src/untitled4.go:17"
        GdbMiResult originalLocationVal = new GdbMiResult("original-location");
        originalLocationVal.value.type = GdbMiValue.Type.String;
        originalLocationVal.value.string = m.group(12);
        bkptVal.tuple.add(originalLocationVal);

        subRes.value = bkptVal;
        return subRes;
    }

    private static Collection<GdbMiResult> parseBreakpointHitLine(String line) {
        Collection<GdbMiResult> result = new ArrayList<GdbMiResult>();

        Pattern p = Pattern.compile(
                "(?:reason=\"([^\"]+)\")," +
                        "(?:disp=\"([^\"]+)\")," +
                        "(?:bkptno=\"(\\d+)\")," +
                        "(?:frame=\\{([^\\}].+)\\})," +
                        "(?:thread-id=\"([^\"]+)\")," +
                        "(?:stopped-threads=\"([^\"]+)\")," +
                        "(?:core=\"(\\d+)\")"
        );
        Matcher m = p.matcher(line);

        if (!m.find()) {
            return result;
        }

        // reason="breakpoint-hit"
        GdbMiResult reasonVal = new GdbMiResult("reason");
        reasonVal.value.type = GdbMiValue.Type.String;
        reasonVal.value.string = m.group(1);
        result.add(reasonVal);

        // disp="keep"
        GdbMiResult dispVal = new GdbMiResult("disp");
        dispVal.value.type = GdbMiValue.Type.String;
        dispVal.value.string = m.group(2);
        result.add(dispVal);

        // bkptno="1"
        GdbMiResult bkptNoVal = new GdbMiResult("bkptno");
        bkptNoVal.value.type = GdbMiValue.Type.String;
        bkptNoVal.value.string = m.group(3);
        result.add(bkptNoVal);

        // frame={*}
        result.add(parseBreakpointHitLineFrameLine(m.group(4)));

        // thread-id="1"
        GdbMiResult threadIdVal = new GdbMiResult("thread-id");
        threadIdVal.value.type = GdbMiValue.Type.String;
        threadIdVal.value.string = m.group(5);
        result.add(threadIdVal);

        // stopped-threads="all"
        GdbMiResult stoppedThreadsVal = new GdbMiResult("stopped-threads");
        stoppedThreadsVal.value.type = GdbMiValue.Type.String;
        stoppedThreadsVal.value.string = m.group(6);
        result.add(stoppedThreadsVal);

        // core="6"
        GdbMiResult coreVal = new GdbMiResult("core");
        coreVal.value.type = GdbMiValue.Type.String;
        coreVal.value.string = m.group(7);
        result.add(coreVal);

        return result;
    }

    private static GdbMiResult parseBreakpointHitLineFrameLine(String line) {
        GdbMiResult subRes = new GdbMiResult("frame");
        GdbMiValue frameVal = new GdbMiValue(GdbMiValue.Type.Tuple);

        Pattern p = Pattern.compile(
                "(?:addr=\"([^\"].+?)\")," +
                        "(?:func=\"([^\"].+?)\")," +
                        "(?:args=\\[([^\\]]*)\\])," +
                        "(?:file=\"([^\"].+)\")," +
                        "(?:fullname=\"([^\"].+)\")," +
                        "(?:line=\"(\\d+)\")"
        );
        Matcher m = p.matcher(line);

        if (!m.find()) {
            return subRes;
        }

        // addr="0x0000000000400c57"
        GdbMiResult addrVal = new GdbMiResult("addr");
        addrVal.value.type = GdbMiValue.Type.String;
        addrVal.value.string = m.group(1);
        frameVal.tuple.add(addrVal);

        // func="main.main"
        GdbMiResult funcVal = new GdbMiResult("func");
        funcVal.value.type = GdbMiValue.Type.String;
        funcVal.value.string = m.group(2);
        frameVal.tuple.add(funcVal);

        // args=[]
        frameVal.tuple.add(parseArgsLine(m.group(3)));

        // file="/var/www/personal/untitled4/src/untitled4.go"
        GdbMiResult fileVal = new GdbMiResult("file");
        fileVal.value.type = GdbMiValue.Type.String;
        fileVal.value.string = m.group(4);
        frameVal.tuple.add(fileVal);

        // fullname="/var/www/personal/untitled4/src/untitled4.go"
        GdbMiResult fullnameVal = new GdbMiResult("fullname");
        fullnameVal.value.type = GdbMiValue.Type.String;
        fullnameVal.value.string = m.group(5);
        frameVal.tuple.add(fullnameVal);

        // line="17"
        GdbMiResult lineVal = new GdbMiResult("line");
        lineVal.value.type = GdbMiValue.Type.String;
        lineVal.value.string = m.group(6);
        frameVal.tuple.add(lineVal);

        subRes.value = frameVal;
        return subRes;
    }

    private static GdbMiResult parseStackListLine(String line) {
        GdbMiResult subRes = new GdbMiResult("stack");
        GdbMiValue stackListVal = new GdbMiValue(GdbMiValue.Type.List);

        stackListVal.list.results = new ArrayList<GdbMiResult>();
        stackListVal.list.type = GdbMiList.Type.Results;
        stackListVal.list.results.addAll(parseStackListFrameLine(line));

        subRes.value = stackListVal;
        return subRes;
    }

    private static Collection<GdbMiResult> parseStackListFrameLine(String line) {
        Collection<GdbMiResult> result = new ArrayList<GdbMiResult>();

        Pattern p = Pattern.compile(
                "\\{(?:level=\"(\\d+)\")," +
                        "(?:addr=\"([^\"]+)\")," +
                        "(?:func=\"([^\"]+)\")," +
                        "(?:file=\"([^\"]+)\")," +
                        "(?:fullname=\"([^\"].+)\")," +
                        "(?:line=\"(\\d+)\")\\}"
        );
        Matcher m = p.matcher(line);

        while (m.find()) {
            GdbMiResult subRes = new GdbMiResult("frame");
            GdbMiValue frameVal = new GdbMiValue(GdbMiValue.Type.Tuple);

            // level="0"
            GdbMiResult levelVal = new GdbMiResult("level");
            levelVal.value.type = GdbMiValue.Type.String;
            levelVal.value.string = m.group(1);
            frameVal.tuple.add(levelVal);

            // addr="0x0000000000400c57"
            GdbMiResult addrVal = new GdbMiResult("addr");
            addrVal.value.type = GdbMiValue.Type.String;
            addrVal.value.string = m.group(2);
            frameVal.tuple.add(addrVal);

            // func="main.main"
            GdbMiResult funcVal = new GdbMiResult("func");
            funcVal.value.type = GdbMiValue.Type.String;
            funcVal.value.string = m.group(3);
            frameVal.tuple.add(funcVal);

            // file="/var/www/personal/untitled4/src/untitled4.go"
            GdbMiResult fileVal = new GdbMiResult("file");
            fileVal.value.type = GdbMiValue.Type.String;
            fileVal.value.string = m.group(4);
            frameVal.tuple.add(fileVal);

            // fullname="/var/www/personal/untitled4/src/untitled4.go"
            GdbMiResult fullnameVal = new GdbMiResult("fullname");
            fullnameVal.value.type = GdbMiValue.Type.String;
            fullnameVal.value.string = m.group(5);
            frameVal.tuple.add(fullnameVal);

            // line="17"
            GdbMiResult lineVal = new GdbMiResult("line");
            lineVal.value.type = GdbMiValue.Type.String;
            lineVal.value.string = m.group(6);
            frameVal.tuple.add(lineVal);

            subRes.value = frameVal;
            result.add(subRes);
        }

        return result;
    }

    private static GdbMiResult parseStackListVariablesLine(String line) {
        GdbMiResult subRes = new GdbMiResult("variables");
        GdbMiValue stackListVarsVal = new GdbMiValue(GdbMiValue.Type.List);
        stackListVarsVal.list.type = GdbMiList.Type.Values;
        stackListVarsVal.list.values = new ArrayList<GdbMiValue>();

        Pattern p = Pattern.compile("\\{(?:name=\"([^\"]+)\")(?:,arg=\"([^\"]+)\")?\\}");
        Matcher m = p.matcher(line);

        while (m.find()) {
            GdbMiValue varVal = new GdbMiValue(GdbMiValue.Type.Tuple);
            varVal.tuple = new ArrayList<GdbMiResult>();

            GdbMiResult varNameVal = new GdbMiResult("name");
            varNameVal.value.type = GdbMiValue.Type.String;
            varNameVal.value.string = m.group(1);
            varVal.tuple.add(varNameVal);

            if (m.group(2) != null) {
                GdbMiResult argVal = new GdbMiResult("arg");
                argVal.value.type = GdbMiValue.Type.String;
                argVal.value.string = m.group(2);
                varVal.tuple.add(argVal);
            }

            stackListVarsVal.list.values.add(varVal);
        }

        subRes.value = stackListVarsVal;
        return subRes;
    }

    private static Collection<GdbMiResult> parseVarCreateLine(String line) {
        Collection<GdbMiResult> result = new ArrayList<GdbMiResult>();

        Pattern p = Pattern.compile("(?:thread-id=\"([^\"]+)\"),");
        Matcher m = p.matcher(line);
        Boolean hasThreadId = false;
        if (m.find()) {
            hasThreadId = true;
        }

        String pattern = "(?:name=\"([^\"]+)\")," +
                "(?:numchild=\"([^\"]+)\")," +
                "(?:value=\"([^\"].*?)\")," +
                "(?:type=\"([^\"]+)\"),";

        if (hasThreadId) {
                pattern += "(?:thread-id=\"([^\"]+)\"),";
        }

        pattern += "(?:has_more=\"([^\"]+)\")";

        p = Pattern.compile(pattern);
        m = p.matcher(line);

        if (!m.find()) {
            return result;
        }

        // name="var1"
        GdbMiResult nameVal = new GdbMiResult("name");
        nameVal.value.type = GdbMiValue.Type.String;
        nameVal.value.string = m.group(1);
        result.add(nameVal);

        // numchild="0"
        GdbMiResult numChildVal = new GdbMiResult("numchild");
        numChildVal.value.type = GdbMiValue.Type.String;
        numChildVal.value.string = m.group(2);
        result.add(numChildVal);

        // value="false"
        GdbMiResult valueVal = new GdbMiResult("value");
        valueVal.value.type = GdbMiValue.Type.String;
        valueVal.value.string = m.group(3);
        result.add(valueVal);

        // type="bool"
        GdbMiResult typeVal = new GdbMiResult("type");
        typeVal.value.type = GdbMiValue.Type.String;
        typeVal.value.string = m.group(4);
        result.add(typeVal);

        if (hasThreadId) {
            // thread-id="1"
            GdbMiResult threadIdVal = new GdbMiResult("thread-id");
            threadIdVal.value.type = GdbMiValue.Type.String;
            threadIdVal.value.string = m.group(5);
            result.add(threadIdVal);
        }

        // has_more="0"
        GdbMiResult hasMoreVal = new GdbMiResult("has_more");
        hasMoreVal.value.type = GdbMiValue.Type.String;
        if (hasThreadId) {
            hasMoreVal.value.string = m.group(6);
        } else {
            hasMoreVal.value.string = m.group(5);
        }
        result.add(hasMoreVal);

        return result;
    }

    private static GdbMiResult parseChangelistLine(String line) {
        GdbMiResult result = new GdbMiResult("changelist");
        result.value.type = GdbMiValue.Type.List;
        result.value.list = new GdbMiList();

        Pattern p = Pattern.compile(
                "(?:\\{name=\"([^\"]+)\"," +
                        "value=\"([^\"].*?)\"," +
                        "in_scope=\"([^\"]+)\"," +
                        "type_changed=\"([^\"]+)\"," +
                        "has_more=\"([^\"]+)\"\\})+"
        );
        Matcher m = p.matcher(line);
        if (m.find()) {
            parseChangelistLineReal(line, result, true);
        }

        p = Pattern.compile(
                "(?:\\{name=\"([^\"]+)\"," +
                        "in_scope=\"([^\"]+)\"," +
                        "type_changed=\"([^\"]+)\"," +
                        "has_more=\"([^\"]+)\"\\})+"
        );
        m = p.matcher(line);
        if (m.find()) {
            parseChangelistLineReal(line, result, false);
        }

        return result;
    }

    private static void parseChangelistLineReal(String line, GdbMiResult result, Boolean includeValue) {
        String regex = "(?:\\{name=\"([^\"]+)\",";

        if (includeValue) {
                regex += "value=\"([^\"].*?)\",";
        }

        regex += "in_scope=\"([^\"]+)\"," +
                "type_changed=\"([^\"]+)\"," +
                "has_more=\"([^\"]+)\"\\})+";

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(line);

        while (m.find()) {
            Integer groupId = 0;
            GdbMiValue changeVal = new GdbMiValue(GdbMiValue.Type.Tuple);

            // name: "var5"
            GdbMiResult nameVal = new GdbMiResult("name");
            nameVal.value.type = GdbMiValue.Type.String;
            nameVal.value.string = m.group(++groupId);
            changeVal.tuple.add(nameVal);

            if (includeValue) {
                // value: "3,3300000000000001"
                GdbMiResult valueVal = new GdbMiResult("value");
                valueVal.value.type = GdbMiValue.Type.String;
                valueVal.value.string = m.group(++groupId);
                changeVal.tuple.add(valueVal);
            }

            // in_scope: "true"
            GdbMiResult inScopeVal = new GdbMiResult("in_scope");
            inScopeVal.value.type = GdbMiValue.Type.String;
            inScopeVal.value.string = m.group(++groupId);
            changeVal.tuple.add(inScopeVal);

            // type_changed: "false"
            GdbMiResult typeChangedVal = new GdbMiResult("type_changed");
            typeChangedVal.value.type = GdbMiValue.Type.String;
            typeChangedVal.value.string = m.group(++groupId);
            changeVal.tuple.add(typeChangedVal);

            // has_more: "0"
            GdbMiResult hasMoreVal = new GdbMiResult("has_more");
            hasMoreVal.value.type = GdbMiValue.Type.String;
            hasMoreVal.value.string = m.group(++groupId);
            changeVal.tuple.add(hasMoreVal);

            if (result.value.list.values == null) {
                result.value.list.type = GdbMiList.Type.Values;
                result.value.list.values = new ArrayList<GdbMiValue>();
            }

            result.value.list.values.add(changeVal);
        }
    }

    private static GdbMiResult parseMsgLine(String line) {
        Pattern p = Pattern.compile("(?:msg=\"([^\"]+)\")");

        Matcher m = p.matcher(line);

        // msg="No frames found."
        GdbMiResult result = new GdbMiResult("msg");
        result.value.type = GdbMiValue.Type.String;

        if (m.find()) {
            result.value.string = m.group(1);
        }

        return result;
    }

    private static GdbMiResult parseRunningThreadId(String line) {
        Pattern p = Pattern.compile("(?:thread-id=\"([^\"]+)\")");
        Matcher m = p.matcher(line);

        // thread-id="all"
        GdbMiResult result = new GdbMiResult("thread-id");
        result.value.type = GdbMiValue.Type.String;

        if (m.find()) {
            result.value.string = m.group(1);
        }

        return result;
    }

    private static Collection<GdbMiResult> parseEndSteppingRangeLine(String line) {
        Collection<GdbMiResult> result = new ArrayList<GdbMiResult>();

        Pattern p = Pattern.compile(
                "(?:reason=\"([^\"]+)\")," +
                        "(?:frame=\\{([^\\}].+)\\})," +
                        "(?:thread-id=\"([^\"]+)\")," +
                        "(?:stopped-threads=\"([^\"]+)\")," +
                        "(?:core=\"(\\d+)\")"
        );
        Matcher m = p.matcher(line);

        if (!m.find()) {
            return result;
        }

        // reason="end-stepping-range"
        GdbMiResult reasonVal = new GdbMiResult("reason");
        reasonVal.value.type = GdbMiValue.Type.String;
        reasonVal.value.string = m.group(1);
        result.add(reasonVal);

        // frame={*}
        result.add(parseBreakpointHitLineFrameLine(m.group(2)));

        // thread-id="1"
        GdbMiResult threadIdVal = new GdbMiResult("thread-id");
        threadIdVal.value.type = GdbMiValue.Type.String;
        threadIdVal.value.string = m.group(3);
        result.add(threadIdVal);

        // stopped-threads="all"
        GdbMiResult stoppedThreadsVal = new GdbMiResult("stopped-threads");
        stoppedThreadsVal.value.type = GdbMiValue.Type.String;
        stoppedThreadsVal.value.string = m.group(4);
        result.add(stoppedThreadsVal);

        // core="6"
        GdbMiResult coreVal = new GdbMiResult("core");
        coreVal.value.type = GdbMiValue.Type.String;
        coreVal.value.string = m.group(5);
        result.add(coreVal);

        return result;
    }

    private static GdbMiResult parseNumChildChildsLine(String line) {
        GdbMiResult result = new GdbMiResult("children");
        result.value.type = GdbMiValue.Type.List;
        result.value.list = new GdbMiList();
        result.value.list.type = GdbMiList.Type.Results;
        result.value.list.results = new ArrayList<GdbMiResult>();

        Pattern p = Pattern.compile(
                "(?:child=\\{" +
                        "(?:name=\"([^\"]+)\")," +
                        "(?:exp=\"([^\"]+)\")," +
                        "(?:numchild=\"(\\d+)\")," +
                        "(?:value=\"([^\"].*?)\")," +
                        "(?:type=\"([^\"]+)\")," +
                        "(?:thread-id=\"([^\"]+)\")" +
                        "\\})+"
        );
        Matcher m = p.matcher(line);

        Pattern stringP = Pattern.compile("0x\\w+\\s(?:<(?:[^>].+?)>\\s)?\\\\\"(.*)");
        Matcher stringM;

        while (m.find()) {
            GdbMiResult childVal = new GdbMiResult("child");
            childVal.value.type = GdbMiValue.Type.Tuple;
            childVal.value.tuple = new ArrayList<GdbMiResult>();

            GdbMiResult nameVal = new GdbMiResult("name");
            nameVal.value.type = GdbMiValue.Type.String;
            nameVal.value.string = m.group(1);
            childVal.value.tuple.add(nameVal);

            GdbMiResult expVal = new GdbMiResult("exp");
            expVal.value.type = GdbMiValue.Type.String;
            expVal.value.string = m.group(2);
            childVal.value.tuple.add(expVal);

            GdbMiResult numChildVal = new GdbMiResult("numchild");
            numChildVal.value.type = GdbMiValue.Type.String;
            numChildVal.value.string = m.group(3);
            childVal.value.tuple.add(numChildVal);

            GdbMiResult valueVal = new GdbMiResult("value");
            valueVal.value.type = GdbMiValue.Type.String;
            valueVal.value.string = m.group(4);
            stringM = stringP.matcher(valueVal.value.string);
            if (stringM.find()) {
                valueVal.value.string = stringM.group(1).substring(0, stringM.group(1).length() - 2);
            }
            childVal.value.tuple.add(valueVal);

            GdbMiResult typeVal = new GdbMiResult("type");
            typeVal.value.type = GdbMiValue.Type.String;
            typeVal.value.string = m.group(5);
            childVal.value.tuple.add(typeVal);

            GdbMiResult threadIdVal = new GdbMiResult("thread-id");
            threadIdVal.value.type = GdbMiValue.Type.String;
            threadIdVal.value.string = m.group(6);
            childVal.value.tuple.add(threadIdVal);

            result.value.list.results.add(childVal);
        }

        return result;
    }

    private static Collection<GdbMiResult> parseNumChildLine(String line) {
        Collection<GdbMiResult> result = new ArrayList<GdbMiResult>();

        Pattern p = Pattern.compile(
                "(?:numchild=\"([^\"]+)\")," +
                        "(?:children=\\[((?!\\],has_more).+?)\\])," +
                        "(?:has_more=\"([^\"]+)\")"
        );
        Matcher m = p.matcher(line);

        // numchild="2"
        if (!m.find()) {
            return result;
        }

        GdbMiResult numChildVal = new GdbMiResult("numchild");
        numChildVal.value.type = GdbMiValue.Type.String;
        numChildVal.value.string = m.group(1);
        result.add(numChildVal);

        result.add(parseNumChildChildsLine(m.group(2)));

        // has_more="0"
        GdbMiResult hasMoreVal = new GdbMiResult("has_more");
        hasMoreVal.value.type = GdbMiValue.Type.String;
        hasMoreVal.value.string = m.group(3);
        result.add(hasMoreVal);

        return result;
    }

    private static GdbMiResult parseArgsLine(String line) {
        // args=[{name="i",value="0x0"}]

        GdbMiResult result = new GdbMiResult("args");
        result.value.type = GdbMiValue.Type.List;
        result.value.list = new GdbMiList();
        result.value.list.type = GdbMiList.Type.Values;
        result.value.list.values = new ArrayList<GdbMiValue>();

        Pattern p = Pattern.compile(
                "(?:\\{(?:name=\"([^\"]+)\")," +
                        "(?:value=\"([^\"\\}].*?)\")" +
                        "\\})+"
        );
        Matcher m = p.matcher(line);

        while (m.find()) {
            GdbMiValue varVal = new GdbMiValue(GdbMiValue.Type.Tuple);
            varVal.tuple = new ArrayList<GdbMiResult>();

            GdbMiResult varNameVal = new GdbMiResult("name");
            varNameVal.value.type = GdbMiValue.Type.String;
            varNameVal.value.string = m.group(1);
            varVal.tuple.add(varNameVal);


            GdbMiResult valueVal = new GdbMiResult("value");
            valueVal.value.type = GdbMiValue.Type.String;
            valueVal.value.string = m.group(2);
            varVal.tuple.add(valueVal);

            result.value.list.values.add(varVal);
        }

        return result;
    }

    private static Collection<GdbMiResult> parseSignalReceivedLine(String line) {
        Collection<GdbMiResult> result = new ArrayList<GdbMiResult>();

        Pattern p = Pattern.compile(
                "(?:reason=\"([^\"]+)\")," +
                        "(?:signal-name=\"([^\"]+)\")," +
                        "(?:signal-meaning=\"([^\"]+)\")," +
                        "(?:frame=\\{([^\\}].+?)\\})," +
                        "(?:thread-id=\"([^\"]+)\")," +
                        "(?:stopped-threads=\"([^\"]+)\")," +
                        "(?:core=\"(\\d+)\")"
        );
        Matcher m = p.matcher(line);

        if (!m.find()) {
            return result;
        }

        // reason="signal-received",
        GdbMiResult reasonVal = new GdbMiResult("reason");
        reasonVal.value.type = GdbMiValue.Type.String;
        reasonVal.value.string = m.group(1);
        result.add(reasonVal);

        // signal-name="SIGSEGV",
        GdbMiResult signalNameVal = new GdbMiResult("signal-name");
        signalNameVal.value.type = GdbMiValue.Type.String;
        signalNameVal.value.string = m.group(2);
        result.add(signalNameVal);

        // signal-meaning="Segmentation fault",
        GdbMiResult signalMeaningVal = new GdbMiResult("signal-meaning");
        signalMeaningVal.value.type = GdbMiValue.Type.String;
        signalMeaningVal.value.string = m.group(3);
        result.add(signalMeaningVal);

        // frame={*}
        result.add(parseBreakpointHitLineFrameLine(m.group(4)));

        // thread-id="1",
        GdbMiResult threadIdVal = new GdbMiResult("thread-id");
        threadIdVal.value.type = GdbMiValue.Type.String;
        threadIdVal.value.string = m.group(5);
        result.add(threadIdVal);

        // stopped-threads="all",
        GdbMiResult stoppedThreadsVal = new GdbMiResult("stopped-threads");
        stoppedThreadsVal.value.type = GdbMiValue.Type.String;
        stoppedThreadsVal.value.string = m.group(6);
        result.add(stoppedThreadsVal);

        // core="1"
        GdbMiResult coreVal = new GdbMiResult("core");
        coreVal.value.type = GdbMiValue.Type.String;
        coreVal.value.string = m.group(7);
        result.add(coreVal);

        return result;
    }

    private static Collection<GdbMiResult> parseFunctionFinishedLine(String line) {
        return parseEndSteppingRangeLine(line);
    }

    private static Collection<GdbMiResult> parseLocationReachedLine(String line) {
        return parseEndSteppingRangeLine(line);
    }

    private static Collection<GdbMiResult> parseStoppedFrameLine(String line) {
        Collection<GdbMiResult> result = new ArrayList<GdbMiResult>();

        Pattern p = Pattern.compile(
                "(?:frame=\\{([^\\}].+?)\\})," +
                        "(?:thread-id=\"([^\"]+)\")," +
                        "(?:stopped-threads=\"([^\"]+)\")," +
                        "(?:core=\"(\\d+)\")"
        );
        Matcher m = p.matcher(line);

        if (!m.find()) {
            return result;
        }

        // frame={*}
        result.add(parseBreakpointHitLineFrameLine(m.group(1)));

        // thread-id="1",
        GdbMiResult threadIdVal = new GdbMiResult("thread-id");
        threadIdVal.value.type = GdbMiValue.Type.String;
        threadIdVal.value.string = m.group(2);
        result.add(threadIdVal);

        // stopped-threads="all",
        GdbMiResult stoppedThreadsVal = new GdbMiResult("stopped-threads");
        stoppedThreadsVal.value.type = GdbMiValue.Type.String;
        stoppedThreadsVal.value.string = m.group(3);
        result.add(stoppedThreadsVal);

        // core="1"
        GdbMiResult coreVal = new GdbMiResult("core");
        coreVal.value.type = GdbMiValue.Type.String;
        coreVal.value.string = m.group(4);
        result.add(coreVal);

        return result;
    }

    private static Collection<GdbMiResult> parseStoppedExitedLine(String line) {
        Collection<GdbMiResult> result = new ArrayList<GdbMiResult>();

        Pattern p = Pattern.compile(
                "(?:reason=\"([^\"]+)\")," +
                        "(?:exit-code=\"([^\"]+)\")"
        );
        Matcher m = p.matcher(line);

        if (!m.find()) {
            return result;
        }

        // reason="exited",
        GdbMiResult reasonVal = new GdbMiResult("reason");
        reasonVal.value.type = GdbMiValue.Type.String;
        reasonVal.value.string = m.group(1);
        result.add(reasonVal);

        // exit-code="02",
        GdbMiResult exitCodeVal = new GdbMiResult("exit-code");
        exitCodeVal.value.type = GdbMiValue.Type.String;
        exitCodeVal.value.string = m.group(2);
        result.add(exitCodeVal);

        return result;
    }

}
