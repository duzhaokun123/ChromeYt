package org.matrix.chromext.utils.reflect

import org.luckypray.dexkit.result.MethodData
import org.luckypray.dexkit.wrap.DexMethod
import org.matrix.chromext.Chrome
import java.lang.reflect.Method

fun Class<*>.findMethodOrNull(findSuper: Boolean = true, filter: (Method) -> Boolean): Method? {
  declaredMethods.forEach { method ->
    if (filter(method)) {
      method.isAccessible = true
      return method
    }
  }
  if (findSuper) {
    superclass?.let { superClass ->
      return superClass.findMethodOrNull(true, filter)
    }
  }
  return null
}

fun Class<*>.findMethod(findSuper: Boolean = true, filter: (Method) -> Boolean): Method {
  return findMethodOrNull(findSuper, filter)
    ?: throw NoSuchMethodException("No method found in ${this.name}")
}

fun Class<*>.findAllMethods(findSuper: Boolean = true, filter: (Method) -> Boolean): List<Method> {
  val methods = mutableListOf<Method>()
  declaredMethods.forEach { method ->
    if (filter(method)) {
      method.isAccessible = true
      methods.add(method)
    }
  }
  if (findSuper) {
    superclass?.let { superClass ->
      methods.addAll(superClass.findAllMethods(true, filter))
    }
  }
  return methods
}

fun MethodData.toMethod(): Method {
  return this.getMethodInstance(Chrome.getContext().classLoader)
}

fun DexMethod.toMethod(): Method {
  return this.getMethodInstance(Chrome.getContext().classLoader)
}

fun Class<*>.getDeclaredMethodOrNull(
  name: String, vararg parameterTypes: Class<*>
): Method? {
  return runCatching { getDeclaredMethod(name, *parameterTypes) }.getOrNull()
}

fun Method.invokeStatic(vararg args: Any?): Any? = invoke(null, *args)

fun loadMethod(signature: String): Method {
  return DexMethod.deserialize(signature).toMethod()
}

fun loadMethodOrNull(signature: String): Method? {
  return runCatching { loadMethod(signature) }.getOrNull()
}