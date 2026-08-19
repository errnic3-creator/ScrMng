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
}
