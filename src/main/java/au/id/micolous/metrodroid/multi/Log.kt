package au.id.micolous.metrodroid.multi

actual object Log {
    private fun ignoreExceptions(f: () -> Unit): Unit {
        try {
            f()
        } catch (e: Exception) {
        }
    }

    actual fun d(tag: String, msg: String) = ignoreExceptions {
        android.util.Log.d(tag, msg)
    }
    actual fun i(tag: String, msg: String) = ignoreExceptions {
        android.util.Log.d(tag, msg)
    }
    actual fun e(tag: String, msg: String) = ignoreExceptions {
        android.util.Log.e(tag, msg)
    }
    actual fun w(tag: String, msg: String) = ignoreExceptions {
        android.util.Log.w(tag, msg)
    }

    actual fun d(tag: String, msg: String, exception: Throwable) = ignoreExceptions {
        android.util.Log.d(tag, msg, exception)
    }
    actual fun i(tag: String, msg: String, exception: Throwable) = ignoreExceptions {
        android.util.Log.i(tag, msg, exception)
    }
    actual fun e(tag: String, msg: String, exception: Throwable) = ignoreExceptions {
        android.util.Log.e(tag, msg, exception)
    }
    actual fun w(tag: String, msg: String, exception: Throwable) = ignoreExceptions {
        android.util.Log.w(tag, msg, exception)
    }
}
