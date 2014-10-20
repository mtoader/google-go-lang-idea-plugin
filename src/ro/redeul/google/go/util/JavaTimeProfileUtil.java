package ro.redeul.google.go.util;

import java.util.*;
import java.util.Map.Entry;

/**
 * Created by bronze1man on 14-10-17.
 * a util class used for java time profile.It can only used in a single thread.
 */
public class JavaTimeProfileUtil {
    /**
     * a simple time record function
     * example:
     *  JavaTimeProfileUtil.simple(null); //null will be ignore or as a start of a timer.
     *  doCheckFile((GoFile) file, result);
     *  JavaTimeProfileUtil.simple(this.getClass().getSimpleName()); //print time from last simple to this one.
     */
    public static void simple(String name){
        long thisTime = System.nanoTime();
        long during = thisTime-simpleLastTime;
        simpleLastTime = thisTime;
        if (name==null) {
            return;
        }
        System.out.println("JavaTimeProfileUtil "+name+" "+((double)during/1e9));
    }
    private static long simpleLastTime =0;

    /**
     * a loop profile class
     * example:
     *      LoopProfile.start();
     *      for (xxx){
     *          LoopProfile.point("a");
     *          xxx
     *          LoopProfile.point("b");
     *          xxx
     *          LoopProfile.point("c");
     *      }
     *      LoopProfile.end(); //you will get a report here.
     * report:
     *    ----------Start JavaTimeProfileUtil.LoopProfile
     *    checkFunctionCallArguments start - checkFunctionCallArguments end : 0.525156
     *    visitBuiltinCallExpression start - visitBuiltinCallExpression end : 0.018451
     *    checkFunctionCallArguments end - checkFunctionCallArguments start : 0.001077
     *    start - checkFunctionCallArguments start : 4.25E-4
     *    visitBuiltinCallExpression end - checkFunctionCallArguments start : 3.3E-5
     *    checkFunctionCallArguments end - visitBuiltinCallExpression start : 2.1E-5
     *    checkFunctionCallArguments end - finish : 1.8E-5
     *    visitBuiltinCallExpression end - visitBuiltinCallExpression start : 1.2E-5
     *    ----------End JavaTimeProfileUtil.LoopProfile
     */
    public static class LoopProfile {
        public static void start() {
            pointTimeDuringMap = new HashMap<String, Long>();
            pointLastPoint = "start";
            pointLastTime = System.nanoTime();
        }

        public static void end() {
            point("finish");
            ArrayList<Entry<String, Long>> list = new ArrayList<Entry<String, Long>>(pointTimeDuringMap.entrySet());
            Collections.sort(list, new Comparator<Entry<String, Long>>() {
                @Override
                public int compare(Entry<String, Long> t1, Entry<String, Long> t2) {
                    return t2.getValue().compareTo(t1.getValue());
                }
            });
            System.out.println("----------Start JavaTimeProfileUtil.LoopProfile");
            for (Entry<String, Long> entry : list) {
                System.out.println(entry.getKey() + " : " + ((double) entry.getValue() / 1e9));
            }
            System.out.println("----------End JavaTimeProfileUtil.LoopProfile");
        }

        public static void point(String pointName) {
            long thisTime = System.nanoTime();
            long during = thisTime - pointLastTime;
            pointLastTime = thisTime;
            String name = pointLastPoint+" - "+pointName;
            pointLastPoint = pointName;
            Long RecordDuring = pointTimeDuringMap.get(name);
            if (RecordDuring==null){
                RecordDuring = 0l;
            }
            RecordDuring += during;
            pointTimeDuringMap.put(name, RecordDuring);
        }

        private static HashMap<String, Long> pointTimeDuringMap = null;
        private static String pointLastPoint = null;
        private static long pointLastTime = 0;
    }
}
