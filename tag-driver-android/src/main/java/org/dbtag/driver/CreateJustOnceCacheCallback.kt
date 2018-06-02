package org.dbtag.driver

import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine

typealias Creator<TKey, TValue> = (TKey, Continuation<TValue>) -> Unit

class CreateJustOnceCacheCallback<TKey, TValue>(private val creator_: Creator<TKey, TValue>) {
    private val cache = HashMap<TKey, ValueWithPendingContinuations<TValue>>()

    fun remove(key: TKey) {
        synchronized(cache) {
            cache.remove(key)
        }
    }

    fun getIfCached(key: TKey): TValue? {
        synchronized(cache) {
            val x = cache[key] ?: return null
            return x.result
        }
    }

    suspend fun get(key: TKey, creator: Creator<TKey, TValue>? = null) : TValue? = suspendCoroutine { cont ->
        // There are 3 possibilities : no-one is working on it so we should start, someone is already working so we should wait,
        // or the answer is already known.
        var weAreCreating = false
        val x = synchronized(cache) {
            cache[key].let {
                it?: ValueWithPendingContinuations<TValue>().apply {
                    cache[key] = this
                    weAreCreating = true
                }
            }
        }
        x.addContinuation(cont)  // will call the callback right away if the value is already available ?????
        if (weAreCreating) {
            creator?:creator_(key, object : Continuation<TValue> {
                override val context = EmptyCoroutineContext
                override fun resume(value: TValue) = x.set(null, value)
                override fun resumeWithException(exception: Throwable) {
                    @Suppress("ReplaceGetOrSet")
                    x.set(exception as Exception,  null)
                    // An exception was signalled during the create
                    synchronized(cache) {
                        cache.remove(key)  // so this will need fetching fresh next time
                    }
                }
            })
        }
    }

    private class ValueWithPendingContinuations<TValue> {
        private var ex: Exception? = null
        internal var result: TValue? = null
        private var pending: MutableList<Continuation<TValue?>>? = ArrayList()  // cleared when value is set

        internal fun addContinuation(cont: Continuation<TValue?>) {
            synchronized(this) {
                pending?.let{
                    it.add(cont)
                    return
                }
            }
            // Drop through here if pending is null
            resume(cont)  // already done
        }

        private fun resume(cont: Continuation<TValue?>) {
            val ex = ex
            if (ex != null)
                cont.resumeWithException(ex)
            else
                cont.resume(result)
        }

        internal operator fun set(ex: Exception?, result: TValue?) {
            this.ex = ex
            this.result = result
            // Call any pending
            synchronized(this) { pending.apply { pending = null } }?.let {
                for (cont in it)
                    resume(cont)
            }
        }
    }
}
