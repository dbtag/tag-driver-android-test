package org.dbtag.driver

import org.dbtag.data.Filter
import org.dbtag.data.Tag
import org.dbtag.data.tag
import org.dbtag.data.write
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine



/**
 * Gets all distinct tags that ever have values, together with the value unit.
 * We could get the same tag more than once if values appear for it with more than one unit.
 */
suspend fun UserQueue.tagsWithValues(filter: Filter) = suspendCoroutine<TAndMs<List<Tag>>>
{ cont-> tagsWithValues(filter, cont) }

fun UserQueue.tagsWithValues(filter: Filter, cont: Continuation<TAndMs<List<Tag>>>)  {
    queue({
        with(getWriter(TagClient.TagsWithValues)) {
            if (filter !== Filter.empty) {
                val emb = embeddedField(1) // FILTER
                filter.write(this)
                emb.close()
            }
            toByteArray()
        }}, { it.tagsWithValues(it.unreadBytesCount()) }, null, cont)
}

internal fun BinaryReader.tagsWithValues(len: Int) : List<Tag> {
    val tags = mutableListOf<Tag>()
    val eor = position + len
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            WireType.VARINT -> readVarint()
            WireType.FIXED64 -> skip(8)
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> {
                val len1 = readVarint().toInt()
                when (field) {
                    1 -> tags.add(tag(len1))  // TAG
                    else -> skip(len1)
                }
            }
        }
    }
    return tags
}
