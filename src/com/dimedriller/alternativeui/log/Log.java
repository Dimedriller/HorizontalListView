package com.dimedriller.alternativeui.log;

@SuppressWarnings("StringBufferMayBeStringBuilder")
public class Log {
    private static final String TAG = "ListView";
    private static final boolean DEBUG = true;
    private static final int FIRST_STACK_TRACE_ENTRY_INDEX = 4;
    private static final int STACK_TRACE_MESSAGE_MAX_SYMBOLS_COUNT = 1000;


    private static void dh(int firstStackTraceEntryIndex, Object obj, Object[] logItems) {
        //noinspection ConstantConditions,PointlessBooleanExpression
        if (!DEBUG)
            return;

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StringBuffer logString = new StringBuffer();
        if (obj != null)
            logString.append(Integer.toHexString(obj.hashCode())).append(" - ");
        logString.append(stackTrace[firstStackTraceEntryIndex].getMethodName());

        int countItems = logItems.length;
        if (countItems != 0) {
            logString.append(": ").append(logItems[0]);
            for (int counterItem = 1; counterItem < countItems; counterItem++)
                logString.append(", ").append(logItems[counterItem]);
        }
        android.util.Log.d(TAG, logString.toString());
    }

    public static void d(Object... logItems) {
        dh(FIRST_STACK_TRACE_ENTRY_INDEX, null, logItems);
    }

    public static void dh(Object obj, Object... logItems) {
        dh(FIRST_STACK_TRACE_ENTRY_INDEX, obj, logItems);
    }

    private static void addToLog(StringBuffer logString, StackTraceElement stackTraceItem) {
        logString.append(stackTraceItem.getClassName())
                .append(".")
                .append(stackTraceItem.getMethodName())
                .append(":")
                .append(stackTraceItem.getLineNumber());
    }

    private static void sth(int firstStackTraceEntryIndex, Object obj) {
        //noinspection ConstantConditions,PointlessBooleanExpression
        if (!DEBUG)
            return;

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StringBuffer logString = new StringBuffer();
        if (obj != null)
            logString.append(Integer.toHexString(obj.hashCode())).append("\n");
        addToLog(logString, stackTrace[firstStackTraceEntryIndex]);
        for(int counterItem = firstStackTraceEntryIndex + 1; counterItem < stackTrace.length; counterItem++) {
            if (logString.length() > STACK_TRACE_MESSAGE_MAX_SYMBOLS_COUNT) {
                android.util.Log.d(TAG, logString.toString());
                logString = new StringBuffer();
            } else
                logString.append("\n");
            addToLog(logString, stackTrace[counterItem]);
        }
        android.util.Log.d(TAG, logString.toString());
    }

    public static void sth(Object obj) {
        sth(FIRST_STACK_TRACE_ENTRY_INDEX, obj);
    }

    public static void st() {
        sth(FIRST_STACK_TRACE_ENTRY_INDEX, null);
    }
}
