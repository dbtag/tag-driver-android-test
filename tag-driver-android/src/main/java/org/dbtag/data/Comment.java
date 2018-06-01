//package org.dbtag.data;
//
//import android.support.annotation.Nullable;
//import android.text.Spanned;
//
//public class Comment
//  {
//  public final long date;
//  public final String tag;
//  private @Nullable String sContent_;  private Spanned content_;
//
//  public Comment(long date, String tag, String content)
//    {
//    this.messageDate = messageDate;
//    this.tag = tag;
//    this.sContent_ = content;
//    }
//
//  public Spanned content()
//    {
//    if (content_ == null)
//      {
//     // content_ = YourCustomClickableSpan.substitute(sContent_);
//      sContent_ = null;
//      }
//    return content_;
//    }
//  }
//
//
////private static class URLSpanConverter implements RichTextUtils.SpanConverter<URLSpan, YourCustomClickableSpan>
////{
////@Override
////public YourCustomClickableSpan convert(URLSpan span)
////{
////return new YourCustomClickableSpan(span.getURL());
////}
////}
