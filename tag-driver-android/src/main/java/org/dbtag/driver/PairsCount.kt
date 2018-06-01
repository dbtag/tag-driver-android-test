package org.dbtag.driver

import org.dbtag.data.Filter
import org.dbtag.data.PairData
import org.dbtag.data.write
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * The [topics] are also kind of a toFilter, because all we are returning is how many matches per pair of tags
 * i.e. not worth keeping ...
 */
suspend fun UserQueue.pairsCount(filter: Filter, ifUpdatedAfter: Long, topics: List<String>) = suspendCoroutine<TAndMs<PairData>>
    { cont-> pairsCount(filter, ifUpdatedAfter, topics, cont) }

fun UserQueue.pairsCount(filter: Filter, ifUpdatedAfter: Long, topics: List<String>, cont: Continuation<TAndMs<PairData>>)  {
    queue({
        with(getWriter(TagClient.PairsCount)) {
            if (filter !== Filter.empty) {
                val emb = embeddedField(1) // FILTER
                filter.write(this)
                emb.close()
            }
            if (ifUpdatedAfter != 0L)
                writeFieldFixed64(2, ifUpdatedAfter)  // IF_UPDATED_AFTER
            topics.forEach({
                writeField(3, it)  // TOPIC
            })
            toByteArray()
        }}, { it.pairsCount(it.unreadBytesCount()) }, null, cont)
}


internal fun BinaryReader.pairsCount(len: Int) : PairData {
    val is0 = mutableListOf<Int>()  // memory efficient - a count, and then the 2 ids
    val tags = mutableListOf<String>()
    val names = mutableListOf<String>()
    val eor = position + len
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            WireType.VARINT -> {
                val value = readVarint()
                when (field) {
//                    1 -> posts = value.toInt() // POSTS
//                    2 -> tagged = value.toInt() // TAGGED
                }
            }
            WireType.FIXED64 -> {
                when (field) {
//                    3 -> latitudeMin = readDouble() // LATITUDE_MIN
//                    4 -> latitudeMax = readDouble() // LATITUDE_MAX
//                    5 -> longitudeMin = readDouble() // LONGITUDE_MIN
//                    6 -> longitudeMax = readDouble() // LONGITUDE_MAX
                    else -> skip(8)
                }
            }
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> {
                val len1 = readVarint().toInt()
                when (field) {
//                    7 -> dateQ = qValsLong(len1)// DATEQ
//                    8 -> durationQ = qValsLong(len1)// DURATIONQ
//                    9 -> valueQ = qValsDouble(len1)// VALUEQ
//                    10 -> unit = readString(len1)  // UNIT
                    else -> skip(len1)
                }
            }
        }
    }
    return PairData(is0, tags, names)
}

//fun pairsCount(startTime: Long, endTime: Long, items: Array<String>, callback: ResultAvailable) {
//    val writer = BinaryWriter().writeString("PairsCount").writeString(token)
//            .writeLong(startTime).writeLong(endTime)
//    val topicsCount = items.size
//    writer.writeInt(topicsCount)
//    for (topic in items) writer.writeString(topic)
//
//    db.queue(writer.toByteArray(),
//            object : ProcessReader() {
//                fun run(ex: Exception, reader: BinaryReader?) {
//                    var result: Any = ex
//                    if (reader != null)
//                        try {
//                            val pairsCount = reader!!.readInt()
//                            val tagsCount = reader!!.readInt()
//                            val `is` = IntArray(pairsCount * 3)  // memory efficient - a count, and then the 2 ids
//                            val tags = arrayOfNulls<String>(tagsCount)
//                            val names = arrayOfNulls<String>(tagsCount)
//                            var isOfs = 0
//                            var newTagsSeen = 0
//                            for (i in 0 until pairsCount) {
//                                `is`[isOfs++] = reader!!.readInt()
//
//                                for (t in 0..1) {
//                                    if (reader!!.readBoolean()) {
//                                        `is`[isOfs++] = newTagsSeen
//                                        tags[newTagsSeen] = reader!!.readString()
//                                        names[newTagsSeen] = reader!!.readString()
//                                        ++newTagsSeen
//                                    } else
//                                        `is`[isOfs++] = reader!!.readInt()
//                                }
//                            }
//                            result = PairData(`is`, tags, names)
//                        } catch (ex2: Exception) {
//                            result = ex2
//                        }
//
//                    if (DEBUG)
//                        if (result is Exception)
//                            Log.e(TAG, "pairsCount", result)
//                    callback(result)
//                }
//            })
//}
