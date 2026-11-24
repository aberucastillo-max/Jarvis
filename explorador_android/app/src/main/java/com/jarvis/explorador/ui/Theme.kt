package com.jarvis.explorador.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.jarvis.explorador.data.Category

val FolderColor = Color(0xFFE8B931)

@Composable
fun ExploradorTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Color(0xFF60CDFF),
            onPrimary = Color(0xFF003A57),
            background = Color(0xFF1C1C1C),
            surface = Color(0xFF202020),
            surfaceVariant = Color(0xFF2B2B2B),
            surfaceContainer = Color(0xFF272727),
            outlineVariant = Color(0xFF3D3D3D),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF005FB8),
            onPrimary = Color.White,
            background = Color(0xFFF3F3F3),
            surface = Color.White,
            surfaceVariant = Color(0xFFF0F0F0),
            surfaceContainer = Color(0xFFF7F7F7),
            outlineVariant = Color(0xFFE1E1E1),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

val selectionBg: Color @Composable get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
val focusBg: Color @Composable get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)

fun iconFor(c: Category): ImageVector = when (c) {
    Category.FOLDER -> Icons.Filled.Folder
    Category.IMAGE -> Icons.Filled.Image
    Category.VIDEO -> Icons.Filled.Movie
    Category.AUDIO -> Icons.Filled.AudioFile
    Category.PDF -> Icons.Filled.PictureAsPdf
    Category.TEXT -> Icons.Filled.Description
    Category.CODE -> Icons.Filled.Code
    Category.ARCHIVE -> Icons.Filled.FolderZip
    Category.DOC -> Icons.AutoMirrored.Filled.Article
    Category.SHEET -> Icons.Filled.TableChart
    Category.SLIDES -> Icons.Filled.Slideshow
    Category.APK -> Icons.Filled.Android
    Category.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile
}

fun colorFor(c: Category): Color = when (c) {
    Category.FOLDER -> FolderColor
    Category.IMAGE -> Color(0xFF2E9E8F)
    Category.VIDEO -> Color(0xFF8E44AD)
    Category.AUDIO -> Color(0xFFE67E22)
    Category.PDF -> Color(0xFFD93025)
    Category.TEXT -> Color(0xFF6B7B8C)
    Category.CODE -> Color(0xFF3B6EA5)
    Category.ARCHIVE -> Color(0xFF9C6B30)
    Category.DOC -> Color(0xFF2B579A)
    Category.SHEET -> Color(0xFF217346)
    Category.SLIDES -> Color(0xFFD24726)
    Category.APK -> Color(0xFF3DDC84)
    Category.OTHER -> Color(0xFF8A8A8A)
}
