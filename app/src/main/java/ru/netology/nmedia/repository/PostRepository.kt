package ru.netology.nmedia.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity

interface PostRepository {
    val data: Flow<PagingData<FeedItem>>
    //val data: Flow<PagingData<Post>>
    //val dbData: Flow<PagingData<Post>>
    //val mediatorData: Flow<PagingData<Post>>

    //suspend fun deletePosts()

    suspend fun getAll()
    fun getNewerCount(id: Long): Flow<Int>
    suspend fun save(post: Post, upload: MediaUpload?)
    suspend fun removeById(id: Long)
    suspend fun likeById(id: Long)
    suspend fun upload(upload: MediaUpload): Media
}
