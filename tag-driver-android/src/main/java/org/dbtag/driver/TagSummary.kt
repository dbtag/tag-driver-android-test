package org.dbtag.driver

import org.dbtag.data.Tag
import org.dbtag.data.variousSummary
import org.dbtag.socketComs.BinaryReader

data class TagSummary(val tag: Tag,
                      val count: Int, val durationCount: Int, val valueCount: Int, val specificValueCount: Int,
                      val duration: Double, val durationMin: Long, val durationMax: Long, val dateMin: Long,
                      val dateMax: Long, val unit: String, val value: Double, val valueMin: Double,
                      val valueMax: Double, val specificValue: Double, val specificValueUnit: String)


fun BinaryReader.tagSummary(len: Int): TagSummary {
    var tag0 = ""
    var originalTag0: String? = null
    var name0 = ""
    with(variousSummary(len, null, { reader, len1, field ->
        when (field) {
            1 -> tag0 = reader.readString(len1) // TAG
            2 -> originalTag0 = reader.readString(len1) // ORIGINAL_TAG
            3 -> name0 = reader.readString(len1) // NAME
            else -> reader.skip(len1)
        }
    })) {
        return TagSummary(Tag(tag0, name0), count, durationCount, valueCount, specificValueCount, duration, durationMin,
                durationMax, dateMin, dateMax, unit, value, valueMin, valueMax, specificValue, specificValueUnit)
    }
}
