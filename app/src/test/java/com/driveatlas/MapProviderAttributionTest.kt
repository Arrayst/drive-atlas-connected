package com.driveatlas

import org.junit.Assert.*
import org.junit.Test

class MapProviderAttributionTest {
 @Test fun contributorNameAndRequiredProviderLinksRemainComplete() {
  assertEquals(listOf("OpenFreeMap", "© OpenMapTiles", "© OpenStreetMap contributors"), mapAttributionLinks.map { it.label })
  assertEquals("https://www.openstreetmap.org/copyright", mapAttributionLinks.last().url)
  assertTrue(mapAttributionLinks.all { safePublicUrl(it.url) })
 }
 @Test fun wideLayoutReservesMeasuredTextPlusPaddingInsteadOfTruncatingIt() {
  assertEquals(251, mapAttributionItemWidth(1682, 225, 26, 64))
  assertEquals(476, mapAttributionItemWidth(1682, 450, 26, 64))
 }
 @Test fun narrowOrLargeTextItemsUseAvailableWidthAndCanGrowVertically() {
  assertEquals(240, mapAttributionItemWidth(240, 450, 26, 64))
  assertEquals(320, mapAttributionItemWidth(320, 675, 26, 64))
  assertEquals(64, mapAttributionItemWidth(320, 20, 26, 64))
 }
}
