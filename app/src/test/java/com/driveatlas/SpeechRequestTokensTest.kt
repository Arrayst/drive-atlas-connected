package com.driveatlas
import org.junit.Assert.*
import org.junit.Test
class SpeechRequestTokensTest {
 @Test fun repeatedRequestGetsUniqueIdsAndOldCallbacksCannotFinishNew(){val t=SpeechRequestTokens();val old=t.begin();val oldPre=t.prerollId;val next=t.begin();assertNotEquals(old,next);assertFalse(t.matches(old));assertFalse(t.matches(oldPre));assertTrue(t.isPreview(next));assertTrue(t.matches(t.prerollId))}
 @Test fun prerollCompletionIsNotPreviewCompletion(){val t=SpeechRequestTokens();t.begin();assertTrue(t.matches(t.prerollId));assertFalse(t.isPreview(t.prerollId))}
 @Test fun stopAndDisposeInvalidateEveryPendingCallback(){val t=SpeechRequestTokens();val id=t.begin();val pre=t.prerollId;t.cancel();assertFalse(t.matches(id));assertFalse(t.matches(pre));assertFalse(t.matches(null));assertNull(t.prerollId)}
 @Test fun networkOrUninstalledVoicesNeverPassOfflineGate(){assertTrue(isOfflineKoreanVoice("ko",false,false));assertFalse(isOfflineKoreanVoice("ko",true,false));assertFalse(isOfflineKoreanVoice("ko",false,true));assertFalse(isOfflineKoreanVoice("en",false,false))}
}
