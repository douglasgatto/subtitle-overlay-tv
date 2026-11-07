package br.com.subtitleoverlay

import java.util.Locale

data class Cue(val fromMs: Long, val toMs: Long, val text: String)

object Srt {
    private val timing = Regex("(\\d{1,2}:)?\\d{2}:\\d{2}[,.]\\d{1,3}\\s*-->\\s*(\\d{1,2}:)?\\d{2}:\\d{2}[,.]\\d{1,3}")
    private val pair = Regex("((?:\\d{1,2}:)?\\d{2}:\\d{2}[,.]\\d{1,3})\\s*-->\\s*((?:\\d{1,2}:)?\\d{2}:\\d{2}[,.]\\d{1,3})")

    fun parse(source: String): List<Cue> {
        val normalized = source.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
        return normalized.split(Regex("\\n\\s*\\n")).mapNotNull { block ->
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val index = lines.indexOfFirst { timing.matches(it) }
            if (index < 0 || index + 1 >= lines.size) return@mapNotNull null
            val match = pair.matchEntire(lines[index]) ?: return@mapNotNull null
            val begin = ms(match.groupValues[1])
            val end = ms(match.groupValues[2])
            if (end <= begin) return@mapNotNull null
            val body = lines.drop(index + 1).joinToString("\n")
                .replace(Regex("<[^>]*>"), "")
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            Cue(begin, end, body)
        }.sortedBy { it.fromMs }
    }

    private fun ms(input: String): Long {
        val chunks = input.replace(',', '.').split(':')
        val secParts = chunks.last().split('.')
        val millis = secParts[1].padEnd(3, '0').take(3).toLong()
        val seconds = secParts[0].toLong()
        val minutes = chunks[chunks.size - 2].toLong()
        val hours = if (chunks.size == 3) chunks[0].toLong() else 0
        return hours * 3600000 + minutes * 60000 + seconds * 1000 + millis
    }
}
