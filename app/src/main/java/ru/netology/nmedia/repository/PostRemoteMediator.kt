package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.paging.LoadType
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostRemoteKeyEntity
import ru.netology.nmedia.error.ApiError
import java.lang.Long.max
import java.lang.Long.min

const val STARTING_PAGE_INDEX = 1

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator (
    private val service: ApiService,
    private val postDao: PostDao,
    private val postRemoteKeyDao: PostRemoteKeyDao,
    private val appDb: AppDb,
) : RemoteMediator<Int, PostEntity>() {
    override suspend fun initialize(): InitializeAction =
        if (postDao.isEmpty()) {
            println("LAUNCH_INITIAL_REFRESH")
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            println("SKIP_INITIAL_REFRESH")
            InitializeAction.SKIP_INITIAL_REFRESH
        }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        try {
            val response = when (loadType) {
                LoadType.REFRESH -> {
                    if (postDao.isEmpty()) {
                        service.getLatest(state.config.pageSize)
                    } else {
                        val lastId = postRemoteKeyDao.max()
                            ?: return MediatorResult.Success(false)
                        service.getAfter(
                            lastId,
                            state.config.pageSize
                        )
                    }
                }
                LoadType.PREPEND -> {
                    return MediatorResult.Success(endOfPaginationReached = true)
                }
                LoadType.APPEND -> {
                    val firstId = postRemoteKeyDao.min()
                        ?: return MediatorResult.Success(false)
                    service.getBefore(
                        firstId,
                        state.config.pageSize
                    )
                }
            }

            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(
                response.code(),
                response.message(),
            )
            if (body.isEmpty()) {
                println("empty response body")
                return MediatorResult.Success(endOfPaginationReached = true)
            } else {
                println(
                    "response body id-s range: ${body.firstOrNull()?.id} and" +
                            " ${body.lastOrNull()?.id}"
                )
            }

            appDb.withTransaction {//all changes postdao+keysDao or prev state
                when (loadType) {
                    LoadType.REFRESH -> {
                        println("trans refresh")
                        //postDao.clear()//lecture version
                        postRemoteKeyDao.insert(
                            listOf(
                                PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.KeyType.AFTER,
                                    max(body.first().id, postDao.max() ?: body.first().id)
                                ),
                                PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.KeyType.BEFORE,
                                    min(body.last().id, postDao.min() ?: body.last().id)
                                )
                            )
                        )
                    }

                    //UNREACHABLE WITHOUT AUTO_PREPEND
                    LoadType.PREPEND -> {
                        println("trans prepend")
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                PostRemoteKeyEntity.KeyType.AFTER,
                                body.first().id
                            )
                        )
                    }

                    LoadType.APPEND -> {
                        println("trans append")
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                PostRemoteKeyEntity.KeyType.BEFORE,
                                body.last().id//maybe NoSuchElementException:List is empty.
                            )
                        )
                    }
                }

                postDao.insert(body.map(PostEntity::fromDto))
            }

            //return MediatorResult.Success(endOfPaginationReached = body.isEmpty())
            //reachable with only non-empty body
            return MediatorResult.Success(endOfPaginationReached = false)
        } catch (e: Exception) {
            println("ERROR MESSAGE: ${e.message}")
            return MediatorResult.Error(e)
        }
    }
}