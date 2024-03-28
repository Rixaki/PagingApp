package ru.netology.nmedia.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.RemoteKeyDao
import ru.netology.nmedia.entity.AttachmentConverter
import ru.netology.nmedia.entity.AttachmentEmbeddable
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.RemoteKey

@Database(
    entities = [PostEntity::class, RemoteKey::class],
    version = 1,
exportSchema = false
)
@TypeConverters(AttachmentConverter::class)
abstract class AppDb : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun keysDao(): RemoteKeyDao
}