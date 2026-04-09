package com.google.android.stardroid.layers

import android.content.SharedPreferences
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.android.stardroid.R
import com.google.android.stardroid.renderables.AstronomicalRenderable
import com.google.android.stardroid.renderables.ConstellationArtRenderable
import com.google.android.stardroid.renderables.ConstellationArtRenderable.AnchorPoint
import com.google.android.stardroid.util.MiscUtil
import org.json.JSONArray
import java.io.IOException

/**
 * Layer that displays artistic illustrations overlaid on constellation positions.
 *
 * Each constellation artwork is positioned using anchor-based mapping:
 * 3 pixel positions on the artwork image are pinned to 3 real star
 * positions (RA/Dec). An affine transformation is computed so the
 * artwork features align precisely with the actual stars.
 *
 * This is the same approach used by Stellarium.
 */
class ConstellationArtLayer(
    private val assetManager: AssetManager,
    resources: Resources,
    preferences: SharedPreferences
) : AbstractRenderablesLayer(resources, false, preferences) {

    override val layerDepthOrder = 15 // Just above constellations (10) but below planets (60)

    override val layerNameId = R.string.show_constellation_art_pref

    override val preferenceId = "source_provider.constellation_art"

    override fun initializeAstroSources(sources: ArrayList<AstronomicalRenderable>) {
        try {
            val jsonStr = assetManager.open("constellation_art.json").bufferedReader().use {
                it.readText()
            }
            val jsonArray = JSONArray(jsonStr)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val imagePath = obj.getString("image")

                val sizeArray = obj.getJSONArray("image_size")
                val imgW = sizeArray.getDouble(0).toFloat()
                val imgH = sizeArray.getDouble(1).toFloat()

                val anchorsArray = obj.getJSONArray("anchors")
                val anchors = ArrayList<AnchorPoint>(anchorsArray.length())
                for (j in 0 until anchorsArray.length()) {
                    val anchorObj = anchorsArray.getJSONObject(j)
                    anchors.add(
                        AnchorPoint(
                            px = anchorObj.getDouble("px").toFloat(),
                            py = anchorObj.getDouble("py").toFloat(),
                            ra = anchorObj.getDouble("ra").toFloat(),
                            dec = anchorObj.getDouble("dec").toFloat()
                        )
                    )
                }

                if (anchors.size < 3) {
                    Log.w(TAG, "Skipping $id: need at least 3 anchors, got ${anchors.size}")
                    continue
                }

                try {
                    val inputStream = assetManager.open(imagePath)
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream.close()

                    if (bitmap != null) {
                        sources.add(
                            ConstellationArtRenderable(
                                id, bitmap, imgW, imgH, anchors
                            )
                        )
                        Log.d(TAG, "Loaded constellation art for: $id " +
                            "(${anchors.size} anchors, ${bitmap.width}x${bitmap.height})")
                    } else {
                        Log.w(TAG, "Failed to decode bitmap for: $id ($imagePath)")
                    }
                } catch (e: IOException) {
                    Log.w(TAG, "Could not load artwork for $id: ${e.message}")
                }
            }
            Log.d(TAG, "Loaded ${sources.size} constellation artworks")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading constellation art data", e)
        }
    }

    companion object {
        private val TAG = MiscUtil.getTag(ConstellationArtLayer::class.java)
    }
}
