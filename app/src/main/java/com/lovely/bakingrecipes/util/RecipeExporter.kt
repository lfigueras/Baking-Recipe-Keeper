package com.lovely.bakingrecipes.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
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

    // Brand palette (matches the app theme).
    private const val CARAMEL = 0xFFC8956C.toInt()
    private const val CREAM = 0xFFF2DFC8.toInt()
    private const val WARM = 0xFFFDF6EE.toInt()
    private const val ESPRESSO = 0xFF3B2A1A.toInt()
    private const val MOCHA = 0xFF9C7B5E.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()

    fun sharePdf(
        context: Context,
        data: PastryWithIngredients,
        scale: Double,
        displayServings: Int
    ) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 44f
        val contentWidth = pageWidth - 2 * margin
        val headerHeight = 104f
        val footerY = pageHeight - 28f

        val serif = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        val serifBold = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        val sans = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = WHITE; textSize = 26f; typeface = serifBold
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CREAM; textSize = 13f; typeface = sans
        }
        val chipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ESPRESSO; textSize = 10f; typeface = sans
        }
        val chipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CREAM }
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CARAMEL; textSize = 16f; typeface = serifBold
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ESPRESSO; textSize = 12f; typeface = serif
        }
        val bulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CARAMEL }
        val stepNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = WHITE; textSize = 11f; typeface = serifBold; textAlign = Paint.Align.CENTER
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CREAM; strokeWidth = 2f
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MOCHA; textSize = 9f; typeface = sans; textAlign = Paint.Align.CENTER
        }
        val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CARAMEL }
        val bgPaint = Paint().apply { color = WARM }

        val p = data.pastry

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        var y: Float

        fun drawBackground(c: Canvas) {
            c.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)
        }

        fun drawFooter(c: Canvas) {
            c.drawLine(margin, footerY - 12f, pageWidth - margin, footerY - 12f, dividerPaint)
            c.drawText("Baking Recipe Keeper", pageWidth / 2f, footerY, footerPaint)
        }

        drawBackground(canvas)
        // Header band on the first page.
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), headerHeight, bandPaint)
        canvas.drawText(p.name, margin, 52f, titlePaint)
        if (p.category.isNotBlank()) {
            canvas.drawText(p.category, margin, 76f, subtitlePaint)
        }
        y = headerHeight + 30f

        fun newPage() {
            drawFooter(canvas)
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            drawBackground(canvas)
            y = margin + 8f
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > footerY - 24f) newPage()
        }

        // Wrapped body/heading text starting at [x], advancing y line by line.
        fun drawWrapped(text: String, paint: Paint, x: Float, lineHeight: Float) {
            val maxWidth = pageWidth - margin - x
            var line = StringBuilder()
            text.split(" ").forEach { word ->
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
                    ensureSpace(lineHeight)
                    canvas.drawText(line.toString(), x, y, paint)
                    y += lineHeight
                    line = StringBuilder(word)
                } else {
                    line = StringBuilder(candidate)
                }
            }
            if (line.isNotEmpty()) {
                ensureSpace(lineHeight)
                canvas.drawText(line.toString(), x, y, paint)
                y += lineHeight
            }
        }

        fun sectionHeading(title: String) {
            ensureSpace(34f)
            y += 12f
            canvas.drawText(title, margin, y, headingPaint)
            y += 8f
            canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
            y += 16f
        }

        // Meta chips row (servings / prep / bake / difficulty / tags).
        val chips = buildList {
            if (p.servings > 0) add("$displayServings servings")
            if (p.prepMinutes > 0) add("Prep ${p.prepMinutes} min")
            if (p.cookMinutes > 0) add("Bake ${p.cookMinutes} min")
            if (p.difficulty.isNotBlank()) add(p.difficulty)
            data.tags.forEach { add("#${it.name}") }
        }
        if (chips.isNotEmpty()) {
            val chipH = 20f
            val padH = 10f
            val gap = 6f
            var x = margin
            ensureSpace(chipH + 4f)
            val rowTop = { y - chipH + 4f }
            chips.forEach { label ->
                val w = chipTextPaint.measureText(label) + padH * 2
                if (x + w > pageWidth - margin) {
                    x = margin
                    y += chipH + gap
                    ensureSpace(chipH + 4f)
                }
                val top = rowTop()
                canvas.drawRoundRect(RectF(x, top, x + w, top + chipH), chipH / 2, chipH / 2, chipBgPaint)
                canvas.drawText(label, x + padH, top + chipH - 6f, chipTextPaint)
                x += w + gap
            }
            y += chipH
        }

        if (p.description.isNotBlank()) {
            y += 8f
            drawWrapped(p.description, bodyPaint, margin, 17f)
        }

        if (data.ingredients.isNotEmpty()) {
            sectionHeading("Ingredients")
            data.ingredients.forEach { ing ->
                ensureSpace(17f)
                canvas.drawCircle(margin + 3f, y - 4f, 2.5f, bulletPaint)
                drawWrapped(
                    "${formatAmount(ing.amount * scale)} ${ing.unit.label} ${ing.name}",
                    bodyPaint,
                    margin + 14f,
                    17f
                )
            }
        }

        if (data.steps.isNotEmpty()) {
            sectionHeading("Baking Procedure")
            data.steps.sortedBy { it.position }.forEachIndexed { index, step ->
                ensureSpace(22f)
                val circleY = y - 4f
                canvas.drawCircle(margin + 8f, circleY, 9f, bulletPaint)
                canvas.drawText("${index + 1}", margin + 8f, circleY + 4f, stepNumPaint)
                drawWrapped(step.instruction, bodyPaint, margin + 26f, 17f)
                y += 6f
            }
        }

        drawFooter(canvas)
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
