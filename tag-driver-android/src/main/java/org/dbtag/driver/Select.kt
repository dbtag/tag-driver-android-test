package org.dbtag.driver

import android.os.SystemClock
import org.dbtag.data.Filter
import org.dbtag.data.MessageConstructArgs
import org.dbtag.data.MessagesData
import org.dbtag.data.write
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader
import org.dbtag.socketComs.GetBuffer
import java.util.concurrent.Executor
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine


object Parts {
    const val None = 0
    const val Mid = 1
    const val Id = 2
    const val Date = 4
    const val Until = 8
    const val Content = 16
    const val Updated = 32
    const val Deleted = 64
    const val Importance = 128
    const val Location = 256
    const val Comments = 512
    const val Attachments = 1024
    const val JoinedTags = 2048
    const val FirstThumbnail = 4096
    const val All = Integer.MAX_VALUE and (FirstThumbnail).inv()
}

suspend fun <T>UserQueue.select(filter: Filter, joins: Any, subFilter: Filter, limit: Int, ifUpdatedAfter: Long,
                                desc: Boolean, parts: Int, cons: (MessageConstructArgs) -> T, executor: Executor? = null) =
    select({getSelectWriteBytes(filter, limit, ifUpdatedAfter, desc, parts)}, cons, executor)


internal fun UserQueue.getSelectWriteBytes(filter: Filter, limit: Int, ifUpdatedAfter: Long,
                                      desc: Boolean, parts: Int) =
        with(getWriter(TagClient.Select)) {
    //        val JOIN = 2  // TODO: not used right now !
    //        val SUB_FILTER = 3
    //        val PARTS = 7  // same as enum SelectRequest

            if (filter !== Filter.empty) {
                val emb = embeddedField(1) // FILTER
                filter.write(this)
                emb.close()
            }
            if (ifUpdatedAfter != 0L)
                writeFieldFixed64(4, ifUpdatedAfter) // IF_UPDATED_AFTER
            if (limit != 0)
                writeFieldVarint(5, limit.toLong()) // LIMIT
            if (desc)
                writeFieldVarint(6, 1) // DESC
            writeFieldVarint(7, parts.toLong()) // PARTS

            toByteArray()
        }

//private suspend fun UserQueue.select(byteArray: ByteArray, toFilter: Filter, cons: (MessageConstructArgs) -> Any)
//        = suspendCoroutine<messagesData> { cont-> select(byteArray, toFilter, cons, cont) }

suspend fun <T>UserQueue.select(getBuffer: GetBuffer, cons: (MessageConstructArgs) -> T, executor: Executor? = null) =
        queue(getBuffer, { it.messagesData(cons) }, executor)


internal fun <T>BinaryReader.messagesData(cons: (MessageConstructArgs) -> T) : MessagesData<T> {
    var serverFreshness: Long = 0
    val messages = mutableListOf<T>()
    var args: MessageConstructArgs? = null
    val eor = bufferSize
    while (position != eor) {
        val DALL_MESSAGE = 1
        // val DALL_SKIPPED_MESSAGES
        val DALL_SERVER_FRESHNESS = 10

        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            WireType.VARINT -> {
                val value = readVarint()
                if (field == DALL_SERVER_FRESHNESS)
                    serverFreshness = value
            }
            WireType.FIXED64 -> skip(8)
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> {
                val len = readVarint().toInt()
                if (field == DALL_MESSAGE) {
                    if (args == null)
                        args = MessageConstructArgs()
                    with (args) {
                        read(len)
                    }
                    val x = cons(args)
                    if (x != null)
                      messages.add(x)
                }
                else
                    skip(len)
            }
        }
    }
    return MessagesData(messages, serverFreshness)
}



//fun newMessagesAsyncContext(parts: Int, cons: (MessageConstructArgs) -> Any,
//                            callback: ResultCallback<messagesData>, handler: Handler?): SelectAsyncContext {
//    return SelectAsyncContext(parts, cons, callback, handler)
//}
//
///***
// * Holds code1Summaries to make repeatedly calling selectAsync GC-efficient
// */
//class SelectAsyncContext(private val parts_: Int, private val construct_: (MessageConstructArgs) -> Any,
//                         callback: ResultCallback<messagesData>, private val handler_: Handler?) {
//    private val callback_: Runnable
//    private var byteArray_: ByteArray? = null
//    private var filter_: Filter = Filter.empty
//    private var ifUpdatedAfter_: Long = 0
//    private var limit_: Int = 0
//    private var desc_: Boolean = false
//    private var messagesData_: messagesData? = null
//    private var ex_: Exception? = null
//
//    init {
//        callback_ = Runnable { callback(ex_, if (ex_ != null) null else messagesData_) }
//    }
//
//    fun db(): Parts {
//        return this@Parts
//    }
//    //    public int parts() { return parts_; }
//
//    fun selectAsync(toFilter: Filter, limit: Int, ifUpdatedAfter: Long, desc: Boolean) {
//        // We work to ensure that we have the most up-to-messageDate version of all messages in a messageDate range
//        if (byteArray_ == null || filter_ !== toFilter || ifUpdatedAfter_ != ifUpdatedAfter || limit_ != limit ||
//                desc_ != desc) {
//            byteArray_ = getSelectWriteBytes(toFilter, limit, ifUpdatedAfter, desc, parts_)
//            filter_ = toFilter
//            ifUpdatedAfter_ = ifUpdatedAfter
//            limit_ = limit
//            desc_ = desc
//        }
//        db.queue(gwb_, pr_)
//    }
//
