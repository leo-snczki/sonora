package dev.leosnczki.sonora

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** The launcher icon's artwork is centered on its canvas, and the docs icon shows the same artwork. */
class LauncherIconTest {
    private val root = File(System.getProperty("sonora.rootDir") ?: "..")
    private val foreground = File(root, "app/src/main/res/drawable/ic_launcher_foreground.xml")
    private val docsIcon = File(root, "docs/assets/icon.svg")

    private fun parse(file: File) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)

    private fun elements(file: File, tag: String): List<Element> {
        val nodes = parse(file).getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    /** Samples the points of a path made of move and (smooth) cubic commands, absolute or relative. */
    private fun samplePath(pathData: String): List<Pair<Double, Double>> {
        val tokens = Regex("[A-Za-z]|-?[0-9]*\\.?[0-9]+").findAll(pathData).map { it.value }.toList()
        val points = mutableListOf<Pair<Double, Double>>()
        var x = 0.0
        var y = 0.0
        var lastControl: Pair<Double, Double>? = null
        var command = ' '
        var i = 0
        fun next() = tokens[i++].toDouble()
        fun cubic(c1: Pair<Double, Double>, c2: Pair<Double, Double>, end: Pair<Double, Double>) {
            for (step in 1..50) {
                val t = step / 50.0
                val u = 1 - t
                points += Pair(
                    u * u * u * x + 3 * u * u * t * c1.first + 3 * u * t * t * c2.first + t * t * t * end.first,
                    u * u * u * y + 3 * u * u * t * c1.second + 3 * u * t * t * c2.second + t * t * t * end.second,
                )
            }
            lastControl = c2
            x = end.first
            y = end.second
        }
        while (i < tokens.size) {
            if (tokens[i][0].isLetter()) command = tokens[i++][0]
            val dx = if (command.isLowerCase()) x else 0.0
            val dy = if (command.isLowerCase()) y else 0.0
            when (command) {
                'M', 'm' -> {
                    x = dx + next()
                    y = dy + next()
                    points += Pair(x, y)
                    lastControl = null
                    if (i < tokens.size && !tokens[i][0].isLetter()) error("implicit line-to in $pathData")
                }
                'C', 'c' -> cubic(Pair(dx + next(), dy + next()), Pair(dx + next(), dy + next()), Pair(dx + next(), dy + next()))
                'S', 's' -> {
                    val c1 = lastControl?.let { Pair(2 * x - it.first, 2 * y - it.second) } ?: Pair(x, y)
                    cubic(c1, Pair(dx + next(), dy + next()), Pair(dx + next(), dy + next()))
                }
                else -> error("unsupported path command '$command' in $pathData")
            }
        }
        return points
    }

    @Test
    fun launcher_foreground_artwork_is_centered_on_the_viewport() {
        val vector = parse(foreground).documentElement
        val width = vector.getAttribute("android:viewportWidth").toDouble()
        val height = vector.getAttribute("android:viewportHeight").toDouble()
        val paths = elements(foreground, "path")
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
        assertTrue(abs(centerX - width / 2) <= 0.25, "artwork x center is $centerX, expected ${width / 2}")
        assertTrue(abs(centerY - height / 2) <= 0.25, "artwork y center is $centerY, expected ${height / 2}")
    }

    @Test
    fun docs_icon_matches_the_launcher_foreground() {
        val vectorPaths = elements(foreground, "path").map { it.getAttribute("android:pathData") }
        val svgPaths = elements(docsIcon, "path").map { it.getAttribute("d") }
        assertEquals(vectorPaths, svgPaths)
    }
}
