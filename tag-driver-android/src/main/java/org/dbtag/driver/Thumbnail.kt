package org.dbtag.driver

import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader
import java.io.EOFException
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

suspend fun UserQueue.thumbnail(mid: Int, comment: Int, index: Int, maxSize: Int) = queue({
    with(getWriter(TagClient.Thumbnail)) {
        writeFieldVarint(1, mid.toLong())      // MID
        writeFieldVarint(2, comment.toLong())  // COMMENT
        writeFieldVarint(3, index.toLong())    // INDEX
        writeFieldVarint(4, maxSize.toLong())  // MAX_SIZE
        toByteArray()
    }}, { it.thumbnailBytes(it.bufferSize - it.position) })

fun BinaryReader.thumbnailBytes(len: Int): ByteArray {
    var bytes: ByteArray? = null
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
                    1 -> bytes = readBytes(len2) // BYTES
                    else -> skip(len)
                }
            }
        }
    }
    if (bytes == null)
        throw EOFException()
    return bytes
}
