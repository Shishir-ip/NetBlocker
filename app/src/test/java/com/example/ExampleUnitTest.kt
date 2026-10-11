package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BlockedRule
import com.example.engine.NativeFirewallEngine
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {

    private lateinit var engine: NativeFirewallEngine

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        engine = NativeFirewallEngine(context)
    }

    @Test
    fun testIptablesScriptGeneration() {
        val rules = listOf(
            BlockedRule(
                packageName = "com.test.app1",
                appName = "Test App 1",
                uid = 10123,
                isBlocked = true,
                blockWifi = true,
                blockMobile = false,
                blockLan = false
            ),
            BlockedRule(
                packageName = "com.test.app2",
                appName = "Test App 2",
                uid = 10124,
                isBlocked = true,
                blockWifi = false,
                blockMobile = true,
                blockLan = true
            )
        )

        val script = engine.buildIptablesScript(rules)

        assertTrue(script.contains("iptables -N NETBLOCKER_OUT"))
        assertTrue(script.contains("--uid-owner 10123 -o wlan+ -j DROP"))
        assertTrue(script.contains("--uid-owner 10124 -o rmnet+ -j DROP"))
        assertTrue(script.contains("--uid-owner 10124 -o eth+ -j DROP"))
    }

    @Test
    fun testAdbShellScriptGeneration() {
        val rules = listOf(
            BlockedRule(
                packageName = "com.test.browser",
                appName = "Browser",
                uid = 10099,
                isBlocked = true,
                blockWifi = true,
                blockMobile = true,
                blockLan = true
            )
        )

        val adbScript = engine.buildAdbShellScript(rules)

        assertTrue(adbScript.contains("cmd netpolicy set restrict-background-rules 10099 1"))
        assertTrue(adbScript.contains("cmd appops set com.test.browser RUN_IN_BACKGROUND ignore"))
    }

    @Test
    fun testWindowsBatchScriptGeneration() {
        val rules = listOf(
            BlockedRule(
                packageName = "com.test.chat",
                appName = "Chat",
                uid = 10050,
                isBlocked = true,
                blockWifi = true,
                blockMobile = true,
                blockLan = true
            )
        )

        val batScript = engine.buildWindowsBatScript(rules)

        assertTrue(batScript.contains("@echo off"))
        assertTrue(batScript.contains("adb shell cmd netpolicy set restrict-background-rules 10050 1"))
    }
}
