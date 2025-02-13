package com.flipperdevices.archive.search.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flipperdevices.archive.api.SearchApi
import com.flipperdevices.archive.search.api.IS_EXIT_ON_OPEN_KEY
import com.flipperdevices.archive.search.model.SearchState
import com.flipperdevices.bridge.dao.api.delegates.KeyParser
import com.flipperdevices.bridge.dao.api.delegates.key.UtilsKeyApi
import com.flipperdevices.bridge.dao.api.model.FlipperKeyPath
import com.flipperdevices.bridge.synchronization.api.SynchronizationApi
import com.flipperdevices.bridge.synchronization.api.SynchronizationState
import com.github.terrakok.cicerone.Router
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import tangle.inject.TangleParam
import tangle.viewmodel.VMInject

class SearchViewModel @VMInject constructor(
    @TangleParam(IS_EXIT_ON_OPEN_KEY)
    private val exitOnOpen: Boolean,
    private val utilsKeyApi: UtilsKeyApi,
    private val keyParser: KeyParser,
    private val synchronizationApi: SynchronizationApi
) : ViewModel() {
    private val queryFlow = MutableStateFlow("")
    private val searchState = MutableStateFlow<SearchState>(SearchState.Loading)
    init {
        queryFlow.mapLatest { query ->
            searchState.emit(SearchState.Loading)
            utilsKeyApi.search(query)
                .map { keys -> keys.map { keyParser.parseKey(it) to it } }
                .collect {
                    searchState.emit(SearchState.Loaded(it.toImmutableList()))
                }
        }.launchIn(viewModelScope)
    }

    fun getState(): StateFlow<SearchState> = searchState

    fun getSynchronizationState(): StateFlow<SynchronizationState> =
        synchronizationApi.getSynchronizationState()

    fun onChangeText(text: String) {
        viewModelScope.launch {
            queryFlow.emit(text)
        }
    }

    fun openKeyScreen(router: Router, keyPath: FlipperKeyPath) {
        router.sendResult(SearchApi.SEARCH_RESULT_KEY, keyPath)
        if (exitOnOpen) {
            router.exit()
        }
    }
}
