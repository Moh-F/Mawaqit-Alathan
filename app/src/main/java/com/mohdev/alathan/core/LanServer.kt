package com.mohdev.alathan.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.Executors

/**
 * LAN-only control + log page. OFF by default. Reachable only from private IP ranges and only with the
 * one-time token shown on the TV. Plain HTTP on the home network; never expose it to the internet.
 */
object LanServer {
    private var server: ServerSocket? = null
    private val pool = Executors.newCachedThreadPool { r -> Thread(r, "lan").apply { isDaemon = true } }
    private val main = Handler(Looper.getMainLooper())
    private var token = ""
    private var page = ""

    /** Shown in the settings panel while running. */
    var url by mutableStateOf("")
        private set

    val running get() = server != null

    fun start(ctx: Context) {
        if (server != null) return
        runCatching {
            val rnd = SecureRandom()
            token = (1..20).map { "abcdefghijklmnopqrstuvwxyz0123456789"[rnd.nextInt(36)] }.joinToString("")
            page = ctx.assets.open("panel.html").bufferedReader().use { it.readText() }
            val s = ServerSocket(0)
            server = s
            url = "http://${localIp() ?: "?"}:${s.localPort}/?token=$token"
            pool.execute { acceptLoop(s) }
            AppLog.i("lan", "started on port ${s.localPort}")
        }.onFailure { AppLog.e("lan", "start failed", it); stop() }
    }

    fun stop() {
        runCatching { server?.close() }
        server = null
        url = ""
        AppLog.i("lan", "stopped")
    }

    private fun localIp(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }?.hostAddress
    }.getOrNull()

    private fun acceptLoop(s: ServerSocket) {
        while (!s.isClosed) {
            val c = runCatching { s.accept() }.getOrNull() ?: break
            pool.execute { runCatching { handle(c) }.onFailure { AppLog.d("lan", "conn: ${it.message}") }; runCatching { c.close() } }
        }
    }

    private fun readLine(i: BufferedInputStream): String? {
        val b = ByteArrayOutputStream()
        while (true) {
            val c = i.read()
            if (c < 0) return if (b.size() == 0) null else b.toString("ISO-8859-1")
            if (c == '\n'.code) return b.toString("ISO-8859-1").trimEnd('\r')
            if (b.size() > 8192) throw IllegalStateException("line too long")
            b.write(c)
        }
    }

    private fun send(o: OutputStream, code: Int, type: String, body: ByteArray, extra: String = "") {
        val head = "HTTP/1.1 $code OK\r\nContent-Type: $type\r\nContent-Length: ${body.size}\r\n" +
            "Cache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\n$extra" + "Connection: close\r\n\r\n"
        o.write(head.toByteArray(Charsets.ISO_8859_1)); o.write(body); o.flush()
    }

    private fun tokenOk(given: String?): Boolean =
        given != null && MessageDigest.isEqual(given.toByteArray(), token.toByteArray())

    private fun handle(sock: Socket) {
        sock.soTimeout = 15_000
        val addr: InetAddress = sock.inetAddress
        val out = sock.getOutputStream()
        if (!(addr.isLoopbackAddress || addr.isSiteLocalAddress)) {
            send(out, 403, "text/plain", "Forbidden".toByteArray()); return
        }
        val input = BufferedInputStream(sock.getInputStream())
        val reqLine = readLine(input) ?: return
        val headers = HashMap<String, String>()
        while (true) {
            val l = readLine(input) ?: break
            if (l.isEmpty()) break
            val i = l.indexOf(':')
            if (i > 0) headers[l.substring(0, i).trim().lowercase()] = l.substring(i + 1).trim()
        }
        val parts = reqLine.split(" ")
        if (parts.size < 2) return
        val method = parts[0]
        val target = parts[1]
        val path = target.substringBefore('?')
        val query = target.substringAfter('?', "").split("&").filter { it.contains("=") }
            .associate { it.substringBefore('=') to java.net.URLDecoder.decode(it.substringAfter('='), "UTF-8") }
        val len = headers["content-length"]?.toIntOrNull() ?: 0
        if (len > 65_536) { send(out, 413, "text/plain", "Too large".toByteArray()); return }
        val body = ByteArray(len)
        var read = 0
        while (read < len) { val n = input.read(body, read, len - read); if (n < 0) break; read += n }

        if (!tokenOk(query["token"] ?: headers["x-token"])) {
            send(out, 403, "text/plain", "Forbidden".toByteArray()); return
        }
        val json = "application/json; charset=utf-8"
        when {
            method == "GET" && path == "/" -> send(out, 200, "text/html; charset=utf-8", page.toByteArray())
            method == "GET" && path == "/api/schema" -> send(out, 200, json, Settings.schemaJson().toByteArray())
            method == "GET" && path == "/api/settings" -> send(out, 200, json, Settings.toJson().toString().toByteArray())
            method == "POST" && path == "/api/settings" -> {
                val o = runCatching { JSONObject(String(body, Charsets.UTF_8)) }.getOrNull()
                if (o == null) { send(out, 400, "text/plain", "Bad JSON".toByteArray()); return }
                var applied = 0
                val latch = java.util.concurrent.CountDownLatch(1)
                main.post { applied = Settings.applyJson(o); latch.countDown() }
                latch.await(3, java.util.concurrent.TimeUnit.SECONDS)
                send(out, 200, json, JSONObject().put("applied", applied).toString().toByteArray())
            }
            method == "GET" && path == "/api/log" ->
                send(out, 200, json, org.json.JSONArray(AppLog.recent()).toString().toByteArray())
            method == "GET" && path == "/download" ->
                send(out, 200, "text/plain; charset=utf-8", AppLog.readAll().toByteArray(), "Content-Disposition: attachment; filename=\"alathan.log\"\r\n")
            method == "GET" && path == "/stream" -> stream(sock, out)
            else -> send(out, 404, "text/plain", "Not found".toByteArray())
        }
    }

    private fun stream(sock: Socket, out: OutputStream) {
        sock.soTimeout = 0
        out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\nCache-Control: no-cache\r\nConnection: keep-alive\r\n\r\n").toByteArray())
        val q = java.util.concurrent.LinkedBlockingQueue<String>()
        val l: (String) -> Unit = { q.offer(it) }
        AppLog.recent().forEach { q.offer(it) }
        AppLog.addListener(l)
        try {
            while (!sock.isClosed && server != null) {
                val line = q.poll(15, java.util.concurrent.TimeUnit.SECONDS)
                out.write((if (line != null) "data: $line\n\n" else ": keep-alive\n\n").toByteArray())
                out.flush()
            }
        } finally {
            AppLog.removeListener(l)
        }
    }
}
