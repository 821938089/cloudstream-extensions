package com.horis.cloudstreamplugins

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


object GzipUtil {
    fun compress(str: String?): ByteArray? {
        var out: ByteArrayOutputStream? = null
        var gzip: GZIPOutputStream? = null
        return try {
            if (str == null || str.length == 0) {
                return null
            }
            out = ByteArrayOutputStream()
            gzip = GZIPOutputStream(out)
            gzip.write(str.toByteArray(charset("utf-8")))
            gzip.finish()
            out.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                out?.close()
                gzip?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun decompress(by: ByteArray?): String {
        var out: ByteArrayOutputStream? = null
        var gunzip: GZIPInputStream? = null
        return try {
            if (by == null || by.isEmpty()) {
                return ""
            }
            out = ByteArrayOutputStream()
            gunzip = GZIPInputStream(ByteArrayInputStream(by))
            val buffer = ByteArray(1024)
            var n: Int
            while (gunzip.read(buffer).also { n = it } != -1) {
                out.write(buffer, 0, n)
            }
            out.flush()
            String(out.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        } finally {
            try {
                out?.close()
                gunzip?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
