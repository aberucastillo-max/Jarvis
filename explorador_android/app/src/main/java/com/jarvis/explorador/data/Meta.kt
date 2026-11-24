package com.jarvis.explorador.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Fmt {
    private val es = Locale.forLanguageTag("es")
    private val shortDate = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    private val longDate = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy, HH:mm:ss", es)

    fun date(ms: Long): String =
        if (ms <= 0) "" else shortDate.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

    fun dateLong(ms: Long): String =
        if (ms <= 0) "" else longDate.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

    fun size(bytes: Long): String {
        if (bytes < 1024) return "$bytes bytes"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var v = bytes / 1024.0
        var i = 0
        while (v >= 1024 && i < units.lastIndex) {
            v /= 1024
            i++
        }
        val pattern = when {
            v < 10 -> "%.2f %s"
            v < 100 -> "%.1f %s"
            else -> "%.0f %s"
        }
        return String.format(Locale.getDefault(), pattern, v, units[i])
    }

    fun sizeExact(bytes: Long): String = "${size(bytes)} (${NumberFormat.getIntegerInstance().format(bytes)} bytes)"

    fun duration(ms: Long): String {
        val s = ms / 1000
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec)
        else String.format(Locale.ROOT, "%d:%02d", m, sec)
    }
}

object Meta {
    /** Propiedades extra según el tipo (dimensiones, duración, páginas...). */
    fun extras(item: FileItem): List<Pair<String, String>> = runCatching {
        when (item.category) {
            Category.IMAGE -> {
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(item.path, o)
                if (o.outWidth > 0) listOf("Dimensiones" to "${o.outWidth} x ${o.outHeight} píxeles") else emptyList()
            }
            Category.VIDEO, Category.AUDIO -> media(item)
            Category.PDF -> {
                val pfd = ParcelFileDescriptor.open(item.file, ParcelFileDescriptor.MODE_READ_ONLY)
                val r = PdfRenderer(pfd)
                try {
                    listOf("Páginas" to r.pageCount.toString())
                } finally {
                    r.close()
                }
            }
            Category.FOLDER -> {
                val kids = item.file.listFiles()
                if (kids == null) emptyList() else {
                    val dirs = kids.count { it.isDirectory }
                    listOf("Contiene" to "${kids.size - dirs} archivos, $dirs carpetas")
                }
            }
            else -> emptyList()
        }
    }.getOrDefault(emptyList())

    private fun media(item: FileItem): List<Pair<String, String>> {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(item.path)
            val out = mutableListOf<Pair<String, String>>()
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                out += "Duración" to Fmt.duration(it)
            }
            if (item.category == Category.VIDEO) {
                val w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                if (w != null && h != null) out += "Resolución" to "$w x $h"
            } else {
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let { out += "Título" to it }
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let { out += "Artista" to it }
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let { out += "Álbum" to it }
            }
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()?.let {
                out += "Velocidad de bits" to "${it / 1000} kbps"
            }
            return out
        } finally {
            runCatching { r.release() }
        }
    }

    fun audioArt(path: String): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(path)
            r.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        } catch (e: Exception) {
            null
        } finally {
            runCatching { r.release() }
        }
    }

    /** Devuelve el bitmap de la página y el total de páginas. */
    fun renderPdf(file: File, page: Int, widthPx: Int): Pair<Bitmap, Int> {
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        try {
            val count = renderer.pageCount
            val p = renderer.openPage(page.coerceIn(0, count - 1))
            try {
                val w = widthPx.coerceIn(200, 2000)
                val h = (w.toFloat() * p.height / p.width).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return bmp to count
            } finally {
                p.close()
            }
        } finally {
            renderer.close()
        }
    }

    fun readText(file: File, maxBytes: Int = 128 * 1024): String {
        val buf = ByteArray(maxBytes)
        var total = 0
        file.inputStream().use { s ->
            while (total < maxBytes) {
                val n = s.read(buf, total, maxBytes - total)
                if (n < 0) break
                total += n
            }
        }
        val zeros = (0 until minOf(total, 4096)).count { buf[it] == 0.toByte() }
        if (zeros > 8) return "(Archivo binario: no se puede mostrar como texto)"
        val text = String(buf, 0, total, Charsets.UTF_8)
        return if (file.length() > maxBytes) "$text\n\n… (vista previa truncada)" else text
    }

    /** Recuento recursivo de una carpeta: (archivos, carpetas, bytes). */
    suspend fun folderStats(dir: File): Triple<Int, Int, Long> {
        var files = 0
        var dirs = 0
        var bytes = 0L
        val stack = ArrayDeque<File>()
        stack.addLast(dir)
        var steps = 0
        while (stack.isNotEmpty()) {
            if (++steps % 200 == 0) currentCoroutineContext().ensureActive()
            val d = stack.removeLast()
            val kids = d.listFiles() ?: continue
            for (k in kids) {
                if (k.isDirectory) {
                    dirs++
                    stack.addLast(k)
                } else {
                    files++
                    bytes += k.length()
                }
            }
        }
        return Triple(files, dirs, bytes)
    }
}
