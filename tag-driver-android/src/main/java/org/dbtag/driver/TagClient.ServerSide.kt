package org.dbtag.driver

import org.dbtag.data.*
import org.dbtag.protobuf.WireType
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine


//suspend fun Queue.getLastMessageFirstThumbnailAsync(toFilter: Filter, maxSize: Int, thumbnailCache: ThumbnailCache)
//        = suspendCoroutine<ByteArray> { cont->
//    getLastMessageFirstThumbnailAsync0(toFilter, maxSize, thumbnailCache, cont)
//}

// TODO: It would be nicer to do this in just one coms action using a new feature of MessagesAsync that returns attachment bytes in place
// and also shrinks any bitmaps to the maxsize...
fun Queue.getLastMessageFirstThumbnailAsync0(filter: Filter, maxSize: Int, thumbnailCache: ThumbnailCache, cont: Continuation<ByteArray?>) {
    // Get the mid of the most recent message tagging these required tags and then look up that message's item1 attachment bitmap, if any
    MessageMid.asyncFind0(this, filter, 1, desc = true, cont = object: Continuation<TAndMs<MessagesData<MessageMid>>>{
        override val context get() = EmptyCoroutineContext
        override fun resume(value: TAndMs<MessagesData<MessageMid>>) {
            val messages = value.t.messages
            if (messages.size == 1)
                thumbnailCache.getThumbnail(messages[0].mid, 0, 0, maxSize, cont)
            else
                cont.resume(null)
        }
        override fun resumeWithException(exception: Throwable) = cont.resumeWithException(exception)
    })
}


//// TODO: It would be nicer to do this in just one coms action using a new server feature
//fun Queue.getLastMessageFirstAttachmentAsync(toFilter: Filter, callback: ResultCallback<Attachment>) {
//    // Get the mid of the most recent message tagging these required tags
//    asyncSelect(toFilter, 1, 0, true, MessageMid.parts, MessageMid.construct, { _, result ->
//        if (result != null) {
//            val messages = result.messages
//            if (messages.size == 1) {
//                asyncAttachment((messages[0] as MessageMid).mid, 0, 0, callback)
//                return@asyncSelect
//            }
//        }
//        callback(null, null)   // return null
//    })
//}


suspend fun <T> Queue.asyncLastValue(filter: Filter, ifUpdatedAfter: Long, valueTag: String, excludeTag: String, excludeTopic: String,
                                     includeZeroValue: Boolean, lastMessageParts: Int, cons: (Tag, Long, MessageConstructArgs?) -> T) = suspendCoroutine<TAndMs<Pair<Long, List<T>>>> { cont ->
    asyncLastValue0(filter, ifUpdatedAfter, valueTag, excludeTag, excludeTopic, includeZeroValue, lastMessageParts, cons, cont)
}

