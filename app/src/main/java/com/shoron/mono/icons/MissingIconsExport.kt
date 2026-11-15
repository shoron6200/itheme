package com.shoron.mono.icons

/** One installed app without a Thin Line icon. Pure data for export/copy (JVM-tested). */
data class MissingIcon(
    val label: String,
    val packageName: String,
    val activity: String,
    val system: Boolean,
)

/** Text formats for copying/exporting the missing-icon list. */
object MissingIconsExport {

    fun names(items: List<MissingIcon>): String = items.joinToString("\n") { it.label }

    fun packages(items: List<MissingIcon>): String = items.map { it.packageName }.distinct().joinToString("\n")

    fun txt(items: List<MissingIcon>): String = items.joinToString("\n") { "${it.label}\t${it.packageName}" }

    fun csv(items: List<MissingIcon>): String = buildString {
        append("label,package,activity,system\n")
        items.forEach {
            append(csvField(it.label)).append(',')
                .append(csvField(it.packageName)).append(',')
                .append(csvField(it.activity)).append(',')
                .append(it.system).append('\n')
        }
    }

    fun json(items: List<MissingIcon>): String = buildString {
        append("[\n")
        items.forEachIndexed { i, it ->
            append("  {\"label\": ").append(jsonString(it.label))
                .append(", \"package\": ").append(jsonString(it.packageName))
                .append(", \"activity\": ").append(jsonString(it.activity))
                .append(", \"system\": ").append(it.system).append('}')
            if (i < items.lastIndex) append(',')
            append('\n')
        }
        append("]\n")
    }

    private fun csvField(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

    private fun jsonString(value: String): String = buildString {
        append('"')
        value.forEach { c ->
            when {
                c == '"' -> append("\\\"")
                c == '\\' -> append("\\\\")
                c == '\n' -> append("\\n")
                c == '\r' -> append("\\r")
                c == '\t' -> append("\\t")
                c < ' ' -> append(String.format("\\u%04x", c.code))
                else -> append(c)
            }
        }
        append('"')
    }
}
