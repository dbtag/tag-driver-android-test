package org.dbtag.driver

import org.dbtag.socketComs.*
import java.util.concurrent.Executor
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.EmptyCoroutineContext

/**
 * Implements social client_ commands like signIn and createUserAsync - the ones that don't need
 * a token.
 */
//

class TagUnsignedInClient(val socket: SendReceiveQueue) : Queue {
    override fun <T> queue(getBuffer: GetBuffer, cons: (BinaryReader) -> T, executor: Executor?, cont: Continuation<TAndMs<T>>) = socket.queue(getBuffer,
            object : Continuation<ReaderAndMs> {
                override val context get() = EmptyCoroutineContext

                override fun resume(readerAndMs: ReaderAndMs) {
                    if (executor == null)
                        try { cont.resume(TAndMs(cons(readerAndMs.reader), readerAndMs.ms)) } catch (ex: Exception) { cont.resumeWithException(ex) }
                    else
                        executor.execute { try { cont.resume(TAndMs(cons(readerAndMs.reader), readerAndMs.ms)) } catch (ex: Exception) { cont.resumeWithException(ex) } }
                }

                override fun resumeWithException(exception: Throwable) = cont.resumeWithException(exception)
            })

    override fun getWriter(command: Int) = BinaryWriter().apply { writeVarint(command.toLong()) }

    constructor(server: String, port: Int = 3468) : this(SharedClients.inst.get(server, port))
}



//  public static @NonNull String hashAndSalt(@NonNull String password)
//    {
//    SecureRandom random = new SecureRandom();  // TODO: slow to construct every time ?
//    byte saltBytes[] = new byte[64]; random.nextBytes(saltBytes);
//
//    byte[] passwordBytes = password.getBytes();
//    byte[] plainTextWithSaltBytes = new byte[passwordBytes.length + saltBytes.length];
//    System.arraycopy(passwordBytes,  0, plainTextWithSaltBytes, 0, passwordBytes.length);
//    System.arraycopy(saltBytes,  0, plainTextWithSaltBytes, passwordBytes.length, saltBytes.length);
//
//    MessageDigest md = null;
//    try { md = MessageDigest.getInstance("SHA-256"); }
//    catch (NoSuchAlgorithmException e) { e.printStackTrace(); }
//    md.update(plainTextWithSaltBytes);
//    String hash = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
//    String salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP);
//    return hash + " " + salt;
//    }
