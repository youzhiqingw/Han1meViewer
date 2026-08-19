package com.wuwei.han1meviewer.ui.viewmodel.mylist

import android.app.Application
import com.wuwei.han1meviewer.Preferences
import com.wuwei.han1meviewer.logic.NetworkRepo
import com.wuwei.han1meviewer.logic.model.HanimeInfo
import com.wuwei.han1meviewer.logic.model.MyListItems
import com.wuwei.han1meviewer.logic.model.MyListType
import com.wuwei.han1meviewer.logic.state.PageLoadingState
import com.wuwei.han1meviewer.logic.state.WebsiteState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class FavSubViewModel(application: Application) : MyListSubViewModel(application) {

    var favVideoPage = 1
    private var csrfToken: String? = null

    val favVideoStateFlow: StateFlow<PageLoadingState<MyListItems<HanimeInfo>>> = itemsStateFlow.asStateFlow()
    val favVideoFlow: StateFlow<List<HanimeInfo>> = itemsFlow.asStateFlow()

    fun getMyFavVideoItems(userId: String, page: Int) {
        loadItems(MyListType.FAV_VIDEO, userId, page) { csrfToken = it.csrfToken }
    }

    private val _deleteMyFavVideoFlow = MutableSharedFlow<WebsiteState<Boolean>>()
    val deleteMyFavVideoFlow = _deleteMyFavVideoFlow.asSharedFlow()

    fun deleteMyFavVideo(videoCode: String) {
        deleteItem(
            deleteCall = {
                NetworkRepo.addToMyFavVideo(
                    videoCode = videoCode,
                    likeStatus = true,
                    currentUserId = Preferences.savedUserId,
                    token = csrfToken,
                )
            },
            emitTo = _deleteMyFavVideoFlow,
            identifier = videoCode,
            mapState = { it },
        )
    }

    override fun clearMyListItems() {
        super.clearMyListItems()
        favVideoPage = 1
    }
}
