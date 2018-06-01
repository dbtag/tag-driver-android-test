package org.dbtag.data

import org.dbtag.driver.Parts
import org.dbtag.driver.Queue
import org.dbtag.driver.TAndMs
import org.dbtag.driver.select
import java.util.concurrent.Executor
import kotlin.coroutines.experimental.Continuation

class MessageMid(args: MessageConstructArgs) {
    val mid: Int = args.mid

    companion object {
//        suspend fun asyncSelect(queue: Queue, toFilter: Filter, limit: Int = Integer.MAX_VALUE, ifUpdatedAfter: Long = 0L,
//                              desc: Boolean = false, executor: Executor? = null) = suspendCoroutine<messagesData<MessageMid>> { cont->
//            select(queue, toFilter, limit, ifUpdatedAfter, desc, executor, cont)}
//        }
        fun asyncFind0(queue: Queue, filter: Filter, limit: Int = Integer.MAX_VALUE, ifUpdatedAfter: Long = 0L,
                       desc: Boolean = false, executor: Executor? = null, cont: Continuation<TAndMs<MessagesData<MessageMid>>>) {
            queue.select(filter, 0, Filter.empty, limit, ifUpdatedAfter, desc, Parts.Mid, { args: MessageConstructArgs ->
                MessageMid(args)
            }, executor, cont)
        }
    }
}
