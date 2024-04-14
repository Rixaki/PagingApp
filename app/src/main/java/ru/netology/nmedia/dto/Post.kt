package ru.netology.nmedia.dto

import ru.netology.nmedia.enumeration.AttachmentType

sealed interface FeedItem {
    val id: Long
}

data class Post(
    override val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int = 0,
    val attachment: Attachment? = null,
    val ownedByMe: Boolean = false,
) : FeedItem

data class Attachment(
    val url: String,
    val type: AttachmentType,
)

data class Ad(
    override val id: Long,
    val image: String
) : FeedItem

data class TimeHeader(
    override val id: Long,
    val type: TimeType,
    val title: String = "SOME TIME AGO"
) : FeedItem

enum class TimeType() {
    TODAY,
    YESTERDAY,
    LAST_WEEK,
}

