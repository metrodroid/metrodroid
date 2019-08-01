package au.id.micolous.metrodroid.multi
import java.util.logging.Level
import java.util.logging.Logger

actual object Log {
    actual fun d(tag: String, msg: String) {
        Logger.getLogger(tag).log (Level.FINE, msg)
    }
    actual fun i(tag: String, msg: String) {
        Logger.getLogger(tag).log (Level.INFO, msg)
    }
    actual fun e(tag: String, msg: String) {
        Logger.getLogger(tag).log (Level.SEVERE, msg)
    }
    actual fun w(tag: String, msg: String) {
        Logger.getLogger(tag).log (Level.WARNING, msg)
    }

    actual fun d(tag: String, msg: String, exception: Throwable) {
        Logger.getLogger(tag).log (Level.FINE, msg, exception)
    }
    actual fun i(tag: String, msg: String, exception: Throwable) {
        Logger.getLogger(tag).log (Level.INFO, msg, exception)
    }
    actual fun e(tag: String, msg: String, exception: Throwable) {
        Logger.getLogger(tag).log (Level.SEVERE, msg, exception)
    }
    actual fun w(tag: String, msg: String, exception: Throwable) {
        Logger.getLogger(tag).log (Level.WARNING, msg, exception)
    }
}