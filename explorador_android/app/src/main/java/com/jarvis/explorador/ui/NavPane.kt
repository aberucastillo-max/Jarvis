package com.jarvis.explorador.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.explorador.data.Fmt
import java.io.File

private sealed interface NavRow {
    data class Header(val title: String) : NavRow
    data class Quick(val label: String, val path: String, val icon: ImageVector, val tint: Color, val pinned: Boolean) : NavRow
    data class Tree(val path: String, val label: String, val depth: Int, val isRoot: Boolean, val removable: Boolean) : NavRow
}

@Composable
fun NavPane(vm: ExplorerTab, modifier: Modifier) {
    val rows = buildList {
        add(NavRow.Header("Acceso rápido"))
        vm.quickAccess.forEach { q ->
            val (icon, tint) = when (q.kind) {
                "download" -> Icons.Filled.Download to Color(0xFF2E7D32)
                "documents" -> Icons.Filled.Description to Color(0xFF2B579A)
                "pictures" -> Icons.Filled.Image to Color(0xFF2E9E8F)
                "dcim" -> Icons.Filled.PhotoCamera to Color(0xFF546E7A)
                "music" -> Icons.Filled.MusicNote to Color(0xFFE67E22)
                "movies" -> Icons.Filled.Movie to Color(0xFF8E44AD)
                else -> Icons.Filled.Folder to FolderColor
            }
            add(NavRow.Quick(q.label, q.path, icon, tint, pinned = false))
        }
        vm.pins.forEach { p -> add(NavRow.Quick(displayName(p), p, Icons.Filled.Star, FolderColor, pinned = true)) }
        add(NavRow.Header("Este dispositivo"))
        vm.roots.forEach { r ->
            fun walk(path: String, depth: Int) {
                add(NavRow.Tree(path, if (depth == 0) r.label else displayName(path), depth, depth == 0, r.removable))
                if (path in vm.treeExpanded) vm.treeChildren[path]?.forEach { walk(it, depth + 1) }
            }
            walk(r.path, 0)
        }
    }

    LazyColumn(
        modifier.background(MaterialTheme.colorScheme.background).padding(vertical = 4.dp),
    ) {
        items(rows, key = {
            when (it) {
                is NavRow.Header -> "h:" + it.title
                is NavRow.Quick -> (if (it.pinned) "p:" else "q:") + it.path
                is NavRow.Tree -> "t:" + it.path
            }
        }) { row ->
            when (row) {
                is NavRow.Header -> Text(
                    row.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 4.dp),
                )
                is NavRow.Quick -> QuickRow(vm, row)
                is NavRow.Tree -> TreeRow(vm, row)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickRow(vm: ExplorerTab, row: NavRow.Quick) {
    var menu by remember { mutableStateOf(false) }
    val current = vm.currentDir?.path == row.path
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .height(34.dp)
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (current) selectionBg else Color.Transparent)
                .combinedClickable(
                    onClick = { vm.navigate(File(row.path)) },
                    onLongClick = { menu = true },
                )
                .padding(start = 26.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(row.icon, null, tint = row.tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(row.label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (row.pinned) {
                Icon(Icons.Filled.PushPin, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
            }
        }
        DropdownMenu(menu, { menu = false }) {
            DropdownMenuItem(text = { Text("Abrir en nueva pestaña") }, onClick = { menu = false; vm.openInNewTab(File(row.path)) })
            DropdownMenuItem(text = { Text("Abrir en el otro panel") }, onClick = { menu = false; vm.openInOtherPane(File(row.path)) })
            DropdownMenuItem(text = { Text("Abrir en nueva ventana") }, onClick = { menu = false; vm.openInNewWindow(File(row.path)) })
            if (row.pinned) {
                DropdownMenuItem(text = { Text("Desanclar de Acceso rápido") }, onClick = { menu = false; vm.unpin(row.path) })
            }
        }
    }
}

@Composable
private fun TreeRow(vm: ExplorerTab, row: NavRow.Tree) {
    val current = vm.currentDir?.path == row.path
    val expanded = row.path in vm.treeExpanded
    val loadedEmpty = vm.treeChildren[row.path]?.isEmpty() == true
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (current) selectionBg else Color.Transparent)
            .clickable { vm.navigate(File(row.path)) }
    ) {
        Row(
            Modifier.fillMaxWidth().height(34.dp).padding(start = (4 + row.depth * 14).dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).clickable(enabled = !loadedEmpty) { vm.toggleTree(row.path) },
                contentAlignment = Alignment.Center,
            ) {
                if (!loadedEmpty) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandMore else Icons.Filled.ChevronRight, "Expandir",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp),
                    )
                }
            }
            val icon = when {
                row.isRoot && row.removable -> Icons.Filled.SdCard
                row.isRoot -> Icons.Filled.TabletAndroid
                else -> Icons.Filled.Folder
            }
            Icon(
                icon, null,
                tint = if (row.isRoot) MaterialTheme.colorScheme.primary else FolderColor,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                row.label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = if (row.isRoot) FontWeight.Medium else FontWeight.Normal,
            )
        }
        if (row.isRoot) {
            val space = remember(row.path, vm.freeSpaceTick) {
                runCatching { File(row.path).let { it.usableSpace to it.totalSpace } }.getOrNull()
            }
            if (space != null && space.second > 0) {
                val used = 1f - space.first.toFloat() / space.second
                Column(Modifier.padding(start = 58.dp, end = 12.dp, bottom = 6.dp)) {
                    LinearProgressIndicator(
                        progress = { used },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color = if (used > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Text(
                        "${Fmt.size(space.first)} libres de ${Fmt.size(space.second)}",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
