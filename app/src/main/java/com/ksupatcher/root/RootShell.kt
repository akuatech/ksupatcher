package com.ksupatcher.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter

/**
 * Thin wrapper around a persistent su process for structured root command execution.
 * Special thanks / Credit to JoshuaDoes for the initial stream-based implementation.
 */
object RootShell {

    private var suProcess: Process? = null
    private var suStdin: BufferedWriter? = null
    private var suStdout: BufferedReader? = null
    private var suStderr: BufferedReader? = null

    @Synchronized
    private fun runRootBlock(input: String): Triple<Boolean, String, String> {
        if (suProcess == null) {
            try {
                suProcess = Runtime.getRuntime().exec("su")
                suStdin = suProcess!!.outputStream.bufferedWriter()
                suStdout = suProcess!!.inputStream.bufferedReader()
                suStderr = suProcess!!.errorStream.bufferedReader()
            } catch (e: Exception) {
                e.printStackTrace()
                return Triple(false, "", e.toString())
            }
        }

        if (input.isBlank()) {
            return Triple(false, "", "")
        }

        val marker = "__CMD_DONE_${System.nanoTime()}__"

        try {
            // execute command
            suStdin!!.write(input)
            suStdin!!.newLine()
            suStdin!!.write("echo $marker")
            suStdin!!.newLine()
            suStdin!!.flush()

            val outBuf = StringBuilder()
            val errBuf = StringBuilder()

            while (true) {
                val line = suStdout!!.readLine() ?: break
                if (line == marker) break
                outBuf.appendLine(line)
            }

            while (suStderr!!.ready()) {
                errBuf.appendLine(suStderr!!.readLine())
            }

            return Triple(true, outBuf.toString().trimEnd(), errBuf.toString().trimEnd())
        } catch (e: Exception) {
            e.printStackTrace()
            suProcess?.destroy()
            suProcess = null
            return Triple(false, "", e.toString())
        }
    }

    fun isRooted(): Boolean {
        val (success, out, _) = runRootBlock("id")
        return success && out.contains("uid=0(root)")
    }

    suspend fun run(vararg cmds: String): String = withContext(Dispatchers.IO) {
        val cmdStr = cmds.joinToString("\n")
        val (success, out, err) = runRootBlock(cmdStr)
        if (!success) {
            error("Shell execution failed. Error: $err")
        }
        out
    }

    suspend fun getProp(key: String): String? = withContext(Dispatchers.IO) {
        run("getprop $key").trim().takeIf { it.isNotEmpty() }
    }
}