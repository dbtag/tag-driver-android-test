package org.dbtag.data

/**
 * A shorter version of Message - should be an option in many places....
 */
class MessageShortenedForGeo(args: MessageConstructArgs) : M0Impl(args) {
    val latitude = args.latitude
    val longitude = args.longitude
}
