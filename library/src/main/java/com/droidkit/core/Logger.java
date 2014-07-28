package com.droidkit.core;

import android.os.Environment;
import android.os.Message;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class Logger {
    public static final boolean ENABLED = true;
    public static final String TAG = "newage";

    private static final boolean WRITE_TO_FILE = false;

    private static Loop loop;

    public static void initialize() {
        if (loop == null) {
            loop = new LogLoop();
            loop.start();
            loop.postMessage(Message.obtain(loop.handler, 0, (int) (System.currentTimeMillis() / 1000L), 0, null), 0);
        }
    }

    public static void v(String msg, Object... args) {
        v(TAG, msg, args);
    }

    public static void v(Throwable t) {
        v(TAG, "Exception thrown", t);
    }

    public static void v(String msg, Throwable t) {
        v(TAG, msg, t);
    }

    public static void v(String tag, String msg, Throwable t) {
        print(Log.VERBOSE, tag, msg, t);
    }

    public static void v(String tag, String msg, Object... args) {
        msg = format(msg, args);
        print(Log.VERBOSE, tag, msg);
    }

    public static void i(String msg, Object... args) {
        i(TAG, msg, args);
    }

    public static void i(String tag, String msg, Object... args) {
        msg = format(msg, args);
        print(Log.INFO, tag, msg);
    }

    public static void d(String msg, Object... args) {
        d(TAG, msg, args);
    }

    public static void d(Throwable t) {
        d(TAG, "Exception thrown", t);
    }

    public static void d(String tag, String msg, Object... args) {
        msg = format(msg, args);
        print(Log.DEBUG, tag, msg);
    }

    public static void d(String tag, String msg, Throwable t) {
        print(Log.DEBUG, tag, msg, t);
    }

    public static void e(String msg, Throwable t) {
        e(TAG, msg, t);
    }

    public static void e(Throwable t) {
        e(TAG, "Exception thrown", t);
    }

    public static void e(String tag, String msg, Object... args) {
        msg = format(msg, args);
        print(Log.ERROR, tag, msg);
    }

    public static void e(String tag, String msg, Throwable t) {
        print(Log.ERROR, tag, msg, t);
    }

    public static void w(String msg, Object... args) {
        w(TAG, msg, args);
    }

    public static void w(Throwable t) {
        w(TAG, "Exception thrown", t);
    }

    public static void w(String msg, Throwable t) {
        w(TAG, msg, t);
    }

    public static void w(String tag, String msg, Object... args) {
        msg = format(msg, args);
        print(Log.WARN, tag, msg);
    }

    public static void w(String tag, String msg, Throwable t) {
        print(Log.WARN, tag, msg, t);
    }

    private static String format(String str, Object[] args) {
        return args.length > 0 ? String.format(str, args) : str;
    }

    private static void print(int level, String tag, String msg) {
        initialize();
        loop.postMessage(Message.obtain(loop.handler, level, new Object[]{tag, msg}), 0);
    }

    private static void print(int level, String tag, String msg, Throwable throwable) {
        initialize();
        loop.postMessage(Message.obtain(loop.handler, level, (int) (System.currentTimeMillis() / 1000l), 0, new Object[]{tag, msg, throwable}), 0);
    }

    static class LogLoop extends Loop {
        private File file;

        public LogLoop() {
            super("LogLoop");
        }

        @Override
        public void processMessage(Message msg) {
            String result;

            result = processString(msg.what, msg.arg1, (Object[]) msg.obj);

            if (WRITE_TO_FILE) {
                try {
                    if (file == null)
                        file = new File(Environment.getExternalStorageDirectory(), "newage.txt");

                    if (!file.exists()) {
                        if (file.createNewFile()) {
                            write(file, result);
                        }
                    } else {
                        write(file, result);
                    }
                } catch (Throwable t) {
                    e(TAG, "Cannot deal with the log file", t);
                }
            }
        }

        private static void write(File file, String str) throws FileNotFoundException, IOException {
            DataOutputStream out;

            out = new DataOutputStream(new FileOutputStream(file));
            out.writeUTF(str);
            out.close();
        }

        private static void formatDate(int t, StringBuilder b) {
            Calendar c;

            int day, month, year;
            int minute, hour;

            c = Calendar.getInstance();
            c.setTimeInMillis((long) t * 1000L);

            day = c.get(Calendar.DAY_OF_MONTH);
            month = c.get(Calendar.MONTH);
            year = c.get(Calendar.YEAR);
            hour = c.get(Calendar.HOUR_OF_DAY);
            minute = c.get(Calendar.MINUTE);

            if (month < 10)
                b.append('0');
            b.append(month);
            b.append('/');
            if (day < 10)
                b.append('0');
            b.append(day);
            b.append('/');
            b.append(year % 100);

            b.append(' ');

            if (hour < 10)
                b.append('0');
            b.append(hour);
            b.append(':');
            if (minute < 10)
                b.append('0');
            b.append(minute);
        }

        private static char getLevelChar(int level) {
            switch (level) {
                case Log.VERBOSE:
                    return 'V';
                case Log.DEBUG:
                    return 'D';
                case Log.ERROR:
                    return '!';
                case Log.WARN:
                    return '?';
                case Log.INFO:
                    return 'I';
                default:
                    return '.';
            }
        }

        private static void log(int level, String tag, String message, Throwable throwable) {
            switch (level) {
                case Log.DEBUG: {
                    if (throwable != null)
                        Log.d(tag, message, throwable);
                    else
                        Log.d(tag, message);

                    break;
                }
                case Log.ERROR: {
                    if (throwable != null)
                        Log.e(tag, message, throwable);
                    else
                        Log.e(tag, message);

                    break;
                }
                case Log.INFO: {
                    if (throwable != null)
                        Log.i(tag, message, throwable);
                    else
                        Log.i(tag, message);

                    break;
                }
                case Log.WARN: {
                    if (throwable != null)
                        Log.w(tag, message, throwable);
                    else
                        Log.w(tag, message);

                    break;
                }
                case Log.VERBOSE:
                default: {
                    if (throwable != null)
                        Log.v(tag, message, throwable);
                    else
                        Log.v(tag, message);

                    break;
                }
            }
        }

        private static String processString(int level, int time, Object[] data) {
            StringBuilder builder;

            if (data != null) {
                String tag = (String) data[0];
                String message = (String) data[1];
                Throwable throwable = data.length >= 3 ? (Throwable) data[2] : null;
                String exception = throwable != null ? Log.getStackTraceString(throwable) : "";

                builder = new StringBuilder(tag.length() + message.length() + exception.length() + 14);
                formatDate(time, builder);
                builder.append(' ');
                builder.append('[');
                builder.append(getLevelChar(level));
                builder.append(']');
                builder.append(' ');
                builder.append('<');
                builder.append(tag);
                builder.append('>');
                builder.append(':');
                builder.append(' ');
                builder.append(message);

                if (exception.length() > 0) {
                    builder.append('\n');
                    builder.append(exception);
                }

                log(level, tag, message, throwable);

                return builder.toString();
            } else {
                String result;

                builder = new StringBuilder(46);
                builder.append("\n\n======== CHECKPOINT ");
                formatDate(time, builder);
                builder.append(" ========\n");

                result = builder.toString();

                log(Log.INFO, TAG, result, null);

                return result;
            }
        }
    }
}
