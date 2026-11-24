package com.jarvis.explorador.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jarvis.explorador.data.Category
import com.jarvis.explorador.data.FileItem
import com.jarvis.explorador.data.FileTypes
import com.jarvis.explorador.data.Fmt
import com.jarvis.explorador.data.Meta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PreviewPane(vm: ExplorerTab, modifier: Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
        val selected = remember(vm.selection, vm.currentItems) { vm.selectedItems() }
        val item = vm.focused
        when {
            selected.size > 1 -> MultiSummary(selected)
            item != null -> ItemPreview(item, vm)
            else -> Text(
                "Selecciona un archivo para obtener una vista previa.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
        }
    }
}

@Composable
private fun MultiSummary(items: List<FileItem>) {
    val files = items.count { !it.isDir }
    val dirs = items.size - files
    val bytes = items.filter { !it.isDir }.sumOf { it.size }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            items.take(3).forEachIndexed { i, it ->
                Icon(
                    iconFor(it.category), null, tint = colorFor(it.category),
                    modifier = Modifier.size(72.dp).padding(start = (i * 18).dp, top = (i * 10).dp),
                )
            }
        }
        Spacer(Modifier.size(16.dp))
        Text("${items.size} elementos seleccionados", fontWeight = FontWeight.SemiBold)
        Text("$files archivos, $dirs carpetas", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        if (bytes > 0) Text("Tamaño de archivos: ${Fmt.sizeExact(bytes)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun ItemPreview(item: FileItem, vm: ExplorerTab) {
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxWidth().weight(1f).padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            PreviewContent(item, vm)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Properties(item, vm)
    }
}

@Composable
private fun PreviewContent(item: FileItem, vm: ExplorerTab) {
    val file = File(item.path)
    when {
        item.category == Category.IMAGE -> ZoomableImage(file, item.lastModified)
        item.category == Category.VIDEO -> Box(
            Modifier.fillMaxSize().clickable { vm.open(item) },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(file).build(),
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            Icon(Icons.Filled.PlayCircle, "Reproducir", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(64.dp))
        }
        item.category == Category.PDF -> PdfPreview(file)
        item.category == Category.AUDIO -> AudioPreview(item)
        FileTypes.isTextPreviewable(item.category) -> TextPreview(file)
        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(iconFor(item.category), null, tint = colorFor(item.category), modifier = Modifier.size(120.dp))
            if (!item.isDir) {
                Text("No hay vista previa disponible.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ZoomableImage(file: File, stamp: Long) {
    var scale by remember(file, stamp) { mutableFloatStateOf(1f) }
    var offset by remember(file, stamp) { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 8f)
        offset = if (scale == 1f) Offset.Zero else offset + pan
    }
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(file).memoryCacheKey("${file.path}#$stamp#preview").build(),
        contentDescription = file.name,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(file) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f
                })
            }
            .transformable(state)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
    )
}

@Composable
private fun PdfPreview(file: File) {
    var page by remember(file) { mutableIntStateOf(0) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val widthPx = if (constraints.hasBoundedWidth) constraints.maxWidth else 1000
        val result by produceState<Pair<Bitmap, Int>?>(null, file, page, widthPx) {
            value = withContext(Dispatchers.IO) { runCatching { Meta.renderPdf(file, page, widthPx) }.getOrNull() }
        }
        val r = result
        if (r == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Image(
                        r.first.asImageBitmap(), "Página ${page + 1}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (page > 0) page-- }, enabled = page > 0) { Icon(Icons.Filled.ChevronLeft, "Anterior") }
                    Text("Página ${page + 1} de ${r.second}", fontSize = 13.sp)
                    IconButton(onClick = { if (page < r.second - 1) page++ }, enabled = page < r.second - 1) {
                        Icon(Icons.Filled.ChevronRight, "Siguiente")
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioPreview(item: FileItem) {
    val art by produceState<Bitmap?>(null, item.path) { value = withContext(Dispatchers.IO) { Meta.audioArt(item.path) } }
    val a = art
    if (a != null) {
        Image(a.asImageBitmap(), item.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
    } else {
        Icon(iconFor(item.category), null, tint = colorFor(item.category), modifier = Modifier.size(120.dp))
    }
}

@Composable
private fun TextPreview(file: File) {
    val text by produceState<String?>(null, file) {
        value = withContext(Dispatchers.IO) { runCatching { Meta.readText(file) }.getOrElse { "No se pudo leer: ${it.message}" } }
    }
    val t = text
    if (t == null) {
        CircularProgressIndicator()
    } else {
        SelectionContainer(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            Text(t, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp, softWrap = false)
        }
    }
}

@Composable
private fun Properties(item: FileItem, vm: ExplorerTab) {
    val extras by produceState<List<Pair<String, String>>>(emptyList(), item.path, item.lastModified) {
        value = withContext(Dispatchers.IO) { Meta.extras(item) }
    }
    val folderStats by produceState<Triple<Int, Int, Long>?>(null, item.path) {
        value = if (item.isDir) withContext(Dispatchers.IO) { runCatching { Meta.folderStats(item.file) }.getOrNull() } else null
    }
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(iconFor(item.category), null, tint = colorFor(item.category), modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                SelectionContainer { Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis) }
                Text(item.typeName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.size(8.dp))
        if (!item.isDir) Prop("Tamaño", Fmt.sizeExact(item.size))
        if (item.isDir) {
            val s = folderStats
            Prop("Tamaño", if (s == null) "Calculando…" else Fmt.sizeExact(s.third))
            if (s != null) Prop("Contenido", "${s.first} archivos, ${s.second} carpetas")
        }
        Prop("Modificado", Fmt.dateLong(item.lastModified))
        extras.filter { !(item.isDir && it.first == "Contiene") }.forEach { (k, v) -> Prop(k, v) }
        Prop("Ubicación", item.parent)
        if (item.hidden) Prop("Atributos", "Oculto")
        Spacer(Modifier.size(6.dp))
        Row {
            TextButton(onClick = { vm.open(item) }) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Abrir")
            }
            if (!item.isDir) {
                TextButton(onClick = { vm.openWith(item) }) { Text("Abrir con…") }
                TextButton(onClick = { vm.share(listOf(item)) }) {
                    Icon(Icons.Filled.Share, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Compartir")
                }
            }
        }
    }
}

@Composable
private fun Prop(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(96.dp))
        SelectionContainer(Modifier.weight(1f)) { Text(value, fontSize = 12.sp) }
    }
}
