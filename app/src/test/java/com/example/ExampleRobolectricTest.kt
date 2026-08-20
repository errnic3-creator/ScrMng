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
}
