package com.jarvis.explorador.ui

import androidx.activity.compose.BackHandler
import android.app.Activity
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Unarchive
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.explorador.data.Category
import com.jarvis.explorador.data.Density
import com.jarvis.explorador.data.FileItem
import com.jarvis.explorador.data.Fmt
import com.jarvis.explorador.data.GroupBy
import com.jarvis.explorador.data.NaturalOrder
import com.jarvis.explorador.data.SortField
import com.jarvis.explorador.data.ViewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed interface DialogReq {
    data class Rename(val item: FileItem) : DialogReq
    data object NewFolder : DialogReq
    data class Delete(val items: List<FileItem>) : DialogReq
    data class PickFolder(val items: List<FileItem>, val move: Boolean) : DialogReq
    data class CloseDoc(val pane: Pane, val index: Int) : DialogReq
}

/** Cierra una pestaña de documento pidiendo confirmación si hay cambios. */
fun requestCloseDoc(vm: ExplorerViewModel, pane: Pane, i: Int, dialog: (DialogReq) -> Unit) {
    val d = pane.docs.getOrNull(i) ?: return
    if (d.dirty) dialog(DialogReq.CloseDoc(pane, i)) else vm.closeDoc(pane, i)
}

@Composable
fun ExplorerScreen(vm: ExplorerViewModel) {
    if (!vm.started) return
    val s = vm.s
    val snackbar = remember { SnackbarHostState() }
    var dialog by remember { mutableStateOf<DialogReq?>(null) }
    val rootFocus = remember { FocusRequester() }
    val tab = vm.tab

    LaunchedEffect(s.message) {
        val m = s.message ?: return@LaunchedEffect
        s.message = null
        snackbar.showSnackbar(m)
    }
    LaunchedEffect(Unit) { runCatching { rootFocus.requestFocus() } }

    BackHandler(enabled = vm.editorActive || tab.selectionMode || tab.searchResults != null || tab.canBack) {
        when {
            vm.editorActive -> requestCloseDoc(vm, vm.pane, vm.pane.activeDoc ?: 0) { dialog = it }
            tab.selectionMode -> tab.setSelectionModeOn(false)
            tab.searchResults != null -> tab.cancelSearch()
            else -> tab.goBack()
        }
    }

    val requestDialog: (DialogReq) -> Unit = { dialog = it }
    val onInteract: () -> Unit = { runCatching { rootFocus.requestFocus() } }

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
            CommandBar(vm, requestDialog)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.weight(1f).fillMaxWidth()) {
                if (s.showNav) {
                    NavPane(tab, Modifier.width(s.navWidth.dp).fillMaxHeight())
                    Splitter { s.dragNav(it) }
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    PanesArea(vm, requestDialog, onInteract)
                }
                if (s.showPreview) {
                    Splitter { s.dragPreview(-it) }
                    PreviewPane(tab, Modifier.width(s.previewWidth.dp).fillMaxHeight())
                }
            }
            StatusBar(vm)
        }
    }

    dialog?.let { d -> Dialogs(d, vm) { dialog = null } }
}

