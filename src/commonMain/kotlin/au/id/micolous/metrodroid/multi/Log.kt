package au.id.micolous.metrodroid.multi

expect object Log {
    fun d(tag: String, msg: String)
    fun d(tag: String, msg: String, exception: Throwable)

    fun e(tag: String, msg: String)
    fun e(tag: String, msg: String, exception: Throwable)

    fun w(tag: String, msg: String)
    fun w(tag: String, msg: String, exception: Throwable)

    fun i(tag: String, msg: String)
    fun i(tag: String, msg: String, exception: Throwable)
}
