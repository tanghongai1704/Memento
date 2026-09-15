package com.tangai.memento.domain.model

fun Connection.displayLabel(connectedUsersById: Map<String, User>): String {
    val connectedUser = members.asSequence()
        .filter { it.status == MemberStatus.ACTIVE }
        .mapNotNull { connectedUsersById[it.userId] }
        .firstOrNull()
    return name?.takeIf { it.isNotBlank() }
        ?: connectedUser?.let { "${it.displayName} (@${it.username})" }
        ?: if (type == ConnectionType.GROUP) "Group" else "Direct connection"
}
