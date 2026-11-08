package com.driveatlas

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

/** HTTPS public map/source viewer only. No JS bridge, file access, location, credentials or native navigation. */
class PublicWebActivity:ComponentActivity(){
 private var web:WebView?=null
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  val url=intent.getStringExtra("public_url")?:run{finish();return}
  if(!safePublicUrl(url)){finish();return}
  val layout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(245,241,231))}
  val title=TextView(this).apply{text=getString(R.string.web_title);textSize=20f;setTextColor(Color.rgb(32,61,56));setPadding(24,14,24,4)}
  val status=TextView(this).apply{text=getString(R.string.web_pending,android.net.Uri.parse(url).host);textSize=14f;setTextColor(Color.rgb(75,95,86));setPadding(24,4,24,10)}
  layout.addView(title);layout.addView(status)
  val row=LinearLayout(this)
  row.addView(Button(this).apply{text=getString(R.string.web_return);setOnClickListener{finish()}},LinearLayout.LayoutParams(0,64,1f))
  row.addView(Button(this).apply{text=getString(R.string.web_reload);setOnClickListener{web?.reload()}},LinearLayout.LayoutParams(220,64))
  layout.addView(row)
  val view=WebView(this);web=view
  @Suppress("SetJavaScriptEnabled")
  view.settings.javaScriptEnabled=true
  view.settings.domStorageEnabled=true
  view.settings.allowFileAccess=false;view.settings.allowContentAccess=false
  view.settings.setGeolocationEnabled(false)
  view.settings.mixedContentMode=android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
  view.settings.javaScriptCanOpenWindowsAutomatically=false
  view.webViewClient=object:WebViewClient(){
   override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean{
    val allowed=safePublicUrl(request.url.toString());if(!allowed)status.text=getString(R.string.web_blocked);return !allowed
   }
   override fun onPageFinished(view:WebView,url:String){status.text=getString(R.string.web_loaded,android.net.Uri.parse(url).host)}
   override fun onReceivedError(view:WebView,request:WebResourceRequest,error:WebResourceError){if(request.isForMainFrame)status.text=getString(R.string.web_error)}
  }
  layout.addView(view,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));setContentView(layout);view.loadUrl(url)
  onBackPressedDispatcher.addCallback(this,object:OnBackPressedCallback(true){override fun handleOnBackPressed(){finish()}})
 }
 override fun onDestroy(){web?.destroy();web=null;super.onDestroy()}
}
internal fun safePublicUrl(value:String):Boolean=runCatching{
 val u=java.net.URI(value);val host=u.host?:return@runCatching false
 u.scheme=="https" && u.userInfo==null && (host=="map.kakao.com"||host.endsWith(".kakao.com")||host=="www.openstreetmap.org"||host=="commons.wikimedia.org"||host=="creativecommons.org"||host=="operations.osmfoundation.org"||host=="korean.visitkorea.or.kr")
}.getOrDefault(false)
