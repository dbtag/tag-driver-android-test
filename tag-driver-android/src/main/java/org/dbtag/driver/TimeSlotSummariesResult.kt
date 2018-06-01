package org.dbtag.driver

import org.dbtag.data.*
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Gets code1Summaries in time-slots, but does not bother breaking down into top 10 codes in topics.
 */
suspend fun Queue.asyncTimeSlotSummariesResult(filter: Filter, ifUpdatedAfter: Long, specificValueTag: String,
                                               timeSlots: Iterable<Long>, wrapEvery: Long) = suspendCoroutine<TAndMs<TimeSlotSummariesResult>>
  { cont-> asyncTimeSlotSummariesResult(filter, ifUpdatedAfter, specificValueTag, timeSlots, wrapEvery, cont) }


fun Queue.asyncTimeSlotSummariesResult(filter: Filter, ifUpdatedAfter: Long, specificValueTag: String,
                                               timeSlots: Iterable<Long>, wrapEvery: Long, cont: Continuation<TAndMs<TimeSlotSummariesResult>>) {
    queue({
        with(getWriter(TagClient.TimeSlotSummaries)) {
            if (filter !== Filter.empty) {
                val emb = embeddedField(1) // FILTER
                filter.write(this)
                emb.close()
            }
            if (ifUpdatedAfter != 0L)
                writeFieldFixed64(2, ifUpdatedAfter) // IF_UPDATED_AFTER
            if (!specificValueTag.isEmpty())
                writeField(3, specificValueTag) // SPECIFIC_VALUE_TAG

            val embTimeSlot = embeddedField(4) // TIME_SLOT
            var slot = 0L
            for (s in timeSlots) {
                writeVarint(s - slot) // write differences, so timeSlots[] must be increasing
                slot = s
            }
            embTimeSlot.close()
            if (wrapEvery != 0L)
                writeFieldVarint(5, wrapEvery) // WRAP_EVERY
            toByteArray()
        }}, { it.timeSlotSummariesResult() }, null, cont)
}


//private fun BinaryReader.timeSlotSummariesResult(): TimeSlotSummariesResult {
//    val code1Summaries = mutableListOf<VariousSummary>()
//    val eor = bufferSize
//    while (position != eor) {
//        val key = readByte().toInt()
//        val field = (key shr 3)
//        when (key and 7) {
//            WireType.VARINT -> readVarint()
//            WireType.FIXED64 -> skip(8)
//            WireType.FIXED32 -> skip(4)
//            WireType.LENGTH_DELIMITED -> {
//                val len = readVarint().toInt()
//                if (field == 1)  // SUMMARY
//                    code1Summaries.add(variousSummary(len))
//                else
//                    skip(len)
//            }
//        }
//    }
//    return TimeSlotSummariesResult(code1Summaries)
//}

// Different version
private fun BinaryReader.timeSlotSummariesResult(): TimeSlotSummariesResult {
    val ret = mutableListOf<List<Tag>>()
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
                if (field == 1)  // SLOT
                    ret.add(wibblyWobbly(len))
                else
                    skip(len)
            }
        }
    }
    return TimeSlotSummariesResult(ret)
}

private fun BinaryReader.wibblyWobbly(len: Int): List<Tag> {
    val ret = mutableListOf<Tag>()
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
                if (field == 1)  // TAG
                    ret.add(tag(len2))
                else
                    skip(len)
            }
        }
    }
    return ret
}

