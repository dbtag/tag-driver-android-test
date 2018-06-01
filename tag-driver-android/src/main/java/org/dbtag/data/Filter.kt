package org.dbtag.data

import org.dbtag.socketComs.BinaryWriter

data class Filter(var requiredTags: List<Tag> = emptyList(), var excludedTags: List<Tag> = emptyList(),
                  var startTime: Long = 0L, var endTime: Long = Long.MAX_VALUE,
                  var fromLat: Double = 0.0, var fromLong: Double = 0.0, var toLat: Double = 0.0, var toLong: Double = 0.0,
                  var mustHave: Int = 0) {

    companion object {
        val empty = Filter()
        const val MUST_HAVE_UNTIL = 1
        const val MUST_HAVE_LOCATION = 2
        const val MUST_HAVE_ATTACHMENT = 4  // TODO: notes, comments ?
        const val MUST_HAVE_VALUE = 8
        const val MUST_HAVE_IMAGE_ATTACHMENT = 16

    }
}

fun Filter.require(vararg tags: Tag): Filter {
    requiredTags = requiredTags.ensurePresent(tags)
    return this
}

fun Filter.exclude(vararg tags: Tag): Filter {
    excludedTags = excludedTags.ensurePresent(tags)
    return this
}


fun Filter.timeRange(startTime: Long, endTime: Long): Filter {
    this.startTime = startTime
    this.endTime = endTime
    return this
}

fun Filter.inside(fromLat: Double, fromLong: Double, toLat: Double, toLong: Double): Filter {
    this.fromLat = fromLat
    this.fromLong = fromLong
    this.toLat = toLat
    this.toLong = toLong
    return this
}

// Handy methods
fun Filter.isRequired(tag: String) = requiredTags.firstOrNull { tagsEqual(it.tag, tag) } != null


fun Filter.write(writer: BinaryWriter) {
    requiredTags.forEach { writer.writeField(1, it.tag) }  // REQUIRED_TAG
    excludedTags.forEach { writer.writeField(2, it.tag) }  // EXCLUDED_TAG
    if (startTime != 0L)
        writer.writeFieldFixed64(3, startTime)  // START_TIME
    if (endTime != java.lang.Long.MAX_VALUE)
        writer.writeFieldFixed64(4, endTime)    // END_TIME
    if (mustHave != 0)
        writer.writeFieldVarint(5, mustHave.toLong())  // MUST_HAVE
    if (fromLat != 0.0)
        writer.writeField(6, fromLat)   // FROM_LAT
    if (fromLong != 0.0)
        writer.writeField(7, fromLong)  // FROM_LONG
    if (toLat != 0.0)
        writer.writeField(8, toLat)     // TO_LAT
    if (toLong != 0.0)
        writer.writeField(9, toLong)    // TO_LONG
}


// Add each new value unless it is already present
private fun List<Tag>.ensurePresent(tags: Array<out Tag>) =
    toMutableList().apply {
        addAll(tags.mapNotNull { tag ->
            if (any { it == tag })
             null
            else
             tag })
    }
