package com.jarvis.explorador.ui

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.explorador.MainActivity
import com.jarvis.explorador.data.Density
import com.jarvis.explorador.data.FileItem
import com.jarvis.explorador.data.FileOps
import com.jarvis.explorador.data.FileTypes
import com.jarvis.explorador.data.GroupBy
import com.jarvis.explorador.data.Grouping
import com.jarvis.explorador.data.ItemGroup
import com.jarvis.explorador.data.NaturalOrder
import com.jarvis.explorador.data.QuickPlace
import com.jarvis.explorador.data.SortField
import com.jarvis.explorador.data.Sorting
import com.jarvis.explorador.data.StorageRoot
import com.jarvis.explorador.data.ViewMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class PaneLayout(val label: String) {
    SINGLE("Un panel"), SIDE_BY_SIDE("Dos paneles en paralelo"), STACKED("Dos paneles apilados")
}

/**
 * Estado compartido por todas las pestañas y paneles de una ventana:
 * ajustes de vista, portapapeles, ubicaciones, árbol de carpetas y mensajes.
 */
class Shared(val app: Application, val scope: CoroutineScope) {
    val prefs: SharedPreferences = app.getSharedPreferences("explorador", Context.MODE_PRIVATE)

    private inline fun <reified T : Enum<T>> enumPref(key: String, def: T): T =
        prefs.getString(key, null)?.let { v -> enumValues<T>().firstOrNull { it.name == v } } ?: def

    fun save(block: SharedPreferences.Editor.() -> Unit) = prefs.edit().apply(block).apply()

    // Enlaces con el ViewModel (pestañas, paneles, ventanas)
    var onViewChanged: () -> Unit = {}
    var onFsChanged: () -> Unit = {}
    var onSessionChanged: () -> Unit = {}
    var openInNewTab: (File, String?) -> Unit = { _, _ -> }
    var openInOtherPane: (File, String?) -> Unit = { _, _ -> }
    var otherPaneDir: () -> File? = { null }
    var openInEditor: (File) -> Unit = {}
    var isDual: () -> Boolean = { false }
    var transferToOther: (List<File>, Boolean) -> Unit = { _, _ -> }

    // Editor de texto integrado
    var editorFont by mutableFloatStateOf(prefs.getFloat("editorFont", 14f)); private set
    var editorWrap by mutableStateOf(prefs.getBoolean("editorWrap", true)); private set
    fun changeEditorFont(v: Float) { editorFont = v.coerceIn(10f, 28f); save { putFloat("editorFont", editorFont) } }
    fun toggleEditorWrap() { editorWrap = !editorWrap; save { putBoolean("editorWrap", editorWrap) } }

    var roots: List<StorageRoot> = emptyList(); private set
    var quickAccess: List<QuickPlace> = emptyList(); private set
    var pins by mutableStateOf(prefs.getString("pins", "")!!.split('\n').filter { it.isNotBlank() }); private set
    var message by mutableStateOf<String?>(null)
    var busy by mutableStateOf<String?>(null); private set
    var freeSpaceTick by mutableIntStateOf(0); private set

    var clipboard by mutableStateOf<List<String>>(emptyList())
    var clipCut by mutableStateOf(false)

    // Vista
    var sortField by mutableStateOf(enumPref("sortField", SortField.NAME)); private set
    var sortAsc by mutableStateOf(prefs.getBoolean("sortAsc", true)); private set
    var groupBy by mutableStateOf(enumPref("groupBy", GroupBy.NONE)); private set
    var viewMode by mutableStateOf(enumPref("viewMode", ViewMode.DETAILS)); private set
    var density by mutableStateOf(enumPref("density", Density.COMPACT)); private set
    var showHidden by mutableStateOf(prefs.getBoolean("showHidden", false)); private set
    var showNav by mutableStateOf(prefs.getBoolean("showNav", true)); private set
    var showPreview by mutableStateOf(prefs.getBoolean("showPreview", true)); private set
    var tapOpensFolders by mutableStateOf(prefs.getBoolean("tapOpensFolders", true)); private set
    var navWidth by mutableFloatStateOf(prefs.getFloat("navWidth", 240f)); private set
    var previewWidth by mutableFloatStateOf(prefs.getFloat("previewWidth", 340f)); private set
    var colDate by mutableFloatStateOf(prefs.getFloat("colDate", 140f)); private set
    var colType by mutableFloatStateOf(prefs.getFloat("colType", 170f)); private set
    var colSize by mutableFloatStateOf(prefs.getFloat("colSize", 90f)); private set
    var colLocation by mutableFloatStateOf(prefs.getFloat("colLocation", 240f)); private set
    var showColDate by mutableStateOf(prefs.getBoolean("showColDate", true)); private set
    var showColType by mutableStateOf(prefs.getBoolean("showColType", true)); private set
    var showColSize by mutableStateOf(prefs.getBoolean("showColSize", true)); private set

