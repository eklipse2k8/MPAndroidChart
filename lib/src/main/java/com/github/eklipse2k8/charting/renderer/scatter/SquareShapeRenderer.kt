package com.github.eklipse2k8.charting.renderer.scatter

import android.graphics.Canvas
import android.graphics.Paint
import com.github.eklipse2k8.charting.interfaces.datasets.IScatterDataSet
import com.github.eklipse2k8.charting.utils.ColorTemplate
import com.github.eklipse2k8.charting.utils.Utils
import com.github.eklipse2k8.charting.utils.ViewPortHandler

/** Created by wajdic on 15/06/2016. Created at Time 09:08 */
class SquareShapeRenderer : IShapeRenderer {
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
    val shapeStrokeSizeHalf = shapeStrokeSize / 2f
    val shapeHoleColor = dataSet.scatterShapeHoleColor
    if (shapeSize > 0.0) {
      renderPaint.style = Paint.Style.STROKE
      renderPaint.strokeWidth = shapeStrokeSize
      c.drawRect(
          posX - shapeHoleSizeHalf - shapeStrokeSizeHalf,
          posY - shapeHoleSizeHalf - shapeStrokeSizeHalf,
          posX + shapeHoleSizeHalf + shapeStrokeSizeHalf,
          posY + shapeHoleSizeHalf + shapeStrokeSizeHalf,
          renderPaint)
      if (shapeHoleColor != ColorTemplate.COLOR_NONE) {
        renderPaint.style = Paint.Style.FILL
        renderPaint.color = shapeHoleColor
        c.drawRect(
            posX - shapeHoleSizeHalf,
            posY - shapeHoleSizeHalf,
            posX + shapeHoleSizeHalf,
            posY + shapeHoleSizeHalf,
            renderPaint)
      }
    } else {
      renderPaint.style = Paint.Style.FILL
      c.drawRect(
          posX - shapeHalf, posY - shapeHalf, posX + shapeHalf, posY + shapeHalf, renderPaint)
    }
  }
}