// TODO: what about "joins() As Join"  ?
fun <T> Queue.asyncLastValue0(filter: Filter, ifUpdatedAfter: Long, valueTag: String, excludeTag: String, excludeTopic: String,
                              includeZeroValue: Boolean, lastMessageParts: Int, cons: (Tag, Long, MessageConstructArgs?) -> T,
                              cont: Continuation<TAndMs<Pair<Long, List<T>>>>) {
    queue({
        with(getWriter(TagClient.LastValue)) {
            val FILTER = 1
            val JOIN = 2  // TODO: not used
            val IF_UPDATED_AFTER = 3
            val VALUE_TAG = 4
            val EXCLUDE_TAG = 5
            val EXCLUDE_TOPIC = 6
            val INCLUDE_ZERO_VALUE = 7
            val LAST_MESSAGE_PARTS = 8
            if (filter !== Filter.empty) {
                val emb = embeddedField(FILTER)
                filter.write(this)
                emb.close()
            }
            if (ifUpdatedAfter != 0L)
                writeFieldFixed64(IF_UPDATED_AFTER, ifUpdatedAfter)

            writeField(VALUE_TAG, valueTag)
            if (!excludeTag.isEmpty())
                writeField(EXCLUDE_TAG, excludeTag)
            if (!excludeTopic.isEmpty())
                writeField(EXCLUDE_TOPIC, excludeTopic)
            if (includeZeroValue)
                writeFieldVarint(INCLUDE_ZERO_VALUE, 1L)
            if (lastMessageParts != 0)
                writeFieldVarint(LAST_MESSAGE_PARTS, lastMessageParts.toLong())
            toByteArray()
        }}, { with(it) {
            var serverTime: Long = 0
            val ret = mutableListOf<T>()
            var lastTagNameValue: Tag? = null
            var lastDate: Long = 0
            val args = MessageConstructArgs()

            val eor = bufferSize
            while (position != eor) {
                val SERVER_TIME = 1
                val TAG_NAME_VALUE = 2
                val DATE = 3
                val LAST_MESSAGE = 4

                val key = readByte().toInt()
                val field = (key shr 3)
                when (key and 7) {
                    WireType.VARINT -> {
                        val value = readVarint()
                        if (field == SERVER_TIME)
                            serverTime = value
                    }
                    WireType.FIXED64 -> skip(8)
                    WireType.FIXED32 -> skip(4)
                    WireType.LENGTH_DELIMITED -> {
                        val len = readVarint().toInt()
                        when (field) {
                            TAG_NAME_VALUE -> {
                                if (lastTagNameValue != null) {
                                    val x = cons(lastTagNameValue, lastDate, null)
                                    if (x != null)
                                      ret.add(x)
                                }
                                lastTagNameValue = tag(len)
                                lastDate = 0
                            }
                            LAST_MESSAGE -> {
                                with(args) { read(len) }
                                if (lastTagNameValue != null) {
                                    val x = cons(lastTagNameValue, lastDate, args)
                                    if (x != null)
                                      ret.add(x)
                                    lastTagNameValue = null
                                }
                            }
                            else -> skip(len)
                        }
                    }
                }
            }
            if (lastTagNameValue != null) { // flush
                val x = cons(lastTagNameValue, lastDate, null)
                if (x != null)
                  ret.add(x)
            }
            Pair(serverTime, ret)
        } }, null, cont)
}


