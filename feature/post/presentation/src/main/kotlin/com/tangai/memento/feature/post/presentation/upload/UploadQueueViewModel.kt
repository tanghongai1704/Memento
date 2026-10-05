package com.tangai.memento.feature.post.presentation.upload

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class UploadQueueViewModel @Inject constructor(
    private val queue: PostUploadQueue
) : ViewModel() {
    val items: StateFlow<List<PostUploadItem>> = queue.items

    init {
        queue.start()
    }

    fun retry(id: String) = queue.retry(id)
    fun retryAll() = queue.retryAll()
    fun discard(id: String) = queue.discard(id)
}
