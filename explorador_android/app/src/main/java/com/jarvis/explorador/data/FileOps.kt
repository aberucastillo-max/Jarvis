package com.jarvis.explorador.data

import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileOps {

    fun toItem(f: File): FileItem {
        val isDir = f.isDirectory
        val ext = if (isDir) "" else f.extension.lowercase()
        return FileItem(
            path = f.absolutePath,
            name = f.name,
            isDir = isDir,
            size = if (isDir) 0L else f.length(),
            lastModified = f.lastModified(),
            extension = ext,
            category = FileTypes.category(ext, isDir),
            typeName = FileTypes.typeName(ext, isDir),
            hidden = f.isHidden,
            parent = f.parent ?: "",
        )
    }

    fun list(dir: File, showHidden: Boolean): List<FileItem> {
        val files = dir.listFiles() ?: throw IOException("No se puede acceder a «${dir.path}».")
        return files.asSequence()
            .filter { showHidden || !it.isHidden }
            .map { toItem(it) }
            .toList()
    }

    fun search(root: File, query: String, showHidden: Boolean, limit: Int = 5000): Flow<List<FileItem>> = flow {
        val stack = ArrayDeque<File>()
        stack.addLast(root)
        val batch = ArrayList<FileItem>()
        var count = 0
        var last = System.currentTimeMillis()
        while (stack.isNotEmpty() && count < limit) {
            currentCoroutineContext().ensureActive()
            val dir = stack.removeLast()
            val children = dir.listFiles() ?: continue
            for (f in children) {
                if (!showHidden && f.isHidden) continue
                if (f.isDirectory) stack.addLast(f)
                if (f.name.contains(query, ignoreCase = true)) {
                    batch.add(toItem(f))
                    count++
                }
            }
            val now = System.currentTimeMillis()
            if (batch.isNotEmpty() && now - last > 250) {
                emit(batch.toList())
                batch.clear()
                last = now
            }
        }
        if (batch.isNotEmpty()) emit(batch.toList())
    }.flowOn(Dispatchers.IO)

    /** "foto.jpg" -> "foto - copia.jpg", "foto - copia (2).jpg"... */
    private fun copyName(dir: File, name: String): File {
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var candidate = File(dir, "$base - copia$ext")
        var n = 2
        while (candidate.exists()) {
            candidate = File(dir, "$base - copia ($n)$ext")
            n++
        }
        return candidate
    }

    private fun numberedName(dir: File, name: String): File {
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var n = 2
        var candidate = File(dir, "$base ($n)$ext")
        while (candidate.exists()) {
            n++
            candidate = File(dir, "$base ($n)$ext")
        }
        return candidate
    }

    private fun checkNotInside(src: File, destDir: File) {
        val s = src.canonicalPath
        val d = destDir.canonicalPath
        if (d == s || d.startsWith("$s/")) throw IOException("No se puede copiar «${src.name}» dentro de sí misma.")
    }

    fun copy(src: File, destDir: File): File {
        if (src.isDirectory) checkNotInside(src, destDir)
        val direct = File(destDir, src.name)
        val target = if (direct.exists()) copyName(destDir, src.name) else direct
        if (src.isDirectory) {
            if (!src.copyRecursively(target, overwrite = false)) throw IOException("Error al copiar «${src.name}».")
        } else {
            src.copyTo(target, overwrite = false)
        }
        return target
    }

    fun move(src: File, destDir: File): File {
        if (src.parentFile?.canonicalPath == destDir.canonicalPath) return src
        if (src.isDirectory) checkNotInside(src, destDir)
        val direct = File(destDir, src.name)
        val target = if (direct.exists()) numberedName(destDir, src.name) else direct
        if (!src.renameTo(target)) {
            // Distinto volumen (p. ej. interna -> SD): copiar y borrar.
            if (src.isDirectory) {
                if (!src.copyRecursively(target)) throw IOException("Error al mover «${src.name}».")
            } else {
                src.copyTo(target)
            }
            if (!src.deleteRecursively()) throw IOException("Se copió «${src.name}» pero no se pudo borrar el original.")
        }
        return target
    }

    fun delete(f: File) {
        if (!f.deleteRecursively()) throw IOException("No se pudo eliminar «${f.name}».")
    }

    fun rename(f: File, newName: String): File {
        validateName(newName)
        val target = File(f.parentFile, newName)
        if (target.exists() && !target.path.equals(f.path, ignoreCase = true)) {
            throw IOException("Ya existe un elemento llamado «$newName».")
        }
        if (!f.renameTo(target)) throw IOException("No se pudo cambiar el nombre.")
        return target
    }

    fun mkdir(dir: File, name: String): File {
        validateName(name)
        val target = File(dir, name)
        if (target.exists()) throw IOException("Ya existe «$name».")
        if (!target.mkdirs()) throw IOException("No se pudo crear la carpeta.")
        return target
    }

    fun uniqueNewFolderName(dir: File): String {
        if (!File(dir, "Nueva carpeta").exists()) return "Nueva carpeta"
        var n = 2
        while (File(dir, "Nueva carpeta ($n)").exists()) n++
        return "Nueva carpeta ($n)"
    }

    private fun validateName(name: String) {
        if (name.isBlank()) throw IOException("El nombre no puede estar vacío.")
        if (name.any { it in "/\\:*?\"<>|" } || name == "." || name == "..") {
            throw IOException("Un nombre no puede contener: \\ / : * ? \" < > |")
        }
    }

    /** «nombre.ext» libre en [dir]: «nombre (2).ext», «nombre (3).ext»… */
    fun uniqueChild(dir: File, name: String): File {
        val dot = name.lastIndexOf('.')
        return if (dot > 0) uniqueFile(dir, name.substring(0, dot), name.substring(dot)) else uniqueFile(dir, name, "")
    }

    private fun uniqueFile(dir: File, base: String, ext: String): File {
        var f = File(dir, "$base$ext")
        var n = 2
        while (f.exists()) {
            f = File(dir, "$base ($n)$ext")
            n++
        }
        return f
    }

    /** Comprime archivos y carpetas en un .zip dentro de [dir]. */
    fun zip(sources: List<File>, dir: File, baseName: String): File {
        val target = uniqueFile(dir, baseName, ".zip")
        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(target))).use { zos ->
                fun add(f: File, entryName: String) {
                    if (f.isDirectory) {
                        zos.putNextEntry(ZipEntry("$entryName/"))
                        zos.closeEntry()
                        f.listFiles()?.forEach { add(it, "$entryName/${it.name}") }
                    } else {
                        zos.putNextEntry(ZipEntry(entryName).apply { time = f.lastModified() })
                        f.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
                sources.forEach { add(it, it.name) }
            }
        } catch (e: Exception) {
            target.delete()
            throw e
        }
        return target
    }

    /** Extrae un .zip en una carpeta nueva con su nombre, junto al archivo. */
    fun unzip(zip: File, dir: File): File {
        val out = uniqueFile(dir, zip.nameWithoutExtension, "")
        if (!out.mkdirs()) throw IOException("No se pudo crear la carpeta destino.")
        val root = out.canonicalPath + File.separator
        ZipInputStream(BufferedInputStream(FileInputStream(zip))).use { zis ->
            while (true) {
                val e = zis.nextEntry ?: break
                val f = File(out, e.name)
                if (!f.canonicalPath.startsWith(root)) throw IOException("Entrada no válida en el ZIP: ${e.name}")
                if (e.isDirectory) f.mkdirs()
                else {
                    f.parentFile?.mkdirs()
                    FileOutputStream(f).use { zis.copyTo(it) }
                    if (e.time > 0) f.setLastModified(e.time)
                }
                zis.closeEntry()
            }
        }
        return out
    }

    fun storageRoots(ctx: Context): List<StorageRoot> {
        val primary = Environment.getExternalStorageDirectory().absolutePath
        val out = mutableListOf(StorageRoot("Almacenamiento interno", primary, false))
        val sm = ctx.getSystemService(StorageManager::class.java)
        ctx.getExternalFilesDirs(null).filterNotNull().forEach { d ->
            val p = d.absolutePath.substringBefore("/Android/data")
            if (p != primary && out.none { it.path == p } && File(p).canRead()) {
                val label = runCatching { sm?.getStorageVolume(File(p))?.getDescription(ctx) }.getOrNull() ?: "Tarjeta SD"
                out += StorageRoot(label, p, true)
            }
        }
        return out
    }

    fun quickAccess(): List<QuickPlace> {
        val root = Environment.getExternalStorageDirectory()
        return listOf(
            QuickPlace("Descargas", File(root, "Download").path, "download"),
            QuickPlace("Documentos", File(root, "Documents").path, "documents"),
            QuickPlace("Imágenes", File(root, "Pictures").path, "pictures"),
            QuickPlace("Cámara", File(root, "DCIM").path, "dcim"),
            QuickPlace("Música", File(root, "Music").path, "music"),
            QuickPlace("Vídeos", File(root, "Movies").path, "movies"),
        ).filter { File(it.path).isDirectory }
    }
}
