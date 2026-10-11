package com.example.server

import androidx.annotation.Keep
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

@Keep
object AdbHelperServer {

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size < 2) {
            println("Usage: AdbHelperServer <port> <secret_token>")
            return
        }

        val port = args[0].toIntOrNull() ?: 19527
        val expectedToken = args[1]

        println("[NetBlockerServer] Starting privileged helper on 127.0.0.1:$port...")

        val serverSocket = try {
            ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"))
        } catch (e: Exception) {
            println("[NetBlockerServer] Error binding to port $port: ${e.message}")
            return
        }

        println("[NetBlockerServer] Helper active and listening for local app connections.")

        while (!serverSocket.isClosed) {
            try {
                val clientSocket = serverSocket.accept()
                thread(name = "ClientHandler-${clientSocket.port}") {
                    handleClient(clientSocket, expectedToken)
                }
            } catch (e: Exception) {
                if (serverSocket.isClosed) break
                println("[NetBlockerServer] Accept error: ${e.message}")
            }
        }
    }

    private fun handleClient(socket: Socket, expectedToken: String) {
        try {
            socket.soTimeout = 15000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            // Step 1: Validate Authentication Token
            val authLine = reader.readLine()
            if (authLine == null || !authLine.startsWith("TOKEN ")) {
                writer.println("ERR Unauthorized")
                socket.close()
                return
            }

            val clientToken = authLine.removePrefix("TOKEN ").trim()
            if (clientToken != expectedToken) {
                writer.println("ERR Invalid Token")
                socket.close()
                return
            }

            writer.println("AUTH_OK")

            // Step 2: Handle commands
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val cmdLine = line ?: break
                when {
                    cmdLine == "PING" -> {
                        writer.println("PONG")
                    }
                    cmdLine.startsWith("EXEC ") -> {
                        val shellCommand = cmdLine.removePrefix("EXEC ")
                        val result = runShellCommand(shellCommand)
                        writer.println("EXIT ${result.first}")
                        writer.println("BEGIN_OUTPUT")
                        writer.println(result.second)
                        writer.println("END_OUTPUT")
                    }
                    cmdLine == "QUIT" -> {
                        break
                    }
                    else -> {
                        writer.println("ERR Unknown command")
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun runShellCommand(cmd: String): Pair<Int, String> {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))
            val output = StringBuilder()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }
            while (errReader.readLine().also { line = it } != null) {
                output.appendLine("ERR: $line")
            }

            val code = process.waitFor()
            Pair(code, output.toString().trim())
        } catch (e: Exception) {
            Pair(-1, "Server exec error: ${e.message ?: "Unknown"}")
        }
    }
}
