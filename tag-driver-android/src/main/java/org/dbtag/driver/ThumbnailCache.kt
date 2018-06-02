package org.dbtag.driver

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.dbtag.data.Filter
import org.dbtag.data.MessageIdCommentIndexMaxSize
import org.dbtag.data.Tag
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlin.coroutines.experimental.startCoroutine
import kotlin.coroutines.experimental.suspendCoroutine

class ThumbnailCache(val db: UserQueue) {
    private val cache = CreateJustOnceCacheCallback({
        (mid, comment, index, maxSize): MessageIdCommentIndexMaxSize, cont: Continuation<ByteArray> ->
          suspend { db.thumbnail(mid, comment, index, maxSize)}.startCoroutine(cont)
    })

    /**
     * Gets the bytes of a bitmap that visualises an attachment to a message, by index starting from 0
     */
    suspend fun getThumbnail(mid: Int, comment: Int, index: Int, maxSize: Int) = cache.get(MessageIdCommentIndexMaxSize(mid, comment, index, maxSize))


    /**
     * Gets a bitmap that visualises an attachment to a message, by index starting from 0
     */
    suspend fun getBitmap(mid: Int, comment: Int, index: Int, maxSize: Int) =
        cache.get(MessageIdCommentIndexMaxSize(mid, comment, index, maxSize))?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }
}


enum class Typo { Profile, Cover }

class CoverAndProfileCache(val thumbnailCache: ThumbnailCache) {
    private data class TagTypeMaxSize(val tag: String, val type: Typo, val maxSize: Int)

    private val cache = CreateJustOnceCacheCallback<TagTypeMaxSize, Bitmap?> (
            { (tag, type, maxSize), cont -> suspend { thumbnailCache.db.bitmap(tag, if (type == Typo.Cover) "c" else "p", maxSize,
                    thumbnailCache) }.startCoroutine(cont) })


    /**
     * Gets a photo, trying our cache first.
     */
    suspend fun get(tag: String, type: Typo, maxSize: Int) = cache.get(TagTypeMaxSize(tag, type, maxSize))

    fun getIfCached(tag: String, type: Typo, maxSize: Int) = cache.getIfCached(TagTypeMaxSize(tag, type, maxSize))

//    /**
//     * Convenient alternative that knows about special targets.
//     */
//    fun get(tag: String, type: Typo, maxSize: Int, view: ImageView, defaultBitmap: Bitmap?) {
//        // For now, get rid of anything that might be sitting there because it's probably wrong,
//        // and we don't want it on show if the network stuff is slow
//        view.setImageDrawable(null)
//        get(tag, type, maxSize, { _, result ->
//            result?:defaultBitmap?.let { view.setImageDrawableWithFadeIn(it) }
//        })
//    }

//    /**
//     * Convenient alternative that knows about special targets.
//     */
//    fun get(tag: String, type: Typo, maxSize: Int, view: TextView, width: Int, height: Int) {
//        // For now, get rid of anything that might be sitting there because it's probably wrong,
//        // and we don't want it on show if the network stuff is slow
//        view.setCompoundDrawables(null, null, null, null)
//        get(tag, type, maxSize, { _, result ->
//            if (result != null) {
//                val bitmapDrawable = BitmapDrawable(view.resources, result)
//                val imgWidth = height.toFloat() * bitmapDrawable.intrinsicWidth / bitmapDrawable.intrinsicHeight
//                bitmapDrawable.setBounds(0, 0, imgWidth.toInt(), height.toFloat().toInt())
//
//                if (Looper.myLooper() == Looper.getMainLooper())
//                    view.setCompoundDrawables(bitmapDrawable, null, null, null)
//                else
//                    view.post { view.setCompoundDrawables(null, null, bitmapDrawable, null) }
//            }
//        })
//    }

    //  /**
    //   * Only for the current logged in user (tag)
    //   */
    //  public void setPhoto(final String tag, final String name, byte[] bytes, final Runnable onComplete)
    //    {
    //    db.queue(new BinaryWriter()
    //                 .writeString("SetPhoto").writeString(token).writeString(tag).writeString(name).writeBytes(bytes)
    //                 .toByteArray(), new ProcessReader() { @Override public void run(Exception ex, BinaryReader reader)
    //      {
    //      if (ex == null)
    //        {
    //        cache.remove(new TagTypeMaxSize(tag, name, 0));  // there should now be a new version of this
    //        if (onComplete != null)
    //          onComplete.run();
    //        }
    //      }});
    //    }

}

// tag = userTag

// These don't use the CoverAndProfileCache, only the ThumbnailCache
suspend fun UserQueue.profile(tag: String, maxSize: Int, thumbnailCache: ThumbnailCache)
        = bitmap(tag, "p", maxSize, thumbnailCache)

suspend fun UserQueue.cover(tag: String, maxSize: Int, thumbnailCache: ThumbnailCache)
        = bitmap(tag, "c", maxSize, thumbnailCache)

suspend fun UserQueue.bitmap(tag: String, sysCode: String, maxSize: Int, thumbnailCache: ThumbnailCache) =
    getLastMessageFirstThumbnail(Filter(listOf(Tag("sys.$sysCode"), Tag(tag))), maxSize, thumbnailCache)?.let {
        BitmapFactory.decodeByteArray(it, 0, it.size)
    }



//fun ImageView.setImageDrawableWithFadeIn(bitmap: Bitmap) {
//    setImageDrawableWithFadeIn(BitmapDrawable(resources, bitmap))
//}
//
//fun ImageView.setImageDrawableWithFadeIn(drawable: Drawable) {
//    // If we are already on the UI thread, it probably means we should show directly without fade
//    if (Looper.myLooper() == Looper.getMainLooper()) {
//        visibility = View.VISIBLE
//        setImageDrawable(drawable)
//    }
//    else
//        post {
//            setImageDrawable(drawable)
//            val animation = AlphaAnimation(0f, 1f)
//            animation.duration = 600  // fade in for a calm effect
//            startAnimation(animation)
//            visibility = View.VISIBLE
//        }
//}
