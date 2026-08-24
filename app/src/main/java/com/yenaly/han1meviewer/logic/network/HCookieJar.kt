package com.wuwei.han1meviewer.logic.network

import com.wuwei.han1meviewer.Preferences
import com.wuwei.han1meviewer.util.CookieString
import com.wuwei.han1meviewer.util.toLoginCookieList
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.Collections

/**
 * 用於管理 Cookie。
 *
 * #issue-71: 我竟然栽倒在 Cookie 管理上好幾年了！你去看我以前的管理方式，
 * 是完全錯誤的，竟然還能維持應用正常運行，太離譜了！怪不得切換簡體繁體一直不起作用！
 *
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2024/03/13 013 15:20
 */
class HCookieJar : CookieJar {

    companion object {
        private const val MAX_HOST_COUNT = 200

        @JvmStatic
        val cookieMap: MutableMap<String, MutableList<Cookie>> =
            Collections.synchronizedMap(
                object : LinkedHashMap<String, MutableList<Cookie>>(16, 0.75f, true) {
                    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MutableList<Cookie>>): Boolean {
                        return size > MAX_HOST_COUNT
                    }
                }
            )
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val cookies = mutableListOf<Cookie>()
        cookieMap[host]?.let { cookies.addAll(it) }

        cookies.addAll(Preferences.loginCookieStateFlow.value.toLoginCookieList(host))
        cookies.addAll(Preferences.cloudFlareCookieStateFlow.value.toLoginCookieList(host))

        return cookies
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val now = System.currentTimeMillis()

        // 过滤掉已过期 Cookie（expiresAt == Long.MIN_VALUE 为会话 Cookie，永不过期）
        val validCookies = cookies.filter { it.expiresAt == Long.MIN_VALUE || it.expiresAt >= now }

        // 合并登录 Cookie（不覆盖响应 Cookie）
        val merged = mutableListOf<Cookie>()
        merged.addAll(validCookies)
        merged += Preferences.loginCookieStateFlow.value.toLoginCookieList(host)

        cookieMap[host] = merged
    }
}