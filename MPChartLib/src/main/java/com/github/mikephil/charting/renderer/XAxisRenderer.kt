package com.github.mikephil.charting.renderer

import android.graphics.*
import android.graphics.Paint.Align
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.LimitLine.LimitLabelPosition
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.XAxis.XAxisPosition
import com.github.mikephil.charting.utils.*
import kotlin.math.roundToInt

open class XAxisRenderer(
    viewPortHandler: ViewPortHandler,
    @JvmField protected var mXAxis: XAxis,
    trans: Transformer?
) : AxisRenderer(viewPortHandler, trans, mXAxis) {
  private fun setupGridPaint() {
    mGridPaint!!.color = mXAxis.gridColor
    mGridPaint!!.strokeWidth = mXAxis.gridLineWidth
    mGridPaint!!.pathEffect = mXAxis.gridDashPathEffect
  }

  override fun computeAxis(min: Float, max: Float, inverted: Boolean) {
    if (mTrans == null) return
    // calculate the starting and entry point of the y-labels (depending on
    // zoom / contentrect bounds)
    var min = min
    var max = max
    if (mViewPortHandler.contentWidth() > 10 && !mViewPortHandler.isFullyZoomedOutX) {
      val p1 =
          mTrans!!.getValuesByTouchPoint(
              mViewPortHandler.contentLeft(), mViewPortHandler.contentTop())
      val p2 =
          mTrans!!.getValuesByTouchPoint(
              mViewPortHandler.contentRight(), mViewPortHandler.contentTop())
      if (inverted) {
        min = p2.x.toFloat()
        max = p1.x.toFloat()
      } else {
        min = p1.x.toFloat()
        max = p2.x.toFloat()
      }
      MPPointD.recycleInstance(p1)
      MPPointD.recycleInstance(p2)
    }
    computeAxisValues(min, max)
  }

  override fun computeAxisValues(min: Float, max: Float) {
    super.computeAxisValues(min, max)
    computeSize()
  }

  protected open fun computeSize() {
    val longest = mXAxis.longestLabel
    mAxisLabelPaint.typeface = mXAxis.typeface
    mAxisLabelPaint.textSize = mXAxis.textSize
    val labelSize = Utils.calcTextSize(mAxisLabelPaint, longest)
    val labelWidth = labelSize.width
    val labelHeight = Utils.calcTextHeight(mAxisLabelPaint, "Q").toFloat()
    val labelRotatedSize =
        Utils.getSizeOfRotatedRectangleByDegrees(labelWidth, labelHeight, mXAxis.labelRotationAngle)
    mXAxis.mLabelWidth = labelWidth.roundToInt()
    mXAxis.mLabelHeight = labelHeight.roundToInt()
    mXAxis.mLabelRotatedWidth = labelRotatedSize.width.roundToInt()
    mXAxis.mLabelRotatedHeight = labelRotatedSize.height.roundToInt()
    FSize.recycleInstance(labelRotatedSize)
    FSize.recycleInstance(labelSize)
  }

  override fun renderAxisLabels(c: Canvas?) {
    if (!mXAxis.isEnabled || !mXAxis.isDrawLabelsEnabled) return
    val yoffset = mXAxis.yOffset
    mAxisLabelPaint.typeface = mXAxis.typeface
    mAxisLabelPaint.textSize = mXAxis.textSize
    mAxisLabelPaint.color = mXAxis.textColor
    val pointF = MPPointF.getInstance(0f, 0f)
    if (mXAxis.position === XAxisPosition.TOP) {
      pointF.x = 0.5f
      pointF.y = 1.0f
      drawLabels(c, mViewPortHandler.contentTop() - yoffset, pointF)
    } else if (mXAxis.position === XAxisPosition.TOP_INSIDE) {
      pointF.x = 0.5f
      pointF.y = 1.0f
      drawLabels(c, mViewPortHandler.contentTop() + yoffset + mXAxis.mLabelRotatedHeight, pointF)
    } else if (mXAxis.position === XAxisPosition.BOTTOM) {
      pointF.x = 0.5f
      pointF.y = 0.0f
      drawLabels(c, mViewPortHandler.contentBottom() + yoffset, pointF)
    } else if (mXAxis.position === XAxisPosition.BOTTOM_INSIDE) {
      pointF.x = 0.5f
      pointF.y = 0.0f
      drawLabels(c, mViewPortHandler.contentBottom() - yoffset - mXAxis.mLabelRotatedHeight, pointF)
    } else { // BOTH SIDED
      pointF.x = 0.5f
      pointF.y = 1.0f
      drawLabels(c, mViewPortHandler.contentTop() - yoffset, pointF)
      pointF.x = 0.5f
      pointF.y = 0.0f
      drawLabels(c, mViewPortHandler.contentBottom() + yoffset, pointF)
    }
    MPPointF.recycleInstance(pointF)
  }

  override fun renderAxisLine(c: Canvas?) {
    if (!mXAxis.isDrawAxisLineEnabled || !mXAxis.isEnabled) return
    mAxisLinePaint!!.color = mXAxis.axisLineColor
    mAxisLinePaint!!.strokeWidth = mXAxis.axisLineWidth
    mAxisLinePaint!!.pathEffect = mXAxis.axisLineDashPathEffect
    if (mXAxis.position === XAxisPosition.TOP ||
        mXAxis.position === XAxisPosition.TOP_INSIDE ||
        mXAxis.position === XAxisPosition.BOTH_SIDED) {
      c!!.drawLine(
          mViewPortHandler.contentLeft(),
          mViewPortHandler.contentTop(),
          mViewPortHandler.contentRight(),
          mViewPortHandler.contentTop(),
          mAxisLinePaint!!)
    }
    if (mXAxis.position === XAxisPosition.BOTTOM ||
        mXAxis.position === XAxisPosition.BOTTOM_INSIDE ||
        mXAxis.position === XAxisPosition.BOTH_SIDED) {
      c!!.drawLine(
          mViewPortHandler.contentLeft(),
          mViewPortHandler.contentBottom(),
          mViewPortHandler.contentRight(),
          mViewPortHandler.contentBottom(),
          mAxisLinePaint!!)
    }
  }

  /**
   * draws the x-labels on the specified y-position
   *
   * @param pos
   */
  protected open fun drawLabels(c: Canvas?, pos: Float, anchor: MPPointF?) {
    val labelRotationAngleDegrees = mXAxis.labelRotationAngle
    val centeringEnabled = mXAxis.isCenterAxisLabelsEnabled
    val positions = FloatArray(mXAxis.mEntryCount * 2)
    run {
      var i = 0
      while (i < positions.size) {

        // only fill x values
        if (centeringEnabled) {
          positions[i] = mXAxis.mCenteredEntries[i / 2]
        } else {
          positions[i] = mXAxis.mEntries[i / 2]
        }
        i += 2
      }
    }
    mTrans?.pointValuesToPixel(positions)
    var i = 0
    while (i < positions.size) {
      var x = positions[i]
      if (mViewPortHandler.isInBoundsX(x)) {
        val label = mXAxis.valueFormatter!!.getFormattedValue(mXAxis.mEntries[i / 2], mXAxis)
        if (mXAxis.isAvoidFirstLastClippingEnabled) {

          // avoid clipping of the last
          if (i / 2 == mXAxis.mEntryCount - 1 && mXAxis.mEntryCount > 1) {
            val width = Utils.calcTextWidth(mAxisLabelPaint, label).toFloat()
            if (width > mViewPortHandler.offsetRight() * 2 &&
                x + width > mViewPortHandler.chartWidth)
                x -= width / 2

            // avoid clipping of the first
          } else if (i == 0) {
            val width = Utils.calcTextWidth(mAxisLabelPaint, label).toFloat()
            x += width / 2
          }
        }
        drawLabel(c, label, x, pos, anchor, labelRotationAngleDegrees)
      }
      i += 2
    }
  }

  protected fun drawLabel(
      c: Canvas?,
      formattedLabel: String?,
      x: Float,
      y: Float,
      anchor: MPPointF?,
      angleDegrees: Float
  ) {
    Utils.drawXAxisValue(c, formattedLabel, x, y, mAxisLabelPaint, anchor, angleDegrees)
  }

  private var mRenderGridLinesPath = Path()

  private var mRenderGridLinesBuffer = FloatArray(2)

  override fun renderGridLines(c: Canvas?) {
    if (!mXAxis.isDrawGridLinesEnabled || !mXAxis.isEnabled) return
    val clipRestoreCount = c!!.save()
    c.clipRect(gridClippingRect!!)
    if (mRenderGridLinesBuffer.size != mAxis.mEntryCount * 2) {
      mRenderGridLinesBuffer = FloatArray(mXAxis.mEntryCount * 2)
    }
    val positions = mRenderGridLinesBuffer
    run {
      var i = 0
      while (i < positions.size) {
        positions[i] = mXAxis.mEntries[i / 2]
        positions[i + 1] = mXAxis.mEntries[i / 2]
        i += 2
      }
    }
    mTrans?.pointValuesToPixel(positions)
    setupGridPaint()
    val gridLinePath = mRenderGridLinesPath
    gridLinePath.reset()
    var i = 0
    while (i < positions.size) {
      drawGridLine(c, positions[i], positions[i + 1], gridLinePath)
      i += 2
    }
    c.restoreToCount(clipRestoreCount)
  }

  @JvmField protected var mGridClippingRect = RectF()

  open val gridClippingRect: RectF?
    get() {
      mGridClippingRect.set(mViewPortHandler.contentRect)
      mGridClippingRect.inset(-mAxis.gridLineWidth, 0f)
      return mGridClippingRect
    }

  /**
   * Draws the grid line at the specified position using the provided path.
   *
   * @param c
   * @param x
   * @param y
   * @param gridLinePath
   */
  protected open fun drawGridLine(c: Canvas?, x: Float, y: Float, gridLinePath: Path) {
    gridLinePath.moveTo(x, mViewPortHandler.contentBottom())
    gridLinePath.lineTo(x, mViewPortHandler.contentTop())

    // draw a path because lines don't support dashing on lower android versions
    c!!.drawPath(gridLinePath, mGridPaint!!)
    gridLinePath.reset()
  }

  @JvmField protected var mRenderLimitLinesBuffer = FloatArray(2)

  @JvmField protected var mLimitLineClippingRect = RectF()

  /**
   * Draws the LimitLines associated with this axis to the screen.
   *
   * @param c
   */
  override fun renderLimitLines(c: Canvas?) {
    val limitLines = mXAxis.limitLines
    if (limitLines.isEmpty()) return
    val position = mRenderLimitLinesBuffer
    position[0] = 0f
    position[1] = 0f
    for (i in limitLines.indices) {
      val l = limitLines[i]
      if (!l.isEnabled) continue
      val clipRestoreCount = c!!.save()
      mLimitLineClippingRect.set(mViewPortHandler.contentRect)
      mLimitLineClippingRect.inset(-l.lineWidth, 0f)
      c.clipRect(mLimitLineClippingRect)
      position[0] = l.limit
      position[1] = 0f
      mTrans?.pointValuesToPixel(position)
      renderLimitLineLine(c, l, position)
      renderLimitLineLabel(c, l, position, 2f + l.yOffset)
      c.restoreToCount(clipRestoreCount)
    }
  }

  var mLimitLineSegmentsBuffer = FloatArray(4)

  private val mLimitLinePath = Path()

  private fun renderLimitLineLine(c: Canvas?, limitLine: LimitLine, position: FloatArray) {
    mLimitLineSegmentsBuffer[0] = position[0]
    mLimitLineSegmentsBuffer[1] = mViewPortHandler.contentTop()
    mLimitLineSegmentsBuffer[2] = position[0]
    mLimitLineSegmentsBuffer[3] = mViewPortHandler.contentBottom()
    mLimitLinePath.reset()
    mLimitLinePath.moveTo(mLimitLineSegmentsBuffer[0], mLimitLineSegmentsBuffer[1])
    mLimitLinePath.lineTo(mLimitLineSegmentsBuffer[2], mLimitLineSegmentsBuffer[3])
    mLimitLinePaint!!.style = Paint.Style.STROKE
    mLimitLinePaint!!.color = limitLine.lineColor
    mLimitLinePaint!!.strokeWidth = limitLine.lineWidth
    mLimitLinePaint!!.pathEffect = limitLine.dashPathEffect
    c!!.drawPath(mLimitLinePath, mLimitLinePaint!!)
  }

  private fun renderLimitLineLabel(
      c: Canvas?,
      limitLine: LimitLine,
      position: FloatArray,
      yOffset: Float
  ) {
    val label = limitLine.label

    // if drawing the limit-value label is enabled
    if (label.isNotEmpty()) {
      mLimitLinePaint!!.style = limitLine.textStyle
      mLimitLinePaint!!.pathEffect = null
      mLimitLinePaint!!.color = limitLine.textColor
      mLimitLinePaint!!.strokeWidth = 0.5f
      mLimitLinePaint!!.textSize = limitLine.textSize
      val xOffset = limitLine.lineWidth + limitLine.xOffset
      val labelPosition = limitLine.labelPosition
      if (labelPosition === LimitLabelPosition.RIGHT_TOP) {
        val labelLineHeight = Utils.calcTextHeight(mLimitLinePaint, label).toFloat()
        mLimitLinePaint!!.textAlign = Align.LEFT
        c!!.drawText(
            label,
            position[0] + xOffset,
            mViewPortHandler.contentTop() + yOffset + labelLineHeight,
            mLimitLinePaint!!)
      } else if (labelPosition === LimitLabelPosition.RIGHT_BOTTOM) {
        mLimitLinePaint!!.textAlign = Align.LEFT
        c!!.drawText(
            label,
            position[0] + xOffset,
            mViewPortHandler.contentBottom() - yOffset,
            mLimitLinePaint!!)
      } else if (labelPosition === LimitLabelPosition.LEFT_TOP) {
        mLimitLinePaint!!.textAlign = Align.RIGHT
        val labelLineHeight = Utils.calcTextHeight(mLimitLinePaint, label).toFloat()
        c!!.drawText(
            label,
            position[0] - xOffset,
            mViewPortHandler.contentTop() + yOffset + labelLineHeight,
            mLimitLinePaint!!)
      } else {
        mLimitLinePaint!!.textAlign = Align.RIGHT
        c!!.drawText(
            label,
            position[0] - xOffset,
            mViewPortHandler.contentBottom() - yOffset,
            mLimitLinePaint!!)
      }
    }
  }

  init {
    mAxisLabelPaint.color = Color.BLACK
    mAxisLabelPaint.textAlign = Align.CENTER
    mAxisLabelPaint.textSize = Utils.convertDpToPixel(10f)
  }
}