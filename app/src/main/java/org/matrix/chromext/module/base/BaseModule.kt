package org.matrix.chromext.module.base

import org.matrix.chromext.logger.AndroidLogger
import org.matrix.chromext.logger.Logger

abstract class BaseModule(val id: String, vararg val process: String = [PROCESS_MAIN]) {
  companion object {
    const val PROCESS_MAIN = ""
  }

  val logger = object : Logger {
    override fun writeText(level: Logger.Level, text: String) {
      AndroidLogger.writeText(level, "[$id] $text")
    }
  }

  abstract fun load()
}