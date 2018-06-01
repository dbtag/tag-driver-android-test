package org.dbtag.driver

import org.dbtag.data.UpdateReply
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader
import kotlin.coroutines.experimental.suspendCoroutine

// TODO: could send computer name, etc, in this as they will never subsequently change - assuming the
// socket is recycled for further use...
//suspend fun Queue.updateAsync(version: Int) = suspendCoroutine<UpdateReply> { cont->
//    queue({
//        with(getWriter(TagClient.Update)) {
//            val VERSION = 1
//            writeFieldVarint(VERSION, version.toLong())
//            toByteArray()
//        }}, { it.UpdateReply() }, null, cont)
//}

private fun BinaryReader.UpdateReply() : UpdateReply {
    var apkVersionCode = 0
    var apkBytes: ByteArray? = null
    val APK_VERSION_CODE = 1
    val APK_BYTES = 2
    val eor = bufferSize
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            WireType.VARINT -> {
                val value = readVarint()
                if (field == APK_VERSION_CODE)
                    apkVersionCode = value.toInt()
            }
            WireType.FIXED64 -> skip(8)
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> {
                val len2 = readVarint().toInt()
                if (field == APK_BYTES)
                    apkBytes = readBytes(len2)
                else
                    skip(len2)
            }
        }
    }
    return UpdateReply(apkVersionCode, apkBytes)
}

