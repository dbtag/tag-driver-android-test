package org.dbtag.driver

import org.dbtag.data.MessageConstructArgs
import org.dbtag.data.NameAndSize
import org.dbtag.data.TagNameDateContentAttachments
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader

fun BinaryReader.tagNameDateContentAttachments(len: Int): TagNameDateContentAttachments {
    var date: Long = 0
    var content = ""
    var attachments: MutableList<NameAndSize>? = null
    val eor = position + len
    while (position != eor) {
        val key = readVarint().toInt()
        val field = key shr 3
        when (key and 7) {
            WireType.VARINT -> readVarint()
            WireType.FIXED64 -> {
                when (field) {
                    MessageConstructArgs.MESSAGE_DATE -> date = readLong()
                    else -> skip(8)
                }
            }
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> {
                val len2 = readVarint().toInt()
                when (field) {
                    MessageConstructArgs.MESSAGE_ATTACHMENT -> {
                        if (attachments == null)
                            attachments = mutableListOf()
                        attachments.add(nameAndSize(len2))
                    }
                    MessageConstructArgs.MESSAGE_CONTENT -> content = readString(len2)
                    else -> skip(len2)
                }
            }
        }
    }
    return TagNameDateContentAttachments("from.tag", "from.name", date, content,
            if (attachments == null || attachments.size == 0)
                emptyList()
            else
                attachments)
}