package de.kaigothe.campusilmenau.ui.launch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class LaunchTimelineTest {

    // Ein typisches Telefon: 1080 × 2400 Pixel bei Dichte 2,625.
    private val width = 1080f
    private val height = 2400f
    private val unit = LaunchTimeline.UNIT_DP * 2.625f

    private fun frame(progress: Float) = LaunchFrame(progress, width, height, unit)

    @Test
    fun `erster Frame - Ziege unverkleinert, Startpunkt in der Bildschirmmitte, voll gefuellt`() {
        val frame = frame(0f)
        assertEquals(1f, frame.scale, 1e-6f)
        assertEquals(1f, frame.coverOpacity, 1e-6f)
        assertEquals(0f, frame.wallBlend, 1e-6f)
        assertEquals(0f, frame.edgeOpacity, 1e-6f)

        // Der Startpunkt der Maske (420 | 500) liegt in der Bildschirmmitte.
        val startX = frame.originX + LaunchTimeline.START_X * frame.unitScaled
        val startY = frame.originY + LaunchTimeline.START_Y * frame.unitScaled
        assertEquals(width / 2f, startX, 0.01f)
        assertEquals(height / 2f, startY, 0.01f)
    }

    @Test
    fun `die Ziege ist im ersten Frame so gross wie das Splash-Symbol`() {
        // Splash-Symbol: 288 dp, Ziege mit Skalierung 0,47 auf einem 1024er-Raster, 996 Einheiten breit.
        val goatWidthDp = 996f * LaunchTimeline.UNIT_DP
        assertEquals(996f * 0.47f / 1024f * 288f, goatWidthDp, 1e-3f)
        assertTrue("etwa 131 dp", goatWidthDp in 130f..133f)
    }

    @Test
    fun `Ausholen - bei 0,32 auf 0,88 verkleinert`() {
        assertEquals(LaunchTimeline.ANTICIPATION_SCALE, frame(0.32f).scale, 1e-4f)
    }

    @Test
    fun `die Ziegenfuellung blendet zwischen 0,26 und 0,40 aus`() {
        assertEquals(1f, frame(0.26f).coverOpacity, 1e-6f)
        assertEquals(0f, frame(0.40f).coverOpacity, 1e-6f)
        assertEquals(0f, frame(1f).coverOpacity, 1e-6f)
        assertTrue(frame(0.33f).coverOpacity in 0.01f..0.99f)
    }

    @Test
    fun `am Ende ist der ganze Bildschirm Loch`() {
        val frame = frame(1f)
        // Der Anker liegt in der Bildschirmmitte.
        assertEquals(width / 2f, frame.centerX, 0.01f)
        assertEquals(height / 2f, frame.centerY, 0.01f)

        // Der Innenkreis um den Anker deckt die am weitesten entfernte Ecke ab.
        val farthestCorner = hypot(width / 2f, height / 2f)
        val innerRadius = LaunchTimeline.ANCHOR_INNER_RADIUS * frame.unitScaled
        assertTrue("Innenkreis $innerRadius deckt Ecke $farthestCorner", innerRadius >= farthestCorner)

        assertEquals(0f, frame.edgeOpacity, 1e-6f)
    }

    @Test
    fun `die Endskalierung hat einen Mindestwert`() {
        // Auf einem winzigen Bildschirm reicht die Mindestskalierung.
        val small = LaunchFrame(1f, 100f, 100f, unit)
        assertEquals(LaunchTimeline.MINIMUM_END_SCALE, small.endScale, 1e-3f)
        // Auf einem grossen Tablet braucht es mehr.
        val tablet = LaunchFrame(1f, 2560f, 1600f, unit)
        assertTrue(tablet.endScale > LaunchTimeline.MINIMUM_END_SCALE)
    }

    @Test
    fun `die Skalierung waechst nach dem Ausholen stetig`() {
        var previous = frame(0.32f).scale
        for (step in 34..100 step 2) {
            val current = frame(step / 100f).scale
            assertTrue("bei $step %: $current > $previous", current > previous)
            previous = current
        }
    }

    @Test
    fun `das Kantenlicht ist nur im mittleren Zoom sichtbar`() {
        assertEquals(0f, frame(0f).edgeOpacity, 1e-6f)
        val visible = (30..100).map { frame(it / 100f).edgeOpacity }.maxOrNull() ?: 0f
        assertTrue("irgendwann sichtbar: $visible", visible > 0.5f)
    }

    @Test
    fun `die Haltephase blendet die Vollfarbe in den Verlauf`() {
        assertEquals(0f, frame(0f).wallBlend, 1e-6f)
        assertEquals(1f, frame(0.10f).wallBlend, 1e-6f)
        assertEquals(1f, frame(0.8f).wallBlend, 1e-6f)
    }

    @Test
    fun `die Startansicht skaliert von 1,08 auf 1,0`() {
        assertEquals(1.08f, LaunchTimeline.contentScale(0f), 1e-6f)
        assertEquals(1.08f, LaunchTimeline.contentScale(0.32f), 1e-6f)
        assertEquals(1f, LaunchTimeline.contentScale(1f), 1e-6f)
        assertTrue(LaunchTimeline.contentScale(0.6f) in 1f..1.08f)
    }

    @Test
    fun `Ease-Funktionen an den Raendern`() {
        assertEquals(0f, Ease.clamp(-1f, 0f, 1f), 0f)
        assertEquals(1f, Ease.clamp(2f, 0f, 1f), 0f)
        assertEquals(0.5f, Ease.clamp(0.5f, 0f, 1f), 0f)
        assertEquals(0f, Ease.inOut(0f), 1e-6f)
        assertEquals(0.5f, Ease.inOut(0.5f), 1e-6f)
        assertEquals(1f, Ease.inOut(1f), 1e-6f)
        assertEquals(1f, Ease.inCubic(1f), 1e-6f)
        assertEquals(1f, Ease.outCubic(1f), 1e-6f)
    }
}
