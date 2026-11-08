package android.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * android.jar 在本地 JVM 单元测试里只提供会抛 "not mocked" 的空壳实现，
 * 这里提供一个可用的替身：日志按 logcat 风格打印到标准输出，方便在测试结果里查看。
 */
public final class Log {

  private Log() {}

  private static int print(String level, String tag, String msg, Throwable tr) {
    StringBuilder line = new StringBuilder();
    line.append(level).append('/').append(tag).append(": ").append(msg);
    if (tr != null) {
      line.append('\n').append(getStackTraceString(tr));
    }
    System.out.println(line);
    return 0;
  }

  private static String levelName(int priority) {
    switch (priority) {
      case 2:
        return "V";
      case 3:
        return "D";
      case 4:
        return "I";
      case 5:
        return "W";
      case 6:
        return "E";
      case 7:
        return "A";
      default:
        return "?";
    }
  }

  public static int v(String tag, String msg) {
    return print("V", tag, msg, null);
  }

  public static int v(String tag, String msg, Throwable tr) {
    return print("V", tag, msg, tr);
  }

  public static int d(String tag, String msg) {
    return print("D", tag, msg, null);
  }

  public static int d(String tag, String msg, Throwable tr) {
    return print("D", tag, msg, tr);
  }

  public static int i(String tag, String msg) {
    return print("I", tag, msg, null);
  }

  public static int i(String tag, String msg, Throwable tr) {
    return print("I", tag, msg, tr);
  }

  public static int w(String tag, String msg) {
    return print("W", tag, msg, null);
  }

  public static int w(String tag, String msg, Throwable tr) {
    return print("W", tag, msg, tr);
  }

  public static int w(String tag, Throwable tr) {
    return print("W", tag, "", tr);
  }

  public static int e(String tag, String msg) {
    return print("E", tag, msg, null);
  }

  public static int e(String tag, String msg, Throwable tr) {
    return print("E", tag, msg, tr);
  }

  public static int wtf(String tag, String msg) {
    return print("A", tag, msg, null);
  }

  public static int wtf(String tag, String msg, Throwable tr) {
    return print("A", tag, msg, tr);
  }

  public static int wtf(String tag, Throwable tr) {
    return print("A", tag, "", tr);
  }

  public static int println(int priority, String tag, String msg) {
    return print(levelName(priority), tag, msg, null);
  }

  public static boolean isLoggable(String tag, int level) {
    return true;
  }

  public static String getStackTraceString(Throwable tr) {
    if (tr == null) {
      return "";
    }
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    tr.printStackTrace(pw);
    pw.flush();
    return sw.toString();
  }
}
