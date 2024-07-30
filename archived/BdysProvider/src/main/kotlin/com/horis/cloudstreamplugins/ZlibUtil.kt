import java.io.*
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

/**
 * zlib 压缩算法
 */
object ZlibUtil {
    /**
     * 压缩
     *
     * @param data
     * 待压缩数据
     * @return byte[] 压缩后的数据
     */
    fun compress(data: ByteArray): ByteArray {
        var output = ByteArray(0)
        val compresser = Deflater()
        compresser.reset()
        compresser.setInput(data)
        compresser.finish()
        val bos = ByteArrayOutputStream(data.size)
        try {
            val buf = ByteArray(1024)
            while (!compresser.finished()) {
                val i = compresser.deflate(buf)
                bos.write(buf, 0, i)
            }
            output = bos.toByteArray()
        } catch (e: Exception) {
            output = data
            e.printStackTrace()
        } finally {
            try {
                bos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        compresser.end()
        return output
    }

    /**
     * 压缩
     *
     * @param data
     * 待压缩数据
     *
     * @param os
     * 输出流
     */
    fun compress(data: ByteArray, os: OutputStream?) {
        val dos = DeflaterOutputStream(os)
        try {
            dos.write(data, 0, data.size)
            dos.finish()
            dos.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 解压缩
     *
     * @param data
     * 待压缩的数据
     * @return byte[] 解压缩后的数据
     */
    fun decompress(data: ByteArray): ByteArray {
        var output = ByteArray(0)
        val decompresser = Inflater()
        decompresser.reset()
        decompresser.setInput(data)
        val o = ByteArrayOutputStream(data.size)
        try {
            val buf = ByteArray(1024)
            while (!decompresser.finished()) {
                val i = decompresser.inflate(buf)
                o.write(buf, 0, i)
            }
            output = o.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                o.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        decompresser.end()
        return output
    }

    /**
     * 解压缩
     *
     * @param is
     * 输入流
     * @return byte[] 解压缩后的数据
     */
    fun decompress(`is`: InputStream?): ByteArray {
        val iis = InflaterInputStream(`is`)
        val o = ByteArrayOutputStream(1024)
        try {
            var i = 1024
            val buf = ByteArray(i)
            while (iis.read(buf, 0, i).also { i = it } > 0) {
                o.write(buf, 0, i)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return o.toByteArray()
    }
}