    // Árbol de carpetas
    var treeExpanded by mutableStateOf<Set<String>>(emptySet()); private set
    val treeChildren = mutableStateMapOf<String, List<String>>()

    fun init() {
        if (roots.isNotEmpty()) return
        roots = FileOps.storageRoots(app)
        quickAccess = FileOps.quickAccess()
    }

    fun sortBy(field: SortField) {
        if (sortField == field) sortAsc = !sortAsc
        else {
            sortField = field
            sortAsc = field == SortField.NAME || field == SortField.TYPE || field == SortField.LOCATION
        }
        save { putString("sortField", sortField.name); putBoolean("sortAsc", sortAsc) }
        onViewChanged()
    }

    fun resetSortIfLocation() {
        if (sortField == SortField.LOCATION) sortField = SortField.NAME
    }

    fun setSortDirection(asc: Boolean) { sortAsc = asc; save { putBoolean("sortAsc", asc) }; onViewChanged() }
    fun setGrouping(g: GroupBy) { groupBy = g; save { putString("groupBy", g.name) }; onViewChanged() }
    fun setView(mode: ViewMode) { viewMode = mode; save { putString("viewMode", mode.name) } }
    fun setRowDensity(d: Density) { density = d; save { putString("density", d.name) } }
    fun toggleHidden() {
        showHidden = !showHidden
        save { putBoolean("showHidden", showHidden) }
        onFsChanged()
    }
    fun toggleNav() { showNav = !showNav; save { putBoolean("showNav", showNav) } }
    fun togglePreview() { showPreview = !showPreview; save { putBoolean("showPreview", showPreview) } }
    fun ensurePreview() { if (!showPreview) togglePreview() }
    fun toggleTapMode() { tapOpensFolders = !tapOpensFolders; save { putBoolean("tapOpensFolders", tapOpensFolders) } }
    fun dragNav(d: Float) { navWidth = (navWidth + d).coerceIn(160f, 480f); save { putFloat("navWidth", navWidth) } }
    fun dragPreview(d: Float) { previewWidth = (previewWidth + d).coerceIn(220f, 800f); save { putFloat("previewWidth", previewWidth) } }

    fun resizeColumn(field: SortField, d: Float) {
        when (field) {
            SortField.DATE -> colDate = (colDate + d).coerceIn(60f, 400f)
            SortField.TYPE -> colType = (colType + d).coerceIn(60f, 400f)
            SortField.SIZE -> colSize = (colSize + d).coerceIn(50f, 300f)
            SortField.LOCATION -> colLocation = (colLocation + d).coerceIn(80f, 600f)
            SortField.NAME -> Unit
        }
        save {
            putFloat("colDate", colDate); putFloat("colType", colType)
            putFloat("colSize", colSize); putFloat("colLocation", colLocation)
        }
    }

    fun toggleColumn(field: SortField) {
        when (field) {
            SortField.DATE -> showColDate = !showColDate
            SortField.TYPE -> showColType = !showColType
            SortField.SIZE -> showColSize = !showColSize
            else -> Unit
        }
        save { putBoolean("showColDate", showColDate); putBoolean("showColType", showColType); putBoolean("showColSize", showColSize) }
    }

    fun pin(path: String) {
        if (path in pins) return
        pins = pins + path
        save { putString("pins", pins.joinToString("\n")) }
        message = "Anclado a Acceso rápido"
    }

    fun unpin(path: String) {
        pins = pins - path
        save { putString("pins", pins.joinToString("\n")) }
    }

    fun toggleTree(path: String) {
        if (path in treeExpanded) treeExpanded = treeExpanded - path
        else {
            treeExpanded = treeExpanded + path
            loadTreeChildren(path)
        }
    }

    fun reloadTree() {
        treeExpanded.forEach { loadTreeChildren(it) }
        freeSpaceTick++
    }

    private fun loadTreeChildren(path: String) {
        scope.launch {
            val kids = withContext(Dispatchers.IO) {
                File(path).listFiles()
                    ?.filter { it.isDirectory && (showHidden || !it.isHidden) }
                    ?.map { it.path }
                    ?.sortedWith(compareBy(NaturalOrder) { File(it).name })
                    ?: emptyList()
            }
            treeChildren[path] = kids
        }
    }

    /** Ejecuta una operación de archivos en segundo plano y refresca todas las pestañas. */
    fun runOp(label: String, block: suspend () -> String) {
        scope.launch {
            busy = label
            val r = withContext(Dispatchers.IO) { runCatching { block() } }
            busy = null
            message = r.getOrElse { "Error: ${it.message}" }
            onFsChanged()
        }
    }

