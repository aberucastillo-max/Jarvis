package com.jarvis.explorador.ui

import android.graphics.Paint
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

/** Un documento de texto abierto en una pestaña del explorador. */
class EditorDoc(val file: File, text: String) {
    val id = ++editorCounter
    val name: String get() = file.name
    var value by mutableStateOf(TextFieldValue(text)); private set
    var saved by mutableStateOf(text); private set
    var undoCount by mutableIntStateOf(0); private set
    var redoCount by mutableIntStateOf(0); private set
    val dirty get() = value.text != saved

    var findOpen by mutableStateOf(false)
    var query by mutableStateOf("")
    var replacement by mutableStateOf("")
    var scrollTick by mutableIntStateOf(0); private set

    private val undo = ArrayDeque<TextFieldValue>()
    private val redo = ArrayDeque<TextFieldValue>()
    private var lastSnap = 0L

    fun edit(v: TextFieldValue) {
        if (v.text != value.text) {
            val now = SystemClock.uptimeMillis()
            if (undo.isEmpty() || now - lastSnap > 700) {
                undo.addLast(value)
                if (undo.size > 300) undo.removeFirst()
                lastSnap = now
            }
            redo.clear()
            counts()
        }
        value = v
    }

    private fun set(v: TextFieldValue, snapshot: Boolean) {
        if (snapshot && v.text != value.text) {
            undo.addLast(value); redo.clear(); lastSnap = 0L; counts()
        }
        value = v
    }

    fun undo() { val p = undo.removeLastOrNull() ?: return; redo.addLast(value); value = p; counts() }
    fun redo() { val n = redo.removeLastOrNull() ?: return; undo.addLast(value); value = n; counts() }
    private fun counts() { undoCount = undo.size; redoCount = redo.size }

    fun save(): Boolean = runCatching {
        file.writeText(value.text)
        saved = value.text
    }.isSuccess

    fun matches(): List<Int> {
        val q = query
        if (q.isEmpty()) return emptyList()
        val t = value.text
        val out = ArrayList<Int>()
        var i = t.indexOf(q, 0, ignoreCase = true)
        while (i >= 0 && out.size < 10000) {
            out += i
            i = t.indexOf(q, i + q.length, ignoreCase = true)
        }
        return out
    }

    fun findNext(forward: Boolean) {
        val m = matches()
        if (m.isEmpty()) return
        val sel = value.selection.min
        val i = if (forward) m.firstOrNull { it > sel } ?: m.first() else m.lastOrNull { it < sel } ?: m.last()
        set(value.copy(selection = TextRange(i, i + query.length)), snapshot = false)
        scrollTick++
    }

    fun replaceAll(): Int {
        val n = matches().size
        if (n > 0) set(TextFieldValue(value.text.replace(query, replacement, ignoreCase = true), TextRange(0)), snapshot = true)
        return n
    }

    private companion object { var editorCounter = 0 }
}

@Composable
fun EditorView(doc: EditorDoc, s: Shared, onSave: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surface)
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown || !e.isCtrlPressed) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.Z -> if (e.isShiftPressed) doc.redo() else doc.undo()
                    Key.Y -> doc.redo()
                    Key.S -> onSave()
                    Key.F -> doc.findOpen = true
                    else -> return@onPreviewKeyEvent false
                }
                true
            }
    ) {
        if (doc.findOpen) FindBar(doc, s)
        EditorBody(doc, s, Modifier.weight(1f).fillMaxWidth())
        EditorStatus(doc, s)
    }
}

