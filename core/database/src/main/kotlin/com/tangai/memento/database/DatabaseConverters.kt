package com.tangai.memento.database

import androidx.room.TypeConverter
import com.tangai.memento.domain.model.ConnectionStatus
import com.tangai.memento.domain.model.ConnectionType
import com.tangai.memento.domain.model.LayoutType
import com.tangai.memento.domain.model.LocalSyncStatus
import com.tangai.memento.domain.model.MediaType
import com.tangai.memento.domain.model.MemberRole
import com.tangai.memento.domain.model.MemberStatus
import com.tangai.memento.domain.model.PostStatus
import com.tangai.memento.domain.model.PostType

class DatabaseConverters {
    @TypeConverter
    fun connectionTypeToString(value: ConnectionType): String = value.name

    @TypeConverter
    fun stringToConnectionType(value: String): ConnectionType = ConnectionType.valueOf(value)

    @TypeConverter
    fun connectionStatusToString(value: ConnectionStatus): String = value.name

    @TypeConverter
    fun stringToConnectionStatus(value: String): ConnectionStatus = when (value) {
        // Compatibility for local databases created before PENDING was removed.
        "PENDING" -> ConnectionStatus.CLOSED
        else -> ConnectionStatus.valueOf(value)
    }

    @TypeConverter
    fun memberRoleToString(value: MemberRole): String = value.name

    @TypeConverter
    fun stringToMemberRole(value: String): MemberRole = MemberRole.valueOf(value)

    @TypeConverter
    fun memberStatusToString(value: MemberStatus): String = value.name

    @TypeConverter
    fun stringToMemberStatus(value: String): MemberStatus = MemberStatus.valueOf(value)

    @TypeConverter
    fun postTypeToString(value: PostType): String = value.name

    @TypeConverter
    fun stringToPostType(value: String): PostType = PostType.valueOf(value)

    @TypeConverter
    fun layoutTypeToString(value: LayoutType): String = value.name

    @TypeConverter
    fun stringToLayoutType(value: String): LayoutType = LayoutType.valueOf(value)

    @TypeConverter
    fun postStatusToString(value: PostStatus): String = value.name

    @TypeConverter
    fun stringToPostStatus(value: String): PostStatus = PostStatus.valueOf(value)

    @TypeConverter
    fun localSyncStatusToString(value: LocalSyncStatus): String = value.name

    @TypeConverter
    fun stringToLocalSyncStatus(value: String): LocalSyncStatus = LocalSyncStatus.valueOf(value)

    @TypeConverter
    fun mediaTypeToString(value: MediaType): String = value.name

    @TypeConverter
    fun stringToMediaType(value: String): MediaType = MediaType.valueOf(value)
}
