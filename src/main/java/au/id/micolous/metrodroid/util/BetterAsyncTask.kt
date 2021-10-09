/*
 * BetterAsyncTask.kt
 *
 * Copyright (C) 2012 Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.id.micolous.metrodroid.util

import android.app.Activity
import android.app.AlertDialog
import android.os.AsyncTask
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import au.id.micolous.farebot.R
import au.id.micolous.metrodroid.multi.Localizer
import java.lang.ref.WeakReference

abstract class BetterAsyncTask<Result> @JvmOverloads constructor(activity: Activity, showLoading: Boolean = true, loadingText: String? = null, private val mFinishOnError: Boolean = false) : AsyncTask<Void, ProgressBar, BetterAsyncTask.TaskResult<Result>>() {

    private val mProgressBar: ProgressBar? = activity.findViewById(R.id.progressbar)
    private val mProgressText: TextView? = activity.findViewById(R.id.progresstext)
    protected val mWeakActivity: WeakReference<Activity> = WeakReference(activity)

    constructor(activity: Activity, showLoading: Boolean, finishOnError: Boolean) : this(activity, showLoading, null, finishOnError)

    init {
        if (showLoading) {
            mProgressBar?.isIndeterminate = true
            setLoadingText(loadingText)
        }
    }

    fun cancelIfRunning() {
        if (status != Status.FINISHED) {
            super.cancel(true)
        }

        mProgressBar?.visibility = View.GONE
        mProgressText?.visibility = View.GONE
    }

    private fun setLoadingText(text: String?) {
        mProgressText?.text = text?.ifEmpty { null } ?: Localizer.localizeString(R.string.loading)
    }

    override fun doInBackground(vararg unused: Void): TaskResult<Result> =
        try {
            TaskResult(doInBackground())
        } catch (e: Exception) {
            Log.e(TAG, "Error in task:", e)
            TaskResult(e)
        }

    override fun onPreExecute() {
        super.onPreExecute()
        mProgressBar?.visibility = View.VISIBLE
        mProgressText?.visibility = View.VISIBLE
    }

    override fun onPostExecute(result: TaskResult<Result>) {
        mProgressBar?.visibility = View.GONE
        mProgressText?.visibility = View.GONE
        result.exception?.also {
            onError(it)
            return
        }

        onResult(result.obj)
    }

    private fun onError(ex: Exception) {
        val activity = mWeakActivity.get() ?: return
        val dialog = AlertDialog.Builder(activity)
                .setTitle(R.string.error)
                .setMessage(ex.toString())
                .setPositiveButton(android.R.string.ok, null)
                .create()
        if (mFinishOnError) {
            dialog.setOnDismissListener { activity.finish() }
        }
        dialog.show()
    }

    protected abstract fun doInBackground(): Result

    protected abstract fun onResult(result: Result?)

    class TaskResult<T> {
        val obj: T?
        val exception: Exception?

        constructor(obj: T) {
            this.obj = obj
            exception = null
        }

        constructor(exception: Exception) {
            this.exception = exception
            obj = null
        }
    }

    companion object {
        private const val TAG = "BetterAsyncTask"
    }
}