    fun openInNewWindow(dir: File) {
        try {
            val i = Intent(app, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_PATH, dir.path)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                        Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
                )
            app.startActivity(i)
        } catch (e: Exception) {
            message = "No se pudo abrir otra ventana: ${e.message}"
        }
    }
}

private var tabCounter = 0

/** Una pestaña del explorador: carpeta, historial, contenido, selección y búsqueda. */
class ExplorerTab(val s: Shared) {
    val id = ++tabCounter
    private val scope get() = s.scope

    // ---- Delegados a los ajustes compartidos (los usa la interfaz) ----
    val roots get() = s.roots
    val quickAccess get() = s.quickAccess
    val pins get() = s.pins
    val clipboard get() = s.clipboard
    val clipCut get() = s.clipCut
    val busy get() = s.busy
    val freeSpaceTick get() = s.freeSpaceTick
    val treeExpanded get() = s.treeExpanded
    val treeChildren get() = s.treeChildren
    val sortField get() = s.sortField
    val sortAsc get() = s.sortAsc
    val groupBy get() = s.groupBy
    val viewMode get() = s.viewMode
    val density get() = s.density
    val showHidden get() = s.showHidden
    val tapOpensFolders get() = s.tapOpensFolders
    val colDate get() = s.colDate
    val colType get() = s.colType
    val colSize get() = s.colSize
    val colLocation get() = s.colLocation
    val showColDate get() = s.showColDate
    val showColType get() = s.showColType
    val showColSize get() = s.showColSize
    fun sortBy(f: SortField) = s.sortBy(f)
    fun resizeColumn(f: SortField, d: Float) = s.resizeColumn(f, d)
    fun pin(path: String) = s.pin(path)
    fun unpin(path: String) = s.unpin(path)
    fun toggleTree(path: String) = s.toggleTree(path)

    // ---- Estado propio ----
    var currentDir by mutableStateOf<File?>(null); private set
    private var rawItems: List<FileItem> = emptyList()
    var currentItems by mutableStateOf<List<FileItem>>(emptyList()); private set
    var groups by mutableStateOf<List<ItemGroup>>(emptyList()); private set
    var visibleItems by mutableStateOf<List<FileItem>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var selection by mutableStateOf<Set<String>>(emptySet()); private set
    var focused by mutableStateOf<FileItem?>(null); private set
    var selectionMode by mutableStateOf(false); private set
    var collapsed by mutableStateOf<Set<String>>(emptySet()); private set
    var filter by mutableStateOf(""); private set
    var searchResults by mutableStateOf<List<FileItem>?>(null); private set
    var searching by mutableStateOf(false); private set
    private var searchJob: Job? = null
    private var loadJob: Job? = null
    private var pendingSelect: String? = null

    private val backStack = mutableStateListOf<String>()
    private val fwdStack = mutableStateListOf<String>()
    val canBack get() = backStack.isNotEmpty()
    val canForward get() = fwdStack.isNotEmpty()
    val canUp get() = currentDir?.let { d -> roots.none { it.path == d.path } && d.parentFile?.canRead() == true } ?: false

    val title: String
        get() {
            if (searchResults != null) return "Búsqueda: $filter"
            val d = currentDir ?: return "…"
            return roots.firstOrNull { it.path == d.path }?.label ?: d.name.ifEmpty { "/" }
        }

    // ================= Navegación =================

    fun navigate(dir: File, record: Boolean = true, select: String? = null) {
        if (!dir.isDirectory) {
            s.message = "No se encuentra la carpeta «${dir.path}»"
            return
        }
        val cur = currentDir
        if (record && cur != null && cur.path != dir.path) {
            backStack.add(cur.path)
            fwdStack.clear()
        }
        cancelSearch()
        filter = ""
        collapsed = emptySet()
        selection = emptySet()
        focused = null
        selectionMode = false
        pendingSelect = select
        currentDir = dir
        s.save { putString("lastDir", dir.path) }
        s.onSessionChanged()
        load()
    }

    fun goBack() {
        val p = backStack.removeLastOrNull() ?: return
        currentDir?.let { fwdStack.add(it.path) }
        navigate(File(p), record = false)
    }

    fun goForward() {
        val p = fwdStack.removeLastOrNull() ?: return
        currentDir?.let { backStack.add(it.path) }
        navigate(File(p), record = false)
    }

    fun goUp() {
        if (canUp) currentDir?.parentFile?.let { navigate(it, select = currentDir?.path) }
    }

    fun refresh() {
        load()
        s.reloadTree()
    }

