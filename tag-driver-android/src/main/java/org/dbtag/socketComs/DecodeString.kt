package org.dbtag.socketComs

object DecodeString {
// The JNI version is a lot faster
//    init {
//        System.loadLibrary("native")
//    }
//    external fun decodeString(array: ByteArray, start: Int, length: Int): String
//

    /**
     * Decodes UTF-8 bytes to a String
     */
    fun decodeString(array: ByteArray, start: Int, length: Int): String {
        // This is as fast as we have managed at decoding UTF8 bytes
        // We use JNI instead for the best speed
        val data = CharArray(length) // at most this many chars if all ascii

        var idx = start
        val last = start + length
        var charLength = 0
        while (idx < last) {
            val b0 = array[idx++].toInt()
            if (b0 and 0x80 == 0)
                data[charLength++] = (b0 and 0xff).toChar()
            else {
                var utfCount = 1
                if (b0 and 0xf0 == 0xe0)
                    utfCount = 2
                else if (b0 and 0xf8 == 0xf0)
                    utfCount = 3
                var v = b0 and (0x1f shr utfCount - 1)
                for (i in 0 until utfCount) {
                    v = v shl 6
                    v = v or (array[idx++].toInt() and 0x3f)
                }
                // We are getting U+030A (Combining Ring Above) but the Android font
                // only knows U+00B0 (Degree Sign)
                if (v == '\u030A'.toInt())
                    v = '\u00B0'.toInt()
                data[charLength++] = v.toChar()
            }
        }
        return String(data, 0, charLength)
    }
}
