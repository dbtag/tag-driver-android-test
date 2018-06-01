package org.dbtag.data

import org.dbtag.driver.Parts

/**
 * A message that also includes a resource, got as the item1 YLabel in From or Content
 * that is not in the toFilter.
 */
class ResourceMessage(args: MessageConstructArgs) : M0Impl(args) {
    val content = args.content

    //String topic0 = "chemical";

    // TODO: what if several chemicals in this message ?  we should have it appear on
    // several positions, and so we'd need to bodge up several unique recycler id's
    fun getResource(topic: String) = content.parseTagsOnly().firstOrNull { it.topic == topic } ?: Tag.empty

    init {

        // Get the tag that is in the given topic - this will be used for the resource



        //
        //    List<YLabel> tagsNotInFilter = null;  // TODO: might be better if the server did this for us
        //    String[] requiredTags = args.toFilter.requiredTags;
        //    if (requiredTags != null)
        //      {
        //      for (YLabel tn : MessageParseTags.parseTags(from, contentAsSpanned))
        //        {
        //        boolean required = false;
        //        for (String requiredTag : requiredTags)
        //          if (tn.tag.equals(requiredTag))
        //            {
        //            required = true;
        //            break;
        //            }
        //        if (!required)
        //          {
        //          if (tagsNotInFilter == null)
        //            tagsNotInFilter = new ArrayList<>();
        //          tagsNotInFilter.add(tn);
        //          }
        //        }
        //      }
        //    this.tagsNotInFilter = (tagsNotInFilter == null ? null : tagsNotInFilter.toArray(new YLabel[tagsNotInFilter.size()]));
    }

    companion object {
        const val parts = Parts.Mid or Parts.Date or Parts.Until or
                          Parts.Updated or Parts.Content
        val construct = fun(args: MessageConstructArgs) { ResourceMessage(args) }
    }
}


//  /**
//   * A message that also includes a topic, got by examining the tags-not-in-toFilter
//   * provided.
//   */
//  private static class ResourceMessage extends M0Impl
//    {
//    private final YLabel[] tagsNotInFilter;
//    private @Nullable YLabel lastTagAndName_;
//    public ResourceMessage(MessageConstructArgs args)
//      {
//      super(args);
//      tagsNotInFilter = args.tagsNotInFilter.toArray(new YLabel[args.tagsNotInFilter.size()]);
//      }
//    public @Nullable YLabel getFirstTagAndName(@NonNull String topic)
//      {
//      if (lastTagAndName_ == null || !lastTagAndName_.topic.equals(topic))
//        {
//        lastTagAndName_ = null;;
//        for (YLabel tn : tagsNotInFilter)
//          if (tn.topic.equals(topic))
//            {
//            lastTagAndName_ = tn;
//            break;
//            }
//        }
//      return lastTagAndName_;
//      }
//
//    public static final MessageConstructArgs.Construct construct = new MessageConstructArgs.Construct() { @NonNull
//    @Override public Object construct(@NonNull MessageConstructArgs args)
//      {
//      return new ResourceMessage(args);
//      }};
//    }
