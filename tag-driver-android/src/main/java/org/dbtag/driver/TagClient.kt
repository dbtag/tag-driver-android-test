package org.dbtag.driver

import org.dbtag.data.Tag
import org.dbtag.socketComs.*
import java.util.concurrent.Executor
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine


typealias ResultCallback<T> = (Exception?, T?) -> Unit


data class TAndMs<T>(val t: T, val ms: Long)

interface Queue {
    fun getWriter0(command: Int): BinaryWriter
    fun <T> queue(getBuffer: GetBuffer, cons: (BinaryReader) -> T, executor: Executor?,  cont: Continuation<TAndMs<T>>)
    }

// TODO: just for now, strips out the Ms part which is going to go completely soon
suspend fun <T> Queue.queue(getBuffer: GetBuffer, cons: (BinaryReader) -> T, executor: Executor? = null) : T =
        suspendCoroutine { cont -> queue(getBuffer, cons, executor,
                object: Continuation<TAndMs<T>> {
                    override val context = cont.context
                    override fun resume(value: TAndMs<T>) = cont.resume(value.t)
                    override fun resumeWithException(exception: Throwable) = cont.resumeWithException(exception)
                }) }


// Knows its user and has an access token
interface UserQueue : Queue {
    val user: Tag
    fun getWriter(command: Int): BinaryWriter
}

class ServerDatabaseUser(val server: String, val database: String, val user: String)

fun String.serverDatabaseUser() : ServerDatabaseUser? {
    val f = indexOf('@')
    if (f == -1 || f == 0)
        return null
    val user = substring(0, f)
    val f2 = indexOf('.', f + 1)
    if (f2 == -1 || f2 == (f + 1) || f2 == length - 1)
        return null
    val database = substring(f + 1, f2)
    val server = substring(f2 + 1)
    return ServerDatabaseUser(server, database, user)
}

fun asAccountName(server: String, database: String, user: String) = "$user@$database.$server"


class ServerDatabaseUserToken(val serverDatabaseUser: ServerDatabaseUser, val token: String)


open class TagClientRefreshToken(server: String, port: Int, user: Tag, initialToken: String, useSocket: SendReceiveQueue?,
                                 private val refreshToken: (SendReceiveQueue, invalidToken: String, Continuation<String>)-> Unit)
    : TagClient(server, port, user, initialToken, useSocket) {

    private val callbacks = mutableListOf<() -> Unit>()
    private var invalidToken = ""

    // If the token expires, then all threads queue here while the first one tries to sort it out
    override fun <T> queue(getBuffer: GetBuffer, cons: (BinaryReader) -> T, executor: Executor?, cont: Continuation<TAndMs<T>>) {
        var weDoIt = false
        var deferred = false
        synchronized(callbacks, {
            if (token.isEmpty()) {
                deferred = true
                weDoIt = callbacks.isEmpty()
                callbacks.add({superQueue(getBuffer, cons, executor, cont)})
            }
        })
        if (weDoIt) {
            refreshToken(socket, invalidToken, object: Continuation<String> {
                override val context = cont.context
                override fun resumeWithException(exception: Throwable) {

//                    // If the refreshToken above has failed again so we have no token,
//                    // then we must need a new password, and we should request sign-in here
//
//                    // TODO: they could sign into a different server, or into a different database,
//                    // or as a different user.
//                    // In that case we will need to check that's what they want, then completely close all activities
//                    // and re-start the home activity again
//                    // https://stackoverflow.com/a/13468685
//                    val z = value
//
//                    val context = Application.Companion.inst.applicationContext
//                    val intent = Intent(context, ActivityHome::class.java)
//                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
//                    context.startActivity(intent)
//
                    cont.resumeWithException(exception)
                }

                override fun resume(value: String) {
                    // We've got a new token so let's re-try
                    synchronized(callbacks, {
                        token = value
                        val pendingCallbacks = callbacks.toList()
                        callbacks.clear()  // and remove them from the waiting list
                        pendingCallbacks
                    }).forEach { it() }  // make the call-backs
                }
            } )
        }
        if (deferred)
            return

        val tokenUsed = token
        super.queue(getBuffer, cons, executor, object: Continuation<TAndMs<T>> {
            override val context = cont.context
            override fun resume(value: TAndMs<T>) = cont.resume(value)

            // The interesting case
            override fun resumeWithException(exception: Throwable) {
                if (exception is BadTokenException) {
                    synchronized(callbacks, {
                        if (token === tokenUsed) {
                            invalidToken = token
                            token = ""
                        }
                    })
                    queue(getBuffer, cons, executor, cont)  // go and try again
                }
                else
                    cont.resumeWithException(exception)
            }
        })
    }
    private fun <T>superQueue(getBuffer: GetBuffer, cons: (BinaryReader) -> T, executor: Executor?, cont: Continuation<TAndMs<T>>) {
       super.queue(getBuffer, cons, executor, cont)
    }
}


