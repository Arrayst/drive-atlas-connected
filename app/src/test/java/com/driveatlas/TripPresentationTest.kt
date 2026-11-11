package com.driveatlas
import org.junit.Assert.assertEquals
import org.junit.Test
class TripPresentationTest {
 @Test fun shortDuration(){assertEquals("0분",tripDuration(0));assertEquals("59분",tripDuration(59))}
 @Test fun exactHours(){assertEquals("1시간",tripDuration(60));assertEquals("12시간",tripDuration(720))}
 @Test fun mixedDuration(){assertEquals("2시간 25분",tripDuration(145));assertEquals("8시간 2분",tripDuration(482))}
}
