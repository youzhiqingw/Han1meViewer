package com.wuwei.han1meviewer.ui.model

import com.wuwei.han1meviewer.logic.model.SearchOption

data class SearchScopeSection(
    val titleRes: Int,
    val options: List<SearchOption>,
    val spanCount: Int = 3,
)