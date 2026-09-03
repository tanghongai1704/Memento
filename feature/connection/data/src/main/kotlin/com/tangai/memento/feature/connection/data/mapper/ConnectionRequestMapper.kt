package com.tangai.memento.feature.connection.data.mapper

import com.tangai.memento.database.model.ConnectionRequestEntity
import com.tangai.memento.database.model.toDomain
import com.tangai.memento.database.model.toEntity
import com.tangai.memento.domain.model.ConnectionRequest

fun ConnectionRequestEntity.toDomainRequest(): ConnectionRequest {
    return this.toDomain()
}

fun ConnectionRequest.toRequestEntity(): ConnectionRequestEntity {
    return this.toEntity()
}
