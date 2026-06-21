package com.example.data

data class ForumSection(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String, // "satellite", "tech", "sports", "news", "forum", "trade"
    val topicsCount: Int,
    val postsCount: Int,
    val isHeader: Boolean = false
)

data class ForumTopic(
    val id: String,
    val title: String,
    val author: String,
    val authorRank: String,
    val sectionId: String,
    val sectionName: String,
    val date: String,
    val replies: Int,
    val views: Int,
    val body: String,
    val isPinned: Boolean = false,
    val tags: List<String> = emptyList(),
    val repliesList: List<TopicReply> = emptyList()
)

data class TopicReply(
    val id: String,
    val author: String,
    val authorRank: String,
    val avatarColorSeed: Int,
    val date: String,
    val content: String,
    val likes: Int = 0
)

data class DirectMessage(
    val id: String,
    val sender: String,
    val relativeTime: String,
    val lastMessage: String,
    val isUnread: Boolean,
    val avatarColorSeed: Int,
    val messages: List<ConversationMessage> = emptyList()
)

data class ConversationMessage(
    val id: String,
    val sender: String,
    val text: String,
    val relativeTime: String,
    val isMines: Boolean
)

data class InAppNotification(
    val id: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val relativeTime: String,
    val topicId: String,
    val type: String // "like", "reply", "pm"
)

data class UserProfile(
    val username: String,
    val rank: String,
    val postsCount: Int,
    val reactionPoints: Int,
    val registrationDate: String,
    val points: Int,
    val avatarColorSeed: Int
)
