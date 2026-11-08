package de.robv.android.xposed;

/**
 * Xposed API 在主工程里是 compileOnly，本地 JVM 单元测试的 classpath 上并不存在，
 * 这里提供最小替身：日志打印到标准输出，避免被测代码调用日志时抛 NoClassDefFoundError。
 */
public final class XposedBridge {

  private XposedBridge() {}

  public static void log(String text) {
    System.out.println("Xposed: " + text);
  }

  public static void log(Throwable t) {
    if (t != null) {
      t.printStackTrace(System.out);
    }
  }
}
