package org.dbtag.driver

import org.dbtag.data.*
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader


/**
 * Matches speech with several possible text's to go at, which are complete things to match
 */
suspend fun UserQueue.matchSpeech(texts: Iterable<String>) = queue({
    with(getWriter(TagClient.MatchSpeech)) {
        for (text in texts)
            writeField(1, text) // TEXT
        toByteArray()
    }}, { it.matchTopicsResult() })


/**
 * Left matches against partially typed text
 */
suspend fun UserQueue.leftMatchTagNameOrCode(filter: Filter, text: String, limit: Int) = queue({
    with(getWriter(TagClient.LeftMatchTagNameOrCode)) {
        if (filter !== Filter.empty) {
            val emb = embeddedField(1) // FILTER
            filter.write(this)
            emb.close()
        }
        writeField(3, text) // TEXT
        if (limit != 0)
            writeFieldVarint(4, limit.toLong()) // LIMIT
        toByteArray()
    }}, { it.matchTopicsResult() })


private fun BinaryReader.matchTopicsResult() : MatchTopicsResult {
    var exactSingleMatch = false
    val matchTopics = mutableListOf<MatchTopic>()
    val topicDirects = mutableListOf<Tag>()
    val eor = bufferSize
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            WireType.VARINT -> {
                val value = readVarint()
                if (field == 1) // EXACT_SINGLE_MATCH
                    exactSingleMatch = (value != 0L)
            }
            WireType.FIXED64 -> skip(8)
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> {
                val len = readVarint().toInt()
                when (field) {
                    2 -> matchTopics.add(matchTopic(len)) // MATCH_TOPIC
                    3 -> topicDirects.add(tag(len))  // TOPIC_DIRECT
                    else -> skip(len)
                }
            }
        }
    }
    return MatchTopicsResult(exactSingleMatch, matchTopics, topicDirects)
}
