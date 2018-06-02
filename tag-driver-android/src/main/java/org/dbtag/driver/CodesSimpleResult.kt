package org.dbtag.driver

import org.dbtag.data.CodesSimpleResult
import org.dbtag.data.Tag
import org.dbtag.data.tag
import org.dbtag.socketComs.BinaryReader

suspend fun UserQueue.codesSimpleResult(topic: String, limit: Int, code0: String = "") = queue({
    with(getWriter(TagClient.CodesSimple)) {
        writeField(1, topic) // TOPIC
        writeFieldVarint(2, limit.toLong()) // LIMIT
        if (!code0.isEmpty())
            writeField(3, code0)  // CODE0
        toByteArray()
    } }, { it.codesSimpleResult() })


private fun BinaryReader.codesSimpleResult(): CodesSimpleResult {
    val codes = mutableListOf<Tag>()
    val eor = bufferSize
    while (position != eor) {
        val key = readByte().toInt()
        // val field = (key shr 3)
        when (key and 7) {
            org.dbtag.protobuf.WireType.VARINT -> readVarint()
            org.dbtag.protobuf.WireType.FIXED64 -> skip(8)
            org.dbtag.protobuf.WireType.FIXED32 -> skip(4)
            org.dbtag.protobuf.WireType.LENGTH_DELIMITED -> codes.add(tag(readVarint().toInt()))
        }
    }
    return CodesSimpleResult(codes)
}


