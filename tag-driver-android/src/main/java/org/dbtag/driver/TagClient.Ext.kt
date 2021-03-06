package org.dbtag.driver

import android.net.Uri
import org.dbtag.data.Filter
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.coroutines.experimental.suspendCoroutine


// Extra things we can do
/**
 * Gets the subset of day numbers on which there are messages matching the toFilter
 * by re-packaging timeSlotSummariesAsync
 * @param timeZone used to convert the implicit local time in startHour and endHour to the necessary UTC.
 */
suspend fun UserQueue.busyDays(filter: Filter, startDay: Int, endDay: Int, timeZone: TimeZone) = suspendCoroutine<IntArray> { cont->
    val timeSlots = LongArray(endDay - startDay)
    for (i in timeSlots.indices) {
        var time = (startDay + i) * 86400000L
        time -= timeZone.getOffset(time).toLong()  // convert to UTC;
        timeSlots[i] = time
    }

//        db.timeSlotSummaries(toFilter, 0, null, timeSlots, 0) { ex, result ->
//            var result2: IntArray? = null
//            if (result != null) {
//                // We'll have a VariousSummary for each time-slot given above
//                val days = ArrayList<Int>()
//                for (i in result.indices)
//                    if (result[i].count != 0)
//                        days.add(startDay + i)
//                result2 = IntArray(days.size)
//                for (i in result2.indices)
//                    result2[i] = days[i]
//            }
//            callback.onResult(ex, result2)
//        }
}

//  public static void topicSummariesSorted(@NonNull Parts db, @NonNull Filter toFilter, long ifUpdatedAfter,
//                                               @Nullable String specificValueTag, int limitPerTopic,
//                                               @NonNull final ResultCallback<TopicSummary[]> callback)
//    {
//    db.topicSummaries(toFilter, ifUpdatedAfter, specificValueTag, limitPerTopic, new ResultCallback<TopicSummary[]>()
//      {
//      @Override
//      public void onResult(@Nullable Exception ex, @Nullable TopicSummary[] result)
//        {
//        // Sort the topic summaries into alphabetical topic order
//        if (result != null)
//         Arrays.sort(result, new Comparator<TopicSummary>() { @Override public int compare(TopicSummary lhs, TopicSummary rhs)
//          {
//          return lhs.total().name().compareToIgnoreCase(rhs.total().name());
//          }});
//        callback.onResult(ex, result);
//        }
//      });
//    }

/**
 * Saves all a message's attachments as files to the cacheDir and returns an array of URI's that points to those files.
 */
suspend fun UserQueue.saveAttachments(mid: Int, attachmentCount: Int, cacheDir: String) =
    (0 until attachmentCount).map { index ->
        val result = attachment(mid, 0, index)
        val attachmentName = result.name
        val os = FileOutputStream(cacheDir + File.separator + "attachmentName")  // TODO: attachmentName may contain bad file chars
        os.write(result.bytes)
        os.close()
        Uri.parse("contentAsSpanned://" + "CachedFileProvider.AUTHORITY" + "/" + attachmentName)!! //  + "#" + result.mime))
    }
