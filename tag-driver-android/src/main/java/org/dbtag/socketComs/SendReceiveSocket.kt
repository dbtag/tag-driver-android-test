package org.dbtag.socketComs

import android.content.Context
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.Executor
import kotlin.coroutines.experimental.Continuation


fun Context.wifiOnAndConnected() =
      true // just for now
//    with (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager) {
//        isWifiEnabled && connectionInfo?.let { it.networkId != -1 } ?: false
//    }

/**
 * One of these for each socket connection. Uses a state machine on a single
 * static thread to keep checking non-blocking reads.
 */
class SendReceiveSocket(private val server: String, private val port: Int, private val executor: Executor) {
    private class SocketStreams(socket: Socket) {
        // Only get these streams once, as the getting-code is quite slow
        val inputStream: InputStream = socket.getInputStream()
        val outputStream: OutputStream = socket.getOutputStream()
    }

    // We will keep re-opening a new socket if the old one stops working
    private var socket: SocketStreams? = null

    private var job: Job? = null   // A single item queuing up to run, or nothing right now

//    init {
//        Log.i("SendReceiveSocket", "constructor")
//    }

    fun post(buffers: Array<ByteArray>, cont: Continuation<BinaryReader>) {
        postInternal(writeBuffer(buffers), buffers.size, cont, cont)
    }

    private fun postInternal(writeBuffer: ByteArray, n: Int, cont: Continuation<BinaryReader>, contExt: Continuation<BinaryReader>?) {
        if (job != null)
            throw Exception("Can only queue one item")

        // val n = buffers.size
        val socket = this.socket
        if (socket == null) {
            executor.execute({
//                Log.i("Socket", "ctor")
//                if (!Application.inst.wifiOnAndConnected()) {
//                    Handler(Looper.getMainLooper()).post( {
//                        Toast.makeText(Application.inst, "Flow Book needs Wifi connected", Toast.LENGTH_LONG).show()
//                    })
//                    complete(Exception("Only on Wifi"), null, n, cont)
//                }

                val socket2 = Socket().apply {
                    tcpNoDelay = true
                    soTimeout = 5000  // wait up to 5 seconds for each stream read before getting a SocketTimeoutException
                }
                try {
                    val connectTimeout = 3000
                    socket2.connect(InetSocketAddress(server, port), connectTimeout)  // will block
                } catch (ex0: Exception) {
                    complete(ex0, null, n, cont)
                    return@execute
                }
                val socketWrapper = SocketStreams(socket2).apply { this@SendReceiveSocket.socket = this }
                job = Job(socketWrapper, writeBuffer, { ex, reader -> complete(ex, reader, n, cont) }, contExt)

                // Keep running the state machine on any job, until this new socket is no longer good
                // We'll be running on an executor thread here.
                runSocketStateMachine()
            })

        } else
            job = Job(socket, writeBuffer, { ex, reader -> complete(ex, reader, n, cont) }, contExt)
    }

    private fun runSocketStateMachine() {
        while (job.let {
                    if (it == null) {
                        Thread.sleep(10, 0)  // don't run ragged
                        true
                    }
                    else
                        it.runStateMachine()
                }) { }
    }

    private fun complete(ex: Exception?, reader: BinaryReader?, n: Int, cont: Continuation<BinaryReader>) {
        if (ex is SocketException && ex.message?.contains("ECONNRESET") == true) { //  && ex.message == "104") {  // ESHUTDOWN
            // An existing socket might have been disconnected at the other end for timeout reasons
            // and we will only find this out when we try the next communications.
            // So here we re-try exactly once to hopefully maintain the illusion that nothing has
            // actually gone offline.
            with (job!!) {
                contExt?.let {
                    socket = null
                    job = null
                    postInternal(writeBuffer, n, it, null)
                    return
                }
            }
        }

        job = null
        if (reader == null) {
            socket = null
            for (i in 0 until n)
                cont.resumeWithException(ex!!)
        } else {
            if (n == 1)
                reader.with1(cont)
            else
                reader.withN(n, cont)
        }
    }

