package org.dbtag.driver

import org.dbtag.data.Attachment
import org.dbtag.data.Tag
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

// sys topic extension methods

suspend fun Queue.asyncDelete(idOrMid: String) = suspendCoroutine<TAndMs<IntArray>> { cont->
    val message = MessageBuilder()
    message.references = idOrMid
    message.content = "#sys.delete"
    insert(message.contentAsString(), cont = cont)
}

suspend fun Queue.asyncUnDelete(idOrMid: String) = suspendCoroutine<TAndMs<IntArray>> { cont->
    val message = MessageBuilder()
    message.references = idOrMid
    message.content = "#sys.delete=0"
    insert(message.contentAsString(), cont = cont)
}

suspend fun Queue.asyncNote(idOrMid: String, noterUser: String = "") = suspendCoroutine<TAndMs<IntArray>> { cont->
    asyncNoteHelper(noterUser, idOrMid, 1, cont)
}

suspend fun Queue.asyncUnNote(idOrMid: String, noterUser: String = "") = suspendCoroutine<TAndMs<IntArray>> { cont->
    asyncNoteHelper(noterUser, idOrMid, 0, cont)
}

private fun Queue.asyncNoteHelper(noterUser: String, idOrMid: String, value: Int, cont: Continuation<TAndMs<IntArray>>) {
    var content = "#sys.note"
    if (value == 0) content += "=0"
    val message = MessageBuilder()
    message.references = idOrMid
    message.content = content
    if (!noterUser.isEmpty())
        message.from = Tag("user." + noterUser)
    insert(message.contentAsString(), cont = cont)
}

suspend fun Queue.asyncSetColleague(user: String, fromUser: String = "") = suspendCoroutine<TAndMs<IntArray>> { cont->
    asyncSetColleagueHelper(fromUser, user, 1, cont)
}


suspend fun Queue.asyncSetUnColleague(user: String, fromUser: String = "") = suspendCoroutine<TAndMs<IntArray>> { cont->
    asyncSetColleagueHelper(fromUser, user, 0, cont)
}

private fun Queue.asyncSetColleagueHelper(fromUser: String, user: String, value: Int, cont: Continuation<TAndMs<IntArray>>) {
    var content = "#sys.colleague"
    if (value == 0) content += "=0"
    content += " #user." + user
    asyncColleagueFollowHelper(content, fromUser, cont)
}

suspend fun Queue.asyncSetFollow(tag: String, method: Int, fromUser: String = "") = suspendCoroutine<TAndMs<IntArray>> { cont->
    val content = "#sys.f=" + Integer.toString(method) + " #" + tag
    asyncColleagueFollowHelper(content, fromUser, cont)
}

private fun Queue.asyncColleagueFollowHelper(content: String, fromUser: String, cont: Continuation<TAndMs<IntArray>>) {
    val message = MessageBuilder()
    message.content = content
    if (!fromUser.isEmpty())
        message.from = Tag("user." + fromUser, "")
    insert(message.contentAsString(), cont = cont)
}

// TODO: this profile image might be far too high-res !
suspend fun Queue.asyncSetProfile(imageFileName: String, tag: Tag? = null) = suspendCoroutine<TAndMs<IntArray>> { cont->
    val message = MessageBuilder()
    message.content = "#sys.p"
    if (tag != null)
        message.from = tag
    message.attachments = listOf(Attachment("profile.jpg", File(imageFileName).readFileBytes()))
    insert(message.contentAsString(), cont = cont)
}

suspend fun Queue.asyncSetCover(imageFileName: String, tag: Tag? = null) = suspendCoroutine<TAndMs<IntArray>> { cont->
    val message = MessageBuilder()
    message.content = "#sys.c"
    if (tag != null)
        message.from = tag
//    message.attachments = arrayOf(imageFileName.asAttachmentBytes())
    insert(message.contentAsString(), cont = cont)
}


fun File.readFileBytes(): ByteArray {
    val byteCount = length().toInt()
    val buffer = ByteArray(byteCount)
    val stm = FileInputStream(this)
    stm.read(buffer, 0, byteCount)
    stm.close()
    return buffer
}
