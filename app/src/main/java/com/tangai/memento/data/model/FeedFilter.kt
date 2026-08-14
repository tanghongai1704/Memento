package com.tangai.memento.data.model

sealed interface FeedFilter {
    data object All : FeedFilter
    data class User(val userId: String) : FeedFilter
    // FeedFilter.Group will be added later
}
