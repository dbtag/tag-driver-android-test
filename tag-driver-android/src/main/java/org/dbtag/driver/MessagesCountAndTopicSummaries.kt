package org.dbtag.driver

import org.dbtag.data.*
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader
import kotlin.coroutines.experimental.Continuation

fun Queue.messagesCountAndTopicSummaries(filter: Filter, ifUpdatedAfter: Long, specificValueTag: String = "",
                                         limitPerTopic: Int, cont: Continuation<TAndMs<MessagesCountAndTopicSummaries>>) {
    queue({
        with(getWriter(TagClient.TopicSummaries)) {
            if (filter !== Filter.empty) {
                val emb = embeddedField(1) // FILTER
                filter.write(this)
                emb.close()
            }
            if (ifUpdatedAfter != 0L)
                writeFieldFixed64(2, ifUpdatedAfter) // IF_UPDATED_AFTER
            if (!specificValueTag.isEmpty())
                writeField(3, specificValueTag) // SPECIFIC_VALUE_TAG
            if (limitPerTopic != 0)
                writeFieldVarint(4, limitPerTopic.toLong()) // LIMIT_PER_TOPIC
            toByteArray()
        }}, { it.messagesCountAndTopicSummaries() }, null, cont)
}


private fun BinaryReader.messagesCountAndTopicSummaries(): MessagesCountAndTopicSummaries {
    var messagesCount = 0
    val topicSummaries = mutableListOf<TopicSummary>()
    val eor = bufferSize
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            WireType.VARINT -> {
                val value = readVarint()
                if (field == 1) // MESSAGES_COUNT
                    messagesCount = value.toInt()
            }
            WireType.FIXED64 -> skip(8)
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> {
                val len = readVarint().toInt()
                if (field == 2) // TOPIC_SUMMARY
                    topicSummaries.add(topicSummary(len))
                else
                    skip(len)
            }
        }
    }
    return MessagesCountAndTopicSummaries(messagesCount, topicSummaries)
}