    /** Recarga silenciosa tras una operación (sin perder selección ni búsqueda). */
    fun reload() {
        if (searchResults == null) load()
    }

    private fun load() {
        val dir = currentDir ?: return
        loadJob?.cancel()
        loadJob = scope.launch {
            loading = true
            error = null
            val res = withContext(Dispatchers.IO) { runCatching { FileOps.list(dir, showHidden) } }
            loading = false
            res.onSuccess { rawItems = it }.onFailure {
                rawItems = emptyList()
                error = it.message ?: "Error al leer la carpeta"
            }
            recompute()
            pendingSelect?.let { p ->
                pendingSelect = null
                currentItems.firstOrNull { it.path == p }?.let { selectOnly(it) }
            }
        }
    }

    fun breadcrumbs(): List<Pair<String, File>> {
        val dir = currentDir ?: return emptyList()
        val root = roots.firstOrNull { dir.path == it.path || dir.path.startsWith(it.path + "/") }
        val out = mutableListOf<Pair<String, File>>()
        var acc: File
        val rel: String
        if (root != null) {
            acc = File(root.path)
            out += root.label to acc
            rel = dir.path.removePrefix(root.path)
        } else {
            acc = File("/")
            out += "/" to acc
            rel = dir.path
        }
        rel.split('/').filter { it.isNotEmpty() }.forEach {
            acc = File(acc, it)
            out += it to acc
        }
        return out
    }

    fun navigateToTyped(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        val f = File(t)
        if (f.isDirectory) navigate(f)
        else if (f.isFile) f.parentFile?.let { navigate(it, select = f.path) }
        else s.message = "No se encuentra «$t». Compruebe la ruta."
    }

    // ---- Abrir en otros sitios ----
    fun openInNewTab(dir: File, select: String? = null) = s.openInNewTab(dir, select)
    fun openInOtherPane(dir: File, select: String? = null) = s.openInOtherPane(dir, select)
    fun openInNewWindow(dir: File) = s.openInNewWindow(dir)

    /** "Abrir ubicación del archivo" (desde resultados de búsqueda). */
    fun openLocation(item: FileItem) = navigate(File(item.parent), select = item.path)

    // ================= Lista derivada =================

    fun recompute() {
        val source = searchResults ?: rawItems
        val q = filter.trim()
        val filtered = if (q.isEmpty() || searchResults != null) source
        else source.filter { it.name.contains(q, ignoreCase = true) }
        val sorted = filtered.sortedWith(Sorting.comparator(sortField, sortAsc))
        val g = Grouping.build(sorted, groupBy, sortField, sortAsc)
        currentItems = sorted
        groups = g
        visibleItems = if (groupBy == GroupBy.NONE) sorted else g.filter { it.key !in collapsed }.flatMap { it.items }
        val paths = sorted.mapTo(HashSet()) { it.path }
        if (selection.any { it !in paths }) selection = selection.filterTo(HashSet()) { it in paths }
        val f = focused
        if (f != null && f.path !in paths) focused = null
        else if (f != null) focused = sorted.firstOrNull { it.path == f.path } ?: f
    }

    fun toggleGroup(key: String) {
        collapsed = if (key in collapsed) collapsed - key else collapsed + key
        recompute()
    }

    fun collapseAll() { collapsed = groups.mapTo(HashSet()) { it.key }; recompute() }
    fun expandAll() { collapsed = emptySet(); recompute() }

    // ================= Búsqueda =================

    fun setFilterText(t: String) {
        filter = t
        if (searchResults != null) cancelSearch()
        recompute()
    }

    fun searchSubfolders() {
        val dir = currentDir ?: return
        val q = filter.trim()
        if (q.isEmpty()) return
        searchJob?.cancel()
        searching = true
        searchResults = emptyList()
        selection = emptySet()
        focused = null
        recompute()
        s.onSessionChanged()
        searchJob = scope.launch {
            try {
                FileOps.search(dir, q, showHidden).collect { batch ->
                    searchResults = (searchResults ?: emptyList()) + batch
                    recompute()
                }
            } finally {
                searching = false
            }
        }
    }

    fun cancelSearch() {
        searchJob?.cancel()
        searchJob = null
        searching = false
        if (searchResults != null) {
            searchResults = null
            s.resetSortIfLocation()
            recompute()
        }
    }

    // ================= Selección =================

    fun selectedItems(): List<FileItem> = currentItems.filter { it.path in selection }

    fun selectOnly(item: FileItem) { selection = setOf(item.path); focused = item }

    fun toggle(item: FileItem) {
        selection = if (item.path in selection) selection - item.path else selection + item.path
        focused = item
    }

