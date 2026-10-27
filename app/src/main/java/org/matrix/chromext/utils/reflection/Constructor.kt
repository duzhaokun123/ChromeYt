package org.matrix.chromext.utils.reflection


import org.luckypray.dexkit.wrap.DexMethod
import org.matrix.chromext.Chrome
import java.lang.reflect.Constructor

fun Class<*>.findConstructorOrNull(filter: (Constructor<*>) -> Boolean): Constructor<*>? {
  return declaredConstructors.firstOrNull(filter)
}

fun Class<*>.findConstructor(filter: (Constructor<*>) -> Boolean): Constructor<*> {
  return findConstructorOrNull(filter)
    ?: throw NoSuchMethodException("No constructor found in ${this.name}")
}

val Constructor<*>.paramCount: Int
  get() = parameterTypes.size

fun DexMethod.toConstructor(): Constructor<*> {
  return this.getConstructorInstance(Chrome.getContext().classLoader)
}

fun loadConstructor(signature: String): Constructor<*> {
  return DexMethod.deserialize(signature).toConstructor()
}

fun loadConstructorOrNull(signature: String): Constructor<*>? {
  return runCatching { loadConstructor(signature) }.getOrNull()
}