package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.util.SecurityHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ScrMngr", appName)
  }

  @Test
  fun `security helper hashes and verifies pin correctly`() {
    val pin = "1234"
    val salt = SecurityHelper.generateSalt()
    val hash = SecurityHelper.hashPin(pin, salt)

    assertTrue(SecurityHelper.verifyPin("1234", hash, salt))
    assertFalse(SecurityHelper.verifyPin("9999", hash, salt))
    assertFalse(SecurityHelper.verifyPin("123", hash, salt))
  }

  @Test
  fun `app group active days check correctly handles all days`() {
    val group = com.example.data.model.AppGroupEntity(
      name = "Doomscroll",
      packageNamesCsv = "com.instagram.android,com.zhiliaoapp.musically",
      daysOfWeekCsv = "1,2,3,4,5,6,7",
      isEnabled = true
    )
    val packageList = group.getPackageList()
    assertEquals(2, packageList.size)
    assertTrue(packageList.contains("com.instagram.android"))
    assertTrue(packageList.contains("com.zhiliaoapp.musically"))
    assertTrue(group.isTodayActive())
  }

  @Test
  fun `trigger-on-limit window timer stays inactive below limit`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val app = com.example.ScreenTimeApplication.instance
    val repo = app.repository

    val entity = com.example.data.model.TrackedAppEntity(
      packageName = "com.test.app",
      appName = "Test App",
      maxOpenCount = 3,
      openWindowMinutes = 15,
      isFrequencyLimitEnabled = true,
      isLimitEnabled = true,
      addedTimestamp = System.currentTimeMillis(),
      lockUntilTimestamp = 0L,
      isLocked = false
    )

    val status = repo.evaluateAppStatus(entity)
    assertFalse(status.isFrequencyBreached)
    assertFalse(status.entity.isLocked)
    assertEquals(0L, status.usage.windowResetRemainingSeconds)
  }

  @Test
  fun `timer formatting works for hours and minutes`() {
    val secondsUnder60m = 25 * 60L + 30L // 25:30
    val minutes = secondsUnder60m / 60L
    val seconds = secondsUnder60m % 60L
    val formattedUnder60 = String.format("%02d:%02d", minutes, seconds)
    assertEquals("25:30", formattedUnder60)

    val secondsOver60m = 3600L + (14 * 60L) + 5L // 01:14:05
    val h = secondsOver60m / 3600L
    val m = (secondsOver60m % 3600L) / 60L
    val s = secondsOver60m % 60L
    val formattedOver60 = String.format("%02d:%02d:%02d", h, m, s)
    assertEquals("01:14:05", formattedOver60)
  }
}
