package org.wheatgenetics.utils

import android.os.Handler
import android.view.View
import com.google.android.material.snackbar.Snackbar
import java.util.*
import kotlin.collections.ArrayList

/*
A scheduler for posting Snackbar messages so they don't overlap.
 */
class SnackbarQueue {

    private lateinit var mSnack: Snackbar

    private var isShowing: Boolean = false

    private val mHandler = Handler()

    private var mQueue: ArrayList<SnackJob> = ArrayList()

    private var mLastJob: SnackJob? = null

    init {

        val task = object : TimerTask() {

            override fun run() {

                try {

                    if (mQueue.isNotEmpty() && !isShowing) {

                        isShowing = true

                        val job = mQueue.removeAt(0)

                        var newJob = true

                        mLastJob?.let {

                            if (it.txt == job.txt) newJob = false

                        }

                        if (newJob) {

                            mSnack = Snackbar.make(job.v, job.txt, Snackbar.LENGTH_LONG)

                            mSnack.setAction(job.actionText) {

                                job.action()

                            }

                            mSnack.show()

                            mLastJob = job

                        } else {

                            isShowing = false

                        }
                    }

                } catch (e : IllegalArgumentException) {

                    e.printStackTrace()

                } finally {

                    mHandler.postDelayed({ isShowing = false }, 250)

                    //mHandler.postDelayed({ mLastJob = null }, 10000)
                }
            }
        }

        val reset = object : TimerTask() {

            override fun run() {

                mLastJob = null

            }
        }

        Timer().scheduleAtFixedRate(reset, 0, 1500)
        Timer().scheduleAtFixedRate(task, 0, 1000)

    }

    fun push(job: SnackJob) = mQueue.add(job)

    data class SnackJob(val v: View, val txt: String, val actionText: String = "", val action: () -> Unit = {})
}