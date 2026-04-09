package com.google.android.stardroid.renderables

import android.graphics.Bitmap
import com.google.android.stardroid.math.Vector3
import com.google.android.stardroid.math.getGeocentricCoords
import com.google.android.stardroid.renderer.RendererObjectManager.UpdateType
import java.util.EnumSet

/**
 * A renderable that displays a semi-transparent artistic illustration
 * overlaid on a constellation's position in the sky.
 *
 * The artwork is positioned using anchor-based mapping: 3 pixel positions
 * on the artwork image are pinned to 3 real star positions (RA/Dec).
 * An affine transformation is computed so the artwork features (claws,
 * tail, head, etc.) align precisely with the actual stars.
 *
 * This is the same approach used by Stellarium.
 */
class ConstellationArtRenderable(
    private val constellationName: String,
    private val bitmap: Bitmap,
    private val imgW: Float,
    private val imgH: Float,
    private val anchors: List<AnchorPoint>
) : AbstractAstronomicalRenderable() {

    private val imagePrimitiveList = ArrayList<ImagePrimitive>()
    private val center: Vector3

    init {
        // Calculate approximate center from anchors for search location
        var x = 0f; var y = 0f; var z = 0f
        for (anchor in anchors) {
            val coords = getGeocentricCoords(anchor.ra, anchor.dec)
            x += coords.x; y += coords.y; z += coords.z
        }
        val n = anchors.size.toFloat()
        center = Vector3(x / n, y / n, z / n)
    }

    override val names: List<String>
        get() = emptyList() // Names are already handled by ConstellationsLayer

    override val searchLocation: Vector3
        get() = center

    override fun initialize(): Renderable {
        if (anchors.size >= 3) {
            val star1 = getGeocentricCoords(anchors[0].ra, anchors[0].dec)
            val star2 = getGeocentricCoords(anchors[1].ra, anchors[1].dec)
            val star3 = getGeocentricCoords(anchors[2].ra, anchors[2].dec)

            val primitive = ImagePrimitive(
                bitmap,
                imgW, imgH,
                anchors[0].px, anchors[0].py, star1,
                anchors[1].px, anchors[1].py, star2,
                anchors[2].px, anchors[2].py, star3
            )
            primitive.requiresBlending = true
            imagePrimitiveList.add(primitive)
        }
        return this
    }

    override fun update(): EnumSet<UpdateType> {
        return EnumSet.noneOf(UpdateType::class.java)
    }

    override val images: List<ImagePrimitive>
        get() = imagePrimitiveList

    /**
     * Represents an anchor point mapping a pixel on the artwork image
     * to a real star position on the celestial sphere.
     */
    data class AnchorPoint(
        val px: Float,    // Pixel X on the artwork image
        val py: Float,    // Pixel Y on the artwork image
        val ra: Float,    // Right Ascension in degrees
        val dec: Float    // Declination in degrees
    )
}
