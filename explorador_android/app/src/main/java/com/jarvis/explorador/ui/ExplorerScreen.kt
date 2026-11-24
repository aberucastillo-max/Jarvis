package com.jarvis.explorador.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.explorador.data.Density
import com.jarvis.explorador.data.FileItem
import com.jarvis.explorador.data.Fmt
import com.jarvis.explorador.data.GroupBy
import com.jarvis.explorador.data.SortField
import com.jarvis.explorador.data.ViewMode
import java.io.File

sealed interface DialogReq {
    data class Rename(val item: FileItem) : DialogReq
    data object NewFolder : DialogReq
    data class Delete(val items: List<FileItem>) : DialogReq
}

@Composable
fun ExplorerScreen(vm: ExplorerViewModel) {
    val snackbar = remember { SnackbarHostState() }
    var dialog by remember { mutableStateOf<DialogReq?>(null) }
    val rootFocus = remember { FocusRequester() }

    LaunchedEffect(vm.message) {
        val m = vm.message ?: return@LaunchedEffect
        vm.message = null
        snackbar.showSnackbar(m)
    }
    LaunchedEffect(Unit) { runCatching { rootFocus.requestFocus() } }

    BackHandler(enabled = vm.selectionMode || vm.searchResults != null || vm.canBack) {
        when {
            vm.selectionMode -> vm.setSelectionModeOn(false)
            vm.searchResults != null -> vm.cancelSearch()
            else -> vm.goBack()
        }
    }

    val requestDialog: (DialogReq) -> Unit = { dialog = it }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .focusRequester(rootFocus)
                .focusable()
                .onKeyEvent { handleKey(vm, it, requestDialog) }
        ) {
            AddressBar(vm)
            CommandBar(vm, requestDialog)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.weight(1f).fillMaxWidth()) {
                if (vm.showNav) {
                    NavPane(vm, Modifier.width(vm.navWidth.dp).fillMaxHeight())
                    Splitter { vm.dragNav(it) }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    ContentArea(vm, requestDialog, onInteract = { runCatching { rootFocus.requestFocus() } })
                }
                if (vm.showPreview) {
                    Splitter { vm.dragPreview(-it) }
                    PreviewPane(vm, Modifier.width(vm.previewWidth.dp).fillMaxHeight())
                }
            }
            StatusBar(vm)
        }
    }

    dialog?.let { d -> Dialogs(d, vm) { dialog = null } }
}

private fun handleKey(vm: ExplorerViewModel, e: KeyEvent, dialog: (DialogReq) -> Unit): Boolean {
    if (e.type != KeyEventType.KeyDown) return false
    val ctrl = e.isCtrlPressed
    when {
        ctrl && e.key == Key.A -> vm.selectAll()
        ctrl && e.key == Key.C -> vm.copySelection(false)
        ctrl && e.key == Key.X -> vm.copySelection(true)
        ctrl && e.key == Key.V -> vm.paste()
        ctrl && e.isShiftPressed && e.key == Key.N -> dialog(DialogReq.NewFolder)
        e.key == Key.Delete -> vm.selectedItems().takeIf { it.isNotEmpty() }?.let { dialog(DialogReq.Delete(it)) }
        e.key == Key.F2 -> vm.focused?.let { dialog(DialogReq.Rename(it)) }
        e.key == Key.F5 -> vm.refresh()
        e.key == Key.Backspace -> vm.goBack()
        e.isAltPressed && e.key == Key.DirectionLeft -> vm.goBack()
        e.isAltPressed && e.key == Key.DirectionRight -> vm.goForward()
        e.isAltPressed && e.key == Key.DirectionUp -> vm.goUp()
        e.isAltPressed && e.key == Key.P -> vm.togglePreview()
        e.key == Key.DirectionDown -> vm.moveFocus(1, e.isShiftPressed)
        e.key == Key.DirectionUp -> vm.moveFocus(-1, e.isShiftPressed)
        e.key == Key.Enter -> vm.focused?.let { vm.open(it) }
        e.key == Key.Escape -> if (vm.selectionMode) vm.setSelectionModeOn(false) else vm.clearSelection()
        else -> return false
    }
    return true
}

