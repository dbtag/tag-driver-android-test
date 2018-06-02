package org.dbtag.driver

import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader


/**
 * Gets the databases.
 */
suspend fun Queue.databases() = queue({
        getWriter0(TagClient.Databases).toByteArray()},  { it.databases() })

private fun BinaryReader.databases() : List<String> = mutableListOf<String>().apply {
    val eor = bufferSize
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            WireType.VARINT -> readVarint()
            WireType.FIXED64 -> skip(8)
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> {
                val len = readVarint().toInt()
                when (field) {
                    // 1 -> host = readString(len) // HOST   TODO: this is never returned by servers so remove it
                    2 -> add(readString(len)) // DATABASE
                    else -> skip(len)
                }
            }
        }
    }
}
