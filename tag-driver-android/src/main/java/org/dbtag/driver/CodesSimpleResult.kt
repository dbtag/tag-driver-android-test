package org.dbtag.driver

import org.dbtag.data.CodesSimpleResult
import org.dbtag.data.tag
import org.dbtag.data.Tag
import org.dbtag.socketComs.BinaryReader
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

suspend fun Queue.asyncCodesSimpleResult(topic: String, limit: Int, code0: String = "") = suspendCoroutine<TAndMs<CodesSimpleResult>>
  { cont-> asyncCodesSimpleResult(topic, limit, code0, cont) }

fun Queue.asyncCodesSimpleResult(topic: String, limit: Int, code0: String = "", cont: Continuation<TAndMs<CodesSimpleResult>>) {
    queue({
        with(getWriter(TagClient.CodesSimple)) {
            writeField(1, topic) // TOPIC
            writeFieldVarint(2, limit.toLong()) // LIMIT
            if (!code0.isEmpty())
                writeField(3, code0)  // CODE0
            toByteArray()
        } }, { it.codesSimpleResult() }, null, cont)
}


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

