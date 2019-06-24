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
import android.app.ProgressDialog
import android.os.AsyncTask
import android.text.TextUtils
import android.util.Log
import android.widget.ProgressBar

import au.id.micolous.farebot.R

abstract class BetterAsyncTask<Result> @JvmOverloads constructor(protected val mActivity: Activity, showLoading: Boolean = true, loadingText: String? = null, private val mFinishOnError: Boolean = false) : AsyncTask<Void, ProgressBar, BetterAsyncTask.TaskResult<Result>>() {

    private var mProgressDialog: ProgressDialog? = null

    constructor(activity: Activity, showLoading: Boolean, finishOnError: Boolean) : this(activity, showLoading, null, finishOnError)

    init {
        if (showLoading) {
            mProgressDialog = ProgressDialog(mActivity).also {
                it.setCancelable(false)
                it.isIndeterminate = true
            }
            setLoadingText(loadingText)
        }
    }

    fun cancelIfRunning() {
        if (status != Status.FINISHED) {
            super.cancel(true)
        }

        mProgressDialog?.also {
            it.dismiss()
            mProgressDialog = null
        }
    }

    private fun setLoadingText(text: String?) {
        mProgressDialog?.setMessage(text?.ifEmpty { null } ?: mActivity.getString(R.string.loading))
    }

    override fun doInBackground(vararg unused: Void): TaskResult<Result> {
        try {
            return TaskResult(doInBackground())
        } catch (e: Exception) {
            Log.e(TAG, "Error in task:", e)
            return TaskResult(e)
        }

    }

    override fun onPreExecute() {
        super.onPreExecute()
        mProgressDialog?.show()
    }

    override fun onPostExecute(result: TaskResult<Result>) {
        mProgressDialog?.dismiss()
        result.exception?.also {
            onError(it)
            return
        }

        onResult(result.`object`)
    }

    private fun onError(ex: Exception) {
        val dialog = AlertDialog.Builder(mActivity)
                .setTitle(R.string.error)
                .setMessage(ex.toString())
                .setPositiveButton(android.R.string.ok, null)
                .create()
        if (mFinishOnError) {
            dialog.setOnDismissListener { mActivity.finish() }
        }
        dialog.show()
    }

    protected abstract fun doInBackground(): Result

    protected abstract fun onResult(result: Result?)

    class TaskResult<T> {
        val `object`: T?
        val exception: Exception?

        constructor(`object`: T) {
            this.`object` = `object`
            exception = null
        }

        constructor(exception: Exception) {
            this.exception = exception
            `object` = null
        }
    }

    companion object {
        private const val TAG = "BetterAsyncTask"
    }
}
