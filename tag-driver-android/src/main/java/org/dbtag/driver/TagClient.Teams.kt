package org.dbtag.driver

import org.dbtag.data.Filter
import org.dbtag.data.MessagesData
import org.dbtag.data.Tag
import org.dbtag.data.parseTagsOnly
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine

data class U2(val tag: Tag)

/**
 * A team, including its [members] and [managers] and what it is [following]
 */
data class Team(val team: Tag, val members: List<U2>, val managers: List<U2>,
                val following: List<Tag>, val followingWithNotifications: List<Tag>)


/**
 * Gets all [Team]s in alphabetical team-name order
 */
suspend fun UserQueue.getTeams() = suspendCoroutine<List<Team>> { cont-> getTeams(cont) }


enum class FollowMethod { Unknown, NoFollow, Follow, FollowWithNotifications }

/**
 * Gets all [Team]s
 */
fun UserQueue.getTeams(cont: Continuation<List<Team>>) {
    select(Filter(listOf(Tag("team")), listOf(Tag("sys.n"))), 0, Filter.empty, 100000, 0, false, Parts.Content, { it.content }, null,
            object : Continuation<TAndMs<MessagesData<String>>> {
                override val context = EmptyCoroutineContext
                override fun resumeWithException(exception: Throwable) = cont.resumeWithException(exception)
                override fun resume(value: TAndMs<MessagesData<String>>) {
                    val usersCache = mutableMapOf<String, U2>()  // TODO: should pull in the pre-existing users
                    data class ParsedTeam(val team: Tag, var deleted: Boolean,
                                          val members: MutableSet<U2>, val managers: MutableSet<U2>,
                                          val followingValues: MutableMap<Tag, Boolean>)
                    val teams = mutableMapOf<Tag, ParsedTeam>()

                    for (content in value.t.messages) {
                        val tags = content.parseTagsOnly()
                        val teamTag = tags.firstOrNull({ it.topic == "team" }) ?: continue

                        // Get the [ParsedTeam] to work on
                        with (teams[teamTag] ?: ParsedTeam(teamTag, false, mutableSetOf(), mutableSetOf(), mutableMapOf()).apply { teams[teamTag] = this }) {
                            if (teamTag.value == 0.0)
                                deleted = true
                            else {
                                var seenManager = false
                                var follow = FollowMethod.Unknown
                                for (tag in tags) when {
                                    tag.tag == "sys.manager" -> seenManager = true
                                    tag.tag == "sys.f" -> follow = when(tag.value) {
                                        0.0 -> FollowMethod.NoFollow
                                        2.0 -> FollowMethod.FollowWithNotifications
                                        else -> FollowMethod.Follow
                                    }
                                    tag.topic == "user" -> {
                                        val userCode = tag.code
                                        val u2 = usersCache[userCode] ?: U2(Tag(tag.tag, tag.name)).apply { usersCache.put(userCode, this) }
                                        if (seenManager) {
                                            if (tag.value == 0.0)
                                                managers.remove(u2)
                                            else
                                                managers.add(u2)
                                        } else {
                                            if (tag.value == 0.0)
                                                members.remove(u2)
                                            else
                                                members.add(u2)
                                        }
                                    }
                                    else -> {
                                        if (follow != FollowMethod.Unknown) {
                                            followingValues.remove(tag)
                                            val follow2 = if (tag.hasValue) when (tag.value) {
                                                    0.0 -> FollowMethod.NoFollow
                                                    2.0 -> FollowMethod.FollowWithNotifications
                                                    else -> FollowMethod.Follow
                                            } else
                                                follow
                                            if (follow2 != FollowMethod.NoFollow)
                                                followingValues[tag] = (follow2 == FollowMethod.FollowWithNotifications)
                                        }

                                    }
                                }
                            }
                        }
                    }
                    val newTeams = mutableListOf<Team>()
                    for ((_, team) in teams) with(team) {
                        if (!deleted)
                            newTeams.add(Team(
                                    this.team,
                                    members.toMutableList().apply { sortBy({ it.tag }) },
                                    managers.toMutableList().apply { sortBy({ it.tag }) },
                                    followingValues.filter { !it.value }.mapTo(mutableListOf(), { it.key }).apply { sort() },
                                    followingValues.filter { it.value }.mapTo(mutableListOf(), { it.key }).apply { sort() }))
                    }
                    newTeams.sortBy({ it.team })  // effectively sorts alphabetically by team name
                    cont.resume(newTeams)
                }

            })
}



