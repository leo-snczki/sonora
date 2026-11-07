package dev.leosnczki.sonora

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val CURVE_STEPS = 50
private const val CENTER_TOLERANCE = 0.25

/** A command letter or a number; separators are skipped, anything else is captured in group 2. */
private val PATH_TOKEN = Regex("([A-Za-z]|[-+]?(?:[0-9]+\\.?[0-9]*|\\.[0-9]+)(?:[eE][-+]?[0-9]+)?)|[\\s,]+|(.)")

private data class Point(val x: Double, val y: Double)

/** The launcher icon's artwork is centered on its canvas, and the docs icon shows the same artwork. */
class LauncherIconTest {
    private val root = File(System.getProperty("sonora.rootDir") ?: "..")
    private val foreground = File(root, "app/src/main/res/drawable/ic_launcher_foreground.xml")
    private val docsIcon = File(root, "docs/assets/icon.svg")

    private fun parse(file: File) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)

    private fun elements(doc: Document, tag: String): List<Element> {
        val nodes = doc.getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun tokenize(pathData: String): List<String> = PATH_TOKEN.findAll(pathData).mapNotNull { match ->
        match.groups[2]?.let { error("unexpected '${it.value}' in $pathData") }
        match.groups[1]?.value
    }.toList()

    /** Samples the points of a path made of move and (smooth) cubic commands, absolute or relative. */
    private fun samplePath(pathData: String): List<Point> {
        val tokens = tokenize(pathData)
        val points = mutableListOf<Point>()
        var current = Point(0.0, 0.0)
        var lastControl: Point? = null
        var command = ' '
        var i = 0
        fun next() = tokens[i++].toDouble()
        fun point() = if (command.isLowerCase()) Point(current.x + next(), current.y + next()) else Point(next(), next())
        fun cubic(c1: Point, c2: Point, end: Point) {
            for (step in 1..CURVE_STEPS) {
                val t = step.toDouble() / CURVE_STEPS
                val u = 1 - t
                points += Point(
                    u * u * u * current.x + 3 * u * u * t * c1.x + 3 * u * t * t * c2.x + t * t * t * end.x,
                    u * u * u * current.y + 3 * u * u * t * c1.y + 3 * u * t * t * c2.y + t * t * t * end.y,
                )
            }
            lastControl = c2
            current = end
        }
        while (i < tokens.size) {
            if (tokens[i][0].isLetter()) command = tokens[i++][0]
            when (command) {
                'M', 'm' -> {
                    current = point()
                    points += current
                    lastControl = null
                    if (i < tokens.size && !tokens[i][0].isLetter()) error("implicit line-to in $pathData")
                }
                'C', 'c' -> cubic(point(), point(), point())
                'S', 's' -> {
                    val c1 = lastControl?.let { Point(2 * current.x - it.x, 2 * current.y - it.y) } ?: current
                    cubic(c1, point(), point())
                }
                else -> error("unsupported path command '$command' in $pathData")
            }
        }
        return points
    }

    @Test
    fun launcher_foreground_artwork_is_centered_on_the_viewport() {
        val doc = parse(foreground)
        val width = doc.documentElement.getAttribute("android:viewportWidth").toDouble()
        val height = doc.documentElement.getAttribute("android:viewportHeight").toDouble()
        val paths = elements(doc, "path")
        assertTrue(paths.isNotEmpty(), "no paths in $foreground")

        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        for (path in paths) {
            val halfStroke = (path.getAttribute("android:strokeWidth").toDoubleOrNull() ?: 0.0) / 2
            for ((px, py) in samplePath(path.getAttribute("android:pathData"))) {
                minX = minOf(minX, px - halfStroke)
                maxX = maxOf(maxX, px + halfStroke)
                minY = minOf(minY, py - halfStroke)
                maxY = maxOf(maxY, py + halfStroke)
            }
        }
        val centerX = (minX + maxX) / 2
        val centerY = (minY + maxY) / 2
        assertTrue(abs(centerX - width / 2) <= CENTER_TOLERANCE, "artwork x center is $centerX, expected ${width / 2}")
        assertTrue(abs(centerY - height / 2) <= CENTER_TOLERANCE, "artwork y center is $centerY, expected ${height / 2}")
    }

    @Test
    fun docs_icon_matches_the_launcher_foreground() {
        val vectorPaths = elements(parse(foreground), "path").map { it.getAttribute("android:pathData") }
        val svgPaths = elements(parse(docsIcon), "path").map { it.getAttribute("d") }
        assertEquals(vectorPaths, svgPaths)
    }
}
