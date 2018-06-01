package org.dbtag.socketComs

object DecodeString {
    init {
        System.loadLibrary("native")
    }

    external fun decodeString(array: ByteArray, start: Int, length: Int): String
}