private fun handleKey(vm: ExplorerViewModel, e: KeyEvent, dialog: (DialogReq) -> Unit): Boolean {
    if (e.type != KeyEventType.KeyDown) return false
    val tab = vm.tab
    val ctrl = e.isCtrlPressed
    if (vm.editorActive) {
        // En el editor solo se atienden atajos globales; el resto es para el texto.
        when {
            ctrl && e.key == Key.W -> requestCloseDoc(vm, vm.pane, vm.pane.activeDoc ?: 0, dialog)
            ctrl && e.key == Key.Tab -> vm.nextTab(if (e.isShiftPressed) -1 else 1)
            e.key == Key.F6 -> vm.switchPane()
            else -> return false
        }
        return true
    }
    when {
        ctrl && e.key == Key.Tab -> vm.nextTab(if (e.isShiftPressed) -1 else 1)
        ctrl && e.key == Key.T -> vm.newTab(vm.pane, tab.currentDir)
        ctrl && e.key == Key.W -> vm.closeTab(vm.pane, vm.pane.active)
        ctrl && e.key == Key.N && !e.isShiftPressed -> tab.currentDir?.let { tab.openInNewWindow(it) }
        ctrl && e.key == Key.A -> tab.selectAll()
        ctrl && e.key == Key.C -> tab.copySelection(false)
        ctrl && e.key == Key.X -> tab.copySelection(true)
        ctrl && e.key == Key.V -> tab.paste()
        ctrl && e.isShiftPressed && e.key == Key.N -> dialog(DialogReq.NewFolder)
        e.key == Key.F6 -> vm.switchPane()
        e.key == Key.Delete -> tab.selectedItems().takeIf { it.isNotEmpty() }?.let { dialog(DialogReq.Delete(it)) }
        e.key == Key.F2 -> tab.focused?.let { dialog(DialogReq.Rename(it)) }
        e.key == Key.F5 -> tab.refresh()
        e.key == Key.Backspace -> tab.goBack()
        e.isAltPressed && e.key == Key.DirectionLeft -> tab.goBack()
        e.isAltPressed && e.key == Key.DirectionRight -> tab.goForward()
        e.isAltPressed && e.key == Key.DirectionUp -> tab.goUp()
        e.isAltPressed && e.key == Key.P -> vm.s.togglePreview()
        e.key == Key.DirectionDown -> tab.moveFocus(1, e.isShiftPressed)
        e.key == Key.DirectionUp -> tab.moveFocus(-1, e.isShiftPressed)
        e.key == Key.Enter -> tab.focused?.let { tab.open(it) }
        e.key == Key.Escape -> if (tab.selectionMode) tab.setSelectionModeOn(false) else tab.clearSelection()
        else -> return false
    }
    return true
}

@Composable
fun Splitter(vertical: Boolean = true, onDragDp: (Float) -> Unit) {
    val density = LocalDensity.current
    val cb by rememberUpdatedState(onDragDp)
    val state = rememberDraggableState { px -> cb(with(density) { px.toDp().value }) }
    val mod = if (vertical) Modifier.fillMaxHeight().width(9.dp) else Modifier.fillMaxWidth().height(9.dp)
    Box(
        mod
            .background(MaterialTheme.colorScheme.background)
            .draggable(state, if (vertical) Orientation.Horizontal else Orientation.Vertical),
        contentAlignment = Alignment.Center,
    ) {
        val handle = if (vertical) Modifier.height(32.dp).width(3.dp) else Modifier.width(32.dp).height(3.dp)
        Box(handle.clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
    }
}

// ======================= Paneles y pestañas =======================

@Composable
private fun PanesArea(vm: ExplorerViewModel, dialog: (DialogReq) -> Unit, onInteract: () -> Unit) {
    when (vm.layout) {
        PaneLayout.SINGLE -> PaneView(vm, 0, Modifier.fillMaxSize(), dialog, onInteract)
        PaneLayout.SIDE_BY_SIDE -> BoxWithConstraints(Modifier.fillMaxSize()) {
            val total = maxWidth.value
            Row(Modifier.fillMaxSize()) {
                PaneView(vm, 0, Modifier.weight(vm.splitRatio).fillMaxHeight(), dialog, onInteract)
                Splitter { vm.dragSplit(it / total) }
                PaneView(vm, 1, Modifier.weight(1f - vm.splitRatio).fillMaxHeight(), dialog, onInteract)
            }
        }
        PaneLayout.STACKED -> BoxWithConstraints(Modifier.fillMaxSize()) {
            val total = maxHeight.value
            Column(Modifier.fillMaxSize()) {
                PaneView(vm, 0, Modifier.weight(vm.splitRatio).fillMaxWidth(), dialog, onInteract)
                Splitter(vertical = false) { vm.dragSplit(it / total) }
                PaneView(vm, 1, Modifier.weight(1f - vm.splitRatio).fillMaxWidth(), dialog, onInteract)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PaneView(
    vm: ExplorerViewModel,
    index: Int,
    modifier: Modifier,
    dialog: (DialogReq) -> Unit,
    onInteract: () -> Unit,
) {
    val pane = vm.panes[index]
    val highlight = vm.isDual && vm.activePane == index
    val tab = pane.tab
    val doc = pane.doc
    val activity = LocalContext.current as? Activity
    var dropHover by remember { mutableStateOf(false) }
    val target = remember(pane) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                dropHover = false
                return handleDrop(pane, event, activity)
            }
            override fun onEntered(event: DragAndDropEvent) { dropHover = true }
            override fun onExited(event: DragAndDropEvent) { dropHover = false }
            override fun onEnded(event: DragAndDropEvent) { dropHover = false }
        }
    }
    Column(
        modifier
            .pointerInput(index) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    vm.activatePane(index)
                }
            }
            .dragAndDropTarget(shouldStartDragAndDrop = { true }, target = target)
            .border(if (dropHover) 2.dp else 0.dp, if (dropHover) MaterialTheme.colorScheme.primary else Color.Transparent)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(if (highlight) MaterialTheme.colorScheme.primary else Color.Transparent)
        )
        TabStrip(vm, pane, dialog)
        if (doc != null) {
            key(doc.id) { EditorView(doc, vm.s, onSave = { vm.saveDoc(doc) }, Modifier.weight(1f).fillMaxWidth()) }
        } else {
            key(tab.id) {
                AddressBar(tab)
                Box(Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
                    ContentArea(tab, dialog, onInteract)
                }
            }
        }
    }
}

