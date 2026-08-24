package com.bhoomibot.os.automation

import android.os.Build
import android.util.Log
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.*
import com.bhoomibot.os.MainActivity
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/**
 * BHOOMIBOT OS: PRIORITY LIVE LINK REGRESSION (V8 - ORDERED HANDSHAKE)
 */
@RunWith(AndroidJUnit4::class)
class LiveLinkAutomationTest {

    private val TAG = "BhoomiBotTest"

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private lateinit var device: UiDevice
    private val TIMEOUT = 45000L 

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun runFullRegression() {
        device.waitForIdle()
        handleSystemPermissions()

        val isRobot = Build.MODEL.contains("I2208", ignoreCase = true)

        if (isRobot) {
            runRobotRegression()
        } else {
            // Operator waits for Robot to be ready
            Log.i(TAG, "Operator waiting for Robot initialization...")
            Thread.sleep(25000) 
            runOperatorRegression()
        }
    }

    private fun handleSystemPermissions() {
        val allowPattern = Pattern.compile("(?i)Allow|While using the app|Only this time|Allow all the time")
        repeat(5) {
            val button = device.findObject(By.text(allowPattern))
            if (button != null) {
                button.click()
                Thread.sleep(1000)
            }
        }
    }

    private fun runRobotRegression() {
        Log.i(TAG, "Robot Path: Starting Live Stream")
        
        // STEP 1: Onboarding
        if (device.wait(Until.hasObject(By.textContains("SELECT")), 5000)) {
            scrollAndClick("SELECT Robot")
        }

        // STEP 2: Go Live through Settings
        safeClickDesc("Settings")
        safeClickText("LIVE LINK")
        safeClickText("SAVE & GO LIVE")
        
        // Success Condition: Robot reached Live screen
        waitForText("Live")
        Log.i(TAG, "Robot is LIVE. Waiting for Operator...")
        
        // Keep Robot alive for the Operator to join
        Thread.sleep(90000) 
    }

    private fun runOperatorRegression() {
        Log.i(TAG, "Operator Path: Connecting to Robot")
        
        // STEP 1: Onboarding
        if (device.wait(Until.hasObject(By.textContains("SELECT")), 5000)) {
            scrollAndClick("SELECT Operator")
        }

        // STEP 2: Open Live Stream through Settings (Force handshake)
        safeClickDesc("Settings")
        safeClickText("LIVE LINK")
        safeClickText("SAVE & GO LIVE")
        
        Log.i(TAG, "Waiting for video frame...")
        val found = device.wait(Until.hasObject(By.desc("Live robot feed")), TIMEOUT)
        assertNotNull("FAILED: Robot video feed never arrived on Operator screen.", found)
        
        waitForText("Live")
        Log.i(TAG, "SUCCESS: Live Link established between Robot and Operator.")
    }

    // --- HELPERS ---

    private fun safeClickText(text: String) {
        device.waitForIdle()
        val obj = device.wait(Until.findObject(By.textContains(text)), 10000)
        if (obj == null) scrollAndClick(text) else obj.click()
    }

    private fun safeClickDesc(desc: String) {
        val obj = device.wait(Until.findObject(By.desc(desc)), 10000)
        if (obj != null) obj.click()
    }

    private fun waitForText(text: String) {
        val found = device.wait(Until.hasObject(By.textContains(text)), TIMEOUT)
        assertNotNull("Timed out waiting for text: $text", found)
    }

    private fun scrollAndClick(text: String) {
        var attempts = 0
        while (attempts < 8) {
            val target = device.findObject(By.textContains(text))
            if (target != null) { target.click(); return }
            val scrollable = device.findObject(By.scrollable(true))
            if (scrollable != null) {
                try { scrollable.scroll(Direction.DOWN, 0.5f) } catch (e: Exception) {}
            } else {
                device.swipe(400, 1200, 400, 600, 30)
            }
            attempts++
            Thread.sleep(1000)
        }
        val finalTarget = device.findObject(By.textContains(text))
        assertNotNull("Failed to find '$text'", finalTarget)
        finalTarget!!.click()
    }
}