/**
 * A db connection to a DbTag server, so holds an access token (which corresponds to this user and also identifies a database at the server)
 * Defines the async calls we use to add messages, and get all kinds of interesting code1Summaries
 * from the DBTag server.
 */
open class TagClient(server: String, port: Int = 3468, override val user: Tag, var token: String, useSocket: SendReceiveQueue?)
    : UserQueue {
    override fun <T> queue(getBuffer: GetBuffer, cons: (BinaryReader) -> T, executor: Executor?,  cont: Continuation<TAndMs<T>>) = socket.queue(getBuffer,
            object : Continuation<ReaderAndMs> {
                override val context get() = EmptyCoroutineContext

                override fun resume(value: ReaderAndMs) {
                    if (executor == null)
                        try { cont.resume(TAndMs(cons(value.reader), value.ms)) } catch (ex: Exception) { cont.resumeWithException(ex) }
                    else  // can run the constructor somewhere else with an executor
                        executor.execute { try { cont.resume(TAndMs(cons(value.reader), value.ms)) } catch (ex: Exception) { cont.resumeWithException(ex) } }
                }

                override fun resumeWithException(exception: Throwable) = cont.resumeWithException(exception)
            })

    override fun getWriter0(command: Int) = BinaryWriter().apply { writeVarint(command.toLong()) }

    override fun getWriter(command: Int) = getWriter0(command).apply {
        writeString(token)
    }


    val socket = useSocket ?: SharedClients.inst.get(server, port)

//    val photo: CoverAndProfileCache = CoverAndProfileCache(this)

    override fun toString() = socket.toString() + "  " + user + " " + token
//    override val userTag get() = "user." + user





    //  /**
    //   * Gets some bytes that the DbTag server can identify, for example via a database table read.
    //   */
    //  public void getUriBytes(String uri, @NonNull final ResultAvailable callback)
    //    {
    //    db.queue(new BinaryWriter()
    //            .writeString("GetUriBytes").writeString(token).writeString(uri).toByteArray(),
    //        new ProcessReader()
    //        {
    //        @Override
    //        public void run(@Nullable Exception ex, @Nullable BinaryReader reader)
    //          {
    //          Object result = ex;
    //          if (reader != null)
    //            try
    //              {
    //              result = reader.readBytes(reader.readInt());
    //              }
    //            catch (Exception ex2)
    //              {
    //              result = ex2;
    //              }
    //          if (DEBUG) if (result instanceof Exception)
    //            Log.e(TAG, "getUriBytes", (Exception) result);
    //          callback(result);
    //          }
    //        });
    //    }

    //  public static class PreviousAndNext
    //    {
    //    public final YLabel previous, next;
    //
    //    public PreviousAndNext(YLabel previous, YLabel next)
    //      {
    //      this.previous = previous;
    //      this.next = next;
    //      }
    //    }
    //
    //  /**
    //   * All kinds of fixed ping to go at the top of a wall.
    //   */
    //  public static class TaggedRow
    //    {
    ////    public final static int FRIEND_NOT_FRIENDABLE = 0, FRIEND_NOT_FRIEND = 1, FRIEND_FRIEND = 2,
    ////        FRIEND_NOT_FOLLOWING = 3, FRIEND_FOLLOWING = 4, FRIEND_FOLLOWING_WITH_NOTIFICATIONS = 5;
    ////
    ////    public final int messagesCount;
    ////    public final long minDate, maxDate;
    ////    public final @NonNull PreviousAndNext previousAndNext;
    ////    public final @NonNull TopicSummary[] topicSummaries;
    ////    public final @NonNull String skypeName;
    //////    public final Bitmap cover, profile;
    ////    public final @NonNull String coverLinkUrl, coverLinkText;
    ////    public final int friend;  // one of FRIEND_
    ////    public final @NonNull LineItem[] lineItems;
    ////    public final @NonNull String[] personTags;
    //////    public final @NonNull String[] equipmentTags;
    ////    public final @NonNull Button[] buttons;
    //
    //    public TaggedRow(@NonNull BinaryReader reader) throws EOFException
    //      {
    ////      // Summary statistics
    ////      messagesCount = reader.readInt();
    ////      minDate = reader.readLong();
    ////      maxDate = reader.readLong();
    ////      previousAndNext = new PreviousAndNext(new YLabel(reader), new YLabel(reader));
    ////
    ////      int count = reader.readInt();
    ////      topicSummaries = new TopicSummary[count];
    ////      for (int i = 0; i < count; ++i)
    ////        topicSummaries[i] = new TopicSummary(reader);
    ////
    ////      skypeName = reader.readString();
    //////      count = reader.readInt();
    //////      cover = (count == 0) ? null : BitmapFactory.decodeByteArray(reader.readBytes(count), 0, count);
    //////      count = reader.readInt();
    //////      profile = (count == 0) ? null : BitmapFactory.decodeByteArray(reader.readBytes(count), 0, count);
    ////
    ////      coverLinkUrl = reader.readString();
    ////      coverLinkText = reader.readString();
    ////      friend = reader.readInt();
    ////
    ////      count = reader.readInt();
    ////      lineItems = new LineItem[count];
    ////      for (int i = 0; i < count; ++i)
    ////        lineItems[i] = new LineItem(reader);
    ////
    ////      count = reader.readInt();
    ////      personTags = new String[count];
    ////      for (int i = 0; i < count; ++i)
    ////        personTags[i] = reader.readString();
    ////
    //////      count = reader.readInt();
    //////      equipmentTags = new String[count];
    //////      for (int i = 0; i < count; ++i)
    //////        equipmentTags[i] = reader.readString();
    ////
    ////      count = reader.readInt();
    ////      buttons = new Button[count];
    ////      for (int i = 0; i < count; ++i)
    ////        buttons[i] = new Button(reader);
    //      }
    //
    //
    ////    public static class LineItem
    ////      {
    ////      public final String title, text, tag;
    ////
    ////      public LineItem(@NonNull BinaryReader reader) throws EOFException
    ////        {
    ////        title = reader.readString();
    ////        text = reader.readString();
    ////        tag = reader.readString();
    ////        }
    ////      }
    ////
    ////    public static class Button
    ////      {
    ////      public final String title, type, tag;
    ////
    ////      public Button(@NonNull BinaryReader reader) throws EOFException
    ////        {
    ////        title = reader.readString();
    ////        type = reader.readString();
    ////        tag = reader.readString();
    ////        }
    ////      }
    //    }



    //  public static class SearchKeyResult
    //    {
    //    @NonNull
    //    public final String key, items[], names[];
    //
    //    public SearchKeyResult(@NonNull BinaryReader reader) throws EOFException
    //      {
    //      key = reader.readString();
    //      int count = reader.readInt();
    //      items = new String[count];
    //      names = new String[count];
    //      for (int i = 0; i < count; ++i)
    //        {
    //        items[i] = reader.readString();
    //        names[i] = reader.readString();
    //        }
    //      }
    //    }
    //
    //  public void previousAndNext(@NonNull String tag, int count, @NonNull final ResultAvailable callback)
    //    {
    //    db.queue(new BinaryWriter().writeString("PreviousAndNext").writeString(token).
    //                 writeString(tag).writeInt(count).toByteArray(),
    //        new ProcessReader()
    //        {
    //        @Override
    //        public void run(@Nullable Exception ex, @Nullable BinaryReader reader)
    //          {
    //          Object result = ex;
    //          if (reader != null)
    //            try
    //              {
    //              result = new PsAndNs(reader);
    //              }
    //            catch (Exception ex2)
    //              {
    //              result = ex2;
    //              }
    //          if (DEBUG) if (result instanceof Exception)
    //            Log.e(TAG, "previousAndNext", (Exception) result);
    //          callback(result);
    //          }
    //        });
    //
    //    }
    //
    //  public static class PsAndNs
    //    {
    //    @NonNull
    //    public final YLabel[] tnPrevious, tnNext;
    //    public PsAndNs(@NonNull BinaryReader reader) throws EOFException
    //      {
    //      int count = reader.readInt();
    //      tnPrevious = new YLabel[count];
    //      for (int i = 0; i < count; ++i)
    //        tnPrevious[i] =  new YLabel(reader);
    //      count = reader.readInt();
    //      tnNext = new YLabel[count];
    //      for (int i = 0; i < count; ++i)
    //        tnNext[i] =  new YLabel(reader);
    //      }
    //    }


    companion object {
        const val InsertCredentials = 1
        const val Insert = 2
        const val Attachment = 3
        const val Thumbnail = 4
        const val Count = 5
        const val LeftMatchTagNameOrCode = 6
        const val Codes = 7
        const val LastValue = 8
        const val Select= 9
        const val Topics = 10
        const val Databases = 11
        const val SignIn = 12
        const val CrossTab = 13
        const val TopicSummaries = 14
        const val TCP = 15
        const val TimeSlotSummaries = 16
        const val Name = 17
        const val CodesSimple = 18
        const val TagsWithValues = 19
        const val Message = 20
        const val LastValueTopicPair= 21
        const val MatchSpeech = 22
        const val PairsCount = 23

        //  public final static int FOLLOWING_NO = 0, FOLLOWING_YES = 1, FOLLOWING_YES_WITH_NOTIFICATIONS = 2;

        //  public void lastNotificationTime(final ResultAvailable callback)
        //    {
        //    db.queue(new BinaryWriter()
        //                  .writeString("NotificationsLastTime").writeString(token_)
        //                  .toByteArray(),
        //     new ProcessReader() { @Override public void run(@Nullable Exception ex, @Nullable BinaryReader reader)
        //      {
        //      Object result = ex;
        //      if (result == null)
        //       try
        //        {
        //        result = reader.readLong();
        //        } catch (Exception ex2) { result = ex2; }
        //  if (DEBUG) if (result instanceof Exception)
        //      Log.e(TAG, "lastNotificationTime", (Exception) result);
        //      callback(result);
        //      }});
        //    }
        //

        //  /**
        //   * Used for the menu.
        //   */
        //  public void tagsInTopicOrderByMentionDate(@NonNull String topic, long dateLessThan, int limit, final @NonNull ResultCallback<TagNameMentions> callback)
        //    {
        //    db.queue(new BinaryWriter()
        //            .writeString("TagsInTopicOrderByMentionDate").writeString(token)
        //            .writeString(topic).writeLong(dateLessThan).writeInt(limit)
        //            .toByteArray(), new ProcessReader() { @Override public void run(@Nullable Exception ex, @Nullable BinaryReader reader)
        //      {
        //      TagNameMentions tagNameMentions = null;
        //      if (reader != null)
        //        try
        //          {
        //          tagNameMentions = new TagNameMentions(reader);
        //          }
        //        catch (Exception ex2)
        //          {
        //          ex = ex2;
        //          }
        //      if (DEBUG) if (ex != null)
        //        Log.e(TAG, "tagsInTopicOrderByMentionDate", ex);
        //      callback(ex, tagNameMentions);
        //      }});
        //    }
        //
        //  /**
        //   * Used for the menu.
        //   */
        //  public void tagsInTopicOrderByName(@NonNull String topic, final @NonNull ResultCallback<YLabel[]> callback)
        //    {
        //    db.queue(new BinaryWriter()
        //            .writeString("TagsInTopicOrderByName").writeString(token)
        //            .toByteArray(), new ProcessReader() { @Override public void run(@Nullable Exception ex, @Nullable BinaryReader reader)
        //      {
        //      YLabel[] tags = null;
        //      if (reader != null)
        //        try
        //          {
        //          tags = new YLabel[reader.readInt()];
        //          for (int i = 0; i < tags.length; ++i)
        //            tags[i] = new YLabel(reader);
        //          }
        //        catch (Exception ex2)
        //          {
        //          ex = ex2;  tags = null;
        //          }
        //      if (DEBUG) if (ex != null)
        //        Log.e(TAG, "tagsInTopicOrderByMentionDate", ex);
        //      callback(ex, tags);
        //      }});
        //    }


        //  @Nullable String ref, @Nullable String id, long messageDate, long until, @Nullable String fromTag, @Nullable String fromName,
        //  @Nullable String contentAsSpanned, int importance, double latitude, double longitude,
        //  @Nullable Attachment[] attachments




        //  public void filteredTagsInTopicOrderByName(@Nullable Filter toFilter, @NonNull String topic,
        //                                             @Nullable String compareName, boolean lessThan, int limit,
        //                                             final @NonNull ResultCallback<YLabel[]> callback)
        //    {
        //    BinaryWriter writer = new BinaryWriter().writeString("FilteredTagsInTopicOrderByName").writeString(token);
        //    toFilter.write(writer);
        //    db.queue(writer.writeString(topic)
        //        .writeString(compareName).writeBoolean(lessThan).writeInt(limit).toByteArray(), new ProcessReader() { @Override public void run(@Nullable Exception ex, @Nullable BinaryReader reader)
        //      {
        //      YLabel[] tags = null;
        //      if (reader != null) try
        //        {
        //        tags = new YLabel[reader.readInt()];
        //        for (int i = 0; i < tags.length; ++i)
        //          tags[i] = new YLabel(reader);
        //        } catch (Exception ex2) { ex = ex2;  tags = null;}
        //      if (DEBUG) if (ex != null)
        //        Log.e(TAG, "filteredTagsInTopicOrderByName", ex);
        //      callback(ex, tags);
        //      }});
        //    }

        //  public void maxUpdate(@Nullable final Filter toFilter, final @NonNull ResultCallback<Long> callback)
        //    {
        //    BinaryWriter writer = new BinaryWriter().writeString("MaxUpdated").writeString(token);
        //    toFilter.write(writer);
        //    db.queue(writer.toByteArray(), new ProcessReader() { @Override public void run(@Nullable Exception ex, @Nullable BinaryReader reader)
        //      {
        //      Long updated = null;
        //      if (reader != null) try
        //        {
        //        updated = reader.readLong();
        //        } catch (Exception ex2) { ex = ex2;  }
        //      if (DEBUG) if (ex != null)
        //        Log.e(TAG, "maxUpdate", ex);
        //      callback(ex, updated);
        //      }});
        //    }



        //  /**
        //   * Give some items, we count up how many times each possible pair occurs in messages.
        //   * For example, with 3 items we look up T1/T1, T1/T2, T1/T3, T2/T2, T2/T3, T3/T3
        //   * so note that we also look for the same topic occurring twice in a message
        //   */
        //  public void pairsCount(@Nullable Filter toFilter, long ifUpdatedAfter, @NonNull String[] items, final @NonNull ResultCallback<YLabel[]> callback)
        //    {
        //    final int FILTER = 1, IF_UPDATED_AFTER = 2, TOPIC = 3;
        //
        //    BinaryWriter writer = new BinaryWriter().writeString("PairsCount").writeString(token);
        //    if (toFilter != null && !toFilter.isEmpty())
        //      { BinaryWriter.StoreEmbedded emb = writer.embeddedField(FILTER); toFilter.write(writer); emb.close(); }
        //    if (ifUpdatedAfter != 0)
        //      writer.writeFieldFixed64(IF_UPDATED_AFTER, ifUpdatedAfter);
        //    for (String topic : items)
        //      writer.writeField(TOPIC, topic);
        //
        //    db.queue(writer.toByteArray(), new ProcessReader() { @Override public void run(@Nullable Exception ex, @Nullable BinaryReader reader)
        //      {
        //      YLabel[] ret = null;
        //      if (reader != null)
        //        try
        //          {
        //          }
        //        catch (Exception ex2)
        //          {
        //          ex = ex2;
        //          }
        //      if (DEBUG) if (ex != null)
        //        Log.e(TAG, "pairsCount", ex);
        //      callback(ex, ret);
        //      }});
        //    }


        ////  public static class Pair
        ////    {
        ////    public final int count, ofs1, ofs2;
        ////    public Pair(int count, int ofs1, int ofs2)
        ////      {
        ////      this.count = count;  this.ofs1 = ofs1;  this.ofs2 = ofs2;
        ////      }
        ////    }
        //
        //  /**
        //   * Gets the LUB and GLB
        //   */
        //  public void LUBandGLB(long startTime, long endTime, @Nullable String[] exactTags, @Nullable String topic, @NonNull final ResultAvailable callback)
        //    {
        //    BinaryWriter writer = new BinaryWriter().writeString("LUBandGLB").writeString(token)
        //        .writeLong(startTime).writeLong(endTime);
        //    if (exactTags == null)
        //      writer.writeInt(0);
        //    else
        //      {
        //      int exactTagCount = exactTags.length;
        //      writer.writeInt(exactTagCount);
        //      for (String exactTag : exactTags)
        //        writer.writeString(exactTag);
        //      }
        //    if (topic == null)
        //      topic = "";
        //    writer.writeString(topic);
        //
        //    db.queue(writer.toByteArray(), new ProcessReader() { @Override public void run(@Nullable Exception ex, @Nullable BinaryReader reader)
        //      {
        //      Object result = ex;
        //      if (reader != null)
        //        try
        //          {
        //          result = new Range(reader.readLong(), reader.readLong());
        //          }
        //        catch (Exception ex2)
        //          {
        //          result = ex2;
        //          }
        //      if (DEBUG) if (result instanceof Exception)
        //        Log.e(TAG, "LUBandGLB", (Exception) result);
        //      callback(result);
        //      }});
        //    }
        //
        //  public static class Range
        //    {
        //    public final long start, end;
        //
        //    public Range(long start, long end)
        //      {
        //      this.start = start;
        //      this.end = end;
        //      }
        //    }

    }
}


