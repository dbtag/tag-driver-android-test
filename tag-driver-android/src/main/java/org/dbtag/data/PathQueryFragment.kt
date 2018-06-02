package org.dbtag.data

import android.net.Uri


fun Uri.toFilter() = Filter(getQueryParameters("require").flatMap { it.split(",").map { Tag(it) } },
        getQueryParameters("exclude").flatMap { it.split(",").map { Tag(it) } })

// TODO: also show time and location, and mustHaveAttachments
fun Filter.toQueryString(title: String = "") = mutableListOf<String>().apply {
    if (title.isNotEmpty())
        add("title=$title")
    if (requiredTags.isNotEmpty())
        add("require=" + requiredTags.joinToString(",") { it.tag })
    if (excludedTags.isNotEmpty())
        add("exclude=" + excludedTags.joinToString(",") { it.tag })
//    if (startTime != 0L)
//        add("starttime=" + "") //  startTime.ToString ("yyyy-MM-ddTHH:mm:ss.fffZ", InvariantCulture))
//    if (endTime != 0L)
//        add("endTime=" + "") //  endTime.ToString ("yyyy-MM-ddTHH:mm:ss.fffZ", InvariantCulture))
    if (fromLat != toLat && fromLong != toLong) {
        add("fromlat=$fromLat")
        add("fromlong=$fromLong")
        add("tolat=$toLat")
        add("tolong=$toLong")
    }
}.joinToString("&")

/**
 * The specific way that DbTag interprets the main part of URLs.
 * The item1 required tag is the tag for the wall.
 */
class PathQueryFragment(val filter: Filter, val msgId: String?) {

    override fun toString(): String {
        var t = filter.toQueryString()
        if (msgId != null)
            t += "#$msgId"
        return t
    }

    companion object {


//        fun parse(s: String): PathQueryFragment {
//            // There might be a fragment in the tag, indicating an MessageId that should be displayed at the top of the viewport.
//            val f = s.indexOf('#')
//            return PathQueryFragment(PathQuery.parse(if (f == -1) s else s.substring(0, f)), if (f == -1) null else s.substring(f + 1))
//        }
    }
}
