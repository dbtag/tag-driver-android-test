package org.dbtag.data

import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader

data class VariousSummary(val count: Int, val durationCount: Int, val valueCount: Int, val specificValueCount: Int,
                          val duration: Double, val durationMin: Long, val durationMax: Long, val dateMin: Long,
                          val dateMax: Long, val unit: String, val value: Double, val valueMin: Double,
                          val valueMax: Double, val specificValue: Double, val specificValueUnit: String) {

    fun getValue(which: Int): Double {
        when (which) {
            VALUE_COUNT -> return count.toDouble()
            VALUE_DURATION -> return duration
        }
        return 0.0
    }

    companion object {
        const val VALUE_COUNT = 0
        const val VALUE_DURATION = 1
    }
}

val VariousSummary.durationAvg get() = if (durationCount == 0) 0 else (duration / durationCount).toInt()
val VariousSummary.valueAvg get() = if (valueCount == 0) 0.0 else value / valueCount
val VariousSummary.specificValueAvg get() = if (specificValueCount == 0) 0.0 else value / specificValueCount
val VariousSummary.valueRate get() = if (duration == 0.0) 0.0 else value / duration
val VariousSummary.specificValueRate get() = if (duration == 0.0) 0.0 else specificValue / duration


fun BinaryReader.variousSummary(len: Int, onVarint: ((value: Long, field: Int) -> Unit)? = null,
                                onLengthDelimited: ((reader: BinaryReader, len: Int, field: Int) ->Unit)? = null) : VariousSummary {
    var count = 0
    var durationCount = 0
    var valueCount = 0
    var specificValueCount = 0
    var duration = 0.0
    var durationMin = 0L
    var durationMax = 0L
    var dateMin = 0L
    var dateMax = 0L
    var unit = ""
    var value = 0.0
    var valueMin = 0.0
    var valueMax = 0.0
    var specificValue = 0.0
    var specificValueUnit = ""

    val eor = position + len
    while (position != eor) {
        //  Could only use readByte here instead of readVarint if the maximum field number is 15
        val key = readVarint().toInt()
        val field = (key shr 3)
        when (key and 7) {
            WireType.VARINT -> {
                val v = readVarint()
                @Suppress("LeakingThis")
                when (field) {
                    6 -> count = v.toInt() // COUNT
                    7 -> durationCount = v.toInt() // DURATION_COUNT
                    8 -> valueCount = v.toInt() // VALUE_COUNT
                    9 -> specificValueCount = v.toInt() // SPECIFIC_VALUE_COUNT
                    11 -> durationMin = v // DURATION_MIN
                    12 -> durationMax = v // DURATION_MAX
                    13 -> dateMin = v // DATE_MIN
                    14 -> dateMax = v // DATE_MAX
                    else -> onVarint?.invoke(v, field)
                }
            }
            WireType.FIXED64 -> {
                val v = readDouble()
                when (field) {
                    10 -> duration = v // DURATION
                    16 -> value = v // VALUE
                    17 -> valueMin = v // VALUE_MIN
                    18 -> valueMax = v // VALUE_MAX
                    20 -> specificValue = v // SPECIFIC_VALUE
                }
            }
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> {
                val len2 = readVarint().toInt()
                when (field) {
                    15 -> unit = readString(len2) // UNIT
                    19  -> specificValueUnit = readString(len2) // SPECIFIC_VALUE_UNIT
                    else -> {
                        if (onLengthDelimited != null)
                            onLengthDelimited(this, len2, field)
                        else
                          skip(len)
                    }
                }
            }
        }
    }
    return VariousSummary(count, durationCount, valueCount, specificValueCount,
            duration, durationMin, durationMax, dateMin, dateMax, unit, value, valueMin,
            valueMax, specificValue, specificValueUnit)
//
//    protected open fun onVarint(value: Long, fieldNumber: Int) {}
//    protected open fun onLengthDelimited(reader: BinaryReader, len: Int, fieldNumber: Int) {
//        reader.skip(len)
//    }

}
