package com.jarvis.explorador.data

import java.text.Normalizer
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Orden "natural" como el Explorador de Windows: archivo2 < archivo10. */
object NaturalOrder : Comparator<String> {
    private fun Char.isAsciiDigit() = this in '0'..'9'

    override fun compare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val ca = a[i]
            val cb = b[j]
            if (ca.isAsciiDigit() && cb.isAsciiDigit()) {
                var si = i
                while (si < a.length && a[si] == '0') si++
                var sj = j
                while (sj < b.length && b[sj] == '0') sj++
                var ei = si
                while (ei < a.length && a[ei].isAsciiDigit()) ei++
                var ej = sj
                while (ej < b.length && b[ej].isAsciiDigit()) ej++
                val li = ei - si
                val lj = ej - sj
                if (li != lj) return li - lj
                for (k in 0 until li) {
                    val d = a[si + k] - b[sj + k]
                    if (d != 0) return d
                }
                i = ei
                j = ej
            } else {
                val d = ca.lowercaseChar().compareTo(cb.lowercaseChar())
                if (d != 0) return d
                i++
                j++
            }
        }
        return (a.length - i) - (b.length - j)
    }
}

object Sorting {
    fun comparator(field: SortField, asc: Boolean): Comparator<FileItem> {
        val base: Comparator<FileItem> = when (field) {
            SortField.NAME -> compareBy(NaturalOrder) { it.name }
            SortField.DATE -> compareBy { it.lastModified }
            SortField.TYPE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.typeName }
            SortField.SIZE -> compareBy { it.size }
            SortField.LOCATION -> compareBy(NaturalOrder) { it.parent }
        }
        val dirsFirst = compareBy<FileItem> { !it.isDir }
        return dirsFirst.then(if (asc) base else base.reversed()).thenBy(NaturalOrder) { it.name }
    }
}

private data class GroupKey(val order: Int, val label: String)

object Grouping {
    fun build(sorted: List<FileItem>, by: GroupBy, sortField: SortField, asc: Boolean): List<ItemGroup> {
        if (by == GroupBy.NONE) return listOf(ItemGroup("all", "", sorted))
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val map = LinkedHashMap<GroupKey, MutableList<FileItem>>()
        for (item in sorted) map.getOrPut(key(item, by, today, zone)) { mutableListOf() }.add(item)
        var keys = map.keys.sortedWith(compareBy<GroupKey> { it.order }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        val reverse = when (by) {
            GroupBy.DATE -> sortField == SortField.DATE && asc
            GroupBy.NAME -> sortField == SortField.NAME && !asc
            GroupBy.SIZE -> sortField == SortField.SIZE && !asc
            GroupBy.TYPE -> sortField == SortField.TYPE && !asc
            GroupBy.NONE -> false
        }
        if (reverse) keys = keys.reversed()
        return keys.map { ItemGroup("${it.order}|${it.label}", it.label, map.getValue(it)) }
    }

    private fun key(item: FileItem, by: GroupBy, today: LocalDate, zone: ZoneId): GroupKey = when (by) {
        GroupBy.NAME -> {
            val c = Normalizer.normalize(item.name.take(1), Normalizer.Form.NFD).firstOrNull()?.uppercaseChar()
            when {
                c == null -> GroupKey(4, "Otros")
                c in '0'..'9' -> GroupKey(0, "0 - 9")
                c in 'A'..'H' -> GroupKey(1, "A - H")
                c in 'I'..'P' -> GroupKey(2, "I - P")
                c in 'Q'..'Z' -> GroupKey(3, "Q - Z")
                else -> GroupKey(4, "Otros")
            }
        }
        GroupBy.TYPE -> GroupKey(if (item.isDir) 0 else 1, item.typeName)
        GroupBy.SIZE -> {
            val s = item.size
            when {
                item.isDir -> GroupKey(-1, "Carpetas")
                s == 0L -> GroupKey(0, "Vacío")
                s < 16L * 1024 -> GroupKey(1, "Diminuto (0 - 16 KB)")
                s < 1024L * 1024 -> GroupKey(2, "Pequeño (16 KB - 1 MB)")
                s < 128L * 1024 * 1024 -> GroupKey(3, "Mediano (1 - 128 MB)")
                s < 1024L * 1024 * 1024 -> GroupKey(4, "Grande (128 MB - 1 GB)")
                s < 4L * 1024 * 1024 * 1024 -> GroupKey(5, "Enorme (1 - 4 GB)")
                else -> GroupKey(6, "Gigantesco (> 4 GB)")
            }
        }
        GroupBy.DATE -> {
            val d = Instant.ofEpochMilli(item.lastModified).atZone(zone).toLocalDate()
            val weekStart = today.with(DayOfWeek.MONDAY)
            when {
                d.isAfter(today) -> GroupKey(-1, "Fecha futura")
                d == today -> GroupKey(0, "Hoy")
                d == today.minusDays(1) -> GroupKey(1, "Ayer")
                !d.isBefore(weekStart) -> GroupKey(2, "A principios de esta semana")
                !d.isBefore(weekStart.minusWeeks(1)) -> GroupKey(3, "La semana pasada")
                d.year == today.year && d.month == today.month -> GroupKey(4, "A principios de este mes")
                d.withDayOfMonth(1) == today.withDayOfMonth(1).minusMonths(1) -> GroupKey(5, "El mes pasado")
                d.year == today.year -> GroupKey(6, "A principios de este año")
                else -> GroupKey(7, "Hace mucho tiempo")
            }
        }
        GroupBy.NONE -> GroupKey(0, "")
    }
}
