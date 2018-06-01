package org.dbtag.data

import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader
import java.text.NumberFormat

open class Tag(val tag: String, val originalName: String = "", val value: Double = Double.NaN, val unit: String = "") : Comparable<Tag> {
    val name by lazy { if (originalName.isNotEmpty()) originalName else code.toSpaced2() }
    val topic by lazy { tag.topicFromTag }
    val code by lazy { tag.codeFromTag }
    val isTopic get()  = tag.isTopic
    val hasValue get() = !value.isNaN()
    val hasName get() = originalName.isNotEmpty() && originalName != code.toSpaced2()

    val topicAsTag get() = Tag(topic)

//    // Does not create the topic as a String necessarily, which saves GC churn
//    // This is why the definition of val topic above is not just lazy
//    fun topicEquals(topic: String): Boolean {
//        if (_topic != null)
//            return _topic == topic
//        val len = topic.length
//        val tag = this.tag
//        if (tag.length < len + 1 || tag[len] != '.') return false
//        return (0..len - 1).none { tag[it] != topic[it] }
//    }
//
//    val isCodeImplicit  get() = tag.isTopic || code == name.codeFromName
//    val codeIfNotImplicit get() = if (isCodeImplicit) "" else code
//    val originalCodeIfNotImplicit get() = if (isCodeImplicit) "" else originalCode


    override fun equals(other: Any?) = other is Tag && tag.equals(other.tag, true)
    override fun hashCode()= tag.toLowerCaseInvariant().hashCode()

    override fun toString(): String {
        var ret = name
        //    if (originalName.length() == 0)
        //      return name();
        //    String c = code();
        //    return c.equals(originalName) ? originalName : c + " " + originalName;
        if (!value.isNaN())
            ret += " " + valueAsString()
    return ret
    }

    val valueAsString get() = {
        if (value.isNaN())
            ""
        // Some handy improvements to units
        else if (unit == "kg" && value < 1)
            "${formatter.format(value * 1000)}g"
        else
            "${formatter.format(value)}$unit"
    }

    fun asTag(): String {
        var ret: String
        if (topic == "user")
            ret = "@" + code
        else {
            ret = "#"
            if (topic == "sys")
                ret += code
            else {
                ret += tag
                if (isTopic)
                    ret += "."
            }
        }
        if (hasName) {
            ret += "("
            // Ensure there are the same number of open- and close-round brackets to keep the hash-tag valid
            var openOverClose = 0
            for (ch in originalName) {
                if (ch == '(')
                    ++openOverClose
                else if (ch == ')')
                    --openOverClose
            }
            while (openOverClose < 0) {
                ret += "("
                ++openOverClose
            }
            ret += originalName
            while (openOverClose > 0) {
                ret += ")"
                --openOverClose
            }
            ret += ")"
        }

        if (!value.isNaN())
            ret += "=" + java.lang.Double.toString(value) + unit

        return ret
    }

    // Compare on the topic if that's different, or else alphabetically on the name
    override fun compareTo(other: Tag): Int {
        val ret = topic.compareTo(other.topic)
        if (ret != 0)
            return ret
        // Use a comparison that considers trailing numbers as these will be common
//        return StringNumericComparator.compare(name, other.name)
        return name.compareTo(other.name)  // TODO: just for now
    }

    open fun asCodeAndName() = asCodeAndName0()

    companion object {
        val empty = Tag("")
        private val formatter = NumberFormat.getInstance()
    }
}

fun Tag.asCodeAndName0(): String {
    if (originalName.isEmpty())
        return name
//    if (code == name.codeFromName)
//        return originalName
    return "$code - $originalName"

}

fun String.firstTag(): Tag? {
    if (get(0) != '#')
        return null
    val f = 1
    var f2 = f
    val end = length
    var bracket = false
    while (f2 < end) {
        val ch = get(f2)
        bracket = (ch == '(')
        if (bracket || ch == ' ')
            break
        ++f2
    }
    if (f2 == f) return null // not a zero length tag
    val tag = substring(f, f2)
    var name = ""
    if (bracket)
        for (f3 in ++f2 until end)
            if (get(f3) == ')') {
                name = substring(f2, f3)
                break
            }
    return Tag(tag, name)
}

fun tagsEqual(tag1: String, tag2: String) = tag1.equals(tag2, true)


val String.isTopic get() = (indexOf('.') == -1)
val String.topicFromTag get() = indexOf('.').let { if (it == -1) "" else substring(0, it) }
//val String.nameFromTag get() = codeFromTag.toSpaced2()
val String.codeFromTag get() = indexOf('.').let { if (it == -1) this else substring(it + 1) }
//// Sadly we don't have the nice version of the topic name handy, so we just capitalise the item1 letter.
//val String.improvedTopic get() = if (isEmpty()) this else get(0).toUpperCase() + substring(1)
//
//val String.hasUpperCase get() : Boolean {
//    return (0 until length).any { get(it) in 'A'..'Z' }
//}

//val String.codeFromName get() : String {
//    val sb = StringBuilder()
//    for (ch in this) {
//        if (ch != ' ')
//            sb.append(ch.toLowerCase())
//    }
//    return sb.toString()
//}



/**
 * Converts an Ascii String to lower-case - so nice and fast with no locale ping
 */
fun String.toLowerCaseInvariant() : String {
    val len = length
    var newValue: CharArray? = null
    for (i in 0 until len) {
        val ch = get(i)
        if (ch in 'A'..'Z') {
            if (newValue == null) {
                newValue = CharArray(len)
                for (j in 0 until i)
                    newValue[j] = get(j)
            }
            newValue[i] = (ch.toInt() + ('a' - 'A')).toChar()
        } else if (newValue != null)
            newValue[i] = ch
    }
    return if (newValue != null) String(newValue) else this
}

fun String.toSpaced2(): String {
    if (isEmpty())
        return ""
    val l = length
    val sb = StringBuilder(l * 4 / 3)  // guess at final length
    var upperNext = false
    for (i in 0 until l) {
        var ch = get(i)
        if (upperNext) {
            upperNext = false
            sb.append(ch.toUpperCase())
            continue
        }
        if (i == 0)
            ch = ch.toUpperCase()
        else if (i + 1 < length) {
            if (ch == '.') {
                sb.append(' ')
                upperNext = true
                continue
            }
            if (get(i + 1) in 'a'..'z' && ch in 'A'..'Z')
              sb.append(' ')
        }
        sb.append(ch)
    }
    return sb.toString()
}


fun BinaryReader.tag(len: Int): Tag {
    var tag = ""
    var name = ""
    var value = Double.NaN
    var unit = ""
    val eor = position + len
    while (position != eor) {
        val key = readVarint().toInt()
        val field = key shr 3
        when (key and 7) {
            WireType.VARINT -> readVarint()
            WireType.FIXED64 -> {
                when (field) {
                    3 -> value = readDouble() // VALUE
                    else -> skip(8)
                }
            }
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> {
                val len2 = readVarint().toInt()
                when (field) {
                    1 -> tag = readString(len2)  // TAG
                    2 -> name = readString(len2) // NAME
                    4 -> unit = readString(len2) // UNIT
                    else -> skip(len2)
                }
            }
        }
    }
    return Tag(tag, name, value, unit)
}