    fun selectAll() { selection = visibleItems.mapTo(HashSet()) { it.path } }
    fun clearSelection() { selection = emptySet(); focused = null }
    fun invertSelection() { selection = visibleItems.map { it.path }.filterTo(HashSet()) { it !in selection } }

    fun setSelectionModeOn(on: Boolean) {
        selectionMode = on
        if (!on) selection = focused?.let { setOf(it.path) } ?: emptySet()
    }

    fun onTap(item: FileItem) {
        when {
            selectionMode -> toggle(item)
            item.isDir && tapOpensFolders -> open(item)
            else -> selectOnly(item)
        }
    }

    fun onDoubleTap(item: FileItem) { if (selectionMode) toggle(item) else open(item) }

    fun onLongPress(item: FileItem) {
        if (item.path !in selection) {
            if (selectionMode) toggle(item) else selectOnly(item)
        } else focused = item
    }

    fun moveFocus(delta: Int, extend: Boolean) {
        val list = visibleItems
        if (list.isEmpty()) return
        val idx = focused?.let { f -> list.indexOfFirst { it.path == f.path } } ?: -1
        val next = list[(if (idx < 0) 0 else idx + delta).coerceIn(0, list.lastIndex)]
        if (extend) { selection = selection + next.path; focused = next } else selectOnly(next)
    }

    fun showPreviewFor(item: FileItem) { s.ensurePreview(); focused = item }

    // ================= Abrir / compartir =================

    private fun uriFor(path: String): Uri =
        FileProvider.getUriForFile(s.app, s.app.packageName + ".files", File(path))

    fun open(item: FileItem) {
        when {
            item.isDir -> navigate(File(item.path))
            FileTypes.isTextPreviewable(item.category) -> s.openInEditor(item.file)
            else -> launchView(item, chooser = false)
        }
    }

    fun edit(item: FileItem) = s.openInEditor(item.file)

    // ---- Arrastrar y soltar ----

    /** Posición en pantalla de cada fila visible (para clic derecho y soltar sobre carpetas). */
    val rowCoords = HashMap<String, LayoutCoordinates>()

    fun hit(rootPos: Offset): String? {
        val it = rowCoords.entries.iterator()
        var found: String? = null
        while (it.hasNext()) {
            val (path, c) = it.next()
            if (!c.isAttached) { it.remove(); continue }
            if (found == null && c.boundsInRoot().contains(rootPos)) found = path
        }
        return found
    }

    fun itemAt(rootPos: Offset): FileItem? = hit(rootPos)?.let { p -> currentItems.firstOrNull { it.path == p } }

    /** Selección de rango (arrastre con clic derecho). */
    fun selectRange(fromPath: String, toPath: String) {
        val list = visibleItems
        val a = list.indexOfFirst { it.path == fromPath }
        val b = list.indexOfFirst { it.path == toPath }
        if (a < 0 || b < 0) return
        selection = list.subList(minOf(a, b), maxOf(a, b) + 1).mapTo(HashSet()) { it.path }
        focused = list[b]
    }

    /** Datos para arrastrar a otras apps (archivos) o a otro panel (rutas). */
    fun dragData(item: FileItem): Pair<ClipData, DragPayload> {
        val items = if (item.path in selection) selectedItems() else listOf(item)
        val payload = DragPayload(items.map { it.path }, currentDir?.path)
        val files = items.filter { !it.isDir }
        val clip = if (files.isNotEmpty()) {
            val uris = files.map { uriFor(it.path) }
            ClipData.newUri(s.app.contentResolver, files[0].name, uris[0]).apply {
                uris.drop(1).forEach { addItem(ClipData.Item(it)) }
            }
        } else ClipData.newPlainText(items[0].name, items.joinToString("\n") { it.path })
        return clip to payload
    }

    private fun rootOf(path: String) = roots.firstOrNull { path == it.path || path.startsWith(it.path + "/") }?.path

    /** Soltar elementos de este explorador: mueve en la misma unidad y copia entre unidades (como Windows). */
    fun dropPaths(paths: List<String>, dest: File) {
        val files = paths.map { File(it) }.filter { it.exists() && it.parentFile?.path != dest.path && it.path != dest.path }
        if (files.isEmpty()) return
        val move = rootOf(files[0].path) == rootOf(dest.path)
        transfer(files, dest, move)
    }

    /** Soltar archivos que vienen de otra app. */
    fun importUris(uris: List<Uri>, dest: File) {
        if (uris.isEmpty()) return
        val cr = s.app.contentResolver
        s.runOp("Copiando ${uris.size} archivo(s)…") {
            var n = 0
            for (u in uris) {
                val name = runCatching {
                    cr.query(u, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                        if (c.moveToFirst()) c.getString(0) else null
                    }
                }.getOrNull() ?: u.lastPathSegment?.substringAfterLast('/') ?: "archivo"
                val target = FileOps.uniqueChild(dest, name)
                cr.openInputStream(u)?.use { input -> target.outputStream().use { input.copyTo(it) } } ?: continue
                n++
            }
            "$n archivo(s) copiado(s) a «${dest.name}»"
        }
    }