@Composable
private fun FindBar(doc: EditorDoc, s: Shared) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val count = doc.matches().size
    Row(
        Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiniField(doc.query, { doc.query = it }, "Buscar", Modifier.width(240.dp).focusRequester(focus)) { doc.findNext(true) }
        Text(if (doc.query.isEmpty()) "" else "  $count", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        IconBtn(Icons.Filled.KeyboardArrowUp, "Anterior") { doc.findNext(false) }
        IconBtn(Icons.Filled.KeyboardArrowDown, "Siguiente") { doc.findNext(true) }
        Spacer(Modifier.width(8.dp))
        MiniField(doc.replacement, { doc.replacement = it }, "Reemplazar por…", Modifier.width(200.dp)) {}
        TextButton(onClick = { val n = doc.replaceAll(); s.message = "$n reemplazo(s)" }, enabled = count > 0) { Text("Reemplazar todo") }
        Spacer(Modifier.weight(1f))
        IconBtn(Icons.Filled.Close, "Cerrar búsqueda") { doc.findOpen = false }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun IconBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MiniField(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier, onEnter: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onEnter() }),
        modifier = modifier,
        decorationBox = { inner ->
            Box(
                Modifier.height(32.dp).clip(shape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape).padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) Text(hint, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                inner()
            }
        },
    )
}

@Composable
private fun EditorBody(doc: EditorDoc, s: Shared, modifier: Modifier) {
    val vScroll = rememberScrollState()
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val density = LocalDensity.current
    val gutterColor = MaterialTheme.colorScheme.outline
    val style = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = s.editorFont.sp,
        lineHeight = (s.editorFont * 1.55f).sp,
        color = MaterialTheme.colorScheme.onSurface,
    )
    LaunchedEffect(doc.scrollTick) {
        val l = layout ?: return@LaunchedEffect
        val top = l.getLineTop(l.getLineForOffset(doc.value.selection.min.coerceIn(0, doc.value.text.length))).toInt()
        if (top < vScroll.value || top > vScroll.value + vScroll.viewportSize - 80) vScroll.animateScrollTo((top - 120).coerceAtLeast(0))
    }
    BoxWithConstraints(modifier) {
        val viewport = maxWidth
        Row(Modifier.fillMaxSize().verticalScroll(vScroll)) {
            val l = layout
            val digits = (doc.value.text.count { it == '\n' } + 1).toString().length.coerceAtLeast(2)
            val gutterW = with(density) { (s.editorFont.sp.toPx() * 0.62f * digits).toDp() } + 20.dp
            Canvas(
                Modifier
                    .width(gutterW)
                    .height(with(density) { (l?.size?.height ?: 0).toDp() } + 20.dp)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (l == null) return@Canvas
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = gutterColor.toArgb()
                    textSize = s.editorFont.sp.toPx() * 0.9f
                    typeface = android.graphics.Typeface.MONOSPACE
                    textAlign = Paint.Align.RIGHT
                }
                val text = doc.value.text
                val top = 10.dp.toPx()
                var n = 0
                drawIntoCanvas { c ->
                    for (i in 0 until l.lineCount) {
                        val start = l.getLineStart(i)
                        if (i == 0 || text.getOrNull(start - 1) == '\n') {
                            n++
                            c.nativeCanvas.drawText(n.toString(), size.width - 8.dp.toPx(), top + l.getLineBaseline(i), paint)
                        }
                    }
                }
            }
            val fieldMod = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            val field: @Composable (Modifier) -> Unit = { m ->
                BasicTextField(
                    value = doc.value,
                    onValueChange = { doc.edit(it) },
                    textStyle = style,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    onTextLayout = { layout = it },
                    modifier = m,
                )
            }
            if (s.editorWrap) field(fieldMod.weight(1f))
            else Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                field(fieldMod.widthIn(min = viewport - gutterW - 24.dp))
            }
        }
    }
}

@Composable
private fun EditorStatus(doc: EditorDoc, s: Shared) {
    val v = doc.value
    val pos = v.selection.min.coerceIn(0, v.text.length)
    val line = v.text.substring(0, pos).count { it == '\n' } + 1
    val lineStart = if (pos == 0) 0 else v.text.lastIndexOf('\n', pos - 1) + 1
    val style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(
        Modifier.fillMaxWidth().height(26.dp).background(MaterialTheme.colorScheme.background).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(doc.file.path, style = style, maxLines = 1, modifier = Modifier.weight(1f))
        Text("Lín $line, Col ${pos - lineStart + 1}   ·   ${v.text.count { it == '\n' } + 1} líneas   ·   ${s.editorFont.toInt()} pt   ·   ", style = style)
        Text(if (doc.dirty) "Sin guardar" else "Guardado", style = style)
    }
}