// Specifics
//suspend fun <T> Queue.asyncUsers(toFilter: Filter, ifUpdatedAfter: Long, lastMessageParts: Int, cons: (YLabel, Long, MessageConstructArgs?) -> T)
//        = suspendCoroutine<Pair<Long, List<T>>> { cont ->
//    asyncLastValue0(toFilter, ifUpdatedAfter, "sys.liveuser", null, null, false, lastMessageParts, cons, cont)
//}
//
//suspend fun <T> UserQueue.asyncFollowing(user: String = this.user, toFilter: Filter, ifUpdatedAfter: Long, lastMessageParts: Int, cons: (YLabel, Long, MessageConstructArgs?) -> T)
//        = suspendCoroutine<Pair<Long, List<T>>> { cont ->
//    asyncLastValue0(toFilter.copy().require("user." + user), ifUpdatedAfter, "sys.follow", null, "user", false, lastMessageParts, cons, cont)
//}
//
//suspend fun <T> UserQueue.asyncBookmarks(user: String = this.user, toFilter: Filter, ifUpdatedAfter: Long, lastMessageParts: Int, cons: (YLabel, Long, MessageConstructArgs?) -> T)
//        = suspendCoroutine<Pair<Long, List<T>>> { cont ->
//    asyncLastValue0(toFilter.copy().require("user." + user), ifUpdatedAfter, "sys.bookmark", null, "user", false, lastMessageParts, cons, cont)
//}
//
//// Good enough for getting one user's colleagues, provided we post-toFilter for topic="user"
//// TODO: pairs of colleagues requires something else
//suspend fun <T> UserQueue.asyncColleagues(user: String = this.user, toFilter: Filter, ifUpdatedAfter: Long, lastMessageParts: Int, cons: (YLabel, Long, MessageConstructArgs?) -> T)
//        = suspendCoroutine<Pair<Long, List<T>>> { cont->
//    val userTag = "user." + user
//    asyncLastValue0(toFilter.copy().require(userTag), ifUpdatedAfter, "sys.colleague", userTag, null, false, lastMessageParts, cons, cont)
//}
//
//// Simple versions
//suspend fun Queue.asyncUsers() = suspendCoroutine<List<YLabel>> { cont ->
//    asyncLastValue0(Filter.empty, 0L, "sys.liveuser", null, null, false, 0, { tn, _, _ -> tn},
//      object: Continuation<Pair<Long, List<YLabel>>> {
//        override val context = cont.context
//        override fun resumeWithException(exception: Throwable) = cont.resumeWithException(exception)
//        override fun resume(value: Pair<Long, List<YLabel>>) = cont.resume(value.second)
//    })
//}
//
//suspend fun UserQueue.asyncColleagues(user: String = this.user) = suspendCoroutine<List<YLabel>> { cont->
//    val userTag = "user." + user
//    asyncLastValue0(Filter(requiredTags = arrayOf(userTag)), 0L, "sys.colleague", userTag, null, false, 0, { tn, _, args -> tn},
//            object: Continuation<Pair<Long, List<YLabel>>> {
//                override val context = cont.context
//                override fun resumeWithException(exception: Throwable) = cont.resumeWithException(exception)
//                override fun resume(value: Pair<Long, List<YLabel>>) = cont.resume(value.second)
//            })
//}
//
//suspend fun UserQueue.asyncFollowing(user: String = this.user) = suspendCoroutine<List<YLabel>> { cont->
//    asyncLastValue0(Filter(requiredTags = arrayOf("user." + user)), 0L, "sys.follow", null, "user", false, 0,
//            { tn, _, _ -> tn },
//            object: Continuation<Pair<Long, List<YLabel>>> {
//                override val context = cont.context
//                override fun resumeWithException(exception: Throwable) = cont.resumeWithException(exception)
//                override fun resume(value: Pair<Long, List<YLabel>>) = cont.resume(value.second)
//            })
//}

//// TODO: asyncFollowedBy is not correct
//suspend fun Queue.asyncFollowedBy(tag: String) = suspendCoroutine<Array<YLabel>> { cont->
//    select(Filter().require("sys.follow", tag), 0, Filter.empty, Integer.MAX_VALUE, 0, true,
//               MessageTagsNotInFilter.parts, MessageTagsNotInFilter.construct, null,
//        object : Continuation<MessagesData<MessageTagsNotInFilter>> {
//            override val context get() = cont.context
//            override fun resumeWithException(exception: Throwable) = cont.resumeWithException(exception)
//
//            override fun resume(value: MessagesData<MessageTagsNotInFilter>) {
//                val coll = ArrayList<YLabel>()
//                for (msg in value.messages) {
//                    for (tn in msg.tagsNotInFilter)
//                        if (tn.topic == "user") {
//                            coll.add(tn)
//                            break
//                        }
//                }
//                cont.resume(coll)
//            }
//        })
//}

//suspend fun UserQueue.asyncBookmarks(user: String = this.user) = suspendCoroutine<List<YLabel>> { cont->
//    asyncLastValue0(Filter(requiredTags = arrayOf("user." + user)), 0L, "sys.bookmark", null, "user", false, 0,
//            { tn, _, _ -> tn },
//            object: Continuation<Pair<Long, List<YLabel>>> {
//                override val context = cont.context
//                override fun resumeWithException(exception: Throwable) = cont.resumeWithException(exception)
//                override fun resume(value: Pair<Long, List<YLabel>>) = cont.resume(value.second)
//            })
//}
