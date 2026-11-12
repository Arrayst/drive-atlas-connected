package com.driveatlas

import android.content.res.ColorStateList
import androidx.compose.ui.graphics.toArgb
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.graphics.Bitmap
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
 private var safetyConnection:PleosConnections?=null
 private var gateOpen=false
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  val url=intent.getStringExtra("public_url")?:run{finish();return}
  if(!safePublicUrl(url)){finish();return}
  fun dp(value:Int)=(value*resources.displayMetrics.density+.5f).toInt()
  val layout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Paper.toArgb())}
  val title=TextView(this).apply{text=getString(R.string.web_title);textSize=20f;setTextColor(Ink.toArgb());setPadding(dp(24),dp(16),dp(24),dp(4))}
  val status=TextView(this).apply{text=getString(R.string.web_pending,android.net.Uri.parse(url).host);textSize=18f;setTextColor(Muted.toArgb());setPadding(dp(24),dp(4),dp(24),dp(12))}
  layout.addView(title);layout.addView(status)
  val row=LinearLayout(this).apply{setPadding(dp(16),0,dp(16),dp(12))}
  fun style(button:Button)=button.apply{
   textSize=18f;isAllCaps=false;setTextColor(Paper.toArgb())
   backgroundTintList=ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled),intArrayOf()),intArrayOf(Muted.toArgb(),Ink.toArgb()))
  }
  row.addView(style(Button(this)).apply{text=getString(R.string.web_return);setOnClickListener{finish()}},LinearLayout.LayoutParams(0,dp(76),1f).apply{marginEnd=dp(12)})
  val reload=style(Button(this)).apply{text=getString(R.string.web_reload);isEnabled=false;setOnClickListener{if(gateOpen)web?.reload()}}
  row.addView(reload,LinearLayout.LayoutParams(dp(220),dp(76)))
  layout.addView(row)
  val view=WebView(this);web=view;view.visibility=android.view.View.INVISIBLE
  @Suppress("SetJavaScriptEnabled")
  view.settings.javaScriptEnabled=true
  view.settings.domStorageEnabled=true
  // Kakao's mobile page tries to install its phone app and may omit waypoint/map UI.
  // Use its public desktop site inside this parked-only viewer instead.
  if(java.net.URI(url).host=="map.kakao.com"){
   view.settings.userAgentString="Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
   view.settings.useWideViewPort=true;view.settings.loadWithOverviewMode=true
  }
  view.settings.allowFileAccess=false;view.settings.allowContentAccess=false
  view.settings.setGeolocationEnabled(false)
  view.settings.mixedContentMode=android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
  view.settings.javaScriptCanOpenWindowsAutomatically=false
  val loadState=PublicPageLoadState()
  view.webViewClient=object:WebViewClient(){
   override fun onPageStarted(view:WebView,url:String,favicon:Bitmap?){loadState.start();if(gateOpen)status.text=getString(R.string.web_pending,android.net.Uri.parse(url).host)}
   override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean{
    val allowed=gateOpen&&safePublicUrl(request.url.toString());if(!allowed){loadState.fail();status.text=getString(R.string.web_blocked)};return !allowed
   }
   override fun onPageFinished(view:WebView,url:String){if(gateOpen&&loadState.canShowLoaded)status.text=getString(R.string.web_loaded,android.net.Uri.parse(url).host)}
   override fun onReceivedError(view:WebView,request:WebResourceRequest,error:WebResourceError){if(gateOpen&&request.isForMainFrame){loadState.fail();status.text=getString(R.string.web_error)}}
   override fun onReceivedHttpError(view:WebView,request:WebResourceRequest,errorResponse:WebResourceResponse){if(gateOpen&&request.isForMainFrame){loadState.fail();status.text=getString(R.string.web_error)}}
  }
  layout.addView(view,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));setContentView(layout)
  status.text=getString(R.string.web_lock_pending)
  safetyConnection=PleosConnections(this,vehicleOnly=true){state->
   val allowed=state.safety==Safety.PARKED
   if(allowed!=gateOpen){
    gateOpen=allowed;reload.isEnabled=allowed
    view.visibility=if(allowed)android.view.View.VISIBLE else android.view.View.INVISIBLE
    if(allowed){view.onResume();view.loadUrl(url)}else{view.stopLoading();view.onPause()}
   }
   if(!allowed)status.text=getString(R.string.web_lock_unknown)
  }
  onBackPressedDispatcher.addCallback(this,object:OnBackPressedCallback(true){override fun handleOnBackPressed(){finish()}})
 }
 override fun onStart(){super.onStart();safetyConnection?.start()}
 override fun onStop(){safetyConnection?.stop();super.onStop()}
 override fun onDestroy(){safetyConnection?.destroy();safetyConnection=null;web?.destroy();web=null;super.onDestroy()}
}
internal fun safePublicUrl(value:String):Boolean=runCatching{
 val u=java.net.URI(value);val host=u.host?:return@runCatching false
 u.scheme=="https" && u.userInfo==null && (host=="map.kakao.com"||host.endsWith(".kakao.com")||host=="www.openstreetmap.org"||host=="openfreemap.org"||host=="www.openmaptiles.org"||host=="openmaptiles.org"||host=="commons.wikimedia.org"||host=="creativecommons.org"||host=="operations.osmfoundation.org"||host=="korean.visitkorea.or.kr"||host=="access.visitkorea.or.kr"||host=="searoad.gtdc.or.kr"||host=="opentour.gtdc.or.kr"||host=="gtdc.or.kr"||host=="project-osrm.org"||host=="routing.openstreetmap.de")
}.getOrDefault(false)

/** A finished callback also follows failed loads; never turn that into success. */
internal class PublicPageLoadState {
 private var failed=false
 val canShowLoaded:Boolean get()=!failed
 fun start(){failed=false}
 fun fail(){failed=true}
}
