package org.dbtag.driver

import org.dbtag.data.SignInResult
import org.dbtag.data.SignInResults
import org.dbtag.protobuf.WireType
import org.dbtag.socketComs.BinaryReader

suspend fun Queue.signIn(user: String, database: String, password: String) = queue({
    with(getWriter0(TagClient.SignIn)) {
        writeField(1, user)  // USER
        writeField(2, database) // DATABASE
        writeField(3, password)  // PASSWORD
        toByteArray()
    }}, { it.signInResults() })


private fun BinaryReader.signInResults() : SignInResults {
    var result = SignInResult.DatabaseNotFound
    var token = ""
    val eor = bufferSize
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        // TODO: , TRACK_LOCATION = 3;
        when (key and 7) {
            WireType.VARINT -> {
                val value = readVarint()
                if (field == 1)  // RESULT
                    result = SignInResult.from(value.toByte())
            }
            WireType.FIXED64 -> skip(8)
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> {
                val len2 = readVarint().toInt()
                if (field == 2)  // TOKEN
                    token = readString(len2)
                else
                    skip(len2)
            }
        }
    }
    return SignInResults(result, token)
}
