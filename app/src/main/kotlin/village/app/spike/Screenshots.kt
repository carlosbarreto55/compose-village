package village.app.spike

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** One state's comparison: how many pixels differ from the baseline, or -1 when either side is missing or sized differently. */
internal class ScreenshotDiff(val state: String, val pixels: Int)

/** Copies the window check's screenshots in [out] to [dir], replacing what was there. Returns how many it copied. */
internal fun saveBaseline(out: File, dir: File): Int {
    dir.mkdirs()
    dir.listFiles { f -> f.extension == "png" }?.forEach { it.delete() }
    val shots = pngs(out)
    shots.forEach { it.copyTo(File(dir, it.name), overwrite = true) }
    return shots.size
}

/**
 * Compares each screenshot in [out] with the one of the same name in [baseline], and each baseline
 * screenshot without a match. For every state that differs, writes a diff image to [diffDir]: the
 * baseline faded, with the differing pixels in red.
 */
internal fun compareScreenshots(out: File, baseline: File, diffDir: File): List<ScreenshotDiff> {
    diffDir.mkdirs()
    diffDir.listFiles { f -> f.extension == "png" }?.forEach { it.delete() }
    val names = (pngs(out) + pngs(baseline)).map { it.name }.toSortedSet()
    return names.map { name ->
        val now = File(out, name).takeIf { it.isFile }?.let(ImageIO::read)
        val before = File(baseline, name).takeIf { it.isFile }?.let(ImageIO::read)
        val state = name.removeSuffix(".png")
        if (now == null || before == null || now.width != before.width || now.height != before.height) {
            return@map ScreenshotDiff(state, -1)
        }
        val diff = BufferedImage(now.width, now.height, BufferedImage.TYPE_INT_RGB)
        var pixels = 0
        for (y in 0 until now.height) for (x in 0 until now.width) {
            val a = before.getRGB(x, y)
            if (a != now.getRGB(x, y)) {
                pixels++
                diff.setRGB(x, y, 0xFF0000)
            } else {
                diff.setRGB(x, y, faded(a))
            }
        }
        if (pixels > 0) ImageIO.write(diff, "png", File(diffDir, name))
        ScreenshotDiff(state, pixels)
    }
}

private fun pngs(dir: File): List<File> = dir.listFiles { f -> f.isFile && f.extension == "png" }?.sortedBy { it.name }.orEmpty()

/** [argb] blended 75% toward white, so the red differences stand out. */
private fun faded(argb: Int): Int {
    fun channel(shift: Int) = ((argb shr shift and 0xFF) + 3 * 255) / 4
    return (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}
