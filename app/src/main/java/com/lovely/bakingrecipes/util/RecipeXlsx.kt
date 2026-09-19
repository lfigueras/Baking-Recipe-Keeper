package com.lovely.bakingrecipes.util

import com.lovely.bakingrecipes.data.PastryWithIngredients
import com.lovely.bakingrecipes.data.formatAmount
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// Builds a minimal, styled .xlsx (OOXML) workbook without any external library.
internal object RecipeXlsx {

    private data class Cell(
        val col: Int,
        val style: Int,
        val text: String? = null,
        val number: String? = null
    )

    // Style indices must match the cellXfs order in STYLES below.
    private const val S_NORMAL = 0
    private const val S_TITLE = 1
    private const val S_SECTION = 2
    private const val S_HEADER = 3
    private const val S_LABEL = 4

    fun build(data: PastryWithIngredients, scale: Double, displayServings: Int): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putEntry("[Content_Types].xml", CONTENT_TYPES)
            zip.putEntry("_rels/.rels", RELS)
            zip.putEntry("xl/workbook.xml", WORKBOOK)
            zip.putEntry("xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            zip.putEntry("xl/styles.xml", STYLES)
            zip.putEntry("xl/worksheets/sheet1.xml", buildSheet(data, scale, displayServings))
        }
        return out.toByteArray()
    }

    private fun buildSheet(
        data: PastryWithIngredients,
        scale: Double,
        displayServings: Int
    ): String {
        val p = data.pastry
        val rows = mutableListOf<List<Cell>>()
        val merges = mutableListOf<String>()

        fun add(cells: List<Cell>): Int {
            rows.add(cells)
            return rows.size
        }

        fun band(style: Int, text: String): List<Cell> =
            listOf(Cell(0, style, text = text), Cell(1, style), Cell(2, style))

        // Title banner (merged across the three columns).
        val titleRow = add(band(S_TITLE, p.name))
        merges.add("A$titleRow:C$titleRow")
        add(emptyList())

        // Recipe info block.
        fun info(label: String, value: String) =
            listOf(Cell(0, S_LABEL, text = label), Cell(1, S_NORMAL, text = value))
        if (p.category.isNotBlank()) add(info("Category", p.category))
        if (p.servings > 0) add(info("Servings", displayServings.toString()))
        if (p.prepMinutes > 0) add(info("Prep (min)", p.prepMinutes.toString()))
        if (p.cookMinutes > 0) add(info("Bake (min)", p.cookMinutes.toString()))
        if (p.difficulty.isNotBlank()) add(info("Difficulty", p.difficulty))
        if (data.tags.isNotEmpty()) add(info("Tags", data.tags.joinToString(", ") { it.name }))

        if (data.ingredients.isNotEmpty()) {
            add(emptyList())
            val sec = add(band(S_SECTION, "Ingredients"))
            merges.add("A$sec:C$sec")
            add(
                listOf(
                    Cell(0, S_HEADER, text = "Amount"),
                    Cell(1, S_HEADER, text = "Unit"),
                    Cell(2, S_HEADER, text = "Ingredient")
                )
            )
            data.ingredients.forEach { ing ->
                add(
                    listOf(
                        Cell(0, S_NORMAL, number = formatAmount(ing.amount * scale)),
                        Cell(1, S_NORMAL, text = ing.unit.label),
                        Cell(2, S_NORMAL, text = ing.name)
                    )
                )
            }
        }

        if (data.steps.isNotEmpty()) {
            add(emptyList())
            val sec = add(band(S_SECTION, "Baking Procedure"))
            merges.add("A$sec:C$sec")
            val hdr = add(
                listOf(
                    Cell(0, S_HEADER, text = "Step"),
                    Cell(1, S_HEADER, text = "Instruction"),
                    Cell(2, S_HEADER)
                )
            )
            merges.add("B$hdr:C$hdr")
            data.steps.sortedBy { it.position }.forEachIndexed { index, step ->
                val rn = add(
                    listOf(
                        Cell(0, S_NORMAL, number = (index + 1).toString()),
                        Cell(1, S_NORMAL, text = step.instruction),
                        Cell(2, S_NORMAL)
                    )
                )
                merges.add("B$rn:C$rn")
            }
        }

        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        sb.append("<cols>")
        sb.append("<col min=\"1\" max=\"1\" width=\"16\" customWidth=\"1\"/>")
        sb.append("<col min=\"2\" max=\"2\" width=\"14\" customWidth=\"1\"/>")
        sb.append("<col min=\"3\" max=\"3\" width=\"48\" customWidth=\"1\"/>")
        sb.append("</cols>")
        sb.append("<sheetData>")
        rows.forEachIndexed { i, row ->
            val rn = i + 1
            if (row.isEmpty()) {
                sb.append("<row r=\"$rn\"/>")
            } else {
                sb.append("<row r=\"$rn\">")
                row.forEach { cell ->
                    val ref = "${colLetter(cell.col)}$rn"
                    when {
                        cell.number != null ->
                            sb.append("<c r=\"$ref\" s=\"${cell.style}\"><v>${cell.number}</v></c>")
                        cell.text != null ->
                            sb.append(
                                "<c r=\"$ref\" s=\"${cell.style}\" t=\"inlineStr\">" +
                                    "<is><t xml:space=\"preserve\">${esc(cell.text)}</t></is></c>"
                            )
                        else ->
                            sb.append("<c r=\"$ref\" s=\"${cell.style}\"/>")
                    }
                }
                sb.append("</row>")
            }
        }
        sb.append("</sheetData>")
        if (merges.isNotEmpty()) {
            sb.append("<mergeCells count=\"${merges.size}\">")
            merges.forEach { sb.append("<mergeCell ref=\"$it\"/>") }
            sb.append("</mergeCells>")
        }
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun colLetter(index: Int): String {
        var i = index
        val sb = StringBuilder()
        while (i >= 0) {
            sb.insert(0, ('A' + (i % 26)))
            i = i / 26 - 1
        }
        return sb.toString()
    }

    private fun esc(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun ZipOutputStream.putEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private const val CONTENT_TYPES =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
            "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
            "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
            "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
            "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
            "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
            "</Types>"

    private const val RELS =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
            "</Relationships>"

    private const val WORKBOOK =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
            "<sheets><sheet name=\"Recipe\" sheetId=\"1\" r:id=\"rId1\"/></sheets>" +
            "</workbook>"

    private const val WORKBOOK_RELS =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
            "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>" +
            "</Relationships>"

    // Fonts/fills/cellXfs indices are referenced by the S_* constants above.
    private const val STYLES =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
            "<fonts count=\"4\">" +
            "<font><sz val=\"11\"/><color rgb=\"FF3B2A1A\"/><name val=\"Calibri\"/></font>" +
            "<font><b/><sz val=\"18\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font>" +
            "<font><b/><sz val=\"12\"/><color rgb=\"FF3B2A1A\"/><name val=\"Calibri\"/></font>" +
            "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font>" +
            "</fonts>" +
            "<fills count=\"4\">" +
            "<fill><patternFill patternType=\"none\"/></fill>" +
            "<fill><patternFill patternType=\"gray125\"/></fill>" +
            "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFC8956C\"/><bgColor indexed=\"64\"/></patternFill></fill>" +
            "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF2DFC8\"/><bgColor indexed=\"64\"/></patternFill></fill>" +
            "</fills>" +
            "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
            "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
            "<cellXfs count=\"5\">" +
            "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
            "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyAlignment=\"1\"><alignment vertical=\"center\"/></xf>" +
            "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"3\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>" +
            "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>" +
            "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>" +
            "</cellXfs>" +
            "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>" +
            "</styleSheet>"
}
