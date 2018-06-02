package org.dbtag.driver

import org.dbtag.data.Attachment
import org.dbtag.data.Tag
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

class MessageBuilder {
    var id = ""
    var references = ""
    var from: Tag? = null
    var content: Any? = null  // String or YLabel
    var importance = 0
    var date = 0L
    var until = 0L
    var latitude = 0.toDouble()
    var longitude = 0.toDouble()
    var attachments: List<Attachment>? = null

    fun hasLocation() = latitude != 0.0 && longitude != 0.0

    fun contentAsString(): String {
        val content = this.content
        if (content is String)
            return content
        if (content !is Array<*>)
            return ""
        var ofs = 0
        val sb = StringBuilder()
        for (x in content) {
            val s = x as? String
            if (ofs != 0 && (s == null || ! (s.startsWith(",") || s.startsWith("."))))
              sb.append(" ")
            sb.append(s ?: (x as Tag).asTag())
            ofs += 1
        }
        return sb.toString()
    }

    fun writeShort(): String {
        // Include all the other code1Summaries like date, until, etc...
        val l =  mutableListOf<String>()
        with (l) {
            if (date != 0L) {
                add("#d=") //  + & Tight([Date], ignoreMs))
                if (until != 0L)
                    add("#u=") //  + & Tight([until], ignoreMs))
            } else {
                if (until != 0L) {
                    add("#d=") //  + & Tight([until], ignoreMs))
                    add("#u")
                }
            }
            val isReference = references.isNotEmpty()
            if (isReference)
                add("#ref=" + references)
            else if (id.isNotEmpty())
                add("#id=" + id)

//                    If Importance <> Importance.Low Then
//                    .Add("#i=" & CType(Importance, Integer).ToString(InvariantCulture))
//            End If
//                    If Not isReference AndAlso HasLocation Then
//                    .Add("#lat=" & Latitude.ToString("N7", InvariantCulture))
//            .Add("#lon=" & Longitude.ToString("N7", InvariantCulture))
//            End If
//
//                    // Attachments also now come before the content - only the #n notifies now come after
//            If writeAttachment IsNot Nothing AndAlso attachments_.Count <> 0 Then
//                    For Each at In attachments_
//            Dim dat = [Date] : If dat.Ticks = 0 Then dat = Date.UtcNow
//            Dim str = "#a(" & at.Name & ")"
//            Dim atValue = writeAttachment?(at.Name, dat, at.Bytes)
//            If Not String.IsNullOrEmpty(atValue) Then str &= "=" & atValue
//            .Add(str)
//            Next at
//                    End If

            val con = contentAsString()
            if (con.isNotEmpty())
                add(con)
        }
        return l.joinToString(" ")
    }
}

suspend fun UserQueue.insert(message: MessageBuilder)  {
    // TODO: any mb attachments are not passed
    val text = message.writeShort()
    insert(text)
}

suspend fun UserQueue.insert(text: String, vararg attachments: ByteArray) = queue({
    with(getWriter(TagClient.Insert)) {
        writeField(1, text)  // TEXT
        for (attachment in attachments)
            writeField(2, attachment) // ATTACHMENT
        toByteArray()
    }}, { it.messagesIds(it.unreadBytesCount()) })


private fun BinaryReader.messagesIds(len: Int): IntArray {
    val ret = mutableListOf<Int>()
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
                if (field == 1) { // MIDS
                    val eor2 = position + len2
                    while (position != eor2)
                        ret.add(readVarint().toInt())
                } else
                    skip(len2)
            }
        }
    }
    // Return as a simple array of mid's
    val ret0 = IntArray(ret.size)
    for (i in ret0.indices)
        ret0[i] = ret[i]
    return ret0
}
