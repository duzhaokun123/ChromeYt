package org.matrix.chromext.utils

import java.lang.reflect.Field
import java.lang.reflect.Method

typealias MethodCondition = Method.() -> Boolean

typealias FieldCondition = Field.() -> Boolean

@Deprecated("use Class<*>.findMethod")
fun findMethod(clz: Class<*>, findSuper: Boolean = false, condition: MethodCondition): Method {
  return findMethodOrNull(clz, findSuper, condition) ?: throw NoSuchMethodException()
}

@Deprecated("use Class<*>.findMethodOrNull")
fun findMethodOrNull(
    clz: Class<*>,
    findSuper: Boolean = false,
    condition: MethodCondition
): Method? {
  var c = clz
  c.declaredMethods
      .firstOrNull { it.condition() }
      ?.let {
        it.isAccessible = true
        return it
      }

  if (findSuper) {
    while (c.superclass?.also { c = it } != null) {
      c.declaredMethods
          .firstOrNull { it.condition() }
          ?.let {
            it.isAccessible = true
            return it
          }
    }
  }
  return null
}

fun Any.invokeMethod(vararg args: Any?, condition: MethodCondition): Any? {
  findMethodOrNull(this::class.java, true, condition)?.let {
    return it(this, *args)
  }
  throw NoSuchMethodException()
}

@Deprecated("use Class<*>.findField")
fun findField(clz: Class<*>, findSuper: Boolean = false, condition: FieldCondition): Field {
  return findFieldOrNull(clz, findSuper, condition) ?: throw NoSuchFieldException()
}

@Deprecated("use Class<*>.findFieldOrNull")
fun findFieldOrNull(clz: Class<*>, findSuper: Boolean = false, condition: FieldCondition): Field? {
  var c = clz
  c.declaredFields
      .firstOrNull { it.condition() }
      ?.let {
        it.isAccessible = true
        return it
      }

  if (findSuper) {
    while (c.superclass?.also { c = it } != null) {
      c.declaredFields
          .firstOrNull { it.condition() }
          ?.let {
            it.isAccessible = true
            return it
          }
    }
  }
  return null
}
