package org.dbtag.driver

import org.dbtag.data.Match
import org.dbtag.data.MatchTopic
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader


fun BinaryReader.matchTopic(len: Int) : MatchTopic {
    var topic = ""
    val matches = mutableListOf<Match>()
    val eor = position + len
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            WireType.VARINT -> readVarint()
            WireType.FIXED64 -> skip(8)
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> {
                val len2 = readVarint().toInt()
                when (field) {
                    1 -> topic = readString(len2) // TOPIC
                    2 -> matches.add(match(len2)) // MATCH
                    else -> skip(len2)
                }
            }
        }
    }
    return MatchTopic(topic, matches)
}
