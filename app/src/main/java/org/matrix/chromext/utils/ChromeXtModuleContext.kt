package org.matrix.chromext.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.view.View
import java.util.Collections


/**
 * a wrapper to make dynamic load id resource possible
 *
 * ```kotlin
 * val id = ChromeXtModuleContext.addStringRes("some string")
 *
 * // some code need a context to load resource
 * context = ChromeXtModuleContext.ofContext(baseContext)
 * context.getString(id)
 *
 * // remeber to clean up after use
 * ChromeXtModuleContext.removeStringRes(id)
 * ```
 */
object ChromeXtModuleContext {
  private val stringRes = Collections.synchronizedSet(hashSetOf<Pair<Int, String>>())

  fun ofContext(context: Context): Context {
    val resources = context.resources
    return object : ContextWrapper(context) {
      val resourcesWrapper = object : Resources(resources.assets, resources.displayMetrics, resources.configuration) {
        override fun getString(getId: Int): String {
          synchronized(stringRes) {
            stringRes.find { (id, _) ->
              id == getId
            }?.let { (_, res) ->
              return res
            }
          }
          return super.getString(getId)
        }
      }

      override fun getResources(): Resources {
        return resourcesWrapper
      }
    }
  }

  fun addStringRes(res: String): Int {
    val id = View.generateViewId()
    synchronized(stringRes) {
      stringRes.add(id to res)
    }
    return id
  }

  fun removeStringRes(id: Int) {
    synchronized(stringRes) {
      stringRes.removeIf { res -> res.first == id }
    }
  }
}