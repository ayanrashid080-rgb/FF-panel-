package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

interface IpAddressResolver {
    suspend fun fetchDeviceIp(): String
}

class NetworkIpAddressResolver : IpAddressResolver {

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    override suspend fun fetchDeviceIp(): String = withContext(Dispatchers.IO) {
        val publicIp = runCatching {
            val request = Request.Builder()
                .url("https://api.ipify.org")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()?.trim().orEmpty()
                    if (isValidIpLiteral(body)) body else null
                } else {
                    null
                }
            }
        }.getOrNull()

        publicIp ?: resolveLocalInterfaceIp() ?: "Unavailable"
    }

    private fun resolveLocalInterfaceIp(): String? {
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            interfaces
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList().asSequence() }
                .filterIsInstance<Inet4Address>()
                .mapNotNull { it.hostAddress }
                .firstOrNull { !it.startsWith("127.") }
        }.getOrNull()
    }

    private fun isValidIpLiteral(candidate: String): Boolean {
        if (candidate.isBlank() || candidate.length > 45) return false
        return candidate.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' || it == '.' || it == ':' }
    }
}
