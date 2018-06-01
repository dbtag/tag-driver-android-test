package org.dbtag.data

import org.dbtag.driver.Parts
import org.dbtag.driver.Queue
import org.dbtag.driver.TAndMs
import org.dbtag.driver.select
import java.util.concurrent.Executor
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Just enough about a message to keep up-to-date and notify about changes
 */
open class MiniMessage(args: MessageConstructArgs) {
    val mid = args.mid
    val date = args.date
    val deleted = args.deleted

    override fun hashCode() = mid
    override fun equals(other: Any?) = when {
        this === other -> true
        javaClass != other?.javaClass -> false
        mid != (other as MiniMessage).mid -> false
        else -> true
    }

    companion object {
        const val parts = Parts.Mid or Parts.Date or Parts.Deleted
    }
}


open class MidiMessage(args: MessageConstructArgs) : MiniMessage(args) {
    val until = args.until
    val content = args.content
    val tags by lazy { content.parseTagsOnly() }
    val from by lazy { with(tags) { if (isNotEmpty()) get(0) else Tag.empty } }

    companion object {
        const val parts = M0Impl.parts or Parts.Content
    }
}


val Message.hasLocation get() = (latitude != 0.0 || longitude != 0.0)

class Message(args: MessageConstructArgs) : MidiMessage(args) {
    val id = args.id
    val updated = args.updated
    val importance = args.importance
    val latitude = args.latitude
    val longitude = args.longitude
    val firstThumbnail = args.firstThumbnail
    val comments = args.comments
    val noters = args.noters
    val takers = args.takers
    val attachments = args.attachments
    val notified = args.notified

    init {
        val a = attachments

    }

//    val fromWasRequired get() = requiredTags_.any { from.tag == it || from.topicEquals(it) }

//    fun messageContent() = messageContent
//    fun notedBy(tag: String) = noters.any { it.tag == tag }
//    val commentsCount get() = comments.size

    companion object {
        const val IMPORTANCE_LOW = 0
        const val IMPORTANCE_NORMAL = 1
        const val IMPORTANCE_HIGH = 2

        const val parts = Parts.All

        suspend fun select(queue: Queue, filter: Filter, limit: Int = Integer.MAX_VALUE, ifUpdatedAfter: Long = 0L,
                           desc: Boolean = false, executor: Executor? = null) = suspendCoroutine< TAndMs<MessagesData<Message>>> { cont->
            select(queue, filter, limit, ifUpdatedAfter, desc, executor, cont)
        }

        fun select(queue: Queue, filter: Filter, limit: Int = Integer.MAX_VALUE, ifUpdatedAfter: Long = 0L,
                         desc: Boolean = false, executor: Executor? = null, cont: Continuation<TAndMs<MessagesData<Message>>>) {
            queue.select(filter, 0, Filter.empty, limit, ifUpdatedAfter, desc, Parts.All, { args: MessageConstructArgs ->
                Message(args)
            }, executor, cont)
        }
    }

    //  public final @NonNull String toAddress;
    //  public final @NonNull YLabel[] referredTags;
    //  public final @Nullable String[] resultingFromRequiredTags;

        //    // Find every tag referred to in this event, enabling for example, fast filtering
        //    // The item1 one will be the 'from'
        //    int size = h.size();  referredTags = new YLabel[1 + size];
        //    referredTags[0] = from;
        //    int ofs = 1;
        //    for (YLabel tn : h)
        //      referredTags[ofs++] = tn;


    //  public void setNoters(TagNameDate[] noters) { noters_ = noters; }
    //
    //  public void setTakers(TagNameDate[] takers) { takers_ = takers; }



    //  // Used for filtering
    //  private final Set<String> referredTags = new HashSet<String>();  // TODO: should also be case-insensitive