/** Soltar en un panel: desde este explorador mueve/copia; desde otra app copia los archivos. */
@OptIn(ExperimentalFoundationApi::class)
private fun handleDrop(pane: Pane, event: DragAndDropEvent, activity: Activity?): Boolean {
    val ev = event.toAndroidDragEvent()
    val tab = pane.tab
    val base = tab.currentDir ?: return false
    val dest = if (pane.doc == null) tab.itemAt(Offset(ev.x, ev.y))?.takeIf { it.isDir }?.file ?: base else base
    val local = ev.localState as? DragPayload
    if (local != null) {
        tab.dropPaths(local.paths, dest)
        return true
    }
    activity?.requestDragAndDropPermissions(ev)
    val clip = ev.clipData ?: return false
    val uris = (0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri }
    if (uris.isEmpty()) return false
    tab.importUris(uris, dest)
    return true
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabStrip(vm: ExplorerViewModel, pane: Pane, dialog: (DialogReq) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Row(Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.Bottom) {
            pane.tabs.forEachIndexed { i, t ->
                val selected = i == pane.active && pane.activeDoc == null
                var menu by remember { mutableStateOf(false) }
                Box {
                    Row(
                        Modifier
                            .height(30.dp)
                            .widthIn(max = 200.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .combinedClickable(
                                onClick = { vm.selectTab(pane, i) },
                                onLongClick = { vm.selectTab(pane, i); menu = true },
                            )
                            .padding(start = 10.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (t.searchResults != null) Icons.Filled.Search else Icons.Filled.Folder, null,
                            tint = if (t.searchResults != null) MaterialTheme.colorScheme.primary else FolderColor,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            t.title, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (pane.tabs.size > 1) {
                            Box(
                                Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).clickable { vm.closeTab(pane, i) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.Close, "Cerrar pestaña", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else Spacer(Modifier.width(8.dp))
                    }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Duplicar pestaña") }, onClick = { menu = false; vm.newTab(pane, t.currentDir) })
                        DropdownMenuItem(text = { Text("Mover al otro panel") }, onClick = { menu = false; vm.moveTabToOtherPane(pane, i) })
                        DropdownMenuItem(
                            text = { Text("Abrir en nueva ventana") },
                            onClick = { menu = false; t.currentDir?.let { t.openInNewWindow(it) } },
                        )
                        if (pane.tabs.size > 1) {
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Cerrar otras pestañas") }, onClick = { menu = false; vm.closeOtherTabs(pane, i) })
                            DropdownMenuItem(text = { Text("Cerrar pestaña") }, onClick = { menu = false; vm.closeTab(pane, i) })
                        }
                    }
                }
            }
        }
        if (pane.docs.isNotEmpty()) {
            Row(Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.Bottom) {
                pane.docs.forEachIndexed { i, d ->
                    val selected = pane.activeDoc == i
                    Row(
                        Modifier
                            .padding(start = 2.dp)
                            .height(30.dp)
                            .widthIn(max = 220.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { vm.selectDoc(pane, i) }
                            .padding(start = 10.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Description, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            d.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (d.dirty) {
                            Spacer(Modifier.width(5.dp))
                            Box(Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary))
                        }
                        Box(
                            Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).clickable { requestCloseDoc(vm, pane, i, dialog) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Close, "Cerrar documento", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Box(
            Modifier.padding(bottom = 2.dp).size(30.dp).clip(RoundedCornerShape(6.dp)).clickable { vm.newTab(pane, pane.tab.currentDir) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, "Nueva pestaña", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ======================= Barra de direcciones (por pestaña) =======================

@Composable
private fun AddressBar(tab: ExplorerTab) {
    BoxWithConstraints(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        val compact = maxWidth < 640.dp
        Row(
            Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavIcon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tab.canBack) { tab.goBack() }
            if (!compact) NavIcon(Icons.AutoMirrored.Filled.ArrowForward, "Adelante", tab.canForward) { tab.goForward() }
            NavIcon(Icons.Filled.ArrowUpward, "Subir un nivel", tab.canUp) { tab.goUp() }
            if (!compact) NavIcon(Icons.Filled.Refresh, "Actualizar", true) { tab.refresh() }
            Spacer(Modifier.width(4.dp))
            Breadcrumb(tab, Modifier.weight(1f))
            Spacer(Modifier.width(6.dp))
            SearchBox(tab, Modifier.width(if (compact) 150.dp else 240.dp))
        }
    }
}

@Composable
private fun NavIcon(icon: ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.85f else 0.3f)
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(19.dp)) }
}

@Composable
private fun Breadcrumb(vm: ExplorerTab, modifier: Modifier) {
    var editing by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    val boxMod = modifier
        .height(32.dp)
        .clip(shape)
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)

    if (vm.searchResults != null) {
        Row(boxMod.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(
                "Resultados de búsqueda en ${vm.currentDir?.name?.ifEmpty { "/" } ?: ""}",
                fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.Close, "Cerrar búsqueda", Modifier.size(18.dp).clickable { vm.cancelSearch() }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

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
            Icon(Icons.Filled.Close, "Cancelar", Modifier.size(18.dp).clickable { editing = false }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        val crumbs = vm.breadcrumbs()
        val state = rememberLazyListState()
        LaunchedEffect(crumbs.size, vm.currentDir) { if (crumbs.isNotEmpty()) state.scrollToItem(crumbs.lastIndex) }
        Row(boxMod.clickable { editing = true }, verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(6.dp))
            Icon(iconFor(Category.FOLDER), null, tint = FolderColor, modifier = Modifier.size(17.dp))
            LazyRow(state = state, modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                itemsIndexed(crumbs) { i, (label, file) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                        Text(
                            label,
                            fontSize = 14.sp,
                            fontWeight = if (i == crumbs.lastIndex) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { vm.navigate(file) }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBox(vm: ExplorerTab, modifier: Modifier) {
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
                    .height(32.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (vm.filter.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, shape)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Icon(Icons.Filled.Close, "Borrar", Modifier.size(17.dp).clickable { vm.setFilterText("") }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
    )
}

// ======================= Barra de comandos (global) =======================

@Composable
private fun CommandBar(vm: ExplorerViewModel, dialog: (DialogReq) -> Unit) {
    vm.pane.doc?.let { d -> EditorCommandBar(vm, d, dialog); return }
    val tab = vm.tab
    val sel = tab.selection
    val hasSel = sel.isNotEmpty()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val labels = maxWidth > 1250.dp
        Row(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolButton(Icons.Filled.CreateNewFolder, "Nueva carpeta", labels) { dialog(DialogReq.NewFolder) }
            BarDivider()
            ToolButton(Icons.Filled.ContentCut, "Cortar", labels, hasSel) { tab.copySelection(true) }
            ToolButton(Icons.Filled.ContentCopy, "Copiar", labels, hasSel) { tab.copySelection(false) }
            ToolButton(Icons.Filled.ContentPaste, "Pegar", labels, tab.clipboard.isNotEmpty()) { tab.paste() }
            ToolButton(Icons.Filled.DriveFileRenameOutline, "Renombrar", labels, sel.size == 1) {
                tab.selectedItems().firstOrNull()?.let { dialog(DialogReq.Rename(it)) }
            }
            ToolButton(Icons.Filled.DriveFileMove, "Mover a…", labels, hasSel) { dialog(DialogReq.PickFolder(tab.selectedItems(), true)) }
            ToolButton(Icons.Filled.Share, "Compartir", labels, hasSel) { tab.share(tab.selectedItems()) }
            ToolButton(Icons.Filled.Delete, "Eliminar", labels, hasSel) { dialog(DialogReq.Delete(tab.selectedItems())) }
            if (vm.isDual) {
                BarDivider()
                ToolButton(Icons.Filled.FileCopy, "Copiar al otro panel", true, hasSel) { vm.transferToOtherPane(false) }
                ToolButton(Icons.Filled.DriveFileMove, "Mover al otro panel", true, hasSel) { vm.transferToOtherPane(true) }
            }
            BarDivider()
            SortMenu(vm)
            GroupMenu(vm)
            ViewMenu(vm)
            PanesMenu(vm)
            BarDivider()
            SelectMenu(tab, labels)
            ToolButton(Icons.Filled.VerticalSplit, "Vista previa", false, true, active = vm.s.showPreview) { vm.s.togglePreview() }
            MoreMenu(vm, dialog)
        }
    }
}

@Composable
private fun EditorCommandBar(vm: ExplorerViewModel, d: EditorDoc, dialog: (DialogReq) -> Unit) {
    val s = vm.s
    val pane = vm.pane
    val item = remember(d.file.path) { com.jarvis.explorador.data.FileOps.toItem(d.file) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolButton(Icons.Filled.Save, "Guardar", true, d.dirty) { vm.saveDoc(d) }
        ToolButton(Icons.AutoMirrored.Filled.Undo, "Deshacer", false, d.undoCount > 0) { d.undo() }
        ToolButton(Icons.AutoMirrored.Filled.Redo, "Rehacer", false, d.redoCount > 0) { d.redo() }
        BarDivider()
        ToolButton(Icons.Filled.Search, "Buscar", true, active = d.findOpen) { d.findOpen = !d.findOpen }
        ToolButton(Icons.AutoMirrored.Filled.WrapText, "Ajuste de línea", true, active = s.editorWrap) { s.toggleEditorWrap() }
        ToolButton(Icons.Filled.TextDecrease, "Letra más pequeña", false) { s.setEditorFont(s.editorFont - 1) }
        ToolButton(Icons.Filled.TextIncrease, "Letra más grande", false) { s.setEditorFont(s.editorFont + 1) }
        BarDivider()
        ToolButton(Icons.Filled.Apps, "Abrir con…", true) { vm.tab.openWith(item) }
        ToolButton(Icons.Filled.Share, "Compartir", true) { vm.tab.share(listOf(item)) }
        ToolButton(Icons.Filled.FolderOpen, "Mostrar carpeta", true) {
            pane.activeDoc = null
            d.file.parentFile?.let { vm.tab.navigate(it, select = d.file.path) }
        }
        BarDivider()
        ToolButton(Icons.Filled.Close, "Cerrar", true) { requestCloseDoc(vm, pane, pane.activeDoc ?: 0, dialog) }
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
        Icon(icon, label, tint = if (enabled) MaterialTheme.colorScheme.primary else c, modifier = Modifier.size(20.dp))
        if (showLabel) {
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 13.sp, color = c, maxLines = 1)
        }
        if (trailing) {
            Icon(Icons.Filled.ChevronRight, null, tint = c, modifier = Modifier.size(14.dp).padding(start = 2.dp))
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
    val s = vm.s
    var open by remember { mutableStateOf(false) }
    Box {
        ToolButton(Icons.AutoMirrored.Filled.Sort, "Ordenar", true, trailing = true) { open = true }
        DropdownMenu(open, { open = false }) {
            MenuHeader("Ordenar por")
            SortField.entries
                .filter { it != SortField.LOCATION || vm.tab.searchResults != null }
                .forEach { f -> CheckItem(f.label, s.sortField == f) { s.sortBy(f); open = false } }
            HorizontalDivider()
            CheckItem("Ascendente", s.sortAsc) { s.setSortDirection(true); open = false }
            CheckItem("Descendente", !s.sortAsc) { s.setSortDirection(false); open = false }
        }
    }
}

@Composable
private fun GroupMenu(vm: ExplorerViewModel) {
    val s = vm.s
    var open by remember { mutableStateOf(false) }
    Box {
        ToolButton(Icons.Filled.Workspaces, "Agrupar", true, active = s.groupBy != GroupBy.NONE, trailing = true) { open = true }
        DropdownMenu(open, { open = false }) {
            MenuHeader("Agrupar por")
            GroupBy.entries.forEach { g -> CheckItem(g.label, s.groupBy == g) { s.setGrouping(g); open = false } }
            if (s.groupBy != GroupBy.NONE) {
                HorizontalDivider()
                CheckItem("Contraer todos los grupos", false) { vm.tab.collapseAll(); open = false }
                CheckItem("Expandir todos los grupos", false) { vm.tab.expandAll(); open = false }
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
    val s = vm.s
    var open by remember { mutableStateOf(false) }
    Box {
        ToolButton(viewIcon(s.viewMode), "Ver", true, trailing = true) { open = true }
        DropdownMenu(open, { open = false }) {
            MenuHeader("Diseño")
            ViewMode.entries.forEach { m -> CheckItem(m.label, s.viewMode == m) { s.setView(m); open = false } }
            HorizontalDivider()
            MenuHeader("Densidad de filas")
            Density.entries.forEach { d -> CheckItem("${d.label} (${d.rowDp} dp)", s.density == d) { s.setRowDensity(d) } }
            HorizontalDivider()
            MenuHeader("Columnas (vista Detalles)")
            CheckItem("Fecha de modificación", s.showColDate) { s.toggleColumn(SortField.DATE) }
            CheckItem("Tipo", s.showColType) { s.toggleColumn(SortField.TYPE) }
            CheckItem("Tamaño", s.showColSize) { s.toggleColumn(SortField.SIZE) }
            HorizontalDivider()
            MenuHeader("Mostrar")
            CheckItem("Panel de navegación", s.showNav) { s.toggleNav() }
            CheckItem("Panel de vista previa", s.showPreview) { s.togglePreview() }
            CheckItem("Elementos ocultos", s.showHidden) { s.toggleHidden() }
            CheckItem("Casillas de selección", vm.tab.selectionMode) { vm.tab.setSelectionModeOn(!vm.tab.selectionMode) }
        }
    }
}

@Composable
private fun PanesMenu(vm: ExplorerViewModel) {
    var open by remember { mutableStateOf(false) }
    Box {
        ToolButton(Icons.Filled.VerticalSplit, "Paneles", true, active = vm.isDual, trailing = true) { open = true }
        DropdownMenu(open, { open = false }) {
            MenuHeader("Distribución")
            PaneLayout.entries.forEach { l -> CheckItem(l.label, vm.layout == l) { vm.setPaneLayout(l); open = false } }
            if (vm.isDual) {
                CheckItem("Intercambiar paneles", false) { vm.swapPanes(); open = false }
                CheckItem("Cambiar de panel (F6)", false) { vm.switchPane(); open = false }
            }
            HorizontalDivider()
            MenuHeader("Pestañas y ventanas")
            CheckItem("Nueva pestaña (Ctrl+T)", false) { vm.newTab(vm.pane, vm.tab.currentDir); open = false }
            DropdownMenuItem(
                text = { Text("Nueva ventana al lado (Ctrl+N)", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Filled.OpenInBrowser, null, Modifier.size(18.dp)) },
                onClick = { open = false; vm.tab.currentDir?.let { vm.tab.openInNewWindow(it) } },
                modifier = Modifier.height(38.dp),
            )
            Text(
                "La nueva ventana se coloca al lado en pantalla dividida; arrastra el divisor de Android para ajustarlas.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).width(300.dp),
            )
        }
    }
}

@Composable
private fun SelectMenu(vm: ExplorerTab, labels: Boolean) {
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
private fun MoreMenu(vm: ExplorerViewModel, dialog: (DialogReq) -> Unit) {
    val s = vm.s
    val tab = vm.tab
    var open by remember { mutableStateOf(false) }
    val sel = tab.selectedItems()
    Box {
        ToolButton(Icons.Filled.MoreHoriz, "Más", false) { open = true }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem(
                text = { Text("Comprimir en ZIP") },
                leadingIcon = { Icon(Icons.Filled.FolderZip, null) },
                enabled = sel.isNotEmpty(),
                onClick = { open = false; tab.compress(sel) },
            )
            DropdownMenuItem(
                text = { Text("Extraer todo") },
                leadingIcon = { Icon(Icons.Filled.Unarchive, null) },
                enabled = sel.size == 1 && sel[0].extension == "zip",
                onClick = { open = false; tab.extract(sel[0]) },
            )
            DropdownMenuItem(
                text = { Text("Copiar a…") },
                leadingIcon = { Icon(Icons.Filled.FileCopy, null) },
                enabled = sel.isNotEmpty(),
                onClick = { open = false; dialog(DialogReq.PickFolder(sel, false)) },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Anclar carpeta actual a Acceso rápido") },
                leadingIcon = { Icon(Icons.Filled.PushPin, null) },
                onClick = { tab.currentDir?.let { s.pin(it.path) }; open = false },
            )
            DropdownMenuItem(
                text = { Text(if (s.showHidden) "Ocultar elementos ocultos" else "Mostrar elementos ocultos") },
                leadingIcon = { Icon(Icons.Filled.Visibility, null) },
                onClick = { s.toggleHidden(); open = false },
            )
            DropdownMenuItem(
                text = { Text("Intercambiar paneles") },
                leadingIcon = { Icon(Icons.Filled.SwapHoriz, null) },
                enabled = vm.isDual,
                onClick = { vm.swapPanes(); open = false },
            )
            HorizontalDivider()
            CheckItem("Un toque abre carpetas", s.tapOpensFolders) { s.toggleTapMode() }
            Text(
                if (s.tapOpensFolders) "Archivos: un toque selecciona (vista previa), doble toque abre."
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
    val tab = vm.tab
    val s = vm.s
    val selItems = remember(tab.selection, tab.currentItems) { tab.selectedItems() }
    val selBytes = remember(selItems) { selItems.filter { !it.isDir }.sumOf { it.size } }
    val free = remember(tab.currentDir, s.freeSpaceTick) {
        tab.currentDir?.let { d -> runCatching { d.usableSpace to d.totalSpace }.getOrNull() }
    }
    Column {
        if (s.busy != null || tab.searching) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (vm.isDual) Text(if (vm.activePane == 0) "Panel 1  ·  " else "Panel 2  ·  ", style = style)
            Text("${tab.currentItems.size} elementos", style = style)
            if (selItems.isNotEmpty()) {
                Text(
                    "     ${selItems.size} seleccionado${if (selItems.size == 1) "" else "s"}" +
                        if (selBytes > 0) "   ${Fmt.size(selBytes)}" else "",
                    style = style,
                )
            }
            s.busy?.let { Text("     $it", style = style) }
            if (tab.searching) Text("     Buscando en subcarpetas…", style = style)
            if (s.clipboard.isNotEmpty()) {
                Text("     Portapapeles: ${s.clipboard.size} (${if (s.clipCut) "cortar" else "copiar"})", style = style)
            }
            Spacer(Modifier.weight(1f))
            if (free != null && free.second > 0) {
                Text("${Fmt.size(free.first)} libres de ${Fmt.size(free.second)}", style = style)
                Spacer(Modifier.width(12.dp))
            }
            StatusViewToggle(Icons.AutoMirrored.Filled.ViewList, s.viewMode == ViewMode.DETAILS) { s.setView(ViewMode.DETAILS) }
            StatusViewToggle(Icons.Filled.GridView, s.viewMode == ViewMode.LARGE_ICONS) { s.setView(ViewMode.LARGE_ICONS) }
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
    val tab = vm.tab
    when (d) {
        is DialogReq.Rename -> {
            val name = d.item.name
            val dot = if (d.item.isDir) -1 else name.lastIndexOf('.')
            NameDialog(
                title = "Cambiar nombre",
                initial = TextFieldValue(name, TextRange(0, if (dot > 0) dot else name.length)),
                confirm = "Cambiar nombre",
                onConfirm = { tab.rename(d.item, it); dismiss() },
                onDismiss = dismiss,
            )
        }
        DialogReq.NewFolder -> {
            val n = remember { tab.suggestedFolderName() }
            NameDialog(
                title = "Nueva carpeta",
                initial = TextFieldValue(n, TextRange(0, n.length)),
                confirm = "Crear",
                onConfirm = { tab.newFolder(it); dismiss() },
                onDismiss = dismiss,
            )
        }
        is DialogReq.Delete -> {
            val items = d.items
            if (items.isEmpty()) { dismiss(); return }
            val text = if (items.size == 1) "¿Seguro que quieres eliminar permanentemente «${items[0].name}»?"
            else "¿Seguro que quieres eliminar permanentemente estos ${items.size} elementos?"
            AlertDialog(
                onDismissRequest = dismiss,
                title = { Text("Eliminar") },
                text = { Text("$text\n\nEsta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(onClick = { tab.delete(items); dismiss() }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = dismiss) { Text("Cancelar") } },
            )
        }
        is DialogReq.CloseDoc -> {
            val doc = d.pane.docs.getOrNull(d.index)
            if (doc == null) { dismiss(); return }
            AlertDialog(
                onDismissRequest = dismiss,
                title = { Text("¿Guardar los cambios?") },
                text = { Text("«${doc.name}» tiene cambios sin guardar.") },
                confirmButton = {
                    TextButton(onClick = {
                        if (vm.saveDoc(doc)) vm.closeDoc(d.pane, d.index)
                        dismiss()
                    }) { Text("Guardar") }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { vm.closeDoc(d.pane, d.index); dismiss() }) {
                            Text("Descartar", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = dismiss) { Text("Cancelar") }
                    }
                },
            )
        }
        is DialogReq.PickFolder -> {
            if (d.items.isEmpty()) { dismiss(); return }
            FolderPicker(
                vm = vm,
                title = "${if (d.move) "Mover" else "Copiar"} ${d.items.size} elemento(s) a…",
                confirm = if (d.move) "Mover aquí" else "Copiar aquí",
                onPick = { dest -> tab.transfer(d.items.map { it.file }, dest, d.move); dismiss() },
                onDismiss = dismiss,
            )
        }
    }
}

@Composable
private fun FolderPicker(
    vm: ExplorerViewModel,
    title: String,
    confirm: String,
    onPick: (File) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = vm.s
    var dir by remember { mutableStateOf(vm.otherPane?.tab?.currentDir ?: vm.tab.currentDir ?: File(s.roots.first().path)) }
    var creating by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    val kids by produceState<List<File>>(emptyList(), dir, refresh) {
        value = withContext(Dispatchers.IO) {
            dir.listFiles()?.filter { it.isDirectory && (s.showHidden || !it.isHidden) }
                ?.sortedWith(compareBy(NaturalOrder) { it.name }) ?: emptyList()
        }
    }
    val isRoot = s.roots.any { it.path == dir.path }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.width(520.dp)) {
                Text(dir.path, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    s.roots.forEach { r -> TextButton(onClick = { dir = File(r.path) }) { Text(r.label) } }
                    vm.otherPane?.tab?.currentDir?.let { o -> TextButton(onClick = { dir = o }) { Text("Otro panel") } }
                    s.pins.forEach { p -> TextButton(onClick = { dir = File(p) }) { Text(displayName(p)) } }
                }
                HorizontalDivider()
                LazyColumn(Modifier.heightIn(max = 340.dp)) {
                    if (!isRoot) {
                        item {
                            PickerRow("..  (subir)") { dir.parentFile?.let { dir = it } }
                        }
                    }
                    items(kids, key = { it.path }) { k -> PickerRow(k.name) { dir = k } }
                }
                if (kids.isEmpty()) Text("Sin subcarpetas", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
            }
        },
        confirmButton = { TextButton(onClick = { onPick(dir) }) { Text(confirm) } },
        dismissButton = {
            Row {
                TextButton(onClick = { creating = true }) { Text("Nueva carpeta") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
    if (creating) {
        val n = remember(dir) { com.jarvis.explorador.data.FileOps.uniqueNewFolderName(dir) }
        NameDialog(
            title = "Nueva carpeta",
            initial = TextFieldValue(n, TextRange(0, n.length)),
            confirm = "Crear",
            onConfirm = { name ->
                creating = false
                runCatching { com.jarvis.explorador.data.FileOps.mkdir(dir, name.trim()) }
                    .onSuccess { dir = it }
                    .onFailure { s.message = it.message }
                refresh++
            },
            onDismiss = { creating = false },
        )
    }
}

@Composable
private fun PickerRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Folder, null, tint = FolderColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        confirmButton = { TextButton(onClick = { onConfirm(value.text) }, enabled = value.text.isNotBlank()) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Utilidad: nombre visible de una ruta. */
fun displayName(path: String): String = File(path).name.ifEmpty { path }
