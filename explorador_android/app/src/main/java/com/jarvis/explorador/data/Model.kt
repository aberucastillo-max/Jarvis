package com.jarvis.explorador.data

import java.io.File

enum class Category { FOLDER, IMAGE, VIDEO, AUDIO, PDF, TEXT, CODE, ARCHIVE, DOC, SHEET, SLIDES, APK, OTHER }

data class FileItem(
    val path: String,
    val name: String,
    val isDir: Boolean,
    val size: Long,
    val lastModified: Long,
    val extension: String,
    val category: Category,
    val typeName: String,
    val hidden: Boolean,
    val parent: String,
) {
    val file: File get() = File(path)
}

enum class SortField(val label: String) {
    NAME("Nombre"), DATE("Fecha de modificación"), TYPE("Tipo"), SIZE("Tamaño"), LOCATION("Ubicación")
}

enum class GroupBy(val label: String) {
    NONE("(Ninguno)"), NAME("Nombre"), DATE("Fecha de modificación"), TYPE("Tipo"), SIZE("Tamaño")
}

enum class ViewMode(val label: String) {
    DETAILS("Detalles"), LIST("Lista"), TILES("Mosaicos"), MEDIUM_ICONS("Iconos medianos"), LARGE_ICONS("Iconos grandes")
}

enum class Density(val label: String, val rowDp: Int) {
    COMPACT("Compacta", 28), NORMAL("Normal", 36), COMFORT("Cómoda", 44)
}

data class ItemGroup(val key: String, val label: String, val items: List<FileItem>)

data class StorageRoot(val label: String, val path: String, val removable: Boolean)

data class QuickPlace(val label: String, val path: String, val kind: String)
