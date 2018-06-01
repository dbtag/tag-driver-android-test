package org.dbtag.driver

import org.dbtag.data.Attachment
import org.dbtag.data.Tag
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

// sys topic extension methods

suspend fun Queue.delete(idOrMid: String) = suspendCoroutine<TAndMs<IntArray>> { cont->
    val message = MessageBuilder()
    message.references = idOrMid
    message.content = "#sys.delete"
    insert(message.contentAsString(), cont = cont)
}

suspend fun Queue.unDelete(idOrMid: String) = suspendCoroutine<TAndMs<IntArray>> { cont->
    val message = MessageBuilder()
    message.references = idOrMid
    message.content = "#sys.delete=0"
    insert(message.contentAsString(), cont = cont)
}

suspend fun Queue.note(idOrMid: String, noterUser: String = "") = suspendCoroutine<TAndMs<IntArray>> { cont->
    noteHelper(noterUser, idOrMid, 1, cont)
}

suspend fun Queue.unNote(idOrMid: String, noterUser: String = "") = suspendCoroutine<TAndMs<IntArray>> { cont->
    noteHelper(noterUser, idOrMid, 0, cont)
}

private fun Queue.noteHelper(noterUser: String, idOrMid: String, value: Int, cont: Continuation<TAndMs<IntArray>>) {
    var content = "#sys.note"
    if (value == 0) content += "=0"
    val message = MessageBuilder()
    message.references = idOrMid
    message.content = content
    if (!noterUser.isEmpty())
        message.from = Tag("user.$noterUser")
    insert(message.contentAsString(), cont = cont)
}

suspend fun Queue.setColleague(user: String, fromUser: String = "") = suspendCoroutine<TAndMs<IntArray>> { cont->
    setColleagueHelper(fromUser, user, 1, cont)
}


suspend fun Queue.setUnColleague(user: String, fromUser: String = "") = suspendCoroutine<TAndMs<IntArray>> { cont->
    setColleagueHelper(fromUser, user, 0, cont)
}

private fun Queue.setColleagueHelper(fromUser: String, user: String, value: Int, cont: Continuation<TAndMs<IntArray>>) {
    var content = "#sys.colleague"
    if (value == 0) content += "=0"
    content += " #user." + user
    colleagueFollowHelper(content, fromUser, cont)
}

suspend fun Queue.setFollow(tag: String, method: Int, fromUser: String = "") = suspendCoroutine<TAndMs<IntArray>> { cont->
    val content = "#sys.f=" + Integer.toString(method) + " #" + tag
    colleagueFollowHelper(content, fromUser, cont)
}

private fun Queue.colleagueFollowHelper(content: String, fromUser: String, cont: Continuation<TAndMs<IntArray>>) {
    val message = MessageBuilder()
    message.content = content
    if (!fromUser.isEmpty())
        message.from = Tag("user." + fromUser, "")
    insert(message.contentAsString(), cont = cont)
}

// TODO: this profile image might be far too high-res !
suspend fun Queue.setProfile(imageFileName: String, tag: Tag? = null) = suspendCoroutine<TAndMs<IntArray>> { cont->
    val message = MessageBuilder()
    message.content = "#sys.p"
    if (tag != null)
        message.from = tag
    message.attachments = listOf(Attachment("profile.jpg", File(imageFileName).readFileBytes()))
    insert(message.contentAsString(), cont = cont)
}

suspend fun Queue.setCover(imageFileName: String, tag: Tag? = null) = suspendCoroutine<TAndMs<IntArray>> { cont->
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
