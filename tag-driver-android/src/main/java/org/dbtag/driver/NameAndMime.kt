package org.dbtag.driver

import org.dbtag.data.NameAndSize
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader


fun BinaryReader.nameAndSize(len: Int): NameAndSize {
    var name = ""
    var size = 0

    val eor = position + len
    while (position != eor) {
        val key = readVarint().toInt()
        val field = (key shr 3)
        when (key and 7) {
            WireType.VARINT -> {
                val value = readVarint()
                if (field == 2) // SIZE
                    size = value.toInt()
            }
            WireType.FIXED64 -> skip(8)
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> {
                val len2 = readVarint().toInt()
                when (field) {
                    1 -> name = readString(len2) // NAME
                    else -> skip(len2)
                }
            }
        }
    }
    return NameAndSize(name, size)
}
