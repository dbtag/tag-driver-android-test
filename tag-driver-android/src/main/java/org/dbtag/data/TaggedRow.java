package dbtag.data;//package org.dbtag.data;
//
//import android.callback.ResultCallback;
//import android.support.annotation.NonNull;
//import android.support.annotation.Nullable;
//
//import org.dbtag.data.Filter;
//import org.dbtag.data.MessageTagsNotInFilter;
//import org.dbtag.data.MessagesData;
//import org.dbtag.data.ResourceTag;
//import org.dbtag.driver.Parts;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class TaggedRow
//  {
//  public final @NonNull ResourceTag[] usersFollowing;  // which users are following the given tag (perhaps we are among them)
//
//
//  public TaggedRow(@NonNull ResourceTag[] usersFollowing)
//    {
//    this.usersFollowing = usersFollowing;
//    }
//
//  public static void createAsync(final Parts db, final @NonNull String tag, @NonNull final ResultCallback<TaggedRow> callback)
//    {
//    db.findAsync(new Filter().require("sys/follow", tag), 0, 100000000, true,
//      Parts.PARTS_TAGS_NOT_IN_FILTER, MessageTagsNotInFilter.construct,
//      new ResultCallback<messagesData>() { @Override public void onResult(@Nullable Exception ex, @Nullable messagesData result)
//        {
//        if (result == null)
//          callback.onResult(ex, null);
//        else
//          {
//          Object[] messages = result.messages;
//          final List<ResourceTag> usersFollowing = new ArrayList<>();
//          for (Object message : messages) for (ResourceTag tn : ((MessageTagsNotInFilter) message).tagsNotInFilter)
//           if (tn.topicEquals("user")) { usersFollowing.add(tn); break; }
//          callback.onResult(null, new TaggedRow(usersFollowing.toArray(new ResourceTag[usersFollowing.size()])));
//          }
//        }});
//    }
//  }
