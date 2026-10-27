package org.matrix.chromext.utils.reflection

import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.wrap.DexClass
import org.matrix.chromext.Chrome
import org.matrix.chromext.utils.Unsafe

fun loadClass(signature: String): Class<*> {
  if (signature.startsWith('L') && signature.endsWith(';')) {
    return DexClass.deserialize(signature).toClass()
  } else {
    return Chrome.getContext().classLoader.loadClass(signature)
  }
}

fun loadClassOrNull(signature: String): Class<*>? {
  return runCatching { loadClass(signature) }.getOrNull()
}

fun ClassData.toClass(): Class<*> {
  return getInstance(Chrome.getContext().classLoader)
}

fun Class<*>.new(): Any {
  return newInstance()
}

fun Class<*>.newOrNull(): Any? {
  return runCatching { new() }.getOrNull()
}

@Suppress("UNCHECKED_CAST")
fun <T> Class<*>.newAs(): T {
  return new() as T
}

fun DexClass.toClass(): Class<*> {
  return this.getInstance(Chrome.getContext().classLoader)
}

fun Class<*>.allocateInstance(): Any {
  return Unsafe.instance.allocateInstance(this)
}