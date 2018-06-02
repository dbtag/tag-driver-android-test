package org.dbtag.data

import org.dbtag.driver.*
import java.util.concurrent.Executor
import kotlin.coroutines.experimental.Continuation

class MessageMid(args: MessageConstructArgs) {
    val mid: Int = args.mid

    companion object {
        suspend fun select(queue: UserQueue, filter: Filter, limit: Int = Integer.MAX_VALUE, ifUpdatedAfter: Long = 0L,
                           desc: Boolean = false, executor: Executor? = null)
        = queue.select(filter, 0, Filter.empty, limit, ifUpdatedAfter, desc, Parts.Mid,
            { args: MessageConstructArgs -> MessageMid(args) }, executor)
    }
}
