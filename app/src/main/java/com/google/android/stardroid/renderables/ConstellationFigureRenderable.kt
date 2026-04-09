package com.google.android.stardroid.renderables

import com.google.android.stardroid.math.Vector3
import com.google.android.stardroid.math.getGeocentricCoords
import com.google.android.stardroid.renderer.RendererObjectManager.UpdateType
import java.util.EnumSet

class ConstellationFigureRenderable(
    private val constellationId: String,
    private val strokes: List<FigureStroke>,
    private val lineColor: Int,
    private val lineWidth: Float
) : AbstractAstronomicalRenderable() {

    private val linePrimitiveList = ArrayList<LinePrimitive>()
    private val center: Vector3

    init {
        var raSum = 0.0; var decSum = 0.0; var count = 0
        for (stroke in strokes) {
            for (vertex in stroke.vertices) {
                raSum += vertex.ra; decSum += vertex.dec; count++
            }
        }
        center = if (count > 0) {
            getGeocentricCoords((raSum / count).toFloat(), (decSum / count).toFloat())
        } else Vector3(1f, 0f, 0f)
    }

    override val names: List<String> get() = emptyList()
    override val searchLocation: Vector3 get() = center

    override fun initialize(): Renderable {
        linePrimitiveList.clear()
        for (stroke in strokes) {
            if (stroke.vertices.size < 2) continue
            val vertices = ArrayList<Vector3>(stroke.vertices.size)
            for (vertex in stroke.vertices) {
                vertices.add(getGeocentricCoords(vertex.ra, vertex.dec))
            }
            linePrimitiveList.add(LinePrimitive(lineColor, vertices, lineWidth))
        }
        return this
    }

    override fun update(): EnumSet<UpdateType> = EnumSet.noneOf(UpdateType::class.java)
    override val lines: List<LinePrimitive> get() = linePrimitiveList

    data class FigureStroke(val comment: String, val vertices: List<FigureVertex>)
    data class FigureVertex(val ra: Float, val dec: Float)
}