@Composable
fun Splitter(onDragDp: (Float) -> Unit) {
    val density = LocalDensity.current
    val cb by rememberUpdatedState(onDragDp)
    val state = rememberDraggableState { px -> cb(with(density) { px.toDp().value }) }
    Box(
        Modifier
            .fillMaxHeight()
            .width(9.dp)
            .background(MaterialTheme.colorScheme.background)
            .draggable(state, Orientation.Horizontal),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Box(Modifier.height(28.dp).width(3.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
    }
}

// ======================= Barra de direcciones =======================

@Composable
private fun AddressBar(vm: ExplorerViewModel) {
    Row(
        Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavIcon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", vm.canBack) { vm.goBack() }
        NavIcon(Icons.AutoMirrored.Filled.ArrowForward, "Adelante", vm.canForward) { vm.goForward() }
        NavIcon(Icons.Filled.ArrowUpward, "Subir un nivel", vm.canUp) { vm.goUp() }
        NavIcon(Icons.Filled.Refresh, "Actualizar", true) { vm.refresh() }
        Spacer(Modifier.width(6.dp))
        Breadcrumb(vm, Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        SearchBox(vm, Modifier.width(280.dp))
    }
}

@Composable
private fun NavIcon(icon: ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.85f else 0.3f)
    Box(
        Modifier.size(38.dp).clip(RoundedCornerShape(6.dp)).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun Breadcrumb(vm: ExplorerViewModel, modifier: Modifier) {
    var editing by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    val boxMod = modifier
        .height(34.dp)
        .clip(shape)
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)

    if (editing) {
        val focus = remember { FocusRequester() }
        val path = vm.currentDir?.path ?: ""
        var value by remember { mutableStateOf(TextFieldValue(path, TextRange(0, path.length))) }
        LaunchedEffect(Unit) { focus.requestFocus() }
        Row(boxMod.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { editing = false; vm.navigateToTyped(value.text) }),
                modifier = Modifier.weight(1f).focusRequester(focus),
            )
            Icon(
                Icons.Filled.Close, "Cancelar",
                Modifier.size(18.dp).clickable { editing = false },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        val crumbs = vm.breadcrumbs()
        val state = rememberLazyListState()
        LaunchedEffect(crumbs.size, vm.currentDir) { if (crumbs.isNotEmpty()) state.scrollToItem(crumbs.lastIndex) }
        Row(boxMod.clickable { editing = true }, verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(6.dp))
            Icon(iconFor(com.jarvis.explorador.data.Category.FOLDER), null, tint = FolderColor, modifier = Modifier.size(18.dp))
            LazyRow(state = state, modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                itemsIndexed(crumbs) { i, (label, file) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.ChevronRight, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            label,
                            fontSize = 14.sp,
                            fontWeight = if (i == crumbs.lastIndex) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { vm.navigate(file) }
                                .padding(horizontal = 5.dp, vertical = 6.dp),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBox(vm: ExplorerViewModel, modifier: Modifier) {
    val shape = RoundedCornerShape(6.dp)
    val name = vm.currentDir?.name?.ifEmpty { "/" } ?: ""
    BasicTextField(
        value = vm.filter,
        onValueChange = { vm.setFilterText(it) },
        singleLine = true,
        textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { vm.searchSubfolders() }),
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                Modifier
                    .height(34.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Box(Modifier.weight(1f)) {
                    if (vm.filter.isEmpty()) {
                        Text(
                            "Buscar en $name", fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                    inner()
                }
                if (vm.filter.isNotEmpty()) {
                    Icon(
                        Icons.Filled.FindInPage, "Buscar en subcarpetas",
                        Modifier.size(20.dp).clickable { vm.searchSubfolders() },
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.Close, "Borrar",
                        Modifier.size(18.dp).clickable { vm.setFilterText("") },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

// ======================= Barra de comandos =======================

@Composable
private fun CommandBar(vm: ExplorerViewModel, dialog: (DialogReq) -> Unit) {
    val sel = vm.selection
    val hasSel = sel.isNotEmpty()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val labels = maxWidth > 1100.dp
        Row(
            Modifier
                .fillMaxWidth()
                .height(42.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolButton(Icons.Filled.CreateNewFolder, "Nueva carpeta", true) { dialog(DialogReq.NewFolder) }
            BarDivider()
            ToolButton(Icons.Filled.ContentCut, "Cortar", labels, hasSel) { vm.copySelection(true) }
            ToolButton(Icons.Filled.ContentCopy, "Copiar", labels, hasSel) { vm.copySelection(false) }
            ToolButton(Icons.Filled.ContentPaste, "Pegar", labels, vm.clipboard.isNotEmpty()) { vm.paste() }
            ToolButton(Icons.Filled.DriveFileRenameOutline, "Renombrar", labels, sel.size == 1) {
                vm.selectedItems().firstOrNull()?.let { dialog(DialogReq.Rename(it)) }
            }
            ToolButton(Icons.Filled.Share, "Compartir", labels, hasSel) { vm.share(vm.selectedItems()) }
            ToolButton(Icons.Filled.Delete, "Eliminar", labels, hasSel) { dialog(DialogReq.Delete(vm.selectedItems())) }
            BarDivider()
            SortMenu(vm)
            GroupMenu(vm)
            ViewMenu(vm)
            BarDivider()
            SelectMenu(vm, labels)
            ToolButton(Icons.Filled.VerticalSplit, "Vista previa", labels, true, active = vm.showPreview) { vm.togglePreview() }
            MoreMenu(vm)
        }
    }
}

@Composable
private fun BarDivider() {
    VerticalDivider(Modifier.height(22.dp).padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun ToolButton(
    icon: ImageVector,
    label: String,
    showLabel: Boolean,
    enabled: Boolean = true,
    active: Boolean = false,
    trailing: Boolean = false,
    onClick: () -> Unit,
) {
    val c = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.9f else 0.32f)
    Row(
        Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) selectionBg else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon, label,
            tint = if (enabled) MaterialTheme.colorScheme.primary else c,
            modifier = Modifier.size(20.dp),
        )
        if (showLabel) {
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 13.sp, color = c, maxLines = 1)
        }
        if (trailing) {
            Icon(
                Icons.Filled.ChevronRight, null, tint = c,
                modifier = Modifier.size(14.dp).padding(start = 2.dp),
            )
        }
    }
}

@Composable
fun CheckItem(label: String, checked: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, fontSize = 14.sp) },
        onClick = onClick,
        leadingIcon = {
            if (checked) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            else Spacer(Modifier.size(18.dp))
        },
        modifier = Modifier.height(38.dp),
    )
}

@Composable
private fun MenuHeader(text: String) {
    Text(
        text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
private fun SortMenu(vm: ExplorerViewModel) {
    var open by remember { mutableStateOf(false) }
    Box {
        ToolButton(Icons.AutoMirrored.Filled.Sort, "Ordenar", true, trailing = true) { open = true }
        DropdownMenu(open, { open = false }) {
            MenuHeader("Ordenar por")
            SortField.entries
                .filter { it != SortField.LOCATION || vm.searchResults != null }
                .forEach { f -> CheckItem(f.label, vm.sortField == f) { vm.sortBy(f); if (vm.sortField == f) open = false } }
            HorizontalDivider()
            CheckItem("Ascendente", vm.sortAsc) { vm.setSortDirection(true); open = false }
            CheckItem("Descendente", !vm.sortAsc) { vm.setSortDirection(false); open = false }
        }
    }
}

@Composable
private fun GroupMenu(vm: ExplorerViewModel) {
    var open by remember { mutableStateOf(false) }
    Box {
        ToolButton(Icons.Filled.Workspaces, "Agrupar", true, active = vm.groupBy != GroupBy.NONE, trailing = true) { open = true }
        DropdownMenu(open, { open = false }) {
            MenuHeader("Agrupar por")
            GroupBy.entries.forEach { g -> CheckItem(g.label, vm.groupBy == g) { vm.setGrouping(g); open = false } }
            if (vm.groupBy != GroupBy.NONE) {
                HorizontalDivider()
                CheckItem("Contraer todos los grupos", false) { vm.collapseAll(); open = false }
                CheckItem("Expandir todos los grupos", false) { vm.expandAll(); open = false }
            }
        }
    }
}

private fun viewIcon(m: ViewMode): ImageVector = when (m) {
    ViewMode.DETAILS -> Icons.AutoMirrored.Filled.ViewList
    ViewMode.LIST -> Icons.Filled.ViewAgenda
    ViewMode.TILES -> Icons.Filled.ViewModule
    ViewMode.MEDIUM_ICONS, ViewMode.LARGE_ICONS -> Icons.Filled.GridView
}

@Composable
private fun ViewMenu(vm: ExplorerViewModel) {
    var open by remember { mutableStateOf(false) }
    Box {
        ToolButton(viewIcon(vm.viewMode), "Ver", true, trailing = true) { open = true }
        DropdownMenu(open, { open = false }) {
            MenuHeader("Diseño")
            ViewMode.entries.forEach { m -> CheckItem(m.label, vm.viewMode == m) { vm.setView(m); open = false } }
            HorizontalDivider()
            MenuHeader("Densidad de filas")
            Density.entries.forEach { d -> CheckItem("${d.label} (${d.rowDp} dp)", vm.density == d) { vm.setRowDensity(d) } }
            HorizontalDivider()
            MenuHeader("Columnas (vista Detalles)")
            CheckItem("Fecha de modificación", vm.showColDate) { vm.toggleColumn(SortField.DATE) }
            CheckItem("Tipo", vm.showColType) { vm.toggleColumn(SortField.TYPE) }
            CheckItem("Tamaño", vm.showColSize) { vm.toggleColumn(SortField.SIZE) }
            HorizontalDivider()
            MenuHeader("Mostrar")
            CheckItem("Panel de navegación", vm.showNav) { vm.toggleNav() }
            CheckItem("Panel de vista previa", vm.showPreview) { vm.togglePreview() }
            CheckItem("Elementos ocultos", vm.showHidden) { vm.toggleHidden() }
            CheckItem("Casillas de selección", vm.selectionMode) { vm.setSelectionModeOn(!vm.selectionMode) }
        }
    }
}

@Composable
private fun SelectMenu(vm: ExplorerViewModel, labels: Boolean) {
    var open by remember { mutableStateOf(false) }
    Box {
        ToolButton(Icons.Filled.SelectAll, "Seleccionar", labels, active = vm.selectionMode, trailing = true) { open = true }
        DropdownMenu(open, { open = false }) {
            CheckItem("Modo selección múltiple", vm.selectionMode) { vm.setSelectionModeOn(!vm.selectionMode); open = false }
            HorizontalDivider()
            CheckItem("Seleccionar todo (Ctrl+A)", false) { vm.selectAll(); open = false }
            CheckItem("No seleccionar nada", false) { vm.clearSelection(); open = false }
            CheckItem("Invertir selección", false) { vm.invertSelection(); open = false }
        }
    }
}

@Composable
private fun MoreMenu(vm: ExplorerViewModel) {
    var open by remember { mutableStateOf(false) }
    Box {
        ToolButton(Icons.Filled.MoreHoriz, "Más", false) { open = true }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem(
                text = { Text("Anclar carpeta actual a Acceso rápido") },
                leadingIcon = { Icon(Icons.Filled.PushPin, null) },
                onClick = { vm.currentDir?.let { vm.pin(it.path) }; open = false },
            )
            DropdownMenuItem(
                text = { Text(if (vm.showHidden) "Ocultar elementos ocultos" else "Mostrar elementos ocultos") },
                leadingIcon = { Icon(Icons.Filled.Visibility, null) },
                onClick = { vm.toggleHidden(); open = false },
            )
            HorizontalDivider()
            CheckItem("Un toque abre carpetas", vm.tapOpensFolders) { vm.toggleTapMode() }
            Text(
                if (vm.tapOpensFolders) "Archivos: un toque selecciona (vista previa), doble toque abre."
                else "Como Windows: un toque selecciona, doble toque abre.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).width(300.dp),
            )
        }
    }
}

// ======================= Barra de estado =======================

@Composable
private fun StatusBar(vm: ExplorerViewModel) {
    val selItems = remember(vm.selection, vm.currentItems) { vm.selectedItems() }
    val selBytes = remember(selItems) { selItems.filter { !it.isDir }.sumOf { it.size } }
    val free = remember(vm.currentDir, vm.freeSpaceTick) {
        vm.currentDir?.let { d -> runCatching { d.usableSpace to d.totalSpace }.getOrNull() }
    }
    Column {
        if (vm.busy != null || vm.searching) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${vm.currentItems.size} elementos", style = style)
            if (selItems.isNotEmpty()) {
                Text(
                    "     ${selItems.size} elemento${if (selItems.size == 1) "" else "s"} seleccionado${if (selItems.size == 1) "" else "s"}" +
                        if (selBytes > 0) "   ${Fmt.size(selBytes)}" else "",
                    style = style,
                )
            }
            vm.busy?.let { Text("     $it", style = style) }
            if (vm.searching) Text("     Buscando en subcarpetas…", style = style)
            else if (vm.searchResults != null) Text("     Resultados de búsqueda", style = style)
            if (vm.clipboard.isNotEmpty()) {
                Text("     Portapapeles: ${vm.clipboard.size} (${if (vm.clipCut) "cortar" else "copiar"})", style = style)
            }
            Spacer(Modifier.weight(1f))
            if (free != null && free.second > 0) {
                Text("${Fmt.size(free.first)} libres de ${Fmt.size(free.second)}", style = style)
                Spacer(Modifier.width(12.dp))
            }
            StatusViewToggle(Icons.AutoMirrored.Filled.ViewList, vm.viewMode == ViewMode.DETAILS) { vm.setView(ViewMode.DETAILS) }
            StatusViewToggle(Icons.Filled.GridView, vm.viewMode == ViewMode.LARGE_ICONS) { vm.setView(ViewMode.LARGE_ICONS) }
        }
    }
}

@Composable
private fun StatusViewToggle(icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (active) selectionBg else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ======================= Diálogos =======================

@Composable
private fun Dialogs(d: DialogReq, vm: ExplorerViewModel, dismiss: () -> Unit) {
    when (d) {
        is DialogReq.Rename -> {
            val name = d.item.name
            val dot = if (d.item.isDir) -1 else name.lastIndexOf('.')
            NameDialog(
                title = "Cambiar nombre",
                initial = TextFieldValue(name, TextRange(0, if (dot > 0) dot else name.length)),
                confirm = "Cambiar nombre",
                onConfirm = { vm.rename(d.item, it); dismiss() },
                onDismiss = dismiss,
            )
        }
        DialogReq.NewFolder -> {
            val n = remember { vm.suggestedFolderName() }
            NameDialog(
                title = "Nueva carpeta",
                initial = TextFieldValue(n, TextRange(0, n.length)),
                confirm = "Crear",
                onConfirm = { vm.newFolder(it); dismiss() },
                onDismiss = dismiss,
            )
        }
        is DialogReq.Delete -> {
            val items = d.items
            val text = if (items.size == 1) "¿Seguro que quieres eliminar permanentemente «${items[0].name}»?"
            else "¿Seguro que quieres eliminar permanentemente estos ${items.size} elementos?"
            AlertDialog(
                onDismissRequest = dismiss,
                title = { Text("Eliminar") },
                text = { Text("$text\n\nEsta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(onClick = { vm.delete(items); dismiss() }) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = dismiss) { Text("Cancelar") } },
            )
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: TextFieldValue,
    confirm: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (value.text.isNotBlank()) onConfirm(value.text) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.text) }, enabled = value.text.isNotBlank()) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Utilidad: nombre visible de una ruta. */
fun displayName(path: String): String = File(path).name.ifEmpty { path }
