package ru.netology.nmedia.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class RemoteKey(
    @PrimaryKey val postId: Long?,
    val prevKey: Int?,
    val nextKey: Int?
)