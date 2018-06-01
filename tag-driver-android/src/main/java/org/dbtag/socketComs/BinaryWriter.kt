package org.dbtag.socketComs

import org.dbtag.protobuf.WireType

import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * Somewhat like DataOutputStream, but writes to our own internal byte array
 */
class BinaryWriter(capacity: Int = 100, private val origin: Int = 0) {
    var buffer: ByteArray
        private set
    var exception: Exception? = null
    private var written: Int = 0

    init {
        buffer = ByteArray(capacity)
        written = origin
    }

    fun clear() {
        written = origin
        exception = null
    }

    private fun ensureRoom(extra: Int) {
        val bufferLength = buffer.size
        if (written + extra > bufferLength) {
            val newBuffer = ByteArray(Math.max(bufferLength * 2, written + extra))
            System.arraycopy(buffer, 0, newBuffer, 0, written)
            buffer = newBuffer
        }
    }

    fun writeByte(value: Byte) {
        ensureRoom(1)
        buffer[written++] = value
    }

    private fun writeLong(value: Long) {
        var v = value
        ensureRoom(8)
        buffer[written++] = v.toByte()
        v = v shr 8
        buffer[written++] = v.toByte()
        v = v shr 8
        buffer[written++] = v.toByte()
        v = v shr 8
        buffer[written++] = v.toByte()
        v = v shr 8
        buffer[written++] = v.toByte()
        v = v shr 8
        buffer[written++] = v.toByte()
        v = v shr 8
        buffer[written++] = v.toByte()
        v = v shr 8
        buffer[written++] = v.toByte()
    }

    private fun writeDouble(value: Double) {
        writeLong(java.lang.Double.doubleToRawLongBits(value))
    }

    fun writeVarint(value: Long) {
        var v = value
        while (v >= 0x80) {
            writeByte((v or 0x80).toByte())
            v = v shr 7
        }
        writeByte(v.toByte())
    }

    fun writeFieldVarint(field: Int, value: Long) {
        writeVarint((field shl 3 or WIRE_TYPE_VARINT).toLong())
        writeVarint(value)
    }

    fun writeField(field: Int, s: String) {
        writeVarint((field shl 3 or WIRE_TYPE_LENGTH_DELIMITED).toLong())
        writeString(s)
    }

    fun writeField(field: Int, value: ByteArray?) {
        writeVarint((field shl 3 or WIRE_TYPE_LENGTH_DELIMITED).toLong())
        if (value == null)
            writeVarint(0)
        else {
            val byteCount = value.size
            writeVarint(byteCount.toLong())
            ensureRoom(byteCount)
            System.arraycopy(value, 0, buffer, written, byteCount)
            written += byteCount
        }
    }

    fun writeFieldFixed64(field: Int, value: Long) {
        writeVarint((field shl 3 or WIRE_TYPE_FIXED64).toLong())
        writeLong(value)
    }

    fun writeField(field: Int, value: Double) {
        writeVarint((field shl 3 or WIRE_TYPE_FIXED64).toLong())
        writeDouble(value)
    }

    fun embeddedField(field: Int): StoreEmbedded {
        return StoreEmbedded(field)
    }

    inner class StoreEmbedded(private val field_: Int) {
        private val ofs0_: Int = written

        init {
            ensureRoom(6)
            written += 6  // ensure the key And len will fit in front
        }

        fun close() {
            val r = ofs0_ + 6
            val len = written - r
            written = ofs0_
            writeVarint((field_ shl 3 or WireType.LENGTH_DELIMITED).toLong())
            writeVarint(len.toLong())
            System.arraycopy(buffer, r, buffer, written, len)
            written += len
        }
    }

    fun writeString(s: String) {
        // This is pretty fast because Android has done it in JNI, but
        // it does create a byte[] every time we use it...
        val bb = s.toByteArray(Charset.defaultCharset())  // luckily the default is what we want - UTF8
        val count = bb.size
        var num = (if (count < 0) 65536 + count else count).toLong()
        while (num >= 0x80) {
            writeByte((num or 0x80).toByte())
            num = num shr 7
        }
        ensureRoom(1 + count)
        buffer[written++] = num.toByte()
        System.arraycopy(bb, 0, buffer, written, count)
        written += count
    }

    fun size(): Int {
        return written - origin
    }

    fun setSize(size: Int) {
        written = origin + size
    }

    fun toByteArray() = ByteArray(size()).apply {
        System.arraycopy(buffer, origin, this, 0, size)
    }

    companion object {
        private const val WIRE_TYPE_VARINT = 0
        private const val WIRE_TYPE_FIXED64 = 1
        private const val WIRE_TYPE_LENGTH_DELIMITED = 2
    }
}// a useful default