    // The socket is expected to be already open (maybe for a long time) on entry
    // The parameter contExt is used to do a single re-try in the case of a remote socket disconnect,
    // probably due to the server at the other end not seeing any activity
    // for a while and so deciding to close the socket.
    private class Job(private val socket: SocketStreams, val writeBuffer: ByteArray, private val cont: (Exception?, BinaryReader?) -> Unit,
                      val contExt: Continuation<BinaryReader>?) {
        private var state = State.Begin
        private var readSoFar = 0
        private val reader = BinaryReader()  // collects the read-data into here

        private enum class State { Begin, ReadingCount, ReadingMain }

        // Should keep returning true to keep the executor thread running this state machine
        fun runStateMachine() = when (state) {
            State.Begin -> {
                try {
                    socket.outputStream.write(writeBuffer)
                    state = State.ReadingCount
                    true
                } catch (ex: Exception) {
                    cont(ex, null)
                    false
                }
            }

            State.ReadingCount -> {
                try {
                    val buf = ByteArray(4)
                    val red = socket.inputStream.read(buf, 0, 4)  // should get all 4 bytes at once
                    if (red != 4) {
                        cont(Exception("not 4"), null)
                        false
                    }
                    else {
                        val rxCount = (((buf[3].toInt() and 0xff shl 8) + (buf[2].toInt() and 0xff) shl 8) + (buf[1].toInt() and 0xff) shl 8) + (buf[0].toInt() and 0xff)

                        if (rxCount >= 10_000_000) {
                            cont(Exception("huge rx count"), null)
                            false
                        } else {
                            reader.bufferSize = rxCount
                            state = State.ReadingMain
                            true
                        }
                    }
                } catch (ex: Exception) {  // for example SocketTimeoutException
                    cont(ex, null)
                    false
                }
            }

            State.ReadingMain -> {
                try {
                    // Might take several calls to read to get a big buffer
                    val ret = socket.inputStream.read(reader.buffer, readSoFar, reader.bufferSize - readSoFar)
                    readSoFar += ret
                    if (readSoFar == reader.bufferSize)
                        cont(null, reader)
                    true
                } catch (ex: Exception) {  // for example SocketTimeoutException
                    cont(ex, null)
                    false
                }
            }
        }
    }
}

fun ByteArray.dump(count: Int = size) : String {
    var ret = ""
    for (j in 0 until minOf(100, count)) {
        val b = this[j]
        val i = b.toInt()
        if (i in 0x20..0x7e)
            ret += b.toChar()
    }
    return ret
}


//// Helpers for on-wire format

private fun ByteArray.setInt(ofs: Int, value: Int): Int {
    var ofs1 = ofs
    var value2 = value
    this[ofs1++] = value2.toByte()
    value2 = value2 shr 8
    this[ofs1++] = value2.toByte()
    value2 = value2 shr 8
    this[ofs1++] = value2.toByte()
    value2 = value2 shr 8
    this[ofs1++] = value2.toByte()
    return ofs1
}

private fun Array<ByteArray>.header() = ByteArray(if (size == 1) 8 else 8 + 1 + 4 + 4 * size).apply {
    val n = this@header.size
    var ofs = setInt(0, 0x69697069)
    val totalSize = (0 until n).sumBy { this@header[it].size }
    ofs = setInt(ofs, if (n == 1) totalSize else totalSize + 1 + 4 + 4 * n)   // the contentLength
    if (n != 1) {
        this[ofs++] = -1 // marks as a batch

        ofs = setInt(ofs, n)
        for (i in 0 until n)
            ofs = setInt(ofs, this@header[i].size)
    }
}

private fun writeBuffer(buffers: Array<ByteArray>) : ByteArray {
    val sends = Array(1 + buffers.size, { if (it == 0) buffers.header() else buffers[it - 1] })

    // Make a single byte buffer with the stuff remaining to write
    var totalWriteLen = (0 until sends.size).sumBy { sends[it].size }

    val writeBuffer = ByteArray(totalWriteLen)
    // Now copy the bytes
    totalWriteLen = 0
    for (i in 0 until sends.size) {
        val bb = sends[i]
        val len0 = bb.size
        System.arraycopy(bb, 0, writeBuffer, totalWriteLen, len0)
        totalWriteLen += len0
    }
    return writeBuffer
}

private fun BinaryReader.readException(): Exception? {
    try {
        if (!readBoolean())
            return null
        // An exception over on the server.
        val typeFullName = readString()
        val message = readString()
        return if (typeFullName == "DbTag.BadTokenException") BadTokenException() else return RemoteException("$typeFullName$message")
    } catch (ex: Exception) { return ex }
}

class BadTokenException : Exception()
class RemoteException(message: String?) : Exception(message)

internal fun BinaryReader.with1(cont: Continuation<BinaryReader>) {
    val ex = readException()
    try {
        if (ex != null)
            cont.resumeWithException(ex)
        else
            cont.resume(this)
    }
    catch (_: Exception) {
    }
}

internal fun BinaryReader.withN(n: Int, cont: Continuation<BinaryReader>) {
    // Get the sizes
    val sizes = IntArray(n)
    for (i in 0 until n) {
        try {
            sizes[i] = readInt()
        }
        catch (ex: Exception) {
            for (i0 in 0 until n) {
                try { cont.resumeWithException(ex) } catch (ignored: Exception) { }
            }
            return
        }
    }

    // Call back with these part readers
    var pos = position
    for (i in 0 until n) {
        val bufferSize = pos + sizes[i]
        setBufferSizeAndPosition(bufferSize, pos)

        // Look for individual exceptions inside the part readers
        val ex = readException()
        try {
            if (ex != null)
                cont.resumeWithException(ex)
            else
                cont.resume(this)
        } catch (_: Exception) { }

        pos = bufferSize
    }
}
