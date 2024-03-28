package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import retrofit2.HttpException
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.entity.RemoteKey
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.error.ApiError
import java.io.IOException

//https://www.youtube.com/watch?v=EpcIjXgg3e4&list=PLbg6Nd4MUHGICER-qVejk-0knCCWLbqp5&index=11
@ExperimentalPagingApi
class PostRemoteMediator(
    private val apiService: ApiService,
    private val appDb: AppDb
) : RemoteMediator<Int, Post>(){
    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Post>
    ): MediatorResult {
        val page = when (val pageKeyData = getKeyPageData(loadType, state)) {
            //getKeyPageData - MediatorSuccess or Int?
            is MediatorResult.Success -> {
                return pageKeyData
            }
            else -> {
                pageKeyData as Int
            }
        }

        try {
            val response = apiService.getAll()
            /*
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
             */

            val body = response.body() ?: throw ApiError(
                response.code(),
                response.message()
            )
            val postsEnt = body.map {
                PostEntity.fromDto(it)
            }
            appDb.postDao().insert(postsEnt)

            val isEndOfList = body.isEmpty()
            appDb.withTransaction {//coroutine block
                if (loadType == LoadType.REFRESH) {
                    appDb.postDao().deleteAll()
                    appDb.keysDao().deleteAll()
                }
                val prevKey = if (page == STARTING_PAGE_INDEX) null else page - 1
                val nextKey = if (isEndOfList) null else page + 1
                val keys = body.map {
                    RemoteKey(it.id, prevKey = prevKey, nextKey = nextKey)
                }
                appDb.keysDao().insertAll(keys)
                appDb.postDao().insert(postsEnt)
            }
            return MediatorResult.Success(endOfPaginationReached = isEndOfList)
        } catch (exception: IOException) {
            return MediatorResult.Error(exception)
        } catch (exception: HttpException) {
            return MediatorResult.Error(exception)
        }
    }

    private suspend fun getKeyPageData(
        loadType: LoadType,
        state: PagingState<Int, Post>
    ): Any {
        return when (loadType) {
            LoadType.REFRESH -> {
                val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                remoteKeys?.nextKey?.minus(1) ?: STARTING_PAGE_INDEX
            }
            LoadType.APPEND -> {
                val remoteKeys = getLastRemoteKey(state)
                val nextKey = remoteKeys?.nextKey
                return nextKey ?: MediatorResult.Success(endOfPaginationReached = false)
            }
            LoadType.PREPEND -> {
                val remoteKeys = getFirstRemoteKey(state)
                val prevKey = remoteKeys?.prevKey ?: return MediatorResult.Success(
                    endOfPaginationReached = false
                )
                prevKey
            }
        }
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(
        state: PagingState<Int, Post>): RemoteKey? = state.anchorPosition?.let { position ->
        state.closestItemToPosition(position)?.id?.let { postId ->
            appDb.keysDao().remoteKeysPostId(postId)
        }
    }

    private suspend fun getLastRemoteKey(state: PagingState<Int, Post>):
            RemoteKey? {
        return state.pages
            .lastOrNull { it.data.isNotEmpty() }
            ?.data?.lastOrNull()
            ?.let { post -> appDb.keysDao().remoteKeysPostId(post.id) }
    }

    private suspend fun getFirstRemoteKey(state: PagingState<Int, Post>):
            RemoteKey? = state.pages
            .firstOrNull { it.data.isNotEmpty() }
            ?.data?.firstOrNull()
            ?.let { post -> appDb.keysDao().remoteKeysPostId(post.id) }
}