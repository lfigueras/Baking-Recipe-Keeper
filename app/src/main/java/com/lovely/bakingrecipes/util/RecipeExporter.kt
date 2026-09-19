package com.lovely.bakingrecipes.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.lovely.bakingrecipes.data.PastryWithIngredients
import com.lovely.bakingrecipes.data.formatAmount
import java.io.File

object RecipeExporter {

    fun buildPlainText(
        data: PastryWithIngredients,
        scale: Double,
        displayServings: Int
    ): String {
        val p = data.pastry
        return buildString {
            appendLine(p.name)
            if (p.category.isNotBlank()) appendLine(p.category)
            val meta = buildList {
                if (p.servings > 0) add("$displayServings servings")
                if (p.prepMinutes > 0) add("Prep ${p.prepMinutes} min")
                if (p.cookMinutes > 0) add("Bake ${p.cookMinutes} min")
                if (p.difficulty.isNotBlank()) add(p.difficulty)
            }
            if (meta.isNotEmpty()) appendLine(meta.joinToString(" · "))
            if (data.tags.isNotEmpty()) appendLine("Tags: ${data.tags.joinToString(", ") { it.name }}")
            if (p.description.isNotBlank()) {
                appendLine()
                appendLine(p.description)
            }
            if (data.ingredients.isNotEmpty()) {
                appendLine()
                appendLine("Ingredients:")
                data.ingredients.forEach { ing ->
                    appendLine("- ${formatAmount(ing.amount * scale)} ${ing.unit.label} ${ing.name}")
                }
            }
            if (data.steps.isNotEmpty()) {
                appendLine()
                appendLine("Baking Procedure:")
                data.steps.sortedBy { it.position }.forEachIndexed { index, step ->
                    appendLine("${index + 1}. ${step.instruction}")
                }
            }
        }.trim()
    }

    fun shareCsv(context: Context, data: PastryWithIngredients, scale: Double) {
        val csv = buildString {
            appendLine("Ingredient,Amount,Unit")
            data.ingredients.forEach { ing ->
                appendLine(
                    "${escapeCsv(ing.name)},${formatAmount(ing.amount * scale)},${escapeCsv(ing.unit.label)}"
                )
            }
        }
        val file = writeToCache(context, "${safeName(data.pastry.name)}.csv", csv.toByteArray())
        shareFile(context, file, "text/csv", data.pastry.name)
    }

    fun sharePdf(
        context: Context,
        data: PastryWithIngredients,
        scale: Double,
        displayServings: Int
    ) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val lineHeight = 20f

        val titlePaint = Paint().apply { textSize = 22f; isFakeBoldText = true }
        val headingPaint = Paint().apply { textSize = 15f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 12f }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = margin + lineHeight

        fun newPageIfNeeded() {
            if (y > pageHeight - margin) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = margin + lineHeight
            }
        }

        fun drawWrapped(text: String, paint: Paint) {
            val maxWidth = pageWidth - 2 * margin
            var line = StringBuilder()
            text.split(" ").forEach { word ->
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) > maxWidth) {
                    newPageIfNeeded()
                    canvas.drawText(line.toString(), margin, y, paint)
                    y += lineHeight
                    line = StringBuilder(word)
                } else {
                    line = StringBuilder(candidate)
                }
            }
            if (line.isNotEmpty()) {
                newPageIfNeeded()
                canvas.drawText(line.toString(), margin, y, paint)
                y += lineHeight
            }
        }

        val p = data.pastry
        drawWrapped(p.name, titlePaint)
        y += 4f

        val meta = buildList {
            if (p.servings > 0) add("$displayServings servings")
            if (p.prepMinutes > 0) add("Prep ${p.prepMinutes} min")
            if (p.cookMinutes > 0) add("Bake ${p.cookMinutes} min")
            if (p.difficulty.isNotBlank()) add(p.difficulty)
        }
        if (meta.isNotEmpty()) drawWrapped(meta.joinToString(" · "), bodyPaint)
        if (p.description.isNotBlank()) {
            y += 6f
            drawWrapped(p.description, bodyPaint)
        }

        if (data.ingredients.isNotEmpty()) {
            y += 10f
            drawWrapped("Ingredients", headingPaint)
            data.ingredients.forEach { ing ->
                drawWrapped(
                    "• ${formatAmount(ing.amount * scale)} ${ing.unit.label} ${ing.name}",
                    bodyPaint
                )
            }
        }

        if (data.steps.isNotEmpty()) {
            y += 10f
            drawWrapped("Baking Procedure", headingPaint)
            data.steps.sortedBy { it.position }.forEachIndexed { index, step ->
                drawWrapped("${index + 1}. ${step.instruction}", bodyPaint)
            }
        }

        document.finishPage(page)

        val file = File(cacheDir(context), "${safeName(p.name)}.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()

        shareFile(context, file, "application/pdf", p.name)
    }

    private fun writeToCache(context: Context, fileName: String, bytes: ByteArray): File {
        val file = File(cacheDir(context), fileName)
        file.writeBytes(bytes)
        return file
    }

    private fun cacheDir(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun shareFile(context: Context, file: File, mimeType: String, subject: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share $subject"))
    }

    private fun escapeCsv(value: String): String =
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }

    private fun safeName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9-_ ]"), "").trim().ifEmpty { "recipe" }
}
