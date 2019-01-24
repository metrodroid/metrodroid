package au.id.micolous.metrodroid.multi

actual object Log {
    actual fun d(tag: String, msg: String) {
        android.util.Log.d(tag, msg)
    }
    actual fun i(tag: String, msg: String) {
        android.util.Log.d(tag, msg)
    }
    actual fun e(tag: String, msg: String) {
        android.util.Log.e(tag, msg)
    }
    actual fun w(tag: String, msg: String) {
        android.util.Log.w(tag, msg)
    }

    actual fun d(tag: String, msg: String, exception: Throwable) {
        android.util.Log.d(tag, msg, exception)
    }
    actual fun i(tag: String, msg: String, exception: Throwable) {
        android.util.Log.i(tag, msg, exception)
    }
    actual fun e(tag: String, msg: String, exception: Throwable) {
        android.util.Log.e(tag, msg, exception)
    }
    actual fun w(tag: String, msg: String, exception: Throwable) {
        android.util.Log.w(tag, msg, exception)
    }
}