//private code1Summaries class ParsedTeam(val team: YLabel, val deleted: Boolean,
//                              val members: MutableSet<U2>, val managers: MutableSet<U2>)
//
//private fun parseTeam(tags: Iterable<YLabel>, usersCache: MutableMap<String, U2>) : ParsedTeam? {
//    var team : YLabel? = null
//    var deleted = false
//    val members = mutableSetOf<U2>()
//    val managers = mutableSetOf<U2>()
//    var seenManager = false
//    for (tag in tags) {
//        if (tag.topic == "team") {
//            if (team == null) {
//                if (tag.value == 0.0)
//                    deleted = true
//                team = tag
//            }
//        } else if (tag.tag == "sys.manager")
//            seenManager = true
//        else if (tag.topic == "user") {
//            val userCode = tag.code
//            var u2 = usersCache[userCode]
//            if (u2 == null) {
//                u2 = U2(tag)
//                usersCache.put(userCode, u2)
//            }
//            if (seenManager) {
//                managers.add(u2)
//            } else {
//                members.add(u2)
//            }
//        }
//    }
//    if (team == null)
//        return null
//    return ParsedTeam(team, deleted, members, managers)
//}
//
///**
// * Gets all [Team]s in alphabetical team-name order
// */
//suspend fun UserQueue.getTeams() = suspendCoroutine<List<Team>> { cont-> getTeams(cont) }
//
///**
// * Gets all [Team]s
// */
//fun UserQueue.getTeams(cont: Continuation<List<Team>>) {
//    select(Filter().require("team").exclude("sys.f"), 0, Filter.empty,  1000, 0, true, Parts.Content, { it.messageContent}, null,
//            object : Continuation<MessagesData<String>> {
//                override val context: CoroutineContext = EmptyCoroutineContext
//                override fun resumeWithException(exception: Throwable) = cont.resumeWithException(exception)
//                override fun resume(value: MessagesData<String>) {
//                    // "async-await pattern for Android activities"
//                    // https://stackoverflow.com/a/43151714
//                    async(CommonPool) {
//                        try {
//                            val usersCache = mutableMapOf<String, U2>()
//                            val seen = mutableListOf<YLabel>()
//                            val teams = mutableListOf<Team>()
//                            val changes = mutableMapOf<YLabel, MutableList<List<YLabel>>>()
//                            for (messageContent in value.messages) {
//                                val tags = messageContent.parseTagsOnly()
//                                var teamTag: YLabel? = null
//                                var userHadValue = false
//                                for (tag in tags) {
//                                    when (tag.topic) {
//                                        "team" -> if (teamTag == null) teamTag = tag
//                                        "user" -> if (tag.hasValue) userHadValue = true
//                                    }
//                                }
//                                if (teamTag != null && userHadValue) {
//                                    var history = changes[teamTag]
//                                    if (history == null) {
//                                        history = mutableListOf<List<YLabel>>()
//                                        changes.set(teamTag, history)
//                                    }
//                                    history.add(tags)
//                                    continue  // need to look at older code1Summaries for this team to complete the picture
//                                }
//
//                                val team0 = parseTeam(tags, usersCache) ?: continue
//                                if (seen.contains(team0.team))
//                                    continue  // ignore older ones
//
//                                seen.add(team0.team)
//                                val members = team0.members
//                                val managers = team0.managers
//
//                                // Apply any changes after that
//                                val history = changes[team0.team]
//                                if (history != null) {
//                                    for (i in history.size - 1 downTo 0) {
//                                        val tags = history[i]
//                                        var seenManager = false
//                                        for (tag in tags) {
//                                            if (tag.tag == "sys.manager")
//                                                seenManager = true
//                                            else if (tag.topic == "user" && tag.hasValue) {
//                                                val add = (tag.value == 1.0)
//                                                val userCode = tag.code
//                                                var u2 = usersCache[userCode]
//                                                if (u2 == null) {
//                                                    u2 = U2(tag)
//                                                    usersCache.put(userCode, u2)
//                                                }
//                                                if (seenManager) {
//                                                    if (add)
//                                                        managers.add(u2)
//                                                    else
//                                                        managers.remove(u2)
//                                                } else {
//                                                    if (add)
//                                                        members.add(u2)
//                                                    else
//                                                        members.remove(u2)
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//
//                                // See what this team is following
//                                if (!team0.deleted) {
//                                    val x88 = lastValue(Filter().require(team0.team.tag), 0, "sys.f", null, "team", false, 0, { tag, _, _ -> tag }).second
//                                    val following = mutableListOf<YLabel>()
//                                    val followingWithNotifications = mutableListOf<YLabel>()
//                                    for (tnv in x88) if (tnv.hasValue) {
//                                        val tag = YLabel(tnv.tag, tnv.name)
//                                        when (tnv.value) {
//                                            1.0 -> following.add(tag)
//                                            2.0 -> followingWithNotifications.add(tag)
//                                        }
//                                    }
//                                    following.sort()
//                                    followingWithNotifications.sort()
//                                    val membersList = members.toMutableList()
//                                    val managersList = managers.toMutableList()
//                                    membersList.sortBy { it.tag }
//                                    managersList.sortBy { it.tag }
//                                    teams.add(Team(team0.team, membersList, managersList, following, followingWithNotifications))
//                                }
//                            }
//                            teams.sortBy({ it.team })  // effectively sorts alphabetically by team name
//                            cont.resume(teams)
//                        } catch (ex: Exception) { cont.resumeWithException(ex) }
//                    }
//                }
//
//            })
//}
