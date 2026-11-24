package com.jarvis.explorador.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jarvis.explorador.data.Category
import com.jarvis.explorador.data.FileItem
import com.jarvis.explorador.data.Fmt
import com.jarvis.explorador.data.GroupBy
import com.jarvis.explorador.data.ItemGroup
import com.jarvis.explorador.data.SortField
import com.jarvis.explorador.data.ViewMode
import java.io.File

/** Estado compartido por todas las vistas para el menú contextual. */
private class ItemCallbacks(
    val vm: ExplorerTab,
    val menuFor: String?,
    val setMenu: (String?) -> Unit,
    val dialog: (DialogReq) -> Unit,
    val onInteract: () -> Unit,
)

@Composable
fun ContentArea(vm: ExplorerTab, dialog: (DialogReq) -> Unit, onInteract: () -> Unit) {
    var menuFor by remember { mutableStateOf<String?>(null) }
    val cb = ItemCallbacks(vm, menuFor, { menuFor = it }, dialog, onInteract)

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { vm.clearSelection(); onInteract() }) }
    ) {
        when {
            vm.loading && vm.currentItems.isEmpty() ->
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            vm.error != null -> CenterMessage(vm.error!!)
            vm.currentItems.isEmpty() && !vm.searching -> CenterMessage(
                when {
                    vm.searchResults != null -> "No hay elementos que coincidan con la búsqueda."
                    vm.filter.isNotBlank() -> "Ningún elemento coincide con «${vm.filter}».\nPulsa la lupa para buscar también en subcarpetas."
                    else -> "Esta carpeta está vacía."
                }
            )
            else -> when (vm.viewMode) {
                ViewMode.DETAILS -> DetailsView(cb)
                ViewMode.LIST -> GridItems(cb, 250.dp) { ListCell(it, cb) }
                ViewMode.TILES -> GridItems(cb, 300.dp) { TileCell(it, cb) }
                ViewMode.MEDIUM_ICONS -> GridItems(cb, 112.dp) { IconCell(it, cb, 72.dp) }
                ViewMode.LARGE_ICONS -> GridItems(cb, 180.dp) { IconCell(it, cb, 140.dp) }
            }
        }
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.itemGestures(item: FileItem, cb: ItemCallbacks): Modifier {
    val vm = cb.vm
    val singleTapOpens = item.isDir && vm.tapOpensFolders && !vm.selectionMode
    return combinedClickable(
        onClick = { cb.onInteract(); vm.onTap(item) },
        onDoubleClick = if (singleTapOpens) null else ({ vm.onDoubleTap(item) }),
        onLongClick = { cb.onInteract(); vm.onLongPress(item); cb.setMenu(item.path) },
    )
}

@Composable
private fun rowBackground(item: FileItem, vm: ExplorerTab): Color = when {
    item.path in vm.selection && vm.focused?.path == item.path -> focusBg
    item.path in vm.selection -> selectionBg
    else -> Color.Transparent
}

private fun itemAlpha(item: FileItem, vm: ExplorerTab): Float = when {
    vm.clipCut && item.path in vm.clipboard -> 0.45f
    item.hidden -> 0.6f
    else -> 1f
}

// ======================= Detalles =======================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailsView(cb: ItemCallbacks) {
    val vm = cb.vm
    val state = rememberLazyListState()
    val grouped = vm.groupBy != GroupBy.NONE
    val rowH = vm.density.rowDp.dp

    // Mantener visible el elemento enfocado (navegación con teclado).
    LaunchedEffect(vm.focused?.path) {
        val target = vm.focused?.path ?: return@LaunchedEffect
        var idx = 0
        var found = -1
        for (g in vm.groups) {
            if (grouped) idx++
            if (grouped && g.key in vm.collapsed) continue
            val i = g.items.indexOfFirst { it.path == target }
            if (i >= 0) { found = idx + i; break }
            idx += g.items.size
        }
        if (found >= 0) {
            val info = state.layoutInfo.visibleItemsInfo
            val first = info.firstOrNull()?.index ?: 0
            val last = info.lastOrNull()?.index ?: 0
            if (found <= first || found >= last) state.scrollToItem((found - 2).coerceAtLeast(0))
        }
    }

    Column(Modifier.fillMaxSize()) {
        DetailsHeader(vm)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            vm.groups.forEach { g ->
                if (grouped) {
                    stickyHeader(key = "h:" + g.key, contentType = "header") {
                        GroupHeader(g, g.key in vm.collapsed) { vm.toggleGroup(g.key) }
                    }
                }
                if (!grouped || g.key !in vm.collapsed) {
                    items(g.items, key = { it.path }, contentType = { "row" }) { item ->
                        DetailsRow(item, cb, rowH)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailsHeader(vm: ExplorerTab) {
    val inSearch = vm.searchResults != null
    Row(
        Modifier.fillMaxWidth().height(32.dp).background(MaterialTheme.colorScheme.surface).padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (vm.selectionMode) {
            Box(Modifier.width(30.dp).fillMaxHeight().clickable {
                if (vm.selection.size == vm.visibleItems.size) vm.clearSelection() else vm.selectAll()
            }, contentAlignment = Alignment.CenterStart) {
                Icon(
                    if (vm.selection.isNotEmpty() && vm.selection.size == vm.visibleItems.size) Icons.Filled.CheckBox
                    else Icons.Filled.CheckBoxOutlineBlank,
                    "Seleccionar todo", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        HeaderCell(vm, SortField.NAME, Modifier.weight(1f), resizable = false)
        if (inSearch) HeaderCell(vm, SortField.LOCATION, Modifier.width(vm.colLocation.dp))
        if (vm.showColDate) HeaderCell(vm, SortField.DATE, Modifier.width(vm.colDate.dp))
        if (vm.showColType) HeaderCell(vm, SortField.TYPE, Modifier.width(vm.colType.dp))
        if (vm.showColSize) HeaderCell(vm, SortField.SIZE, Modifier.width(vm.colSize.dp), alignEnd = true)
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun HeaderCell(
    vm: ExplorerTab,
    field: SortField,
    modifier: Modifier,
    resizable: Boolean = true,
    alignEnd: Boolean = false,
) {
    val density = LocalDensity.current
    val active = vm.sortField == field
    val drag = rememberDraggableState { px -> vm.resizeColumn(field, -with(density) { px.toDp().value }) }
    Row(modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
        if (resizable) {
            // Asa para redimensionar (arrastrar hacia la izquierda ensancha la columna).
            Box(
                Modifier.width(10.dp).fillMaxHeight().draggable(drag, Orientation.Horizontal),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.width(1.dp).height(18.dp).background(MaterialTheme.colorScheme.outlineVariant))
            }
        }
        Row(
            Modifier.weight(1f).fillMaxHeight().clickable { vm.sortBy(field) }.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
        ) {
            Text(
                field.label,
                fontSize = 12.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (active) {
                Icon(
                    if (vm.sortAsc) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown, null,
                    Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun DetailsRow(item: FileItem, cb: ItemCallbacks, rowH: Dp) {
    val vm = cb.vm
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val cell = TextStyle(fontSize = 13.sp, color = secondary)
    val iconSize = if (rowH < 32.dp) 18.dp else 22.dp
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .height(rowH)
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(rowBackground(item, vm))
                .itemGestures(item, cb)
                .padding(start = 6.dp, end = 4.dp)
                .alpha(itemAlpha(item, vm)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (vm.selectionMode) SelectBox(item.path in vm.selection, Modifier.width(30.dp))
            Icon(iconFor(item.category), null, tint = colorFor(item.category), modifier = Modifier.size(iconSize))
            Spacer(Modifier.width(8.dp))
            Text(
                item.name,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (vm.searchResults != null) {
                Text(
                    item.parent, style = cell, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(vm.colLocation.dp).padding(horizontal = 12.dp),
                )
            }
            if (vm.showColDate) {
                Text(Fmt.date(item.lastModified), style = cell, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(vm.colDate.dp).padding(horizontal = 14.dp))
            }
            if (vm.showColType) {
                Text(item.typeName, style = cell, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(vm.colType.dp).padding(horizontal = 14.dp))
            }
            if (vm.showColSize) {
                Text(if (item.isDir) "" else Fmt.size(item.size), style = cell, maxLines = 1, textAlign = TextAlign.End,
                    modifier = Modifier.width(vm.colSize.dp).padding(start = 14.dp, end = 4.dp))
            }
        }
        if (cb.menuFor == item.path) ItemContextMenu(item, cb)
    }
}

@Composable
private fun SelectBox(checked: Boolean, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        Icon(
            if (checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank, null,
            Modifier.size(18.dp),
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GroupHeader(g: ItemGroup, collapsed: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (collapsed) Icons.Filled.ChevronRight else Icons.Filled.ExpandMore, null,
            Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "${g.label} (${g.items.size})", fontSize = 13.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

// ======================= Lista / Mosaicos / Iconos =======================

@Composable
private fun GridItems(cb: ItemCallbacks, minCell: Dp, cell: @Composable (FileItem) -> Unit) {
    val vm = cb.vm
    val grouped = vm.groupBy != GroupBy.NONE
    val state = rememberLazyGridState()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minCell),
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(6.dp),
    ) {
        vm.groups.forEach { g ->
            if (grouped) {
                item(key = "h:" + g.key, span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                    GroupHeader(g, g.key in vm.collapsed) { vm.toggleGroup(g.key) }
                }
            }
            if (!grouped || g.key !in vm.collapsed) {
                items(g.items, key = { it.path }, contentType = { "cell" }) { cell(it) }
            }
        }
    }
}

@Composable
private fun ListCell(item: FileItem, cb: ItemCallbacks) {
    val vm = cb.vm
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .height(vm.density.rowDp.dp)
                .padding(1.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(rowBackground(item, vm))
                .itemGestures(item, cb)
                .padding(horizontal = 6.dp)
                .alpha(itemAlpha(item, vm)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (vm.selectionMode) SelectBox(item.path in vm.selection, Modifier.width(28.dp))
            Icon(iconFor(item.category), null, tint = colorFor(item.category), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(item.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (cb.menuFor == item.path) ItemContextMenu(item, cb)
    }
}

@Composable
private fun TileCell(item: FileItem, cb: ItemCallbacks) {
    val vm = cb.vm
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(2.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(rowBackground(item, vm))
                .itemGestures(item, cb)
                .padding(6.dp)
                .alpha(itemAlpha(item, vm)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (vm.selectionMode) SelectBox(item.path in vm.selection, Modifier.width(26.dp))
            Thumb(item, 52.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.typeName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(
                    if (item.isDir) Fmt.date(item.lastModified) else Fmt.size(item.size),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                )
            }
        }
        if (cb.menuFor == item.path) ItemContextMenu(item, cb)
    }
}

@Composable
private fun IconCell(item: FileItem, cb: ItemCallbacks, size: Dp) {
    val vm = cb.vm
    Box {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(3.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(rowBackground(item, vm))
                .itemGestures(item, cb)
                .padding(6.dp)
                .alpha(itemAlpha(item, vm)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                Thumb(item, size)
                if (vm.selectionMode) {
                    SelectBox(item.path in vm.selection, Modifier.align(Alignment.TopStart))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                item.name, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center, lineHeight = 15.sp,
            )
        }
        if (cb.menuFor == item.path) ItemContextMenu(item, cb)
    }
}

/** Miniatura real para imágenes y vídeos; icono para el resto. */
@Composable
fun Thumb(item: FileItem, size: Dp) {
    val showImage = item.category == Category.IMAGE || item.category == Category.VIDEO
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Icon(
            iconFor(item.category), null, tint = colorFor(item.category).copy(alpha = if (showImage) 0.35f else 1f),
            modifier = Modifier.size(size * 0.8f),
        )
        if (showImage) {
            val ctx = LocalContext.current
            val px = with(LocalDensity.current) { size.roundToPx() }
            val req = remember(item.path, item.lastModified, px) {
                ImageRequest.Builder(ctx)
                    .data(File(item.path))
                    .size(px)
                    .memoryCacheKey("${item.path}#${item.lastModified}#$px")
                    .build()
            }
            AsyncImage(
                model = req,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(RoundedCornerShape(4.dp)),
            )
            if (item.category == Category.VIDEO) {
                Icon(
                    Icons.Filled.PlayCircle, null, tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size((size * 0.3f).coerceAtLeast(14.dp)),
                )
            }
        }
    }
}

// ======================= Menú contextual =======================

@Composable
private fun ItemContextMenu(item: FileItem, cb: ItemCallbacks) {
    val vm = cb.vm
    val clipboard = LocalClipboardManager.current
    val close = { cb.setMenu(null) }
    val onClose by rememberUpdatedState(close)
    DropdownMenu(expanded = true, onDismissRequest = { onClose() }) {
        MenuEntry(if (item.isDir) Icons.Filled.FolderOpen else Icons.AutoMirrored.Filled.OpenInNew, "Abrir") {
            close(); vm.open(item)
        }
        if (!item.isDir) MenuEntry(Icons.AutoMirrored.Filled.OpenInNew, "Abrir con…") { close(); vm.openWith(item) }
        HorizontalDivider()
        if (vm.searchResults != null) {
            HorizontalDivider()
            MenuEntry(Icons.Filled.FolderOpen, "Abrir ubicación del archivo") { close(); vm.openLocation(item) }
            MenuEntry(Icons.Filled.Tab, "Abrir ubicación en nueva pestaña") {
                close(); vm.openInNewTab(File(item.parent), item.path)
            }
            MenuEntry(Icons.Filled.VerticalSplit, "Abrir ubicación en el otro panel") {
                close(); vm.openInOtherPane(File(item.parent), item.path)
            }
            MenuEntry(Icons.Filled.OpenInBrowser, "Abrir ubicación en nueva ventana") {
                close(); vm.openInNewWindow(File(item.parent))
            }
        }
        if (item.isDir) {
            HorizontalDivider()
            MenuEntry(Icons.Filled.Tab, "Abrir en nueva pestaña") { close(); vm.openInNewTab(File(item.path)) }
            MenuEntry(Icons.Filled.VerticalSplit, "Abrir en el otro panel") { close(); vm.openInOtherPane(File(item.path)) }
            MenuEntry(Icons.Filled.OpenInBrowser, "Abrir en nueva ventana") { close(); vm.openInNewWindow(File(item.path)) }
        }
        HorizontalDivider()
        MenuEntry(Icons.Filled.ContentCut, "Cortar") { close(); vm.copySelection(true) }
        MenuEntry(Icons.Filled.ContentCopy, "Copiar") { close(); vm.copySelection(false) }
        MenuEntry(Icons.Filled.DriveFileMove, "Mover a…") { close(); cb.dialog(DialogReq.PickFolder(vm.selectedItems(), move = true)) }
        MenuEntry(Icons.Filled.FileCopy, "Copiar a…") { close(); cb.dialog(DialogReq.PickFolder(vm.selectedItems(), move = false)) }
        MenuEntry(Icons.Filled.DriveFileRenameOutline, "Cambiar nombre") { close(); cb.dialog(DialogReq.Rename(item)) }
        MenuEntry(Icons.Filled.Delete, "Eliminar") { close(); cb.dialog(DialogReq.Delete(vm.selectedItems())) }
        HorizontalDivider()
        MenuEntry(Icons.Filled.Share, "Compartir") { close(); vm.share(vm.selectedItems()) }
        MenuEntry(Icons.Filled.FolderZip, "Comprimir en ZIP") { close(); vm.compress(vm.selectedItems()) }
        if (item.extension == "zip") MenuEntry(Icons.Filled.Unarchive, "Extraer todo") { close(); vm.extract(item) }
        if (item.isDir) MenuEntry(Icons.Filled.PushPin, "Anclar a Acceso rápido") { close(); vm.pin(item.path) }
        MenuEntry(Icons.Filled.Link, "Copiar ruta") { close(); clipboard.setText(AnnotatedString(item.path)) }
        MenuEntry(Icons.Filled.SelectAll, if (vm.selectionMode) "Salir de selección múltiple" else "Selección múltiple") {
            close(); vm.setSelectionModeOn(!vm.selectionMode)
        }
        MenuEntry(Icons.Filled.Info, "Propiedades") { close(); vm.showPreviewFor(item) }
    }
}

@Composable
private fun MenuEntry(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, fontSize = 14.sp) },
        leadingIcon = { Icon(icon, null, Modifier.size(20.dp)) },
        onClick = onClick,
        modifier = Modifier.height(40.dp),
    )
}
