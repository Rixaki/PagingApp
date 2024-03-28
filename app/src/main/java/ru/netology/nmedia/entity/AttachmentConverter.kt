package ru.netology.nmedia.entity

import androidx.room.TypeConverter
import ru.netology.nmedia.dto.Attachment

class AttachmentConverter {
    @TypeConverter
    fun fromEntAttachment(attEnt: AttachmentEmbeddable): Attachment {
        return Attachment(
            url = attEnt.url,
            type = attEnt.type
        )
    }

    @TypeConverter
    fun toEntAttachment(att: Attachment): AttachmentEmbeddable {
        return AttachmentEmbeddable(
            url = att.url,
            type = att.type
        )
    }
}