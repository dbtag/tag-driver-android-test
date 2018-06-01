package org.dbtag.data


class UpdateReply(val apkVersionCode: Int, val apkBytes: ByteArray?)

enum class SignInResult(val value: Byte) {
    Ok(0),
    NoUser(1),
    NoCredentials(2),
    BadPassword(3),
    DatabaseNotFound(4);

    companion object {
        fun from(findValue: Byte) = SignInResult.values().first { it.value == findValue }
    }

}

data class SignInResults(val result: SignInResult, val token: String)

class CreateUserResults(val result: Byte) {
    companion object {
        const val RESULT_DATABASE_NOT_FOUND = 0
        const val RESULT_USER_ALREADY_EXISTS = 1
        const val RESULT_OK = 2
    }
}


//////////////////////////////////////

data class Attachment(val name: String, val bytes: ByteArray)

data class Code1Summaries(val code1Tag: Tag, val summaries: List<CodeOfsSummary>)

data class SuperSummary(val tag: Tag, val unJoined: SimpleSummary?, val joined: SimpleSummary?)

data class SimpleSummary(val count: Count, val topics: List<TopicData>, val tagsWithValues: List<TV>)

data class TV(val tag: Tag, val count: Int, val sum: Double, val values: List<Double>, val unit: String)

data class TopicData(val tag: Tag, val totalCount: Int, val durationSum: Long, val plus: Boolean, val topTags: List<Tag>)

data class CodesResult(val superSummaries : List<SuperSummary>)

data class CodesSimpleResult(val codes: List<Tag>)

data class QVals<T>(val count: Int, val sum: Double, val min: T, val max: T, val q1: T, val q2: T, val q3: T)

data class Count(val posts: Int, val tagged: Int, val latitudeMin: Double, val latitudeMax: Double, val longitudeMin: Double, val longitudeMax: Double,
                 val dateQ: QVals<Long>, val durationQ: QVals<Long>, val valueQ: QVals<Double>, val unit: String)

data class CrossTab(val topic1: String, val topic2: String, val tagValue: String?,
                    val code1Summaries: List<Code1Summaries>, val code2Tags: List<Tag>)

data class ServerDatabases(val host: String, val databases: List<String>)

data class Match(val tag: Tag)

data class MatchTopic(val topic: String, val matches: List<Match>)

data class MatchTopicsResult(val exactSingleMatch: Boolean, val matchTopics: List<MatchTopic>,
                             val topicDirects: List<Tag>)

data class MessageIdCommentIndex(val msgId: String, val comment: Int, val index: Int)

data class MessagesCountAndTopicSummaries(val messagesCount: Int, val topicSummaries: List<TopicSummary>)

data class MessageIdCommentIndexMaxSize(val mid: Int, val comment: Int, val index: Int, val maxSize: Int)

data class MessagesData<out T>(val messages: List<T>, val serverFreshness: Long)

//@Parcelize
data class NameAndSize(val name: String, val size: Int) // : Parcelable


class PairData(val is0: List<Int>, val tags: List<String>, val names: List<String>)


data class TagAndNamePair(val item1: Tag, val item2: Tag)

class TagProfileUpdated(tag: String, originalName: String, val profileUpdated: Long)
    : Tag(tag, originalName)

data class TagCodeResult(val tags: List<Tag>)

open class TagNameDate(tag: String, originalName: String, val date: Long)
    : Tag(tag, originalName)

class TagNameFollow(tag: String, originalName: String, val follow: String)
    : Tag(tag, originalName)

class TagNameMention(tag: String, originalName: String, date: Long, val fromName: String)
    : TagNameDate(tag, originalName, date) {

    val fromNameIfDifferent get() = if (fromName.equals(originalName, ignoreCase = true)) "" else fromName
}

class TagNameDateContentAttachments(tag: String, originalName: String, date: Long,
                                    val content: String, val attachments: List<NameAndSize>) : TagNameDate(tag, originalName, date)

data class TagNameMentions(val allAlphabetically: Boolean, val mentions: List<TagNameMention>)

// code1Summaries class TimeSlotSummariesResult(val variousSummaries: List<VariousSummary>)
data class TimeSlotSummariesResult(val stuff: List<List<Tag>>)

class TopicAndMaxUsePerMessage(val topic: Tag, val maxUsePerMessage: Int)

data class TopicsReply<T>(val serverTime:Long, val topics: List<T>)

data class ValueAndUnit(val value: Double, val unit: String)