    //  /**
    //   * Checks whether we refer to any of the given tags.
    //   */
    //  public boolean refersToAnyOf(@NonNull String[] tags)
    //    {
    //    for (String tag : tags)
    //     if (Arrays.binarySearch(referredTags, tag) >= 0)
    //      return true;
    ////    for (int i = 0; i < tags.length; ++i)
    ////      {
    ////      // The given tags may be of form either Batches or Batches/123
    ////      String tag = tags[i];
    ////      int fGroup = tag.indexOf('.');
    ////      for (String t : referredTags)
    ////        {
    ////        if (tag.equals(t))
    ////          return true;  // easy enough
    ////        if (fGroup == -1)
    ////          {
    ////          int tagLength = tag.length();
    ////          if (t.length() > tagLength + 1 && t.charAt(tagLength) == '.' && t.regionMatches(0, tag, 0, tagLength))
    ////            return true;
    ////          }
    ////        }
    ////      }
    //    return false;
    //    }
    //
    //  public int indexOfTopicReference(String topic, int start)
    //    {
    //    for (; start < referredTags.length; ++start)
    //     if (referredTags[start].topic.equals(topic))
    //      return start;
    //    return -1;
    //    }
    //
    //  public int indexOfTagReference(String tag, int start)
    //    {
    //    for (; start < referredTags.length; ++start)
    //      if (referredTags[start].tag.equals(tag))
    //        return start;
    //    return -1;
    //    }
    //
    //  public @NonNull YLabel getReferredTagAndName(int index)
    //    {
    //    if (index-- == 0)
    //      return from;
    //    return referredTags[index];
    //    }

    //  public YLabel[] getTagAndNamesForTopic(String topic)
    //    {
    //    List<YLabel> stackAtADates = new ArrayList<YLabel>();
    //    if (from.topic.equals(topic))
    //      stackAtADates.add(from);  // don't forget this one
    //    for (YLabel tn : referredTags)
    //     if (tn.topic.equals(topic))
    //       stackAtADates.add(tn);
    //    return toStackAtADates.toArray(new YLabel[toStackAtADates.count()]);
    //    }


    //  public boolean tagWasRequired(String tag)
    //    {
    //    if (resultingFromRequiredTags != null)
    //      {
    //      String topic = YLabel.topicFromTag(tag);
    //      for (String tag2 : resultingFromRequiredTags)
    //        {
    //        if (tag2.equals(tag) || tag2.equals(topic))
    //          return true;
    //        }
    //      }
    //    return false;
    //    }


    //  private Spannable decodeSomething(String source)
    //    {
    //    SpannableStringBuilder sb = new SpannableStringBuilder();
    //    return new SomeSpanner(source)
    //    }

    //  /**
    //   * Decodes html into a spanned ping for our display
    //   */
    //  private Spannable decodeSomething(String source)
    //    {
    //    Spanned spanned = Html.fromHtml(source,
    //      new Html.ImageGetter() { @Override public Drawable getDrawable(@SuppressWarnings("unused") String source2)
    //        {
    //        return null;
    //        }},
    //      new Html.TagHandler() { @Override public void handleTag(boolean opening, String tag, @SuppressWarnings("unused") Editable output, XMLReader xmlReader)
    //        {
    //        if (tag.equalsIgnoreCase("embed"))
    //          {
    //          if (opening)
    //            startEmbed(TagSoupElementAttributes.attributes(xmlReader));
    //          }
    //        }});
    //    SpannableString stackAtADates = YourCustomClickableSpan.linkify(spanned, onTagNameClick_);
    //
    //    // It could be too long for normal purposes
    //    final int maxLength = 700; // TODO: should be 300;
    //    if (stackAtADates.length() <= maxLength)
    //      return stackAtADates;
    //
    //    SpannableStringBuilder sb = new SpannableStringBuilder(stackAtADates.subSequence(0,  maxLength));
    //    sb.append("....");
    //
    //    int start = sb.length();
    //    sb.append("Continue Reading");
    //    sb.setSpan(new GrayPressableClickableSpan(){ @Override public void onClick(@SuppressWarnings("unused") View widget)
    //      {
    //      MainActivity.inst.setFragment(new SingleEventItemFragment(), null);
    //      }}, start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    //    return sb;
    //    }



//    fun setNotedBy(tag: String, name: String, notedBy: Boolean) {
        //    final int notersCount = noters_.length;
        //    for (int i = 0; i < notersCount; ++i)
        //      {
        //      if (noters_[i].tag.equalsIgnoreCase(tag))
        //        {
        //        if (!notedBy)
        //          {
        //          // Remove one item from the array
        //          TagNameDate[] n = new TagNameDate[notersCount - 1];
        //          System.arraycopy(noters_, 0, n, 0, i);
        //          System.arraycopy(noters_, i + 1, n, i, notersCount - (i + 1));
        //          noters_ = n;
        //          }
        //        return;  // all is good
        //        }
        //      }
        //    // Add a new noter at the end
        //    TagNameDate[] n = new TagNameDate[notersCount + 1];
        //    System.arraycopy(noters_, 0, n, 0, notersCount);
        //    n[notersCount] = new TagNameDate(tag, name, System.currentTimeMillis());  // our UTC time, possible not exactly the same as the server UTC time
        //    noters_ = n;
//    }

//    fun extraCommentAdded() {
//        //    commentsCount_ += 1;
//    }


//    val idOrMid get() = if (!id.isNullOrEmpty()) id else "+" + Integer.toString(mid)

}


