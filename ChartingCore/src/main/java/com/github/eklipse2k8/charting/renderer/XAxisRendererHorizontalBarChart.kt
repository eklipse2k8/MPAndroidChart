package com.github.eklipse2k8.charting.renderer

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Path
import android.graphics.RectF
import com.github.eklipse2k8.charting.charts.BarChart
import com.github.eklipse2k8.charting.components.LimitLine.LimitLabelPosition
import com.github.eklipse2k8.charting.components.XAxis
import com.github.eklipse2k8.charting.components.XAxis.XAxisPosition
import com.github.eklipse2k8.charting.utils.*

class XAxisRendererHorizontalBarChart(
    viewPortHandler: ViewPortHandler,
    xAxis: XAxis,
    trans: Transformer,
    private var mChart: BarChart,
) : XAxisRenderer(viewPortHandler, xAxis, trans) {
  override fun computeAxis(min: Float, max: Float, inverted: Boolean) {
    if (mTrans == null) return
    // calculate the starting and entry point of the y-labels (depending on
    // zoom / contentrect bounds)
    var min = min
    var max = max
    if (viewPortHandler.contentWidth() > 10 && !viewPortHandler.isFullyZoomedOutY) {
      val p1 =
          mTrans!!.getValuesByTouchPoint(
              viewPortHandler.contentLeft(), viewPortHandler.contentBottom())
      val p2 =
          mTrans!!.getValuesByTouchPoint(
              viewPortHandler.contentLeft(), viewPortHandler.contentTop())
      if (inverted) {
        min = p2.y.toFloat()
        max = p1.y.toFloat()
      } else {
        min = p1.y.toFloat()
        max = p2.y.toFloat()
      }
      MPPointD.recycleInstance(p1)
      MPPointD.recycleInstance(p2)
    }
    computeAxisValues(min, max)
  }

  override fun computeSize() {
    mAxisLabelPaint.typeface = mXAxis.typeface
    mAxisLabelPaint.textSize = mXAxis.textSize
    val longest = mXAxis.longestLabel
    val labelSize = Utils.calcTextSize(mAxisLabelPaint, longest)
    val labelWidth = labelSize.width + mXAxis.xOffset * 3.5f
    val labelHeight = labelSize.height
    val labelRotatedSize =
        Utils.getSizeOfRotatedRectangleByDegrees(
            labelSize.width, labelHeight, mXAxis.labelRotationAngle)
    mXAxis.mLabelWidth = Math.round(labelWidth)
    mXAxis.mLabelHeight = Math.round(labelHeight)
    mXAxis.mLabelRotatedWidth = (labelRotatedSize.width + mXAxis.xOffset * 3.5f).toInt()
    mXAxis.mLabelRotatedHeight = Math.round(labelRotatedSize.height)
    FSize.recycleInstance(labelRotatedSize)
  }

  override fun renderAxisLabels(c: Canvas?) {
    if (!mXAxis.isEnabled || !mXAxis.isDrawLabelsEnabled) return
    val xoffset = mXAxis.xOffset
    mAxisLabelPaint.typeface = mXAxis.typeface
    mAxisLabelPaint.textSize = mXAxis.textSize
    mAxisLabelPaint.color = mXAxis.textColor
    val pointF = MPPointF.getInstance(0f, 0f)
    if (mXAxis.position === XAxisPosition.TOP) {
      pointF.x = 0.0f
      pointF.y = 0.5f
      drawLabels(c, viewPortHandler.contentRight() + xoffset, pointF)
    } else if (mXAxis.position === XAxisPosition.TOP_INSIDE) {
      pointF.x = 1.0f
      pointF.y = 0.5f
      drawLabels(c, viewPortHandler.contentRight() - xoffset, pointF)
    } else if (mXAxis.position === XAxisPosition.BOTTOM) {
      pointF.x = 1.0f
      pointF.y = 0.5f
      drawLabels(c, viewPortHandler.contentLeft() - xoffset, pointF)
    } else if (mXAxis.position === XAxisPosition.BOTTOM_INSIDE) {
      pointF.x = 1.0f
      pointF.y = 0.5f
      drawLabels(c, viewPortHandler.contentLeft() + xoffset, pointF)
    } else { // BOTH SIDED
      pointF.x = 0.0f
      pointF.y = 0.5f
      drawLabels(c, viewPortHandler.contentRight() + xoffset, pointF)
      pointF.x = 1.0f
      pointF.y = 0.5f
      drawLabels(c, viewPortHandler.contentLeft() - xoffset, pointF)
    }
    MPPointF.recycleInstance(pointF)
  }

  override fun drawLabels(c: Canvas?, pos: Float, anchor: MPPointF?) {
    val labelRotationAngleDegrees = mXAxis.labelRotationAngle
    val centeringEnabled = mXAxis.isCenterAxisLabelsEnabled
    val positions = FloatArray(mXAxis.mEntryCount * 2)
    run {
      var i = 0
      while (i < positions.size) {

        // only fill x values
        if (centeringEnabled) {
          positions[i + 1] = mXAxis.mCenteredEntries[i / 2]
        } else {
          positions[i + 1] = mXAxis.mEntries[i / 2]
        }
        i += 2
      }
    }
    mTrans?.pointValuesToPixel(positions)
    var i = 0
    while (i < positions.size) {
      val y = positions[i + 1]
      if (viewPortHandler.isInBoundsY(y)) {
        val label = mXAxis.valueFormatter!!.getFormattedValue(mXAxis.mEntries[i / 2], mXAxis)
        drawLabel(c, label, pos, y, anchor, labelRotationAngleDegrees)
      }
      i += 2
    }
  }

  override val gridClippingRect: RectF
    get() {
      mGridClippingRect.set(viewPortHandler.contentRect)
      mGridClippingRect.inset(0f, -mAxis.gridLineWidth)
      return mGridClippingRect
    }

  override fun drawGridLine(c: Canvas?, x: Float, y: Float, gridLinePath: Path) {
    gridLinePath.moveTo(viewPortHandler.contentRight(), y)
    gridLinePath.lineTo(viewPortHandler.contentLeft(), y)

    // draw a path because lines don't support dashing on lower android versions
    c!!.drawPath(gridLinePath, mGridPaint!!)
    gridLinePath.reset()
  }

  override fun renderAxisLine(c: Canvas?) {
    if (!mXAxis.isDrawAxisLineEnabled || !mXAxis.isEnabled) return
    mAxisLinePaint!!.color = mXAxis.axisLineColor
    mAxisLinePaint!!.strokeWidth = mXAxis.axisLineWidth
    if (mXAxis.position === XAxisPosition.TOP ||
        mXAxis.position === XAxisPosition.TOP_INSIDE ||
        mXAxis.position === XAxisPosition.BOTH_SIDED) {
      c!!.drawLine(
          viewPortHandler.contentRight(),
          viewPortHandler.contentTop(),
          viewPortHandler.contentRight(),
          viewPortHandler.contentBottom(),
          mAxisLinePaint!!)
    }
    if (mXAxis.position === XAxisPosition.BOTTOM ||
        mXAxis.position === XAxisPosition.BOTTOM_INSIDE ||
        mXAxis.position === XAxisPosition.BOTH_SIDED) {
      c!!.drawLine(
          viewPortHandler.contentLeft(),
          viewPortHandler.contentTop(),
          viewPortHandler.contentLeft(),
          viewPortHandler.contentBottom(),
          mAxisLinePaint!!)
    }
  }

  private var mRenderLimitLinesPathBuffer = Path()

  /**
   * Draws the LimitLines associated with this axis to the screen. This is the standard YAxis
   * renderer using the XAxis limit lines.
   *
   * @param c
   */
  override fun renderLimitLines(c: Canvas?) {
    val limitLines = mXAxis.limitLines
    if (limitLines.isEmpty()) return
    val pts = mRenderLimitLinesBuffer
    pts[0] = 0f
    pts[1] = 0f
    val limitLinePath = mRenderLimitLinesPathBuffer
    limitLinePath.reset()
    for (i in limitLines.indices) {
      val l = limitLines[i]
      if (!l.isEnabled) continue
      val clipRestoreCount = c!!.save()
      mLimitLineClippingRect.set(viewPortHandler.contentRect)
      mLimitLineClippingRect.inset(0f, -l.lineWidth)
      c.clipRect(mLimitLineClippingRect)
      mLimitLinePaint!!.style = Paint.Style.STROKE
      mLimitLinePaint!!.color = l.lineColor
      mLimitLinePaint!!.strokeWidth = l.lineWidth
      mLimitLinePaint!!.pathEffect = l.dashPathEffect
      pts[1] = l.limit
      mTrans?.pointValuesToPixel(pts)
      limitLinePath.moveTo(viewPortHandler.contentLeft(), pts[1])
      limitLinePath.lineTo(viewPortHandler.contentRight(), pts[1])
      c.drawPath(limitLinePath, mLimitLinePaint!!)
      limitLinePath.reset()
      // c.drawLines(pts, mLimitLinePaint);
      val label = l.label

      // if drawing the limit-value label is enabled
      if (label.isNotEmpty()) {
        mLimitLinePaint!!.style = l.textStyle
        mLimitLinePaint!!.pathEffect = null
        mLimitLinePaint!!.color = l.textColor
        mLimitLinePaint!!.strokeWidth = 0.5f
        mLimitLinePaint!!.textSize = l.textSize
        val labelLineHeight = Utils.calcTextHeight(mLimitLinePaint, label).toFloat()
        val xOffset = Utils.convertDpToPixel(4f) + l.xOffset
        val yOffset = l.lineWidth + labelLineHeight + l.yOffset
        val position = l.labelPosition
        if (position === LimitLabelPosition.RIGHT_TOP) {
          mLimitLinePaint!!.textAlign = Align.RIGHT
          c.drawText(
              label,
              viewPortHandler.contentRight() - xOffset,
              pts[1] - yOffset + labelLineHeight,
              mLimitLinePaint!!)
        } else if (position === LimitLabelPosition.RIGHT_BOTTOM) {
          mLimitLinePaint!!.textAlign = Align.RIGHT
          c.drawText(
              label, viewPortHandler.contentRight() - xOffset, pts[1] + yOffset, mLimitLinePaint!!)
        } else if (position === LimitLabelPosition.LEFT_TOP) {
          mLimitLinePaint!!.textAlign = Align.LEFT
          c.drawText(
              label,
              viewPortHandler.contentLeft() + xOffset,
              pts[1] - yOffset + labelLineHeight,
              mLimitLinePaint!!)
        } else {
          mLimitLinePaint!!.textAlign = Align.LEFT
          c.drawText(
              label, viewPortHandler.offsetLeft() + xOffset, pts[1] + yOffset, mLimitLinePaint!!)
        }
      }
      c.restoreToCount(clipRestoreCount)
    }
  }
}
