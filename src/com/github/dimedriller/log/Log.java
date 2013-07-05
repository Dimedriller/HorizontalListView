package com.github.dimedriller.log;

@SuppressWarnings("StringBufferMayBeStringBuilder")
public class Log {
    private static final String TAG = "ListView";
    private static final boolean DEBUG = true;
    private static final int FIRST_STACK_TRACE_ENTRY_INDEX = 3;
    private static final int STACK_TRACE_MESSAGE_MAX_SYMBOLS_COUNT = 1000;


    public static void d(Object... logItems) {
        //noinspection ConstantConditions,PointlessBooleanExpression
        if (!DEBUG)
            return;

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StringBuffer logString = new StringBuffer(stackTrace[FIRST_STACK_TRACE_ENTRY_INDEX].getMethodName());

        int countItems = logItems.length;
        if (countItems != 0) {
            logString.append(": ").append(logItems[0]);
            for (int counterItem = 1; counterItem < countItems; counterItem++)
                logString.append(", ").append(logItems[counterItem]);
        }
        android.util.Log.d(TAG, logString.toString());
    }

    private static void addToLog(StringBuffer logString, StackTraceElement stackTraceItem) {
        logString.append(stackTraceItem.getClassName())
                .append(".")
                .append(stackTraceItem.getMethodName())
                .append(":")
                .append(stackTraceItem.getLineNumber());
    }

    public static void stackTrace() {
        //noinspection ConstantConditions,PointlessBooleanExpression
        if (!DEBUG)
            return;

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StringBuffer logString = new StringBuffer();
        addToLog(logString, stackTrace[FIRST_STACK_TRACE_ENTRY_INDEX]);
        for(int counterItem = FIRST_STACK_TRACE_ENTRY_INDEX + 1; counterItem < stackTrace.length; counterItem++) {
            if (logString.length() > STACK_TRACE_MESSAGE_MAX_SYMBOLS_COUNT) {
                android.util.Log.d(TAG, logString.toString());
                logString = new StringBuffer();
            } else
                logString.append("\n");
            addToLog(logString, stackTrace[counterItem]);
        }
        android.util.Log.d(TAG, logString.toString());
    }
}
