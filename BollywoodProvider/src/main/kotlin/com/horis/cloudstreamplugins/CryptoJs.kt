package com.horis.cloudstreamplugins

import org.mozilla.javascript.Context

var cryptoJsSource: String? = null

object CryptoJs {

    val source = """
        function aesEncrypt(data, key) {
            return CryptoJS.AES.encrypt(data, key).toString()
        }
    """.trimIndent()

    val rhino by lazy {
        val ctx = Context.enter()
        ctx.optimizationLevel = -1
        val scope = ctx.initSafeStandardObjects()
        cryptoJsSource?.let {
            ctx.evaluateString(scope, it, "CryptoJs", 0, null)
        }
        ctx.evaluateString(scope, source, "CryptoJs", 0, null)
        ctx to scope
    }

    // 返回结果为 base64 编码密文
    fun aesEncrypt(data: String, key: String): String {
        val (rhino, scope) = rhino
        return rhino.evaluateString(
            scope,
            """aesEncrypt("$data", "$key")""",
            "aesEncrypt",
            0,
            null
        ).toString()
    }
}