    fun newTextFile() {
        val dir = currentDir ?: return
        s.runOp("Creando documento…") {
            val f = FileOps.uniqueChild(dir, "Nuevo documento de texto.txt")
            f.createNewFile()
            withContext(Dispatchers.Main) {
                pendingSelect = f.path
                s.openInEditor(f)
            }
            "Creado «${f.name}»"
        }
    }

    fun openWith(item: FileItem) = launchView(item, chooser = true)

    private fun launchView(item: FileItem, chooser: Boolean) {
        try {
            val view = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uriFor(item.path), FileTypes.mime(item.extension))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            val intent = if (chooser) Intent.createChooser(view, "Abrir «${item.name}» con") else view
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            s.app.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            s.message = "No hay ninguna aplicación instalada para abrir «${item.name}»"
        } catch (e: Exception) {
            s.message = "No se pudo abrir: ${e.message}"
        }
    }

    fun share(items: List<FileItem>) {
        val files = items.filter { !it.isDir }
        if (files.isEmpty()) {
            s.message = "Selecciona archivos (no carpetas) para compartir"
            return
        }
        try {
            val uris = files.map { uriFor(it.path) }
            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).setType(FileTypes.mime(files[0].extension)).putExtra(Intent.EXTRA_STREAM, uris[0])
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).setType("*/*").putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
            intent.clipData = ClipData.newRawUri("", uris[0]).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            s.app.startActivity(Intent.createChooser(intent, "Compartir").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            s.message = "No se pudo compartir: ${e.message}"
        }
    }

    // ================= Operaciones de archivo =================

    fun copySelection(cut: Boolean) {
        val sel = selectedItems()
        if (sel.isEmpty()) return
        s.clipboard = sel.map { it.path }
        s.clipCut = cut
        s.message = "${sel.size} elemento(s) ${if (cut) "cortado(s)" else "copiado(s)"}. Ve a la carpeta destino y pulsa Pegar."
    }

    fun paste() {
        val dest = currentDir ?: return
        val src = s.clipboard
        if (src.isEmpty()) return
        transfer(src.map { File(it) }, dest, s.clipCut)
        if (s.clipCut) s.clipboard = emptyList()
    }

    /** Copia o mueve archivos a una carpeta (pegar, mover a…, al otro panel). */
    fun transfer(files: List<File>, dest: File, move: Boolean) {
        if (files.isEmpty()) return
        s.runOp(if (move) "Moviendo ${files.size} elemento(s)…" else "Copiando ${files.size} elemento(s)…") {
            var n = 0
            for (f in files) {
                if (!f.exists()) continue
                if (move) FileOps.move(f, dest) else FileOps.copy(f, dest)
                n++
            }
            "$n elemento(s) ${if (move) "movido(s)" else "copiado(s)"} a «${dest.name.ifEmpty { dest.path }}»"
        }
    }

    fun delete(items: List<FileItem>) {
        if (items.isEmpty()) return
        s.runOp("Eliminando ${items.size} elemento(s)…") {
            items.forEach { FileOps.delete(it.file) }
            withContext(Dispatchers.Main) { s.clipboard = s.clipboard.filter { p -> items.none { it.path == p } } }
            "${items.size} elemento(s) eliminado(s)"
        }
    }

    fun rename(item: FileItem, newName: String) {
        if (newName == item.name) return
        s.runOp("Cambiando nombre…") {
            val target = FileOps.rename(item.file, newName.trim())
            withContext(Dispatchers.Main) { pendingSelect = target.path }
            "Nombre cambiado a «${target.name}»"
        }
    }

    fun newFolder(name: String) {
        val dir = currentDir ?: return
        s.runOp("Creando carpeta…") {
            val f = FileOps.mkdir(dir, name.trim())
            withContext(Dispatchers.Main) { pendingSelect = f.path }
            "Carpeta «${f.name}» creada"
        }
    }

    fun suggestedFolderName(): String = currentDir?.let { FileOps.uniqueNewFolderName(it) } ?: "Nueva carpeta"

    fun compress(items: List<FileItem>) {
        val dir = currentDir ?: return
        if (items.isEmpty()) return
        s.runOp("Comprimiendo ${items.size} elemento(s)…") {
            val base = if (items.size == 1) items[0].name.substringBeforeLast('.', items[0].name) else (dir.name.ifEmpty { "Archivo" })
            val zip = FileOps.zip(items.map { it.file }, dir, base)
            withContext(Dispatchers.Main) { pendingSelect = zip.path }
            "Creado «${zip.name}»"
        }
    }

    fun extract(item: FileItem) {
        val dir = currentDir ?: return
        s.runOp("Extrayendo «${item.name}»…") {
            val out = FileOps.unzip(item.file, dir)
            withContext(Dispatchers.Main) { pendingSelect = out.path }
            "Extraído en «${out.name}»"
        }
    }
}

