package com.shenjj.edu.voice

import com.shenjj.edu.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * AOQ 建连凭证（对应 token 接口响应的字段）。
 *
 * 凭证是**一次性**的：每次建连都要重新取，不能复用或缓存。
 */
data class AoqCredential(
    val sid: String,
    val token: String,
    val certFingerprint: String,
    val workspaceIdHash: String,
    val endpoints: List<AoqEndpoint>,
)

/** 接入点（响应里可返回多个 route_index，SDK 自行择优/回退） */
data class AoqEndpoint(
    val routeIndex: Int,
    val endpoint: String,
    val port: Int,
    val tcpPort: Int,
)

sealed interface TokenResult {
    data class Ok(val credential: AoqCredential) : TokenResult
    data class Failed(val reason: String) : TokenResult
}

/**
 * 换取 AOQ 建连凭证。
 *
 * `POST https://{WorkspaceId}.{region}.maas.aliyuncs.com/api/v1/webrtc/realtime?model={model}`
 * 头部：`Authorization: Bearer <AOQ_API_KEY>`、`x-dashscope-rtc-transport: moq`、
 * `Content-Type: application/json`。
 *
 * API Key 与 WorkspaceId 只存在于 local.properties（已 gitignore），经 BuildConfig 注入，
 * 不进版本库。**正式上线时应改为由自有后端签发凭证**，客户端不再持有 API Key。
 */
class AoqTokenClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
) {

    private val url: String =
        "https://${BuildConfig.AOQ_WORKSPACE_ID}.${BuildConfig.AOQ_REGION}" +
            ".maas.aliyuncs.com/api/v1/webrtc/realtime?model=${BuildConfig.AOQ_MODEL}"

    suspend fun fetch(): TokenResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.AOQ_API_KEY
        if (apiKey.isBlank() || BuildConfig.AOQ_WORKSPACE_ID.isBlank()) {
            return@withContext TokenResult.Failed("未配置 AOQ 凭证（local.properties 缺 AOQ_API_KEY / AOQ_WORKSPACE_ID）")
        }
        val request = Request.Builder()
            .url(url)
            .post("{}".toRequestBody(JSON_MEDIA_TYPE))
            .addHeader("Authorization", "Bearer $apiKey")
            // moq 传输是 AOQ 的前提，缺这个头会退化成 WebRTC 通道
            .addHeader("x-dashscope-rtc-transport", "moq")
            .build()
        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext TokenResult.Failed(
                        "凭证接口 ${response.code}：${body.take(200)}",
                    )
                }
                parse(body)
            }
        } catch (e: Exception) {
            TokenResult.Failed("凭证接口请求失败：${e.message}")
        }
    }

    /**
     * 响应字段在不同文档版本里有 snake_case / camelCase 两种写法，这里都兼容读取。
     */
    private fun parse(body: String): TokenResult = try {
        val root = JSONObject(body)
        // 有的网关会把业务体再包一层 data
        val data = if (root.has("data") && root.optJSONObject("data") != null) {
            root.getJSONObject("data")
        } else {
            root
        }
        val sid = data.str("sid")
        val token = data.str("aoqTokenForClient", "aoq_token_for_client", "token")
        val fingerprint = data.str(
            "clientRelayCertFingerprint",
            "client_relay_cert_fingerprint",
            "certFingerprint",
        )
        val extra = data.optJSONObject("extraInfo") ?: data.optJSONObject("extra_info")
        val workspaceIdHash = extra?.str("workspaceIdHash", "workspace_id_hash").orEmpty()

        val endpointArray = data.optJSONArray("clientRelayEndpoints")
            ?: data.optJSONArray("client_relay_endpoints")
        val endpoints = buildList {
            if (endpointArray != null) {
                for (i in 0 until endpointArray.length()) {
                    val o = endpointArray.optJSONObject(i) ?: continue
                    add(
                        AoqEndpoint(
                            routeIndex = o.optInt("route_index", o.optInt("routeIndex", i)),
                            endpoint = o.str("endpoint", "host", "domain"),
                            port = o.optInt("port", 443),
                            tcpPort = o.optInt("tcp_port", o.optInt("tcpPort", 0)),
                        ),
                    )
                }
            }
        }

        if (sid.isEmpty() || token.isEmpty() || endpoints.isEmpty()) {
            TokenResult.Failed("凭证响应缺少必要字段：${body.take(200)}")
        } else {
            TokenResult.Ok(
                AoqCredential(
                    sid = sid,
                    token = token,
                    certFingerprint = fingerprint,
                    workspaceIdHash = workspaceIdHash,
                    endpoints = endpoints,
                ),
            )
        }
    } catch (e: Exception) {
        TokenResult.Failed("凭证响应解析失败：${e.message}")
    }

    /** 依次尝试多个候选键名，取第一个非空字符串 */
    private fun JSONObject.str(vararg keys: String): String {
        keys.forEach { key ->
            val value = optString(key, "")
            if (value.isNotEmpty()) return value
        }
        return ""
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
