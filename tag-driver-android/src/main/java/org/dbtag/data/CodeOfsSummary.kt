package org.dbtag.data

import org.dbtag.socketComs.BinaryReader

data class CodeOfsSummary(val ofs: Int,
                          val count: Int, val durationCount: Int, val valueCount: Int, val specificValueCount: Int,
                          val duration: Double, val durationMin: Long, val durationMax: Long, val dateMin: Long,
                          val dateMax: Long, val unit: String, val value: Double, val valueMin: Double,
                          val valueMax: Double, val specificValue: Double, val specificValueUnit: String)

fun BinaryReader.codeOfsSummary(len: Int) : CodeOfsSummary {
    var ofs = 0
    with (variousSummary(len,
            { value, field ->
                if (field == 1)  // OFS
                  ofs = value.toInt()  })) {
        return CodeOfsSummary(ofs, count, durationCount, valueCount, specificValueCount, duration,
                durationMin, durationMax, dateMin, dateMax, unit, value, valueMin, valueMax, specificValue, specificValueUnit)
    }
}
//    fun lookUpOfs(tags: List<YLabel>) {
//        tag_ = tags[ofs_]
//    }
//}
