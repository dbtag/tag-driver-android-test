package org.dbtag.driver

import org.dbtag.data.Attachment
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Gets the bytes of an attachment to a message, by index starting from 0.
 * We don't cache these as they could be big.
 */
suspend fun Queue.attachment(mid: Int, comment: Int, index: Int) = suspendCoroutine<TAndMs<Attachment>>
  { cont-> attachment(mid, comment, index, cont) }

fun Queue.attachment(mid: Int, comment: Int, index: Int, cont: Continuation<TAndMs<Attachment>>) {
    queue({
        with(getWriter(TagClient.Attachment)) {
            writeFieldVarint(1, mid.toLong())     // MID
            writeFieldVarint(2, comment.toLong()) // COMMENT
            writeFieldVarint(3, index.toLong())   // INDEX
            toByteArray()
            }}, { it.attachment() }, null, cont)
}


internal fun BinaryReader.attachment(): Attachment {
    var name = ""
    var bytes = ByteArray(0)
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
                    1 -> name = readString(len)  // NAME
                    2 -> bytes = readBytes(len)  // BYTES
                    else -> skip(len)
                }
            }
        }
    }
    return Attachment(name, bytes)
}
