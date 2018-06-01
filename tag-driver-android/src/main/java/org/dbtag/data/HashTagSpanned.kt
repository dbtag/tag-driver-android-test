package org.dbtag.data

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan


/**
 * A SpannedBuilder that finds the hash tags in text.
 */
fun String.toTagSpanned(createSpan: ((Tag) -> Any?)): SpannedBuilder {
    val ret = SpannedBuilder()
    val sb = StringBuilder(length)
    for (any in parseTags()) {
        if (any is Tag) {
            val s = sb.length
            sb.append(any.toString())
            val span = createSpan(any)
            if (span != null)
                ret.append(s, sb.length, 0, span)
            }
        else
            sb.append(any.toString())
        }
    ret.setText(sb.toString())
    return ret
    }

fun String.toTagFlattened(): String {
    val sb = StringBuilder(length)
    for (any in parseTags())
        sb.append(any.toString())
    return sb.toString()
}

// TODO: should highlight the tag(s) that have resulted in us being notified
fun String.toBoldFromTagFlattened() = SpannableStringBuilder().apply {
    var doneFirstTag = false
    for (any in parseTags()) {
        if (!doneFirstTag && any is Tag) {
           // doneFirstTag = true
            val start = length
            append(any.toString())
            setSpan(StyleSpan(Typeface.BOLD), start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            //append("   ")  // a little extra space ?
            // appendAsBoldAndItalic(this, message.messageContent.toTagFlattened() as Spanned, getFollow(message.tags, userNewsTag_))
        }
        else
          append(any.toString())
    }
}
