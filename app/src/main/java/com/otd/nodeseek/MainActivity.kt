package com.otd.nodeseek

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.userAgentString = settings.userAgentString + " NodeSeekApp/1.0"

        // 注入接口
        webView.addJavascriptInterface(WebAppInterface(this), "AndroidApp")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // 每次页面加载完，检查未读数
                checkUnreadCount()
            }
        }

        // 加载网页
        webView.loadUrl("https://www.nodeseek.com")
        createNotificationChannel()
    }

    private fun checkUnreadCount() {
        val jsCode = """
            (function() {
                try {
                    var links = document.querySelectorAll('a');
                    var targetSpan = null;
                    for (var i = 0; i < links.length; i++) {
                        if (links[i].getAttribute('href') && links[i].getAttribute('href').indexOf('/notifications') !== -1) {
                            var span = links[i].querySelector('span');
                            if (span) { targetSpan = span; break; }
                        }
                    }
                    if (targetSpan) {
                        var count = parseInt(targetSpan.innerText.trim());
                        if (count > 0) { AndroidApp.postNotification(count + ""); }
                    }
                } catch(e) {}
            })()
        """
        webView.evaluateJavascript(jsCode, null)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("NS_CHANNEL", "NodeSeek Messages", NotificationManager.IMPORTANCE_DEFAULT)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }
}

class WebAppInterface(private val mContext: Context) {
    @JavascriptInterface
    fun postNotification(count: String) {
        val manager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(mContext, "NS_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("NodeSeek")
            .setContentText("你有 $count 条新消息")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        manager.notify(1, builder.build())
    }
}