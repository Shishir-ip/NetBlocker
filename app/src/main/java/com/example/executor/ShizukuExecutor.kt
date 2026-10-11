package com.example.executor

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.example.BuildConfig
import com.example.shizuku.IShizukuService
import com.example.shizuku.ShizukuUserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

enum class ShizukuState {
    NOT_INSTALLED,
    NOT_RUNNING,
    PERMISSION_NOT_GRANTED,
    READY
}

class ShizukuExecutor(private val context: Context) : PrivilegedExecutor {

    override val name: String = "Shizuku"

    private var userService: IShizukuService? = null
    private var isBinding = false

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(context.packageName, ShizukuUserService::class.java.name))
            .daemon(false)
            .processNameSuffix("netblocker_service")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            userService = IShizukuService.Stub.asInterface(service)
            isBinding = false
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            userService = null
            isBinding = false
        }
    }

    fun getShizukuState(): ShizukuState {
        val isInstalled = try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

        if (!isInstalled) return ShizukuState.NOT_INSTALLED

        val isRunning = try {
            Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }
        if (!isRunning) return ShizukuState.NOT_RUNNING

        val hasPermission = try {
            if (Shizuku.isPreV11()) false
            else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }

        return if (hasPermission) ShizukuState.READY else ShizukuState.PERMISSION_NOT_GRANTED
    }

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        val state = getShizukuState()
        if (state != ShizukuState.READY) return@withContext false

        if (userService != null && userService?.asBinder()?.isBinderAlive == true) {
            return@withContext true
        }

        ensureBound()
    }

    suspend fun ensureBound(): Boolean = withContext(Dispatchers.IO) {
        if (userService != null && userService?.asBinder()?.isBinderAlive == true) {
            return@withContext true
        }

        if (getShizukuState() != ShizukuState.READY) {
            return@withContext false
        }

        return@withContext withTimeoutOrNull(3000) {
            suspendCancellableCoroutine { continuation ->
                val tempConnection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        userService = IShizukuService.Stub.asInterface(service)
                        isBinding = false
                        if (continuation.isActive) {
                            continuation.resume(true)
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        userService = null
                        isBinding = false
                        if (continuation.isActive) {
                            continuation.resume(false)
                        }
                    }
                }

                try {
                    isBinding = true
                    Shizuku.bindUserService(userServiceArgs, tempConnection)
                } catch (e: Exception) {
                    isBinding = false
                    if (continuation.isActive) continuation.resume(false)
                }
            }
        } ?: false
    }

    fun requestPermission(requestCode: Int = 1001) {
        try {
            if (Shizuku.pingBinder() && !Shizuku.isPreV11()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (_: Exception) {}
    }

    override suspend fun exec(cmd: String): ExecResult = withContext(Dispatchers.IO) {
        if (!ensureBound()) {
            return@withContext ExecResult(
                exitCode = -1,
                output = "Shizuku UserService is not bound or Shizuku permission not granted."
            )
        }

        try {
            val response = userService?.execute(cmd)
            if (response == null) {
                return@withContext ExecResult(-1, "Null response from Shizuku service")
            }

            val lines = response.lines()
            val exitCode = lines.firstOrNull()?.toIntOrNull() ?: -1
            val output = lines.drop(1).joinToString("\n").trim()

            ExecResult(exitCode = exitCode, output = output)
        } catch (e: Exception) {
            ExecResult(-1, "Shizuku IPC error: ${e.message ?: "Unknown"}")
        }
    }
}
