package org.dbtag.socketComs

import org.dbtag.data.toLowerCaseInvariant
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class SharedClients(val executor: Executor) {
    private val cache = HashMap<ServerAndPort, SendReceiveQueue>()

    fun get(server: String, port: Int): SendReceiveQueue {
        //    return new SendReceiveQueue(computer, port, executor);  // no sharing, for test
        val key = ServerAndPort(server.toLowerCaseInvariant(), port)
        synchronized(cache) {
            var ret: SendReceiveQueue? = cache[key]
            if (ret == null) {
                ret = SendReceiveQueue(server, port, executor, false)
                cache[key] = ret
            }
            return ret
        }
    }

    private data class ServerAndPort(val server: String, val port: Int)

    companion object {
        val inst = SharedClients(Executors.newCachedThreadPool())
    }
}
