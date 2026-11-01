package org.matrix.chromext.utils.reflect

import java.lang.reflect.Method

fun Any.invokeMethod(
  method: Method,
  vararg args: Any?
): Any? {
  return method.invoke(this, *args)
}

@Suppress("UNCHECKED_CAST")
fun <T> Any.invokeMethodAs(
  method: Method,
  vararg args: Any?,
): T {
  return invokeMethod(method, *args) as T
}
