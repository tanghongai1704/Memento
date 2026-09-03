package com.tangai.memento.feature.connection.data.mapper

import com.tangai.memento.database.model.ConnectionEntity
import com.tangai.memento.database.model.ConnectionMemberEntity
import com.tangai.memento.database.model.toDomain
import com.tangai.memento.database.model.toEntity
import com.tangai.memento.domain.model.Connection
import com.tangai.memento.domain.model.ConnectionMember

fun ConnectionEntity.toDomainWithMembers(members: List<ConnectionMemberEntity>): Connection {
    return this.toDomain(members.map { it.toDomain() })
}

fun Connection.toEntityWithMembers(): Pair<ConnectionEntity, List<ConnectionMemberEntity>> {
    return Pair(
        this.toEntity(),
        this.members.map { it.toEntity(this.id) }
    )
}