data class DragPayload(val paths: List<String>, val sourceDir: String?)

class Pane(first: ExplorerTab) {
    val tabs = mutableStateListOf(first)
    var active by mutableIntStateOf(0)
    val tab: ExplorerTab get() = tabs[active.coerceIn(0, tabs.lastIndex)]

    /** Documentos abiertos en el editor integrado (pestañas a la derecha de las carpetas). */
    val docs = mutableStateListOf<EditorDoc>()
    var activeDoc by mutableStateOf<Int?>(null)
    val doc: EditorDoc? get() = activeDoc?.let { docs.getOrNull(it) }
}

class ExplorerViewModel(app: Application) : AndroidViewModel(app) {
    val s = Shared(app, viewModelScope)
    val panes = mutableStateListOf<Pane>()
    var activePane by mutableIntStateOf(0); private set
    var layout by mutableStateOf(
        s.prefs.getString("layout", null)?.let { v -> PaneLayout.entries.firstOrNull { it.name == v } } ?: PaneLayout.SINGLE
    ); private set
    var splitRatio by mutableFloatStateOf(s.prefs.getFloat("splitRatio", 0.5f)); private set
    private var restoring = false

    val started get() = panes.isNotEmpty()
    val pane: Pane get() = panes[visiblePaneIndex(activePane)]
    val tab: ExplorerTab get() = pane.tab
    val isDual get() = layout != PaneLayout.SINGLE
    val otherPane: Pane? get() = if (isDual && panes.size > 1) panes[1 - visiblePaneIndex(activePane)] else null

    private fun visiblePaneIndex(i: Int) = if (isDual) i.coerceIn(0, panes.lastIndex) else 0

    init {
        s.onViewChanged = { allTabs().forEach { it.recompute() } }
        s.onFsChanged = {
            allTabs().forEach { it.reload() }
            s.reloadTree()
        }
        s.onSessionChanged = { saveSession() }
        s.openInNewTab = { dir, sel -> newTab(pane, dir, sel) }
        s.openInOtherPane = { dir, sel -> openInOtherPane(dir, sel) }
        s.otherPaneDir = { otherPane?.tab?.currentDir }
        s.openInEditor = { f -> openEditor(pane, f) }
        s.isDual = { isDual }
        s.transferToOther = { files, move ->
            otherPane?.tab?.currentDir?.let { dest -> tab.transfer(files, dest, move) }
        }
    }

    private fun allTabs() = panes.flatMap { it.tabs }

    fun start(initialPath: String?) {
        if (started) {
            if (initialPath != null) newTab(pane, File(initialPath), null)
            return
        }
        s.init()
        restoring = true
        val home = s.prefs.getString("lastDir", null)?.let { File(it) }?.takeIf { it.isDirectory && it.canRead() }
            ?: File(s.roots.first().path)
        if (initialPath != null) {
            // Ventana nueva: empieza solo con la carpeta pedida.
            panes += newPane(listOf(initialPath), 0)
            panes += newPane(listOf(initialPath), 0)
            layout = PaneLayout.SINGLE
        } else {
            val saved = s.prefs.getString("session", null)?.split('\n')?.filter { it.isNotBlank() }.orEmpty()
            for (i in 0..1) {
                val line = saved.getOrNull(i)
                val active = line?.substringBefore('|')?.toIntOrNull() ?: 0
                val paths = line?.substringAfter('|', "")?.split('\t')
                    ?.filter { File(it).isDirectory } .orEmpty()
                panes += newPane(paths.ifEmpty { listOf(home.path) }, active)
            }
            activePane = s.prefs.getInt("activePane", 0).coerceIn(0, 1)
        }
        restoring = false
        saveSession()
    }

    private fun newPane(paths: List<String>, active: Int): Pane {
        val tabs = paths.map { p -> ExplorerTab(s).also { it.navigate(File(p), record = false) } }
        val pane = Pane(tabs.first())
        tabs.drop(1).forEach { pane.tabs.add(it) }
        pane.active = active.coerceIn(0, pane.tabs.lastIndex)
        return pane
    }

    fun refreshAll() {
        if (!started) return
        allTabs().forEach { it.reload() }
        s.reloadTree()
    }

