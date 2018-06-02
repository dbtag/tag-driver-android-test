package org.dbtag.driver

import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader
import kotlin.coroutines.experimental.suspendCoroutine

suspend fun UserQueue.insertCredentials(password: String, extraText: String) = queue( {
    with(getWriter(TagClient.InsertCredentials)) {
        if (password.isNotEmpty())
          writeField(1, password) // PASSWORD
        if (extraText.isNotEmpty())
          writeField(2, extraText) // EXTRATEXT
        toByteArray()
    }}, { it.createUserResults() })


private fun BinaryReader.createUserResults() : Int {
    var mid = 0
    val eor = bufferSize
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            WireType.VARINT -> {
                val value = readVarint()
                if (field == 1) // MID
                    mid = value.toInt()
            }
            WireType.FIXED64 -> skip(8)
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> skip(readVarint().toInt())
        }
    }
    return mid
}
