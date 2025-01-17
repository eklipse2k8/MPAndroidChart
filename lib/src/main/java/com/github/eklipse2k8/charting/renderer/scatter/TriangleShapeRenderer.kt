package com.github.eklipse2k8.charting.renderer.scatter

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.github.eklipse2k8.charting.interfaces.datasets.IScatterDataSet
import com.github.eklipse2k8.charting.utils.ColorTemplate
import com.github.eklipse2k8.charting.utils.Utils
import com.github.eklipse2k8.charting.utils.ViewPortHandler

/** Created by wajdic on 15/06/2016. Created at Time 09:08 */
class TriangleShapeRenderer : IShapeRenderer {
  private var mTrianglePathBuffer = Path()
  override fun renderShape(
      c: Canvas,
      dataSet: IScatterDataSet,
      viewPortHandler: ViewPortHandler,
      posX: Float,
      posY: Float,
      renderPaint: Paint?
  ) {
    if (renderPaint == null) {
      return
    }
    val shapeSize = dataSet.scatterShapeSize
    val shapeHalf = shapeSize / 2f
    val shapeHoleSizeHalf = Utils.convertDpToPixel(dataSet.scatterShapeHoleRadius)
    val shapeHoleSize = shapeHoleSizeHalf * 2f
    val shapeStrokeSize = (shapeSize - shapeHoleSize) / 2f
    val shapeHoleColor = dataSet.scatterShapeHoleColor
    renderPaint.style = Paint.Style.FILL

    // create a triangle path
    val tri = mTrianglePathBuffer
    tri.reset()
    tri.moveTo(posX, posY - shapeHalf)
    tri.lineTo(posX + shapeHalf, posY + shapeHalf)
    tri.lineTo(posX - shapeHalf, posY + shapeHalf)
    if (shapeSize > 0.0) {
      tri.lineTo(posX, posY - shapeHalf)
      tri.moveTo(posX - shapeHalf + shapeStrokeSize, posY + shapeHalf - shapeStrokeSize)
      tri.lineTo(posX + shapeHalf - shapeStrokeSize, posY + shapeHalf - shapeStrokeSize)
      tri.lineTo(posX, posY - shapeHalf + shapeStrokeSize)
      tri.lineTo(posX - shapeHalf + shapeStrokeSize, posY + shapeHalf - shapeStrokeSize)
    }
    tri.close()
    c.drawPath(tri, renderPaint)
    tri.reset()
    if (shapeSize > 0.0 && shapeHoleColor != ColorTemplate.COLOR_NONE) {
      renderPaint.color = shapeHoleColor
      tri.moveTo(posX, posY - shapeHalf + shapeStrokeSize)
      tri.lineTo(posX + shapeHalf - shapeStrokeSize, posY + shapeHalf - shapeStrokeSize)
      tri.lineTo(posX - shapeHalf + shapeStrokeSize, posY + shapeHalf - shapeStrokeSize)
      tri.close()
      c.drawPath(tri, renderPaint)
      tri.reset()
    }
  }
}
