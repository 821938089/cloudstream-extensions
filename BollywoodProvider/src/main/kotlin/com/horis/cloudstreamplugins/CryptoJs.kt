package com.horis.cloudstreamplugins

import org.mozilla.javascript.Context

var cryptoJsSource: String? = null

object CryptoJs {

    private val source = """
        function aesEncrypt(data, key) {
            return CryptoJS.AES.encrypt(data, key).toString()
        }
    """.trimIndent()

    private val scope by lazy {
        val ct = Context.enter()
        ct.optimizationLevel = -1
        val scope = ct.initSafeStandardObjects()
        try {
            ct.evaluateString(scope, cryptoJsSource!!, "CryptoJs", 0, null)
            ct.evaluateString(scope, source, "CryptoJs", 0, null)
        } finally {
            Context.exit()
        }
        scope
    }

    // 返回结果为 base64 编码密文
    fun aesEncrypt(data: String, key: String): String {
        val ct = Context.enter()
        ct.optimizationLevel = -1
        try {
            return ct.evaluateString(
                scope,
                """aesEncrypt("$data", "$key")""",
                "aesEncrypt",
                0,
                null
            ).toString()
        } finally {
            Context.exit()
        }
    }
}
