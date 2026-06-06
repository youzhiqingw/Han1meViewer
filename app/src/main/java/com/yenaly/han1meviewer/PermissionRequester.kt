package com.wuwei.han1meviewer

interface PermissionRequester {
    fun requestStoragePermission(
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit
    )
}