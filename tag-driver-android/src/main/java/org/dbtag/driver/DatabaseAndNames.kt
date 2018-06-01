package org.dbtag.driver

import org.dbtag.data.ServerDatabases
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

suspend fun Queue.asyncDatabases() = suspendCoroutine<TAndMs<ServerDatabases>> { cont-> asyncDatabases0(cont) }

fun Queue.asyncDatabases0(cont: Continuation<TAndMs<ServerDatabases>>) {
    queue({
        with(getWriter(TagClient.Databases)) {
            toByteArray()
        }
    }, { it.databaseAndNames() }, null, cont)
}


internal fun BinaryReader.databaseAndNames(): ServerDatabases {
    var host = ""
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
                    1 -> host = readString(len) // HOST
                    2 -> databases.add(readString(len)) // DATABASE
                    else -> skip(len)
                }
            }
        }
    }
    return ServerDatabases(host, databases)
}
