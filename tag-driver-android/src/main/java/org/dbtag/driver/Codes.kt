package org.dbtag.driver

import org.dbtag.data.*
import org.dbtag.socketComs.BinaryReader
import kotlin.coroutines.experimental.suspendCoroutine

enum class CodeSort(val v: Int) {
    Code(0), Name(1), Posts(2), Tagged(3), MRT(4), TopicDuration(5),
    TotalTagValue(6), FirstTopicName(7), Q2Duration(8) }

suspend fun Queue.codes(filter: Filter, topic: String, joinTopic: String, limit: Int,
                        sort: CodeSort, sortTopic: String, desc: Boolean,
                        code0: String) = suspendCoroutine<TAndMs<CodesResult>> { cont->

//    Filter = 1
//    Topic
//    JoinTopic
//    Limit
//    Sort
//    SortTopic
//    Desc
//    Code0

    queue({
        with(getWriter(TagClient.Codes)) {
            if (filter !== Filter.empty) {
                val emb = embeddedField(1) // FILTER
                filter.write(this)
                emb.close()
            }
            writeField(2, topic) // TOPIC
            if (!joinTopic.isEmpty())
                writeField(3, joinTopic) // JOINTOPIC
            writeFieldVarint(4, limit.toLong()) // LIMIT
            writeFieldVarint(5, sort.v.toLong()) // SORT
            if (!sortTopic.isEmpty())
                writeField(6, sortTopic) // SORTTOPIC
            if (desc)
                writeFieldVarint(7, 1L) // DESC
            if (!code0.isEmpty())
                writeField(8, code0)  // CODE0
            toByteArray()
        } }, { it.codesResult() }, null, cont)
}


private fun BinaryReader.codesResult(): CodesResult {
    val ret = mutableListOf<SuperSummary>()
    val eor = bufferSize
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            org.dbtag.protobuf.WireType.VARINT -> readVarint()
            org.dbtag.protobuf.WireType.FIXED64 -> skip(8)
            org.dbtag.protobuf.WireType.FIXED32 -> skip(4)
            org.dbtag.protobuf.WireType.LENGTH_DELIMITED -> {
                val len = readVarint().toInt()
                if (field == 1)
                    ret.add(superSummary(len))
                else
                    skip(len)
            }
        }
    }
    return CodesResult(ret)
}


private fun BinaryReader.superSummary(len: Int): SuperSummary {
    var tag = Tag.empty
    var unJoined: SimpleSummary? = null
    var joined: SimpleSummary? = null

    val eor = position + len
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            org.dbtag.protobuf.WireType.VARINT -> readVarint()
            org.dbtag.protobuf.WireType.FIXED64 -> skip(8)
            org.dbtag.protobuf.WireType.FIXED32 -> skip(4)
            org.dbtag.protobuf.WireType.LENGTH_DELIMITED -> {
                val len1 = readVarint().toInt()
                when(field) {
                    1 -> tag = tag(len1)  // TAG
                    2 -> unJoined = simpleSummary(len1)  // UNJOINED
                    3 -> joined   = simpleSummary(len1)  // JOINED
                    else -> skip(len1)
                }
            }
        }
    }
  return SuperSummary(tag, unJoined, joined)
}

private fun BinaryReader.simpleSummary(len: Int): SimpleSummary {
    var count: Count? = null
    val topics = mutableListOf<TopicData>()
    val tagsWithValues = mutableListOf<TV>()

    val eor = position + len
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            org.dbtag.protobuf.WireType.VARINT -> readVarint()
            org.dbtag.protobuf.WireType.FIXED64 -> skip(8)
            org.dbtag.protobuf.WireType.FIXED32 -> skip(4)
            org.dbtag.protobuf.WireType.LENGTH_DELIMITED -> {
                val len1 = readVarint().toInt()
                when(field) {
                    1 -> count = count(len1)  // COUNT
                    2 -> topics.add(topicData(len1))  // TOPIC
                    3 -> tagsWithValues.add(tv(len1))  // TAGWITHVALUE
                    else -> skip(len1)
                }
            }
        }
    }
    return SimpleSummary(count!!, topics, tagsWithValues)
}

private fun BinaryReader.topicData(len: Int): TopicData {
    var tag = Tag.empty
    var totalCount = 0
    var durationSum = 0L
    var plus = false
    val topTags = mutableListOf<Tag>()


    val eor = position + len
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            org.dbtag.protobuf.WireType.VARINT -> {
                val value = readVarint()
                when (field) {
                    2 -> totalCount = value.toInt() // TOTALCOUNT
                    4 -> plus = (value != 0L) // PLUS
                }
            }
            org.dbtag.protobuf.WireType.FIXED64 -> {
                when (field) {
                    3 -> durationSum = readDouble().toLong() // DURATIONSUM
                    else -> skip(8)
                }
            }
            org.dbtag.protobuf.WireType.FIXED32 -> skip(4)
            org.dbtag.protobuf.WireType.LENGTH_DELIMITED -> {
                val len1 = readVarint().toInt()
                when(field) {
                    1 -> tag = tag(len1)  // TAG
                    5 -> topTags.add(tag(len1))  // TOPTAG
                    else -> skip(len1)
                }
            }
        }
    }
    return TopicData(tag, totalCount, durationSum, plus, topTags)
}


private fun BinaryReader.tv(len: Int): TV {
    var tag = Tag.empty
    var count = 0
    var sum = 0.0
    val values= mutableListOf<Double>()
    var unit = ""

    val eor = position + len
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            org.dbtag.protobuf.WireType.VARINT -> {
                val value = readVarint()
                when (field) {
                    2 -> count = value.toInt() // COUNT
                }
            }
            org.dbtag.protobuf.WireType.FIXED64 -> {
                when (field) {
                    3 -> sum = readDouble() // SUM
                    else -> skip(8)
                }
            }
            org.dbtag.protobuf.WireType.FIXED32 -> skip(4)
            org.dbtag.protobuf.WireType.LENGTH_DELIMITED -> {
                val len1 = readVarint().toInt()
                when(field) {
                    1 -> tag = tag(len1)  // TAG
                    4 -> {                // VALUES
                        val eor2 = position + len1  // an array of values
                        while (position != eor2)
                            values.add(readDouble())
                    }
                    5 -> unit = readString(len1)  // UNIT
                    else -> skip(len1)
                }
            }
        }
    }
    return TV(tag, count, sum, values, unit)
}

