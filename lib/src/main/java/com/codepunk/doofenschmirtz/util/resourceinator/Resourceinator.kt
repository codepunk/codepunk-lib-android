/*
 * Copyright (C) 2018 Codepunk, LLC
 * Author(s): Scott Slater
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codepunk.doofenschmirtz.util.resourceinator

import android.os.AsyncTask
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.Executor

/**
 * An implementation of [AsyncTask] that wraps [Progress] and [Result] in a [Resource]
 * sealed class and sets it to a [MutableLiveData] instance.
 */
abstract class Resourceinator<Params, Progress, Result>(

    /**
     * An optional bundle of data with which to initialize the [Resourceinator].
     */
    data: Bundle? = null

) : AsyncTask<Params, Progress, ResultResource<Progress, Result>>() {

    // region Properties

    /**
     * An optional [Bundle] for holding additional data. This allows, for example, not only
     * progress publication using [Progress], but also additional information along with
     * that progress by putting the desired information into [data] prior to calling
     * [publishProgress].
     */
    private var _data: Bundle? = data

    /**
     * A public, non-nullable wrapper for [_data] that will self-initialize upon first reference.
     */
    val data: Bundle
        get() = _data ?: Bundle().apply { _data = this }

    /**
     * A [LiveData] that will contain progress, results, or exceptions related to this task.
     */
    @Suppress("WEAKER_ACCESS")
    val liveResource: MutableLiveData<Resource<Progress, Result>> =
        MutableLiveData<Resource<Progress, Result>>().apply {
            value = PendingResource(_data)
        }

    // endregion Properties

    // region Inherited methods

    /**
     * Publishes progress without any data. This will initialize the value in [liveResource] to
     * an empty [ProgressResource] instance.
     */
    override fun onPreExecute() {
        onProgressUpdate()
    }

    /**
     * Updates [liveResource] with a [ProgressResource] instance describing this task's progress.
     */
    override fun onProgressUpdate(vararg values: Progress?) {
        liveResource.value = ProgressResource(values, _data)
    }

    /**
     * Updates [liveResource] with the result from [doInBackground].
     */
    override fun onPostExecute(result: ResultResource<Progress, Result>?) {
        liveResource.value = result
    }

    /**
     * Updates [liveResource] with the result from [doInBackground] if the task was cancelled.
     */
    override fun onCancelled(result: ResultResource<Progress, Result>?) {
        liveResource.value = result
    }

    // endregion Inherited methods

    // region Methods

    /**
     * Convenience method for executing this task and getting the results as [LiveData]. Executes
     * this task with [params] and returns [liveResource] for observation.
     */
    @Suppress("UNUSED")
    fun executeAsLiveData(vararg params: Params): LiveData<Resource<Progress, Result>> {
        execute(*params)
        return liveResource
    }

    /**
     * Convenience method for executing this task and getting the results as [LiveData]. Executes
     * this task on the supplied [Executor] [exec] with [params] and returns [liveResource] for
     * observation.
     */
    @Suppress("UNUSED")
    fun executeOnExecutorAsLiveData(
        exec: Executor = THREAD_POOL_EXECUTOR,
        vararg params: Params
    ): LiveData<Resource<Progress, Result>> {
        executeOnExecutor(exec, *params)
        return liveResource
    }

    // endregion Methods

}
