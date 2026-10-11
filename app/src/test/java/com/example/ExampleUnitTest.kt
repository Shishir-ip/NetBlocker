package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppItem
import com.example.engine.BlockingStrategy
import com.example.engine.NativeFirewallEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun testRootIptablesBlockCommand() {
        val app = AppItem(
            packageName = "com.test.app1",
            appName = "Test App 1",
            uid = 10123,
            isSystemApp = false,
            hasInternetPermission = true,
            isBlocked = true,
            blockWifi = true,
            blockMobile = true,
            blockLan = true
        )

        val cmd = engine.buildBlockCommand(app, BlockingStrategy.RootIptables)
        assertTrue(cmd.contains("iptables -N NETBLOCKER_OUT"))
        assertTrue(cmd.contains("--uid-owner 10123 -j DROP"))

        val queryCmd = engine.buildQueryCommand(app, BlockingStrategy.RootIptables)
        assertTrue(queryCmd.contains("iptables -C NETBLOCKER_OUT -m owner --uid-owner 10123 -j DROP"))
        assertTrue(engine.parseQueryState(app, BlockingStrategy.RootIptables, exitCode = 0, output = ""))
        assertFalse(engine.parseQueryState(app, BlockingStrategy.RootIptables, exitCode = 1, output = ""))
    }

    @Test
    fun testPackageNetworkingCommand() {
        val app = AppItem(
            packageName = "com.test.browser",
            appName = "Browser",
            uid = 10099,
            isSystemApp = false,
            hasInternetPermission = true,
            isBlocked = true
        )

        val blockCmd = engine.buildBlockCommand(app, BlockingStrategy.PackageNetworking())
        assertEquals("cmd connectivity set-package-networking-enabled com.test.browser false", blockCmd)

        val unblockCmd = engine.buildUnblockCommand(app, BlockingStrategy.PackageNetworking())
        assertEquals("cmd connectivity set-package-networking-enabled com.test.browser true", unblockCmd)

        val queryCmd = engine.buildQueryCommand(app, BlockingStrategy.PackageNetworking())
        assertEquals("cmd connectivity get-package-networking-enabled com.test.browser", queryCmd)

        assertTrue(engine.parseQueryState(app, BlockingStrategy.PackageNetworking(), 0, "Package networking enabled: false"))
        assertFalse(engine.parseQueryState(app, BlockingStrategy.PackageNetworking(), 0, "Package networking enabled: true"))
    }

    @Test
    fun testUidFirewallRuleCommand() {
        val app = AppItem(
            packageName = "com.test.app2",
            appName = "App 2",
            uid = 10555,
            isSystemApp = false,
            hasInternetPermission = true,
            isBlocked = true
        )

        val blockCmd = engine.buildBlockCommand(app, BlockingStrategy.UidFirewallRule("restricted"))
        assertEquals("cmd connectivity set-uid-firewall-rule restricted 10555 deny", blockCmd)

        val unblockCmd = engine.buildUnblockCommand(app, BlockingStrategy.UidFirewallRule("restricted"))
        assertEquals("cmd connectivity set-uid-firewall-rule restricted 10555 allow", unblockCmd)
    }

    @Test
    fun testBackgroundBlacklistFallbackCommand() {
        val app = AppItem(
            packageName = "com.test.app3",
            appName = "App 3",
            uid = 10777,
            isSystemApp = false,
            hasInternetPermission = true,
            isBlocked = true
        )

        val blockCmd = engine.buildBlockCommand(app, BlockingStrategy.BackgroundBlacklist)
        assertEquals("cmd netpolicy add restrict-background-blacklist 10777", blockCmd)

        val unblockCmd = engine.buildUnblockCommand(app, BlockingStrategy.BackgroundBlacklist)
        assertEquals("cmd netpolicy remove restrict-background-blacklist 10777", unblockCmd)
    }
}
