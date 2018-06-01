package org.dbtag.driver

import org.dbtag.data.Count
import org.dbtag.data.Filter
import org.dbtag.data.QVals
import org.dbtag.data.write
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

suspend fun UserQueue.count(filter: Filter = Filter.empty, ifUpdatedAfter: Long = 0L) = suspendCoroutine<TAndMs<Count>>
  { cont-> count(filter, ifUpdatedAfter, cont) }

fun UserQueue.count(filter: Filter, ifUpdatedAfter: Long, cont: Continuation<TAndMs<Count>>)  {
    queue({
        with(getWriter(TagClient.Count)) {
            // val JOIN = 2  // TODO: not used right now !
            if (filter !== Filter.empty) {
                val emb = embeddedField(1) // FILTER
                filter.write(this)
                emb.close()
            }
            if (ifUpdatedAfter != 0L)
                writeFieldFixed64(3, ifUpdatedAfter)  // IF_UPDATED_AFTER
            toByteArray()
        }}, { it.count(it.unreadBytesCount()) }, null, cont)
}

internal fun BinaryReader.count(len: Int) : Count {
    var posts = 0
    var tagged = 0
    var latitudeMin = 0.0
    var latitudeMax = 0.0
    var longitudeMin = 0.0
    var longitudeMax = 0.0
    var dateQ = QVals(0, 0.0, 0L, 0L, 0L, 0L, 0L)
    var durationQ = QVals(0, 0.0, 0L, 0L, 0L, 0L, 0L)
    var valueQ = QVals(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    var unit = ""

    val eor = position + len
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            WireType.VARINT -> {
                val value = readVarint()
                when (field) {
                    1 -> posts = value.toInt() // POSTS
                    2 -> tagged = value.toInt() // TAGGED
                }
            }
            WireType.FIXED64 -> {
                when (field) {
                    3 -> latitudeMin = readDouble() // LATITUDE_MIN
                    4 -> latitudeMax = readDouble() // LATITUDE_MAX
                    5 -> longitudeMin = readDouble() // LONGITUDE_MIN
                    6 -> longitudeMax = readDouble() // LONGITUDE_MAX
                    else -> skip(8)
                }
            }
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> {
                val len1 = readVarint().toInt()
                when (field) {
                    7 -> dateQ = qValsLong(len1)// DATEQ
                    8 -> durationQ = qValsLong(len1)// DURATIONQ
                    9 -> valueQ = qValsDouble(len1)// VALUEQ
                    10 -> unit = readString(len1)  // UNIT
                    else -> skip(len1)
                }
            }
        }
    }
    return Count(posts, tagged, latitudeMin, latitudeMax, longitudeMin, longitudeMax, dateQ, durationQ, valueQ, unit)
}

private fun BinaryReader.qValsLong(len: Int) : QVals<Long> {
    var count = 0
    var sum = 0.0
    var min = 0L
    var max = 0L
    var q1  = 0L
    var q2  = 0L
    var q3  = 0L

    val eor = position + len
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            WireType.VARINT -> {
                val value = readVarint()
                when (field) {
                    1 -> count = value.toInt() // COUNT
                    3 -> min = value  // MIN
                    4 -> max = value  // MAX
                    5 -> q1  = value  // Q1
                    6 -> q2  = value  // Q2
                    7 -> q3  = value  // Q3
                }
            }
            WireType.FIXED64 -> when (field) {
                2 -> sum = readDouble() // SUM
                else -> skip(8)
            }
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> skip(readVarint().toInt())
        }
    }
    return QVals(count, sum, min, max, q1, q2, q3)
}

private fun BinaryReader.qValsDouble(len: Int) : QVals<Double> {
    var count = 0
    var sum = 0.0
    var min = 0.0
    var max = 0.0
    var q1  = 0.0
    var q2  = 0.0
    var q3  = 0.0

    val eor = position + len
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            WireType.VARINT -> {
                val value = readVarint()
                when (field) {
                    1 -> count = value.toInt() // COUNT
                }
            }
            WireType.FIXED64 -> when (field) {
                2 -> sum = readDouble() // SUM
                3 -> min = readDouble() // MIN
                4 -> max = readDouble() // MAX
                5 -> q1  = readDouble() // Q1
                6 -> q2  = readDouble() // Q2
                7 -> q3  = readDouble() // Q3
                else -> skip(8)
            }
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> skip(readVarint().toInt())
        }
    }
    return QVals(count, sum, min, max, q1, q2, q3)
}