//val fromAsSpanned by lazy {
//    with (SpannedBuilder()) {
//        setText(from.name)
//        val span = ClickableTagSpan(from, clickableTagCallBack__)
//        if (fromWasRequired)
//            span.highlightColor = 0xFFFF6600.toInt()
//        append(0, length, 0, span)
//        this
//    }
//    //      from_ = decodeSomething("<a href=\"" + firstTag + "\">" + fromName + "</a>");  // and maybe some other ping
//}
//
//// Returns the messageContent as text and ClickableTagSpan's
//val contentAsSpanned by lazy  {
//    //    final List<YLabel> h = new ArrayList<>();
//    HashTagSpanned.create(messageContent, HashTagSpanned.CreateSpan { tag ->
//        // String tag = tag.tag;
//        // TODO: Ignore duplicate tags in h
//        //    h.add(tag);
//        val span = ClickableTagSpan(tag, clickableTagCallBack__)
//
//        if (requiredTags_ != null)
//            for (t in requiredTags_) {
//                if (tag.tag == t || tag.topicEquals(t)) {
//                    span.highlightColor = 0xFFFF6600.toInt()
//                    break
//                }
//            }
//        span
//    })
//}


//abstract class GrayPressableClickableSpan : ClickableSpan(), LinkTouchMovementMethod.Pressed {
//    private var pressed_: Boolean = false
//
//    override fun updateDrawState(ds: TextPaint) {
//        ds.color = Color.GRAY
//        ds.bgColor = if (pressed_) Color.LTGRAY else Color.TRANSPARENT
//    }
//
//    override fun setPressed(pressed: Boolean) {
//        pressed_ = pressed
//    }
//
//    override fun getPressed(): Boolean {
//        return pressed_
//    }
//}
//
//
//private var clickableTagCallBack_: ClickableTagSpan.CallBack? = null
//fun setClickableTagCallBack(clickableTagCallBack: ClickableTagSpan.CallBack) {
//    clickableTagCallBack_ = clickableTagCallBack
//}
//
//private val clickableTagCallBack__ = object : ClickableTagSpan.CallBack {
//    override fun onTagNameClick(tag: YLabel) {
//        clickableTagCallBack_?.onTagNameClick(tag)
//    }
//
//    override fun onTagNameLongClick(tag: YLabel) {
//        clickableTagCallBack_?.onTagNameLongClick(tag)
//    }
//
//    override fun onTagNameTouchDown(tag: YLabel, view: View, x: Int, y: Int) {
//        clickableTagCallBack_?.onTagNameTouchDown(tag, view, x, y)
//    }
//
//    override fun onTagNameTouchUp(tag: YLabel) {
//        clickableTagCallBack_?.onTagNameTouchUp(tag)
//    }
//}
