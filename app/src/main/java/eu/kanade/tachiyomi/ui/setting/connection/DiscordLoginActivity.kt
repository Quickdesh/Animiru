// AM (DISCORD_RPC) -->
package eu.kanade.tachiyomi.ui.setting.connection

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import eu.kanade.domain.connection.service.ConnectionPreferences
import eu.kanade.tachiyomi.data.connection.ConnectionManager
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.setComposeContent
import tachiyomi.i18n.MR
import uy.kohesive.injekt.injectLazy
import java.io.File

class DiscordLoginActivity : BaseActivity() {

    private val connectionManager: ConnectionManager by injectLazy()
    private val connectionPreferences: ConnectionPreferences by injectLazy()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setComposeContent { DiscordWebview(this::login) }
    }

    private fun login(token: String) {
        connectionPreferences.connectionToken(connectionManager.discord).set(token)
        connectionPreferences.setConnectionCredentials(connectionManager.discord, "Discord", "Logged In")
        toast(MR.strings.login_success)
        applicationInfo.dataDir.let { File("$it/app_webview/").deleteRecursively() }
        finish()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DiscordWebview(
    login: (String) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )

                    settings.javaScriptEnabled = true
                    settings.databaseEnabled = true
                    settings.domStorageEnabled = true

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            if (url != null && url.endsWith("/app")) {
                                stopLoading()
                                evaluateJavascript(
                                    """
                        (function() {
                            const wreq = (webpackChunkdiscord_app.push([[''], {}, e => { m = []; for (let c in e.c) m.push(e.c[c])}]), m)
                            webpackChunkdiscord_app.pop()
                            const token = wreq.find(m => m?.exports?.default?.getToken !== void 0).exports.default.getToken();
                            return token;
                        })()
                                    """.trimIndent(),
                                ) {
                                    login(it.trim('"'))
                                }
                            }
                        }
                    }
                    loadUrl("https://discord.com/login")
                }
            },
        )
    }
}
// <-- AM (DISCORD_RPC)
