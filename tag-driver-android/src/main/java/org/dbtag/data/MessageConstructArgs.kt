package org.dbtag.data

import org.dbtag.driver.*
import org.dbtag.driver.Queue
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader
import java.util.*
import kotlin.coroutines.experimental.Continuation

// Carries information that will be used to construct some kind of message
class MessageConstructArgs {
    var mid: Int = 0
    var id: String = ""
    var date: Long = 0
    var until: Long = 0
    var content: String = ""
    var deleted: Boolean = false
    var updated: Long = 0
    var importance: Int = 0
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var firstThumbnail: ByteArray? = null

    private var _attachments = mutableListOf<NameAndSize>()
    private var _comments =  mutableListOf<TagNameDateContentAttachments>()
    private var _noters = mutableListOf<TagNameDate>()
    private var _takers =  mutableListOf<TagNameDate>()
    private var _notified =  mutableListOf<Tag>()

    val attachments get() = _attachments.toList()
    val notified get() = _notified.toList()
    val comments get() = _comments.toList()
    val noters get() = _noters.toList()
    val takers get() = _takers.toList()


    private var _codeComments: MutableMap<String, MutableSet<TagNameDate>>? = null // by the topic in #sys.topic

    private fun clear() {
        mid = 0
        id = ""
        date = 0
        until = 0
        content = ""
        deleted = false
        updated = 0
        importance = 0
        latitude = 0.0
        longitude = 0.0
        firstThumbnail = null
        _attachments.clear()
        _comments.clear()
        _noters.clear()
        _takers.clear()
        _notified.clear()
        _codeComments?.clear()
    }

    fun BinaryReader.read(len: Int) {
        clear()

        val eor = position + len
        while (position != eor) {
            val key = readVarint().toInt()
            val field = key shr 3
            when (key and 7) {
                WireType.VARINT -> {
                    val value = readVarint()
                    when (field) {
                        MESSAGE_MID -> mid = value.toInt()
                        MESSAGE_IMPORTANCE -> importance = value.toInt()
                    }
                }
                WireType.FIXED64 -> {
                    when (field) {
                        MESSAGE_DATE -> date = readLong()
                        MESSAGE_UNTIL -> until = readLong()
                        MESSAGE_UPDATED -> updated = readLong()
                        MESSAGE_LATITUDE -> latitude = readDouble()
                        MESSAGE_LONGITUDE -> longitude = readDouble()
                        else -> skip(8)
                    }
                }
                WireType.FIXED32 -> skip(4)
                WireType.LENGTH_DELIMITED -> {
                    val len2 = readVarint().toInt()
                    when (field) {
                        MESSAGE_ID -> id = readString(len2)
                        MESSAGE_COMMENT -> {
                            val comment = tagNameDateContentAttachments(len2)
                            // A convenient place to separate out notes, takes, deletes, shares etc
                            // note, take and share are per user, while delete is from any user

                            // See if there is a #user and a #sys/code
                            var user: Tag? = null
                            var sysTag: Tag? = null
                            for (tn in comment.content.parseTagsOnly()) {
                                if (tn.topic == "user") {
                                    if (user == null) user = tn
                                }
                                else if (tn.topic == "sys") {
                                    if (sysTag == null) sysTag = tn
                                }
                            }

                            var discardComment = false
                            if (user != null && sysTag != null) {
                                discardComment = true
                                val code = sysTag.code
                                if (code == "delete")
                                    deleted = sysTag.value != 0.0
                                else {
                                    // The last occurrence of a code for a user is the one that matters
                                    val userDate = TagNameDate(user.tag, user.originalName, comment.date)
                                    val codeComments = _codeComments.let {
                                        it ?: HashMap<String, MutableSet<TagNameDate>>().apply { _codeComments = this }
                                    }

                                    var coll = codeComments[code]
                                    if (coll == null) {
                                        coll = HashSet()
                                        codeComments[code] = coll
                                    }
                                    if (sysTag.value != 0.0)
                                        coll.add(userDate)  // if already there, nothing happens
                                    else
                                        coll.remove(userDate)
                                }
                            }
                            if (!discardComment)
                                _comments.add(comment)
                        }

                        MESSAGE_ATTACHMENT -> {
                            val a = nameAndSize(len2)
                            val count = 1 // random.nextInt(2) + 2
                            (0 until count).forEach { _attachments.add(a) }
                        }

                        MESSAGE_NOTIFIED -> _notified.add(tag(len2))

                        MESSAGE_CONTENT -> content = readString(len2)

                        MESSAGE_THUMBNAIL -> firstThumbnail =  thumbnailBytes(len2)

                        else -> skip(len2)
                    }
                }
            }
        }

        _codeComments?.let {
            val coll = it["note"]
            if (coll != null && coll.isNotEmpty()) {
                _noters.addAll(coll)
                // Should be in messageDate order
                _noters.sortBy({ it.date})
            }
        }

        if (_attachments.size > 1) {
            val z = 3
        }

    }

    companion object {
        private const val MESSAGE_MID = 1
        private const val MESSAGE_ID = 2
        const val MESSAGE_DATE = 3
        private const val MESSAGE_UNTIL = 4
        const val MESSAGE_CONTENT = 5
        private const val MESSAGE_UPDATED = 6
        private const val MESSAGE_IMPORTANCE = 7
        private const val MESSAGE_LATITUDE = 8
        private const val MESSAGE_LONGITUDE = 9
        private const val MESSAGE_COMMENT = 10
        const val MESSAGE_ATTACHMENT = 11
        private const val MESSAGE_JOINED_TAG = 12
        const val MESSAGE_THUMBNAIL = 13
        const val MESSAGE_NOTIFIED = 14
    }
}


fun Queue.asyncMessage(mid: Int, parts: Int, cont: Continuation<TAndMs<MessageConstructArgs>>) {
    queue({
        with(getWriter(TagClient.Message)) {
            val MID = 1
            val PARTS = 2
            writeFieldVarint(MID, mid.toLong())
            writeFieldVarint(PARTS, parts.toLong())
            toByteArray()
        }}, {
        reader:BinaryReader -> MessageConstructArgs().apply {
        with(reader) {
            read(unreadBytesCount())
        }
    }}, null, cont)
}
