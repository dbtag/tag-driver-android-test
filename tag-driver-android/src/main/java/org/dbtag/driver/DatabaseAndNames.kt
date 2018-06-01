package org.dbtag.driver

import org.dbtag.data.ServerDatabases
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

suspend inline fun Queue.databases() = suspendCoroutine<TAndMs<ServerDatabases>> { cont-> databases(cont) }

fun Queue.databases(cont: Continuation<TAndMs<ServerDatabases>>) {
    queue({
        with(getWriter0(TagClient.Databases)) {
            toByteArray()
        }
    }, { it.databaseAndNames() }, null, cont)
}


internal fun BinaryReader.databaseAndNames(): ServerDatabases {
    // var host = ""
    val databases = mutableListOf<String>()
    val eor = bufferSize
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            WireType.VARINT -> readVarint()
            WireType.FIXED64 -> skip(8)
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> {
                val len = readVarint().toInt()
                when (field) {
                    // 1 -> host = readString(len) // HOST   TODO: this is never returned by servers so remove it
                    2 -> databases.add(readString(len)) // DATABASE
                    else -> skip(len)
                }
            }
        }
    }
    return ServerDatabases(databases)
}
