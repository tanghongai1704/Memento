package com.tangai.memento.feature.home.domain

sealed interface FeedFilter {
    data object All : FeedFilter
    data class Connection(val connectionId: String) : FeedFilter
}
