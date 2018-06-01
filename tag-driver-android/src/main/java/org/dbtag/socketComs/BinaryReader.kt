package org.dbtag.socketComs

import java.io.EOFException

/**
 * Somewhat like DataInputStream, but reads from our own internal byte array
 */
class BinaryReader(buffer: ByteArray? = null, size: Int = 0) {
    var buffer = buffer?: emptyBuffer_
        private set

    private var _bufferSize: Int = size
    var position: Int = 0

    // Need a new buffer if the current one is either too small or more than
    // 10 times bigger than needed (so we don't keep very large buffers
    // beyond the point that they are needed).
    //  || buffer_.length >= 10 * value)
    var bufferSize: Int
        get() = _bufferSize
        set(value) {
            if (buffer.size < value)
                buffer = ByteArray(value)
            _bufferSize = value
            position = 0
        }

    fun setBufferSizeAndPosition(bufferSize: Int, position: Int) {
        _bufferSize = bufferSize
        this.position = position
    }

    //  /**
    //   * Decodes UTF-8 bytes to a String
    //   */
    //  private static String decode(final byte[] array, final int start, final int length)
    //    {
    //// This is as fast as we have managed at decoding UTF8 bytes
    //// We use JNI instead for the best speed
    //    char[] data = new char[length]; // at most this many chars if all ascii
    //
    //    int idx = start, last = start + length, charLength = 0;
    //    while (idx < last)
    //      {
    //      byte b0 = array[idx++];
    //      if ((b0 & 0x80) == 0)
    //        data[charLength++] = (char) (b0 & 0xff);
    //      else
    //        {
    //        int utfCount = 1;
    //        if ((b0 & 0xf0) == 0xe0)
    //          utfCount = 2;
    //        else if ((b0 & 0xf8) == 0xf0)
    //          utfCount = 3;
    //        int val = b0 & (0x1f >> (utfCount - 1));
    //        for (int i = 0; i < utfCount; ++i)
    //          {
    //          val <<= 6;
    //          val |= array[idx++] & 0x3f;
    //          }
    //        // We are getting U+030A (Combining Ring Above) but the Android font
    //        // only knows U+00B0 (Degree Sign)
    //        if (val == '\u030A')
    //          val = '\u00B0';
    //        data[charLength++] = (char) val;
    //        }
    //      }
    //    return new String(data, 0, charLength);
    //    }

    fun unreadBytesCount() = _bufferSize - position

    fun readBytes(count: Int): ByteArray {
        if (position + count > _bufferSize)
            throw EOFException()
        val ret = ByteArray(count)
        System.arraycopy(buffer, position, ret, 0, count)
        position += count
        return ret
    }

    fun readBoolean(): Boolean {
        if (position + 1 > _bufferSize)
            throw EOFException()
        return buffer[position++].toInt() != 0
    }

    fun readByte(): Byte {
        if (position + 1 > _bufferSize)
            throw EOFException()
        return buffer[position++]
    }

    fun readByteNoEOFException(): Byte {
        return buffer[position++]
    }

    fun readInt(): Int {
        if (position + 4 > _bufferSize)
            throw EOFException()
        val b0 = buffer[position++].toInt()
        val b1 = buffer[position++].toInt()
        val b2 = buffer[position++].toInt()
        val b3 = buffer[position++].toInt()
        return (((b3 and 0xff shl 8) + (b2 and 0xff) shl 8) + (b1 and 0xff) shl 8) + (b0 and 0xff)
    }

    fun skip(count: Int) {
        if (position + count > _bufferSize)
            throw EOFException()
        position += count
    }

    fun readLong(): Long {
        val l = readInt()
        val h = readInt()
        return h.toLong() shl 32 or (l.toLong() and 0xffffffffL)
        //    if (ofs_ + 8 > bufferCount_)
        //      throw new EOFException();
        //    byte b0 = buffer_[ofs_++], b1 = buffer_[ofs_++], b2 = buffer_[ofs_++], b3 = buffer_[ofs_++], b4 = buffer_[ofs_++], b5 = buffer_[ofs_++], b6 = buffer_[ofs_++], b7 = buffer_[ofs_++];
        //    return ((((((((((((((b7 & 0xff) << 8) + (b6 & 0xff)) << 8) + (b5 & 0xff)) << 8) + (b4 & 0xff)) << 8) + (b3 & 0xff)) << 8) + (b2 & 0xff)) << 8) + (b1 & 0xff)) << 8)
        //        + (b0 & 0xff);
    }

    fun readDouble() = java.lang.Double.longBitsToDouble(readLong())

    fun readString(count: Int = read7BitEncodedInt()): String {
        if (count == 0)
            return ""
        if (position + count > _bufferSize)
            throw EOFException()
        // val ret2 = decode(buffer_, ofs_, count);
        // Much faster in JNI
        val ret = DecodeString.decodeString(buffer, position, count)
        position += count
        return ret
    }

    private fun read7BitEncodedInt(): Int {
        var num3: Int
        var ret = 0
        var num2 = 0
        do {
            num3 = readByte().toInt()
            ret = ret or (num3 and 0x7f shl num2)
            num2 += 7
        } while (num3 and 0x80 != 0)
        return ret
    }

    // The same as Read7BitEncodedInt but returns a long
    fun readVarint(): Long {
        var num3: Int
        var ret: Long = 0
        var num2 = 0
        do {
            num3 = readByte().toInt()
            ret = ret or ((num3 and 0x7f).toLong() shl num2)
            num2 += 7
        } while (num3 and 0x80 != 0)
        return ret
    }


    companion object {
        private val emptyBuffer_ = byteArrayOf()
    }

}

internal fun BinaryReader.unCompressReader(): BinaryReader {
 // if (true)
      return this // TODO: no compression support in the server right now
//  else {
////    if (this == null)
////        return null
//
//      if (unreadBytesCount() == 0 || readByteNoEOFException().toInt() != 66)
//          return this  // not compressed
//      val originalSize = readInt()
//      val restored = ByteArray(originalSize)
//
//      LZ4.decompressFast(buffer, position, restored, 0, originalSize)
//      return BinaryReader(restored, originalSize)
//  }
}


