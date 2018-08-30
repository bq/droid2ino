package com.bq.robotic.droid2ino.utils

import android.os.Handler
import android.os.HandlerThread

/**
 * Utils class for Handlers.
 */
object HandlerUtils {
    /**
     * Create and start a new [Handler] with a looper from a new [HandlerThread] with the name
     * passed by parameter.
     */
    fun createHandler(name: String): Handler {
        val handlerThread = HandlerThread(name)
        handlerThread.start()
        return Handler(handlerThread.looper)
    }
}