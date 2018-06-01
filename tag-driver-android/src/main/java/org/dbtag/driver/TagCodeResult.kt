package org.dbtag.driver

import org.dbtag.data.tag
import org.dbtag.data.Tag
import org.dbtag.data.TagCodeResult
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader
import kotlin.coroutines.experimental.suspendCoroutine

// An exact match against a code - maybe matching in several topics...
//suspend fun UserQueue.tagCodeResult(code: String) = suspendCoroutine<TagCodeResult> { cont->
//    queue({
//        with(getWriter(TagClient.TagCode)) {
//            val CODE = 1
//            writeField(CODE, code)
//            toByteArray()
//        }}, { it.tagCodeResult() }, null, cont)
//}


private fun BinaryReader.tagCodeResult(): TagCodeResult {
    val tagAndNames = mutableListOf<Tag>()
    val TAG = 1
    val eor = bufferSize
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            WireType.VARINT -> readVarint()
            WireType.FIXED64 -> skip(8)
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> {
                val len2 = readVarint().toInt()
                if (field == TAG)
                    tagAndNames.add(tag(len2))
                else
                    skip(len2)
            }
        }
    }
    return TagCodeResult(tagAndNames)
}
