package org.dbtag.socketComs

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.*
import java.util.concurrent.Executor
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.EmptyCoroutineContext

typealias GetBuffer = () -> ByteArray


data class ReaderAndMs(val reader: BinaryReader, val ms: Long)

//class SendReceiveQueueHandlesBadToken(server: String, port: Int, executor: Executor, useBatching: Boolean)
//    : SendReceiveQueue(server, port, executor, useBatching) {
//
//    override fun queue(getBuffer: GetBuffer, cont: Continuation<ReaderAndMs>) {
//
//    }
//}

/**
 * A socket that serializes (queues) requests made to it.
 */
open class SendReceiveQueue(server: String, port: Int, executor: Executor, private val useBatching: Boolean) {
//    init {
//        Log.i("SendReceiveQueue", "ctor")
//    }
    private val socket = SendReceiveSocket(server, port, executor)

    private class Q(val getBuffer: GetBuffer, val cont: Continuation<ReaderAndMs>)
    private val queue = ArrayDeque<Q>()
    private var active = false

    private var needsProd: Boolean = false

    // This is the main thing
    open fun queue(getBuffer: GetBuffer, cont: Continuation<ReaderAndMs>) {
        val canDefer = true  // some experimental system for combining stuff
        synchronized(this) {
            queue.add(Q(getBuffer, cont))
            if (active)
                return
            if (canDefer) {
                if (!needsProd) {
                    handler_.post(prodTimer)
                    needsProd = true
                }
                return
            }
            active = true
        }
        startAgain()
    }

    private val prodTimer = object : Runnable {
        override fun run() {
            synchronized(this) {
                needsProd = false
                if (active || queue.isEmpty())
                    return
                active = true
            }
            startAgain()
        }
    }

    private fun getJobs(): Array<Q>? {
        synchronized(this) {
            // There may be many queueing up, and potentially we may decide to
            // only send some, not all, to not over-burden the wire
            var n = queue.size
            if (n != 0) {
                //if (n > 5)
                //  Log.i("qc", Integer.toString(n));
                if (!useBatching)
                    n = 1
                return Array(n, { queue.remove() })
            }
        }
        return null
    }

    internal fun startAgain() {
        val jobs = getJobs()
        if (jobs == null) {
            active = false
            return
        }

        // Get all the buffers outside the sync
        val qw = try { Array(jobs.size, { jobs[it].getBuffer() } ) }
        catch (ex: Exception) {// because of an exception getting one of the buffers
            for (job in jobs) try { job.cont.resumeWithException(ex) } catch (ex2: Exception) { }
            return
        }

        // Make the async call
        var ofs = 0
        socket.post(qw, object : Continuation<BinaryReader> {
            override val context get() = EmptyCoroutineContext

            val startTime = SystemClock.elapsedRealtime()  // time it
            var endTime: Long = 0

            override fun resume(value: BinaryReader) {
                if (endTime == 0L)
                    endTime = SystemClock.elapsedRealtime()
                try { jobs[ofs].cont.resume(ReaderAndMs(value.unCompressReader(), endTime - startTime)) } catch (ex2: Exception) { }
                if (++ofs == jobs.size)
                    startAgain() // we're done, so go look for another job
            }

            override fun resumeWithException(exception: Throwable) {
                if (endTime == 0L)
                    endTime = SystemClock.elapsedRealtime()
                try { jobs[ofs].cont.resumeWithException(exception)} catch (ex2: Exception) { }

                if (++ofs == jobs.size)
                    startAgain() // we're done, so go look for another job
            }
        })
//
//
//        { ex, reader ->
//            try { jobs[ofs].processReader(ex, reader.unCompressReader()) } catch (ex2: Exception) { }
//
//            if (++ofs == jobs.size)
//                startAgain() // we're done, so go look for another job
//        })
    }

    companion object {
        private val handler_ = Handler(Looper.getMainLooper())
    }

}

private class Queue <T>{
    private var items = mutableListOf<T>()
    override fun toString() = items.toString()

    fun isEmpty() = items.isEmpty()
    val size get() = items.size

    fun enqueue(element: T) { items.add(element) }
    fun dequeue() = items.removeAt(0)
}
