package org.dbtag.data

import org.dbtag.data.WhatSummary.*
import org.dbtag.driver.TagSummary
import org.dbtag.driver.tagSummary
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader

data class TopicSummary(val totalCount: Int, val all0: List<TagSummary>, val byCount: List<Int>, val byValue: List<Int>,
                        val byValueAvg: List<Int>, val byValueMin: List<Int>, val byValueMax: List<Int>,
                        val byDuration: List<Int>, val byDurationAvg: List<Int>, val byDurationMin: List<Int>,
                        val byDurationMax: List<Int>, val bySValue: List<Int>, val bySValueAvg: List<Int>)

fun TopicSummary.total() = all0[0]


enum class WhatSummary {
    COUNT, VALUE, VALUE_AVG, VALUE_MIN, VALUE_MAX,
    DURATION, DURATION_AVG, DURATION_MIN, DURATION_MAX,
    SPECIFIC_VALUE, SPECIFIC_VALUE_AVG
}

// Choose one of the dimensions
operator fun TopicSummary.get(what: WhatSummary): List<TagSummary> {
    return when (what) {
        COUNT -> byCount
        VALUE -> byValue
        VALUE_AVG -> byValueAvg
        VALUE_MIN -> byValueMin
        VALUE_MAX -> byValueMax
        DURATION -> byDuration
        DURATION_AVG -> byDurationAvg
        DURATION_MIN -> byDurationMin
        DURATION_MAX -> byDurationMax
        SPECIFIC_VALUE -> bySValue
        SPECIFIC_VALUE_AVG -> bySValueAvg
    }.map { i -> all0[i] }
}


fun BinaryReader.topicSummary(len: Int) : TopicSummary {
    var totalCount = 0
    val all = mutableListOf<TagSummary>()
    val byCount = mutableListOf<Int>()
    val byValue = mutableListOf<Int>()
    val byValueAvg = mutableListOf<Int>()
    val byValueMin = mutableListOf<Int>()
    val byValueMax = mutableListOf<Int>()
    val byDuration = mutableListOf<Int>()
    val byDurationAvg = mutableListOf<Int>()
    val byDurationMin = mutableListOf<Int>()
    val byDurationMax = mutableListOf<Int>()
    val bySValue = mutableListOf<Int>()
    val bySValueAvg = mutableListOf<Int>()

    val eor = position + len
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            WireType.VARINT -> {
                val value = readVarint()
                if (field == 1)  // TOTAL_COUNT
                    totalCount = value.toInt()
            }
            WireType.FIXED64 -> skip(8)
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> {
                val len2 = readVarint().toInt()
                when (field) {
                    2 -> all.add(tagSummary(len2))  // ALL
                    3 -> readInts(byCount, len2)  // BY_COUNT
                    4 -> readInts(byValue, len2)  // BY_VALUE
                    5 -> readInts(byValueAvg, len2)  // BY_VALUE_AVG
                    6 -> readInts(byValueMin, len2)  // BY_VALUE_MIN
                    7 -> readInts(byValueMax, len2)  // BY_VALUE_MAX
                    8 -> readInts(byDuration, len2)  // BY_DURATION
                    9 -> readInts(byDurationAvg, len2)  // BY_DURATION_AVG
                    10 -> readInts(byDurationMin, len2)  // BY_DURATION_MIN
                    11 -> readInts(byDurationMax, len2)  // BY_DURATION_MAX
                    12 -> readInts(bySValue, len2)  // BY_SPECIFIC_VALUE
                    13 -> readInts(bySValueAvg, len2)  // BY_SPECIFIC_VALUE_AVG
                    else -> skip(len2)
                }
            }
        }
    }
    return TopicSummary(totalCount, all, byCount, byValue, byValueAvg, byValueMin, byValueMax,
                        byDuration, byDurationAvg, byDurationMin, byDurationMax, bySValue, bySValueAvg)
}

private fun BinaryReader.readInts(coll: MutableList<Int>, len: Int) {
    val eor2 = position + len
    while (position != eor2)
        coll.add(readVarint().toInt())
}
