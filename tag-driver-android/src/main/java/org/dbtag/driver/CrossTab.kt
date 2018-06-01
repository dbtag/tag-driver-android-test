package org.dbtag.driver

import org.dbtag.data.*
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

suspend fun Queue.crossTab(filter: Filter = Filter.empty, ifUpdatedAfter: Long = 0L, topic1: String,
                           topic2: String, tagValue: String = "") = suspendCoroutine<TAndMs<CrossTab>>
  { cont-> crossTab(filter, ifUpdatedAfter, topic1, topic2, tagValue, cont) }

/**
 * Gets a TopicSummary[] array
 */
fun Queue.crossTab(filter: Filter = Filter.empty, ifUpdatedAfter: Long = 0L, topic1: String,
                   topic2: String, tagValue: String = "", cont: Continuation<TAndMs<CrossTab>>) {
    queue({
        with(getWriter(TagClient.CrossTab)) {
            if (filter !== Filter.empty) {
                val emb = embeddedField(1) // FILTER
                filter.write(this)
                emb.close()
            }
            if (ifUpdatedAfter != 0L)
                writeFieldFixed64(2, ifUpdatedAfter) // IF_UPDATED_AFTER
            writeField(3, topic1) // TOPIC1
            writeField(4, topic2) // TOPIC2
            if (!tagValue.isEmpty())
                writeField(5, tagValue) // TAG_VALUE
            toByteArray()
        }}, { it.crossTab(topic1, topic2, tagValue) }, null, cont)
}



private fun BinaryReader.crossTab(topic1: String, topic2: String, tagValue: String?): CrossTab {
    val code1Summaries = mutableListOf<Code1Summaries>()
    val code2Tags = mutableListOf<Tag>()
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
                when (field) {
                    1 -> // CODE2_CODE_AND_NAMES
                        // All the codes and names arrive together
                        run {
                            val eor2 = position + len
                            while (position != eor2) {
                                val code = readString()
                                val name = readString()
                                val tag = topic2 + "." + code
                                code2Tags.add(Tag(tag, name))
                            }
                        }

                    2 -> { // CODE1
                        // TODO: maybe want to limit the maximum number of code1Summaries we can get, with all others going into "other"
                        // but how do we assess which are most important at the server end because there are many different
                        // possibilities of summarized dataPoints_ ?
                        var code1Code = ""
                        var code1Name = ""
                        val summaries = mutableListOf<CodeOfsSummary>()
                        val eor2 = position + len
                        while (position != eor2) {
                            val key2 = readByte().toInt()
                            val field2 = (key2 shr 3)
                            when (key and 7) {
                                WireType.VARINT -> readVarint()
                                WireType.FIXED64 -> skip(8)
                                WireType.FIXED32 -> skip(4)
                                WireType.LENGTH_DELIMITED -> {
                                    val len2 = readVarint().toInt()
                                    when (field2) {
                                        1 -> code1Code = readString(len2) // CODE1_CODE
                                        2 -> code1Name = readString(len2) // CODE1_NAME
                                        3 -> { // CODE2_OFS_AND_SUMMARY
                                            val summary = codeOfsSummary(len2)
                                           // summary.lookUpOfs(code2TagAndNames)
                                            summaries.add(summary)
                                        }
                                        else -> skip(len2)
                                    }
                                }
                            }
                        }
                        val tag = topic1 + "." + code1Code
                        code1Summaries.add(Code1Summaries(Tag(tag, code1Name), summaries))
                    }
                    else -> skip(len)
                }
            }
        }
    }
    return CrossTab(topic1, topic2, tagValue, code1Summaries, code2Tags)
}
