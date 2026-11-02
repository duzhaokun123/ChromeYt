package android.util;

/**
 * android.jar 在本地 JVM 单元测试里只提供会抛 "not mocked" 的空壳实现，
 * 这里提供一个可用的替身，让被测代码里的日志调用正常返回默认值。
 */
public final class Log {

  private Log() {}

  public static int v(String tag, String msg) {
    return 0;
  }

  public static int v(String tag, String msg, Throwable tr) {
    return 0;
  }

  public static int d(String tag, String msg) {
    return 0;
  }

  public static int d(String tag, String msg, Throwable tr) {
    return 0;
  }

  public static int i(String tag, String msg) {
    return 0;
  }

  public static int i(String tag, String msg, Throwable tr) {
    return 0;
  }

  public static int w(String tag, String msg) {
    return 0;
  }

  public static int w(String tag, String msg, Throwable tr) {
    return 0;
  }

  public static int w(String tag, Throwable tr) {
    return 0;
  }

  public static int e(String tag, String msg) {
    return 0;
  }

  public static int e(String tag, String msg, Throwable tr) {
    return 0;
  }

  public static int wtf(String tag, String msg) {
    return 0;
  }

  public static int wtf(String tag, String msg, Throwable tr) {
    return 0;
  }

  public static int wtf(String tag, Throwable tr) {
    return 0;
  }

  public static int println(int priority, String tag, String msg) {
    return 0;
  }

  public static boolean isLoggable(String tag, int level) {
    return false;
  }

  public static String getStackTraceString(Throwable tr) {
    return "";
  }
}