////  public int color()
////    {
////    TagNameDate[] tns = extraTagAndNames;
////    for (TagNameDate tn : tns)
////      {
////      String tag1 = tn.tag;
////      if (tag1.regionMatches(true, 0, "Color/", 0, 6))
////        {
////        try
////          {
////          int intColor = Integer.parseInt(tag1.substring(6));
////          // Watch out - the order of bytes is different to Windows
////          return Color.argb(255, Color.blue(intColor), Color.green(intColor), Color.red(intColor));
////          }
////        catch (NumberFormatException e)
////          {
////          }
////        break;
////        }
////      }
////    if (tns.length != 0)
////      return crazyColor(tns[0].tag);
////    return Color.LTGRAY;
////    }
//private val multiplier = 0x5deece66dL
//
///**
// * Takes a value and sends it somewhere else unexpected, so that nearby input values
// * do not remain nearby.
// * Based on the code in java.util.Random
// */
//@ColorInt private fun crazify(value: Int): Int {
//    return (value * multiplier + 0xbL and (1L shl 48) - 1).ushr(16).toInt()
//}
//
///**
// * Gets a stand-in color corresponding to a String.
// */
//@ColorInt fun crazyColor(s: String): Int {
//    return crazify(s.hashCode()) and 0xffffff or 0xff000000.toInt()
//}
//
//private val random = Random()
//private fun vary(component: Int, noise: Int): Int {
//    return Math.min(255, Math.max(0, component + random.nextInt(noise * 2 + 1) - noise))
//}
//
//@ColorInt fun crazyColorSlightlyRandom(s: String, noise: Int): Int {
//    val color = crazyColor(s)
//    return Color.argb(Color.alpha(color), vary(Color.red(color), noise), vary(Color.green(color), noise), vary(Color.blue(color), noise))
//}

