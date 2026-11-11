package com.driveatlas

/** Used only on the UI thread. Late callbacks from replaced speech cannot release new audio focus. */
internal class SpeechRequestTokens {
 private var generation=0L
 private var active:Long?=null
 val prerollId:String? get()=active?.let{"sim-preroll-$it"}
 fun begin():String {active=++generation;return "sim-preview-$generation"}
 fun isPreview(id:String?)=active?.let{id=="sim-preview-$it"}?:false
 fun matches(id:String?)=active?.let{id=="sim-preview-$it"||id=="sim-preroll-$it"}?:false
 fun cancel(){active=null}
}
internal fun isOfflineKoreanVoice(language:String,networkRequired:Boolean,notInstalled:Boolean)=language=="ko"&&!networkRequired&&!notInstalled
