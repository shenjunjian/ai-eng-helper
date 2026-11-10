package com.shenjj.edu.voice

import com.shenjj.edu.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 对话标题总结器。
 *
 * 挂断时把本次字幕交给文本模型，产出一个短标题用于命名新会话
 * （AGENTS.md：挂断后由 AI 总结本次对话标题 → 保存会话 → 返回首页）。
 *
 * 走百炼的 OpenAI 兼容接口，与语音链路独立：语音引擎此时已经断开，
 * 不能再复用 Data 轨。请求失败或超时一律返回 null，由调用方降级成本地规则标题。
 *
 * API Key 只从 BuildConfig 读（源自 gitignore 的 local.properties）。
 * **正式上线时应改为由自有后端代理**，客户端不再持有 Key。
 */
class SessionTitleSummarizer(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build(),
) {

    /** @return 标题；无法生成时返回 null（调用方降级） */
    suspend fun summarize(transcript: String): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.AOQ_API_KEY
        if (apiKey.isBlank() || transcript.isBlank()) return@withContext null

        val messages = JSONArray()
            .put(
                JSONObject()
                    .put("role", "system")
                    .put(
                        "content",
                        "你是对话记录归档助手。读完一段英语口语对话后，用中文给出一个不超过 10 个字的标题，" +
                            "概括这段对话的话题，像微信会话标题那样自然。" +
                            "只输出标题本身，不要引号、不要标点结尾、不要解释。",
                    ),
            )
            .put(JSONObject().put("role", "user").put("content", transcript.take(MAX_TRANSCRIPT_CHARS)))

        val body = JSONObject()
            .put("model", TEXT_MODEL)
            .put("messages", messages)
            .put("temperature", 0.3)
            .put("max_tokens", 30)
            .toString()

        val request = Request.Builder()
            .url(ENDPOINT)
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val payload = response.body?.string().orEmpty()
                val content = JSONObject(payload)
                    .optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    .orEmpty()
                clean(content)
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 模型偶尔会带引号或多余空白，这里规整并截断到标题长度 */
    private fun clean(raw: String): String? {
        val title = raw
            .trim()
            .trim('"', '「', '」', '“', '”', '《', '》')
            .replace(Regex("[\\r\\n]"), " ")
            .trim()
            .trimEnd('。', '．', '.', '！', '!', '，', ',')
        if (title.isBlank()) return null
        return title.take(MAX_TITLE_CHARS)
    }

    private companion object {
        const val ENDPOINT = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
        /** 轻量文本模型：标题这种短任务不需要大模型 */
        const val TEXT_MODEL = "qwen-flash"
        const val TIMEOUT_SECONDS = 10L
        const val MAX_TRANSCRIPT_CHARS = 4000
        const val MAX_TITLE_CHARS = 14

        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
