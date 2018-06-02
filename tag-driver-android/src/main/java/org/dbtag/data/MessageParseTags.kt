package org.dbtag.data


// Simpler version written out explicitly for performance
fun String.parseTagsOnly(topicBecomesSys: Boolean = true): List<Tag> {
    val ret = mutableListOf<Tag>()
    var startIndex = 0
    val end = length
    while (true) {
        var isAt = false
        var f = startIndex
        while (true) {
            if (f == end) {
                f = -1
                break
            }
            val ch = get(f)
            if (ch == '#')
                break
            if ((ch == '@') && (f == 0 || get(f - 1) == ' ')) {
                isAt = true
                break
            }
            ++f
        }
        if (f == -1)
            break
        with (parse(++f, end, isAt, topicBecomesSys)) {
            startIndex = f
            if (tag != null)
                ret.add(tag)
        }
    }
    return ret
}


private fun Char.isAlpha() = (this in 'A'..'Z') || (this in 'a'..'z')

fun String.parseTags(topicBecomesSys: Boolean = true): List<Any> {
    val ret = mutableListOf<Any>()
    var startIndex = 0
    val end = length  // for perf
    while (true) {
        var isAt = false
        var f = startIndex
        while (true) {
            if (f == end) {
                f = -1
                break
            }
            val ch = get(f)
            if (ch == '#')
                break
            if ((ch == '@') && (f == 0 || get(f - 1) == ' ') && f + 1 < end && get(f + 1).isAlpha()) {
                isAt = true
                break
            }
            ++f
        }
        if (f == -1) {
            if (startIndex != end)
                ret.add(substring(startIndex))
            break
        }

        // We have text from start to f, then a # or @
        if (startIndex != f)
            ret.add(substring(startIndex, f))
        with (parse(++f, end, isAt, topicBecomesSys)) {
            startIndex = index
            if (tag != null)
                ret.add(tag)
        }
    }
    return ret
}


// fun String.parse() = parse(0, length).tag

private data class ParseResult(val index: Int, val tag: Tag?)

private fun String.parse(startIndex: Int, endIndex: Int, isAt: Boolean, topicBecomesSys: Boolean): ParseResult {
    var index = startIndex
    val start0 = index
    var bracket = false
    while (index < endIndex) {
        val ch = get(index)
        bracket = (ch == '(')
        if (bracket || ch == ' ' || ch == '=')
            break
        ++index
    }
    val lastTagChar = get(index - 1)
    if (lastTagChar == '.' || lastTagChar == ',') {
        --index
        bracket = false
    } // dot and comma not allowed at the endIndex of a code

    var tag: Tag? = null
    if (index != start0) {
    // not a zero length tag
        // Look for optional brackets containing a name
        var originalTag = substring(start0, index)
        var name = ""
        if (isAt)
          originalTag = "user.$originalTag"
        else if (topicBecomesSys && originalTag.indexOf('.') == -1)
            originalTag = "sys.$originalTag" //  if no topic, then assume sys. - TODO: or data.
        if (bracket) {
            var bracketCount = 1
            var f3 = ++index
            while (f3 < endIndex) {
                val ch = get(f3)
                if (ch == '(')
                    bracketCount += 1
                else if (ch == ')' && --bracketCount == 0) {
                    name = substring(index, f3)
                    index = ++f3
                    break
                }
                ++f3
            }
        }

        // Look for an optional value   =([+\-]*[0-9,\.]+[a-zA-Z]*
        // TODO: this would be best   =[-+]?[0-9]*\.?[0-9]+([eE][-+]?[0-9]+)?
        var value = Double.NaN
        var unit = ""
        if (index < endIndex && get(index) == '=') {
            var f3 = ++index
            while (f3 < endIndex) {
                val ch = get(f3)
                if (!(ch in '0'..'9' || ch == ',' || ch == '.'))
                    break
                ++f3
            }
            if (f3 > index)
            // the + requirement
            {
                value = java.lang.Double.parseDouble(substring(index, f3))
                index = f3
                while (f3 < endIndex) {
                    val ch = get(f3)
                    if (!(ch in 'a'..'z' || ch in 'A'..'Z'))
                        break
                    ++f3
                }
                unit = substring(index, f3)
                index = f3
            }
        }
        tag = Tag(originalTag, name, value, unit)
    }
    return ParseResult(index, tag)
}