    fun saveSession() {
        if (restoring || panes.isEmpty()) return
        val text = panes.joinToString("\n") { p ->
            "${p.active}|" + p.tabs.mapNotNull { it.currentDir?.path }.joinToString("\t")
        }
        s.save { putString("session", text); putInt("activePane", activePane) }
    }

    fun activatePane(i: Int) {
        if (!isDual || activePane == i) return
        activePane = i
        saveSession()
    }

    fun switchPane() {
        if (isDual) activatePane(1 - visiblePaneIndex(activePane))
    }

    fun setPaneLayout(l: PaneLayout) {
        layout = l
        if (l == PaneLayout.SINGLE) activePane = 0
        s.save { putString("layout", l.name) }
    }

    fun dragSplit(fraction: Float) {
        splitRatio = (splitRatio + fraction).coerceIn(0.2f, 0.8f)
        s.save { putFloat("splitRatio", splitRatio) }
    }

    fun swapPanes() {
        if (panes.size < 2) return
        val a = panes[0]
        panes[0] = panes[1]
        panes[1] = a
        activePane = 1 - activePane
        saveSession()
    }

    // ---- Pestañas ----

    fun newTab(target: Pane, dir: File?, select: String? = null) {
        val d = dir ?: target.tab.currentDir ?: return
        val t = ExplorerTab(s)
        target.tabs.add(target.active + 1, t)
        target.active += 1
        target.activeDoc = null
        t.navigate(d, record = false, select = select)
        saveSession()
    }

    fun selectTab(target: Pane, i: Int) {
        target.active = i.coerceIn(0, target.tabs.lastIndex)
        target.activeDoc = null
        saveSession()
    }

    // ---- Editor de texto integrado ----

    val editorActive get() = pane.doc != null

    fun openEditor(target: Pane, f: File) {
        val existing = target.docs.indexOfFirst { it.file.path == f.path }
        if (existing >= 0) { target.activeDoc = existing; return }
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    if (f.length() > 8L * 1024 * 1024) error("El archivo es demasiado grande para el editor (máx. 8 MB)")
                    f.readText()
                }
            }
            text.onSuccess {
                target.docs.add(EditorDoc(f, it))
                target.activeDoc = target.docs.lastIndex
            }.onFailure { s.message = it.message ?: "No se pudo abrir «${f.name}»" }
        }
    }

    fun selectDoc(target: Pane, i: Int) { target.activeDoc = i }

    fun closeDoc(target: Pane, i: Int) {
        if (i !in target.docs.indices) return
        target.docs.removeAt(i)
        val a = target.activeDoc
        target.activeDoc = when {
            a == null -> null
            target.docs.isEmpty() -> null
            a == i -> null
            a > i -> a - 1
            else -> a
        }
    }

    fun saveDoc(d: EditorDoc): Boolean {
        val ok = d.save()
        s.message = if (ok) "Guardado «${d.name}»" else "No se pudo guardar «${d.name}»"
        if (ok) s.onFsChanged()
        return ok
    }

    fun closeTab(target: Pane, i: Int) {
        if (target.tabs.size <= 1) return
        target.tabs.removeAt(i)
        if (target.active >= i && target.active > 0) target.active -= 1
        target.active = target.active.coerceIn(0, target.tabs.lastIndex)
        saveSession()
    }

    fun closeOtherTabs(target: Pane, i: Int) {
        val keep = target.tabs[i]
        target.tabs.clear()
        target.tabs.add(keep)
        target.active = 0
        saveSession()
    }

    fun nextTab(delta: Int) {
        val p = pane
        if (p.tabs.size < 2) return
        p.active = (p.active + delta).mod(p.tabs.size)
        saveSession()
    }

    fun moveTabToOtherPane(from: Pane, i: Int) {
        if (!isDual) setPaneLayout(PaneLayout.SIDE_BY_SIDE)
        val to = panes.firstOrNull { it !== from } ?: return
        val dir = from.tabs[i].currentDir ?: return
        newTab(to, dir)
        closeTab(from, i)
    }

    fun openInOtherPane(dir: File, select: String?) {
        if (!isDual) setPaneLayout(PaneLayout.SIDE_BY_SIDE)
        val other = otherPane ?: return
        newTab(other, dir, select)
    }

    /** Copia o mueve la selección del panel activo a la carpeta del otro panel. */
    fun transferToOtherPane(move: Boolean) {
        val dest = otherPane?.tab?.currentDir ?: return
        val items = tab.selectedItems()
        if (items.isEmpty()) {
            s.message = "Selecciona elementos para ${if (move) "mover" else "copiar"} al otro panel"
            return
        }
        tab.transfer(items.map { it.file }, dest, move)
    }
}
