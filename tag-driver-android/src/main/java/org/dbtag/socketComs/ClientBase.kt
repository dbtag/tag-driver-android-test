//package org.dbtag.socketComs
//
//import java.io.Closeable
//import java.io.EOFException
//import java.net.InetAddress
//import java.net.InetSocketAddress
//import java.net.SocketAddress
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//
//typealias ProcessReader = (Exception?, BinaryReader?) -> Unit
//
//
//abstract class ClientBase(val server: String)//    rxCount_.order(ByteOrder.LITTLE_ENDIAN);
//    : Closeable  {
//
//    protected abstract fun beginSendAndReceiveImpl()
//
//    private var callback_: ProcessReader? = null
//    //  private final ByteBuffer rxCount_ = ByteBuffer.allocate(4);  // receives the count
//    protected var sends_: Array<ByteBuffer?>? = null
//    protected var n_: Int = 0
//
//    protected val reader_ = BinaryReader()
//    override fun close() {
//    }
//
//    /**
//     * Is async.
//     */
//    fun beginSendAndReceive(buffers: Array<ByteBuffer?>, n: Int, callback: ProcessReader) {
//        callback_ = callback
//
//        n_ = n
//        var sends = sends_
//        if (sends == null || sends.size < n + 1) {
//            sends = arrayOfNulls<ByteBuffer>(n + 1)
//            sends_ = sends
//        }
//
//        var header: ByteBuffer? = sends[0]
//        val headerSize = if (n == 1) 8 else 8 + 1 + 4 + 4 * n
//        if (header == null || header.capacity() < headerSize) {
//            header = ByteBuffer.allocate(headerSize)
//            sends[0] = header
//            header!!.order(ByteOrder.LITTLE_ENDIAN)  // the other way
//            header.putInt(0x69697069)
//        }
//        else
//            header.limit(headerSize).position(4)
//        System.arraycopy(buffers, 0, sends, 1, n)
//
//        // Add up all the lengths of the writers
//        val totalSize = (0..n - 1).sumBy { buffers[it]!!.limit() }
//
//        header.putInt(if (n == 1) totalSize else totalSize + 1 + 4 + 4 * n)   // the contentLength
//        if (n != 1) {
//            header.put((-1).toByte()) // marks as a batch
//            header.putInt(n)
//            for (i in 0..n - 1)
//                header.putInt(buffers[i]!!.limit())
//        }
//        header.flip()
//        beginSendAndReceiveImpl()
//    }
//}
//
//
////fun getEndPoint(host: String, defaultPort: Int): SocketAddress {
////    val address: InetAddress
////    if (host == ".")
////    //  || database(machineName)) == 0))
////        address = InetAddress.getByName(null) // loopback
////    else
////        address = InetAddress.getByName(host)
////    return InetSocketAddress(address, defaultPort)
////}
//
//
//
