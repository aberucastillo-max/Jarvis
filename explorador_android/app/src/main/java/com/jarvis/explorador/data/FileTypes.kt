package com.jarvis.explorador.data

import android.webkit.MimeTypeMap

object FileTypes {
    private val image = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "ico", "tif", "tiff", "dng", "avif")
    private val video = setOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "m4v", "wmv", "flv", "ts", "mpg", "mpeg")
    private val audio = setOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "opus", "wma", "amr", "mid", "midi")
    private val text = setOf("txt", "log", "md", "csv", "ini", "cfg", "conf", "properties", "nfo", "srt", "vtt", "tsv")
    private val code = setOf(
        "kt", "kts", "java", "js", "ts", "py", "html", "htm", "css", "json", "xml", "yml", "yaml", "sh",
        "c", "cpp", "h", "hpp", "cs", "go", "rs", "php", "rb", "swift", "sql", "gradle", "bat", "gs", "toml"
    )
    private val archive = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tgz")
    private val doc = setOf("doc", "docx", "odt", "rtf")
    private val sheet = setOf("xls", "xlsx", "xlsm", "ods")
    private val slides = setOf("ppt", "pptx", "odp")

    private val names = mapOf(
        "jpg" to "Imagen JPEG", "jpeg" to "Imagen JPEG", "png" to "Imagen PNG", "gif" to "Imagen GIF",
        "webp" to "Imagen WebP", "bmp" to "Imagen BMP", "heic" to "Imagen HEIC", "heif" to "Imagen HEIF",
        "mp4" to "Vídeo MP4", "mkv" to "Vídeo MKV", "avi" to "Vídeo AVI", "mov" to "Vídeo QuickTime",
        "webm" to "Vídeo WebM", "3gp" to "Vídeo 3GP",
        "mp3" to "Audio MP3", "wav" to "Audio WAV", "flac" to "Audio FLAC", "m4a" to "Audio M4A",
        "ogg" to "Audio OGG", "opus" to "Audio Opus", "aac" to "Audio AAC",
        "pdf" to "Documento PDF", "txt" to "Documento de texto", "md" to "Documento Markdown",
        "csv" to "Valores separados por comas", "log" to "Archivo de registro",
        "doc" to "Documento de Word 97-2003", "docx" to "Documento de Microsoft Word",
        "xls" to "Hoja de Excel 97-2003", "xlsx" to "Hoja de cálculo de Microsoft Excel",
        "ppt" to "Presentación de PowerPoint 97-2003", "pptx" to "Presentación de Microsoft PowerPoint",
        "zip" to "Carpeta comprimida (zip)", "rar" to "Archivo RAR", "7z" to "Archivo 7-Zip",
        "apk" to "Paquete de aplicación Android", "html" to "Documento HTML", "htm" to "Documento HTML",
        "json" to "Archivo JSON", "xml" to "Documento XML", "js" to "Archivo JavaScript",
        "py" to "Archivo Python", "kt" to "Archivo Kotlin", "java" to "Archivo Java",
    )

    fun category(ext: String, isDir: Boolean): Category = when {
        isDir -> Category.FOLDER
        ext in image -> Category.IMAGE
        ext in video -> Category.VIDEO
        ext in audio -> Category.AUDIO
        ext == "pdf" -> Category.PDF
        ext in text -> Category.TEXT
        ext in code -> Category.CODE
        ext in archive -> Category.ARCHIVE
        ext in doc -> Category.DOC
        ext in sheet -> Category.SHEET
        ext in slides -> Category.SLIDES
        ext == "apk" -> Category.APK
        else -> Category.OTHER
    }

    fun typeName(ext: String, isDir: Boolean): String = when {
        isDir -> "Carpeta de archivos"
        ext.isEmpty() -> "Archivo"
        else -> names[ext] ?: "Archivo ${ext.uppercase()}"
    }

    fun mime(ext: String): String = when (ext) {
        "apk" -> "application/vnd.android.package-archive"
        else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }

    fun isTextPreviewable(c: Category) = c == Category.TEXT || c == Category.CODE
}
