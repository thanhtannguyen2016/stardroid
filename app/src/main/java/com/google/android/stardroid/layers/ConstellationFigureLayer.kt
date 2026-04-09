package com.google.android.stardroid.layers

import android.content.SharedPreferences
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Color
import android.util.Log
import com.google.android.stardroid.R
import com.google.android.stardroid.renderables.AstronomicalRenderable
import com.google.android.stardroid.renderables.ConstellationFigureRenderable
import com.google.android.stardroid.renderables.ConstellationFigureRenderable.FigureStroke
import com.google.android.stardroid.renderables.ConstellationFigureRenderable.FigureVertex
import com.google.android.stardroid.util.MiscUtil
import org.json.JSONArray
import java.io.IOException

class ConstellationFigureLayer(
    private val assetManager: AssetManager,
    resources: Resources,
    preferences: SharedPreferences
) : AbstractRenderablesLayer(resources, false, preferences) {

    override val layerDepthOrder = 12
    override val layerNameId = R.string.show_constellation_figures_pref
    override val preferenceId = "source_provider.constellation_figures"

    override fun initializeAstroSources(sources: ArrayList<AstronomicalRenderable>) {
        try {
            val jsonStr = assetManager.open(FIGURES_FILE).bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val colorStr = obj.optString("color", "0x40FFFFFF")
                val lineWidth = obj.optDouble("line_width", 1.2).toFloat()
                val color = parseColor(colorStr)
                val strokesArray = obj.getJSONArray("strokes")
                val strokes = ArrayList<FigureStroke>(strokesArray.length())
                for (j in 0 until strokesArray.length()) {
                    val strokeObj = strokesArray.getJSONObject(j)
                    val comment = strokeObj.optString("comment", "")
                    val verticesArray = strokeObj.getJSONArray("vertices")
                    val vertices = ArrayList<FigureVertex>(verticesArray.length())
                    for (k in 0 until verticesArray.length()) {
                        val v = verticesArray.getJSONObject(k)
                        vertices.add(FigureVertex(v.getDouble("ra").toFloat(), v.getDouble("dec").toFloat()))
                    }
                    strokes.add(FigureStroke(comment, vertices))
                }
                sources.add(ConstellationFigureRenderable(id, strokes, color, lineWidth))
            }
            Log.d(TAG, "Loaded ${sources.size} constellation figures")
        } catch (e: IOException) {
            Log.e(TAG, "Error reading constellation figures file", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing constellation figures data", e)
        }
    }

    private fun parseColor(colorStr: String): Int {
        return try {
            val cleaned = colorStr.removePrefix("0x").removePrefix("0X")
            cleaned.toLong(16).toInt()
        } catch (e: NumberFormatException) {
            Color.argb(64, 255, 255, 255)
        }
    }

    companion object {
        private val TAG = MiscUtil.getTag(ConstellationFigureLayer::class.java)
        private const val FIGURES_FILE = "constellation_figures.json"
    }
}
