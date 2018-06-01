package org.dbtag.driver

import org.dbtag.data.*
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine


suspend fun Queue.topicsReply(filter: Filter) = suspendCoroutine<TAndMs<TopicsReply<Tag>>>
  { cont-> topicsReply(filter, 0L, Parts.None, { tag, _ -> tag } ,  cont) }

suspend fun <T> Queue.topicsReply(filter: Filter, ifUpdatedAfter: Long, lastMessageParts: Int,
                                  cons: (Tag, MessageConstructArgs?) -> T) = suspendCoroutine<TAndMs<TopicsReply<T>>>
  { cont-> topicsReply(filter, ifUpdatedAfter, lastMessageParts, cons,  cont) }

/**
 * Gets all items.
 * @param ifUpdatedAfter  If this is non-zero, then nothing is returned unless newer dataPoints_ exist.
 */
fun <T> Queue.topicsReply(filter: Filter, ifUpdatedAfter: Long, lastMessageParts: Int,
                          cons: (Tag, MessageConstructArgs?) -> T, cont: Continuation<TAndMs<TopicsReply<T>>>) {
    queue({
        with(getWriter(TagClient.Topics)) {
            //val JOIN = 2
            if (filter !== Filter.empty) {
                val emb = embeddedField(1) // FILTER
                filter.write(this)
                emb.close()
            }
            if (ifUpdatedAfter != 0L)
                writeFieldFixed64(3, ifUpdatedAfter) // IF_UPDATED_AFTER

            if (lastMessageParts != 0)
                writeFieldVarint(4, lastMessageParts.toLong()) // LAST_MESSAGE_PARTS
            toByteArray()
        }}, { it.topicsReply(cons) }, null, cont)
}


private fun <T> BinaryReader.topicsReply(cons: (Tag, MessageConstructArgs?) -> T) : TopicsReply<T> {
    var serverTime: Long = 0
    var lastTopic: Tag? = null
    val args = MessageConstructArgs()

    val topics = mutableListOf<T>()
    val eor = bufferSize
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            WireType.VARINT -> {
                val value = readVarint()
                if (field == 1) // TOPICS_REPLY_SERVER_TIME
                    serverTime = value
            }
            WireType.FIXED64 -> skip(8)
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> {
                val len = readVarint().toInt()
                when (field) {
                    2 -> { // TOPICS_REPLY_TOPIC
                        // val TOPIC_MAX_USE_PER_MESSAGE = 4  // TODO: this seemed good once...
                        if (lastTopic != null) {
                            val x = cons(lastTopic, null)
                            if (x != null)
                              topics.add(x)
                        }
                        lastTopic = tag(len)
                    }
                    3 -> { // TOPICS_REPLY_LAST_MESSAGE
                        with (args) {
                            read(len)
                            lastTopic?.let {
                                val x = cons(it, args)
                                if (x != null)
                                  topics.add(x)
                                lastTopic = null
                            }
                        }

                    }
                    else -> skip(len)
                }
            }
        }
    }
    // Flush
    lastTopic?.let {
        val x = cons(it, null)
        if (x != null)
          topics.add(x)
    }

    return TopicsReply(serverTime, topics)
}
