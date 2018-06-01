package org.dbtag.driver

import org.dbtag.data.Match
import org.dbtag.data.Tag
import org.dbtag.protobuf.WireType

import java.io.EOFException

import org.dbtag.socketComs.BinaryReader

//    public final String messageTag, messageName, messageContent;
//    public final String msgId;
//    public final long messageDate;
//    private CharSequence content_;

fun BinaryReader.match(len: Int) : Match {
    var tag: Tag? = null
    val eor = position + len
    while (position != eor) {
        val key = readByte().toInt()
        val field = (key shr 3)
        when (key and 7) {
            WireType.VARINT -> readVarint()
            WireType.FIXED64 -> skip(8)
            WireType.FIXED32 -> skip(4)
            WireType.LENGTH_DELIMITED -> {
                val len2 = readVarint().toInt()
                when (field) {
                // TODO: no ! should also send and receive ORIGINAL_TAG

                    1 -> {  // TAG
                        val tagStr = readString(len2)
                        tag = Tag(tagStr)
                    }
                    2 -> { // NAME
                        if (tag != null)
                            tag = Tag(tag.tag, readString(len2))
                    }
                    else -> skip(len2)
                }
            }
        }
    }
    if (tag == null)
        throw EOFException()
    return Match(tag)
}

    //    public CharSequence content(final YourCustomClickableSpan.ClickableTagSpan.CallBack onTagNameClick)
    //      {
    //      if (content_ == null)
    //        {
    //        String w = messageContent.trim();
    //        if (!w.endsWith("."))
    //          w += ".";
    //        long date = messageDate;
    //        if (date != 0)  // TODO: people also go through here, so this is a little odd
    //          {
    //          w += "   (";
    //          w += Application.inst.timeAgo.getDateAgo(date);
    //
    //          if (messageName.length() != 0)
    //            w += " #" + messageTag + "(" + messageName + ")";
    //          w += ")";
    //          }
    //
    //        // TODO: maybe better with no click inside ?
    //        content_ = HashTagSpanned.create(w, new CreateSpan()
    //        {
    //        @Nullable
    //        @Override
    //        public Object createSpan(YLabel tag)
    //          {
    //          return new ClickableTagSpan(tag, null);
    //          }
    //        });  // the null was onTagNameClick
    //        }
    //      return content_;
    //      }
