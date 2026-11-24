package com.jarvis.explorador.ui

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ExplorerViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs: SharedPreferences = app.getSharedPreferences("explorador", Context.MODE_PRIVATE)

    private inline fun <reified T : Enum<T>> enumPref(key: String, def: T): T =
        prefs.getString(key, null)?.let { v -> enumValues<T>().firstOrNull { it.name == v } } ?: def

    private fun save(block: SharedPreferences.Editor.() -> Unit) = prefs.edit().apply(block).apply()

    // ---------- Ubicaciones ----------
    var roots: List<StorageRoot> = emptyList(); private set
    var quickAccess: List<QuickPlace> = emptyList(); private set
    var pins by mutableStateOf(prefs.getString("pins", "")!!.split('\n').filter { it.isNotBlank() }); private set

    // ---------- Contenido ----------
    var currentDir by mutableStateOf<File?>(null); private set
    private var rawItems: List<FileItem> = emptyList()
    var currentItems by mutableStateOf<List<FileItem>>(emptyList()); private set
    var groups by mutableStateOf<List<ItemGroup>>(emptyList()); private set
    var visibleItems by mutableStateOf<List<FileItem>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var busy by mutableStateOf<String?>(null); private set
    var message by mutableStateOf<String?>(null)

    // ---------- Selección ----------
    var selection by mutableStateOf<Set<String>>(emptySet()); private set
    var focused by mutableStateOf<FileItem?>(null); private set
    var selectionMode by mutableStateOf(false); private set

    // ---------- Vista ----------
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
    var collapsed by mutableStateOf<Set<String>>(emptySet()); private set

    // ---------- Búsqueda ----------
    var filter by mutableStateOf(""); private set
    var searchResults by mutableStateOf<List<FileItem>?>(null); private set
    var searching by mutableStateOf(false); private set
    private var searchJob: Job? = null

    // ---------- Portapapeles ----------
    var clipboard by mutableStateOf<List<String>>(emptyList()); private set
    var clipCut by mutableStateOf(false); private set

    // ---------- Historial ----------
    private val backStack = mutableStateListOf<String>()
    private val fwdStack = mutableStateListOf<String>()
    val canBack get() = backStack.isNotEmpty()
    val canForward get() = fwdStack.isNotEmpty()
    val canUp get() = currentDir?.let { d -> roots.none { it.path == d.path } && d.parentFile?.canRead() == true } ?: false

    // ---------- Árbol de carpetas ----------
    var treeExpanded by mutableStateOf<Set<String>>(emptySet()); private set
    val treeChildren = mutableStateMapOf<String, List<String>>()

    var freeSpaceTick by mutableIntStateOf(0); private set

    private var loadJob: Job? = null

    fun start() {
        if (currentDir != null) return
        roots = FileOps.storageRoots(getApplication())
        quickAccess = FileOps.quickAccess()
        val last = prefs.getString("lastDir", null)?.let { File(it) }
        val initial = if (last != null && last.isDirectory && last.canRead()) last else File(roots.first().path)
        navigate(initial, record = false)
    }

    fun refreshIfStarted() {
        if (currentDir != null) {
            load()
            treeExpanded.forEach { loadTreeChildren(it) }
        }
    }

    // ================= Navegación =================

    fun navigate(dir: File, record: Boolean = true) {
        if (!dir.isDirectory) {
            message = "No se encuentra la carpeta «${dir.path}»"
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
        currentDir = dir
        save { putString("lastDir", dir.path) }
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
        if (canUp) currentDir?.parentFile?.let { navigate(it) }
    }

    fun refresh() {
        load()
        freeSpaceTick++
    }

    private fun load() {
        val dir = currentDir ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            loading = true
            error = null
            val res = withContext(Dispatchers.IO) { runCatching { FileOps.list(dir, showHidden) } }
            loading = false
            res.onSuccess { rawItems = it }.onFailure {
                rawItems = emptyList()
                error = it.message ?: "Error al leer la carpeta"
            }
            recompute()
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

    /** Ruta escrita a mano en la barra de direcciones. */
    fun navigateToTyped(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        val f = File(t)
        if (f.isDirectory) navigate(f)
        else if (f.isFile) f.parentFile?.let { navigate(it) }
        else message = "No se encuentra «$t». Compruebe la ruta."
    }

    // ================= Lista derivada =================

    private fun recompute() {
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

    // ================= Ajustes de vista =================

    fun sortBy(field: SortField) {
        if (sortField == field) sortAsc = !sortAsc
        else {
            sortField = field
            sortAsc = field == SortField.NAME || field == SortField.TYPE || field == SortField.LOCATION
        }
        save { putString("sortField", sortField.name); putBoolean("sortAsc", sortAsc) }
        recompute()
    }

    fun setSortDirection(asc: Boolean) {
        sortAsc = asc
        save { putBoolean("sortAsc", asc) }
        recompute()
    }

    fun setGrouping(g: GroupBy) {
        groupBy = g
        collapsed = emptySet()
        save { putString("groupBy", g.name) }
        recompute()
    }

    fun toggleGroup(key: String) {
        collapsed = if (key in collapsed) collapsed - key else collapsed + key
        recompute()
    }

    fun collapseAll() {
        collapsed = groups.mapTo(HashSet()) { it.key }
        recompute()
    }

    fun expandAll() {
        collapsed = emptySet()
        recompute()
    }

    fun setView(mode: ViewMode) {
        viewMode = mode
        save { putString("viewMode", mode.name) }
    }

    fun setRowDensity(d: Density) {
        density = d
        save { putString("density", d.name) }
    }

    fun toggleHidden() {
        showHidden = !showHidden
        save { putBoolean("showHidden", showHidden) }
        load()
        treeExpanded.forEach { loadTreeChildren(it) }
    }

    fun toggleNav() {
        showNav = !showNav
        save { putBoolean("showNav", showNav) }
    }

    fun togglePreview() {
        showPreview = !showPreview
        save { putBoolean("showPreview", showPreview) }
    }

    fun showPreviewFor(item: FileItem) {
        if (!showPreview) togglePreview()
        focused = item
    }

    fun toggleTapMode() {
        tapOpensFolders = !tapOpensFolders
        save { putBoolean("tapOpensFolders", tapOpensFolders) }
    }

    fun dragNav(deltaDp: Float) {
        navWidth = (navWidth + deltaDp).coerceIn(160f, 480f)
        save { putFloat("navWidth", navWidth) }
    }

    fun dragPreview(deltaDp: Float) {
        previewWidth = (previewWidth + deltaDp).coerceIn(220f, 800f)
        save { putFloat("previewWidth", previewWidth) }
    }

    fun resizeColumn(field: SortField, deltaDp: Float) {
        when (field) {
            SortField.DATE -> colDate = (colDate + deltaDp).coerceIn(60f, 400f)
            SortField.TYPE -> colType = (colType + deltaDp).coerceIn(60f, 400f)
            SortField.SIZE -> colSize = (colSize + deltaDp).coerceIn(50f, 300f)
            SortField.LOCATION -> colLocation = (colLocation + deltaDp).coerceIn(80f, 600f)
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
        searchJob = viewModelScope.launch {
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
            if (sortField == SortField.LOCATION) sortField = SortField.NAME
            recompute()
        }
    }

    // ================= Selección =================

    fun selectedItems(): List<FileItem> = currentItems.filter { it.path in selection }

    fun selectOnly(item: FileItem) {
        selection = setOf(item.path)
        focused = item
    }

    fun toggle(item: FileItem) {
        selection = if (item.path in selection) selection - item.path else selection + item.path
        focused = item
    }

    fun selectAll() {
        selection = visibleItems.mapTo(HashSet()) { it.path }
    }

    fun clearSelection() {
        selection = emptySet()
        focused = null
    }

    fun invertSelection() {
        selection = visibleItems.map { it.path }.filterTo(HashSet()) { it !in selection }
    }

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

    fun onDoubleTap(item: FileItem) {
        if (selectionMode) toggle(item) else open(item)
    }

    fun onLongPress(item: FileItem) {
        if (item.path !in selection) {
            if (selectionMode) toggle(item) else selectOnly(item)
        } else {
            focused = item
        }
    }

    fun moveFocus(delta: Int, extend: Boolean) {
        val list = visibleItems
        if (list.isEmpty()) return
        val idx = focused?.let { f -> list.indexOfFirst { it.path == f.path } } ?: -1
        val next = list[(if (idx < 0) 0 else idx + delta).coerceIn(0, list.lastIndex)]
        if (extend) {
            selection = selection + next.path
            focused = next
        } else selectOnly(next)
    }

    // ================= Abrir / compartir =================

    private fun uriFor(path: String): Uri {
        val ctx = getApplication<Application>()
        return FileProvider.getUriForFile(ctx, ctx.packageName + ".files", File(path))
    }

    fun open(item: FileItem) {
        if (item.isDir) navigate(File(item.path)) else launchView(item, chooser = false)
    }

    fun openWith(item: FileItem) = launchView(item, chooser = true)

    private fun launchView(item: FileItem, chooser: Boolean) {
        val ctx = getApplication<Application>()
        try {
            val view = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uriFor(item.path), FileTypes.mime(item.extension))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            val intent = if (chooser) Intent.createChooser(view, "Abrir «${item.name}» con") else view
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            message = "No hay ninguna aplicación instalada para abrir «${item.name}»"
        } catch (e: Exception) {
            message = "No se pudo abrir: ${e.message}"
        }
    }

    fun share(items: List<FileItem>) {
        val files = items.filter { !it.isDir }
        if (files.isEmpty()) {
            message = "Selecciona archivos (no carpetas) para compartir"
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
            val chooser = Intent.createChooser(intent, "Compartir").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            getApplication<Application>().startActivity(chooser)
        } catch (e: Exception) {
            message = "No se pudo compartir: ${e.message}"
        }
    }

    // ================= Operaciones de archivo =================

    fun copySelection(cut: Boolean) {
        val s = selectedItems()
        if (s.isEmpty()) return
        clipboard = s.map { it.path }
        clipCut = cut
        message = "${s.size} elemento(s) ${if (cut) "cortado(s)" else "copiado(s)"}. Ve a la carpeta destino y pulsa Pegar."
    }

    fun paste() {
        val dest = currentDir ?: return
        val src = clipboard
        if (src.isEmpty()) return
        val cut = clipCut
        runOp(if (cut) "Moviendo ${src.size} elemento(s)…" else "Copiando ${src.size} elemento(s)…") {
            var n = 0
            for (p in src) {
                val f = File(p)
                if (!f.exists()) continue
                if (cut) FileOps.move(f, dest) else FileOps.copy(f, dest)
                n++
            }
            "$n elemento(s) ${if (cut) "movido(s)" else "copiado(s)"}"
        }
        if (cut) clipboard = emptyList()
    }

    fun delete(items: List<FileItem>) {
        if (items.isEmpty()) return
        runOp("Eliminando ${items.size} elemento(s)…") {
            items.forEach { FileOps.delete(it.file) }
            withContext(Dispatchers.Main) { clipboard = clipboard.filter { p -> items.none { it.path == p } } }
            "${items.size} elemento(s) eliminado(s)"
        }
    }

    fun rename(item: FileItem, newName: String) {
        if (newName == item.name) return
        runOp("Cambiando nombre…") {
            val target = FileOps.rename(item.file, newName.trim())
            withContext(Dispatchers.Main) { selection = setOf(target.path) }
            "Nombre cambiado a «${target.name}»"
        }
    }

    fun newFolder(name: String) {
        val dir = currentDir ?: return
        runOp("Creando carpeta…") {
            val f = FileOps.mkdir(dir, name.trim())
            withContext(Dispatchers.Main) { selection = setOf(f.path) }
            "Carpeta «${f.name}» creada"
        }
    }

    fun suggestedFolderName(): String = currentDir?.let { FileOps.uniqueNewFolderName(it) } ?: "Nueva carpeta"

    private fun runOp(label: String, block: suspend () -> String) {
        viewModelScope.launch {
            busy = label
            val r = withContext(Dispatchers.IO) { runCatching { block() } }
            busy = null
            message = r.getOrElse { "Error: ${it.message}" }
            load()
            treeExpanded.forEach { loadTreeChildren(it) }
            freeSpaceTick++
        }
    }

    // ================= Acceso rápido =================

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

    // ================= Árbol =================

    fun toggleTree(path: String) {
        if (path in treeExpanded) treeExpanded = treeExpanded - path
        else {
            treeExpanded = treeExpanded + path
            loadTreeChildren(path)
        }
    }

    private fun loadTreeChildren(path: String) {
        viewModelScope.launch {
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
}
