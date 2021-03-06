package com.android.example.feature.login

import android.content.Intent
import android.net.Uri
import com.android.example.data.api.AccessTokenParameter
import com.android.example.data.api.GithubAuthService
import com.android.example.data.repository.AccessTokenRepository
import com.android.example.envvar.EnvVar
import com.android.example.model.AccessToken
import timber.log.Timber
import javax.inject.Inject

class LoginHelper @Inject constructor(
        private val githubAuthService: GithubAuthService,
        private val accessTokenRepository: AccessTokenRepository,
        private val envVar: EnvVar
) {
    fun generateAuthorizationUrl(): Uri =
        Uri.Builder().apply {
            scheme("https")
            authority("github.com")
            appendPath("login")
            appendPath("oauth")
            appendPath("authorize")
            appendQueryParameter("client_id", envVar.GITHUB_CLIENT_ID)
        }.build()

    suspend fun handleAuthRedirect(intent: Intent): Boolean {
        val uri = intent.data ?: return false
        if (!uri.toString().startsWith("dgbs://login")) return false
        val tempCode = uri.getQueryParameter("code") ?: return false

        Timber.i("code: $tempCode")

        val param = AccessTokenParameter(
                clientId = envVar.GITHUB_CLIENT_ID,
                clientSecret = envVar.GITHUB_CLIENT_SECRET,
                code = tempCode
        )

        return runCatching {
            val resp = githubAuthService.createAccessToken(param)
            accessTokenRepository.save(AccessToken(resp.accessToken))
        }.onFailure {
            Timber.e(it, "createAccessToken failed!")
        }.isSuccess
    }
}
