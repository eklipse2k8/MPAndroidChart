package com.github.eklipse2k8.charting.renderer.scatter

import android.graphics.Canvas
import android.graphics.Paint
import com.github.eklipse2k8.charting.interfaces.datasets.IScatterDataSet
import com.github.eklipse2k8.charting.utils.Utils
import com.github.eklipse2k8.charting.utils.ViewPortHandler

/** Created by wajdic on 15/06/2016. Created at Time 09:08 */
class ChevronUpShapeRenderer : IShapeRenderer {
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
    val shapeHalf = dataSet.scatterShapeSize / 2f
    renderPaint.style = Paint.Style.STROKE
    renderPaint.strokeWidth = Utils.convertDpToPixel(1f)
    c.drawLine(posX, posY - 2 * shapeHalf, posX + 2 * shapeHalf, posY, renderPaint)
    c.drawLine(posX, posY - 2 * shapeHalf, posX - 2 * shapeHalf, posY, renderPaint)
  }
}