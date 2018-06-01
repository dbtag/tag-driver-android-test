//package org.dbtag.data
//
//import org.dbtag.driver.Parts
//
//class MessagesInTimeRange(val db: UserQueue, val mustBeTagged: Boolean, val minimumImportance: Int,
//                          val requiredTags: Array<String>, val excludedTags: Array<String>?) {
//    private var messages_ = arrayOfNulls<Message>(16)
//    private var messagesCount_: Int = 0  // in increasing time order
//    private val whenComparePast_: Long = 0
//    private var dateCompareFuture_: Long = 0
//    private val pastExhausted_: Boolean = false
//    private var pendingGetSomePast_: Boolean = false
//
//    init {
//        // Get a limited number of the most recent
//        getSomePast(pastLump * 3 / 2)
//    }
//
//    private var ms_: Int = 0
//    private val timer_ = Runnable {
//        refresh()
//        nudgeTimer()
//    }
//
//    private fun nudgeTimer() {
//        val ms = ms_
//        if (ms != 0)
//            Parts.handler.postDelayed(timer_, ms.toLong())
//    }
//
//    fun setAutoRefresh(ms: Int) {
//        ms_ = ms
//        if (ms != 0)
//            nudgeTimer()
//        else
//            Parts.handler.removeCallbacks(timer_)
//    }
//
//    private var busy_: Int = 0
//    private val cache_: Any? = null
//
//    fun refresh() {
//        if (busy_ != BUSY_NO)
//            return
//        if (pendingGetSomePast_)
//        // this will take priority
//        {
//            pendingGetSomePast_ = false
//            getSomePast(pastLump)
//            return
//        }
//        busy_ = BUSY_FUTURE
//
//        // Everything after the last message we have
//        // long idCompare = (messagesCount_ == 0) ? 0 : messages_[messagesCount_ - 1].date;
//        //    cache_ = db.messagesFuture(cache_, mustBeTagged, minimumImportance, dateCompareFuture_, requiredTags, excludedTags, messagesAvailable_);
//    }
//
//    private val messagesAvailable_ = object : ResultCallback<messagesData> {
//        override fun onResult(ex: Exception?, result: messagesData?) {
//            if (result == null) {
//                // TODO: show it ?
//            }
//            else {
//                dateCompareFuture_ = 0 // messagesData.lastDateCompare;
//                val newMessages = result.messages
//                if (newMessages != null) {
//                    Parts.handler.post {
//                        // Just append them at the end of our internal dataPoints_ (the beginning of the dataPoints_ viewed from outside)
//                        val count = newMessages.size
//                        val oldMessagesCount = messagesCount_
//                        setMessagesCount(oldMessagesCount + count)
//                        System.arraycopy(newMessages, 0, messages_, oldMessagesCount, count)
//                        // TODO: onNotifyItemRangeInserted(headers_.count(), count);
//
//                        busy_ = BUSY_NO
//                    }
//                    return
//                }
//            }
//            busy_ = BUSY_NO
//        }
//    }
//
//    private fun getSomePast(limit: Int) {
//        if (busy_ != BUSY_NO)
//            return
//        busy_ = BUSY_PAST
//
//        //    db.messagesPast(mustBeTagged, minimumImportance, whenComparePast_, limit, requiredTags, excludedTags,
//        //        new TagUnsignedInClient.ResultAvailable() { @Override public void onResult(Object result)
//        //          {
//        //          if (result instanceof Exception)
//        //            {
//        //            // TODO: show it
//        //            busy_ = BUSY_NO;  return;
//        //            }
//        //          final messagesData messagesData = (messagesData) result;
//        //          if (messagesData == null)
//        //            {
//        //            // We get a null if no Message's
//        //            pastExhausted_ = true;
//        //            busy_ = BUSY_NO;  return;
//        //            }
//        //          whenComparePast_ = messagesData.lastDateCompare;
//        //          if (dateCompareFuture_ == 0)
//        //            dateCompareFuture_ = messagesData.firstDateCompare;  // must do at least one Past before any Future
//        //          final Message[] newMessages = messagesData.findAsync;
//        //          handler_.post(new Runnable() { @Override public void run()
//        //            {
//        //            // In this case, the new findAsync precede the ones we already have
//        //            // in time, so we must insert at the beginning but in reverse order
//        //            // to keep the most recent at the end
//        //            final int count = newMessages.length;
//        //            if (count != 0)
//        //              {
//        //              final int oldMessagesCount = messagesCount_;
//        //              setMessagesCount(count + oldMessagesCount);
//        //              // Insert to make room
//        //              System.arraycopy(messages_, 0, messages_, count, oldMessagesCount);
//        //              for (int i = 0; i < count; ++i)
//        //                messages_[count - 1 - i] = newMessages[i];
//        //              }
//        //
//        //            // Maybe we can stop trying for things in the past and stop displaying the progress item.
//        //            if (count < limit)
//        //              pastExhausted_ = true;
//        //
//        //            // TODO: notifyDataSetChanged();
//        //            busy_ = BUSY_NO;
//        //            }});
//        //          }});
//    }
//
//    /**
//     * Tries to ensure that some past stuff at the given position is in the process of loading.
//     */
//    fun ensurePast(position: Int) {
//        if (!pastExhausted_ && position + pastLump / 2 >= messagesCount_) {
//            // Try for some more past
//            if (busy_ == BUSY_NO)
//                getSomePast(pastLump)
//            else if (busy_ != BUSY_PAST)
//                pendingGetSomePast_ = true
//        }
//    }
//
//    /**
//     * Sets a new value for messagesCount, increasing the storage space if necessary.
//     */
//    private fun setMessagesCount(value: Int) {
//        if (value > messages_.size) {
//            val newMessages = arrayOfNulls<Message>(Math.max(2 * messages_.size, value))
//            System.arraycopy(messages_, 0, newMessages, 0, messagesCount_)
//            messages_ = newMessages
//        }
//        messagesCount_ = value
//    }
//
//    companion object {
//
//        private const val pastLump = 40
//
//        private const val BUSY_NO = 0
//        private const val BUSY_PAST = 1
//        private const val BUSY_FUTURE = 2
//    }
//}
