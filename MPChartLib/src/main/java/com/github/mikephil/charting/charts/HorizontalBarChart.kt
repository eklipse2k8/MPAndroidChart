package com.github.mikephil.charting.charts

import android.content.Context
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import com.github.mikephil.charting.components.Legend.*
import com.github.mikephil.charting.components.XAxis.XAxisPosition
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.highlight.HorizontalBarHighlighter
import com.github.mikephil.charting.renderer.DataRenderer
import com.github.mikephil.charting.renderer.HorizontalBarChartRenderer
import com.github.mikephil.charting.renderer.XAxisRendererHorizontalBarChart
import com.github.mikephil.charting.renderer.YAxisRendererHorizontalBarChart
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.TransformerHorizontalBarChart
import com.github.mikephil.charting.utils.Utils.convertDpToPixel
import com.github.mikephil.charting.utils.ViewPortHandler
import kotlin.math.max
import kotlin.math.min

private val TAG = HorizontalBarChart::class.java.simpleName

/**
 * BarChart with horizontal bar orientation. In this implementation, x- and y-axis are switched,
 * meaning the YAxis class represents the horizontal values and the XAxis class represents the
 * vertical values.
 *
 * @author Philipp Jahoda
 */
class HorizontalBarChart
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    BarChart(context, attrs, defStyleAttr) {

  override val dataRenderer: DataRenderer =
      HorizontalBarChartRenderer(this, mAnimator, mViewPortHandler)

  override val highlighter = HorizontalBarHighlighter(this)

  init {
    mViewPortHandler = ViewPortHandler()
    mLeftAxisTransformer = TransformerHorizontalBarChart(mViewPortHandler)
    mRightAxisTransformer = TransformerHorizontalBarChart(mViewPortHandler)
    rendererLeftYAxis =
        YAxisRendererHorizontalBarChart(mViewPortHandler, axisLeft!!, mLeftAxisTransformer)
    rendererRightYAxis =
        YAxisRendererHorizontalBarChart(mViewPortHandler, axisRight!!, mRightAxisTransformer)
    rendererXAxis =
        XAxisRendererHorizontalBarChart(mViewPortHandler, xAxis, mLeftAxisTransformer!!, this)
  }

  private val mOffsetsBuffer = RectF()

  override fun calculateLegendOffsets(offsets: RectF) {
    offsets.left = 0f
    offsets.right = 0f
    offsets.top = 0f
    offsets.bottom = 0f
    if (!mLegend.isEnabled || mLegend.isDrawInsideEnabled) return
    when (mLegend.orientation) {
      LegendOrientation.VERTICAL ->
          when (mLegend.horizontalAlignment) {
            LegendHorizontalAlignment.LEFT ->
                offsets.left +=
                    (mLegend.mNeededWidth.coerceAtMost(
                        mViewPortHandler.chartWidth * mLegend.maxSizePercent) + mLegend.xOffset)
            LegendHorizontalAlignment.RIGHT ->
                offsets.right +=
                    (mLegend.mNeededWidth.coerceAtMost(
                        mViewPortHandler.chartWidth * mLegend.maxSizePercent) + mLegend.xOffset)
            LegendHorizontalAlignment.CENTER ->
                when (mLegend.verticalAlignment) {
                  LegendVerticalAlignment.TOP ->
                      offsets.top +=
                          (mLegend.mNeededHeight.coerceAtMost(
                              mViewPortHandler.chartHeight * mLegend.maxSizePercent) +
                              mLegend.yOffset)
                  LegendVerticalAlignment.BOTTOM ->
                      offsets.bottom +=
                          (mLegend.mNeededHeight.coerceAtMost(
                              mViewPortHandler.chartHeight * mLegend.maxSizePercent) +
                              mLegend.yOffset)
                  else -> Unit
                }
          }
      LegendOrientation.HORIZONTAL ->
          when (mLegend.verticalAlignment) {
            LegendVerticalAlignment.TOP -> {
              offsets.top +=
                  (mLegend.mNeededHeight.coerceAtMost(
                      mViewPortHandler.chartHeight * mLegend.maxSizePercent) + mLegend.yOffset)
              if (axisLeft!!.isEnabled && axisLeft!!.isDrawLabelsEnabled)
                  offsets.top +=
                      axisLeft!!.getRequiredHeightSpace(rendererLeftYAxis!!.paintAxisLabels)
            }
            LegendVerticalAlignment.BOTTOM -> {
              offsets.bottom +=
                  (mLegend.mNeededHeight.coerceAtMost(
                      mViewPortHandler.chartHeight * mLegend.maxSizePercent) + mLegend.yOffset)
              if (axisRight!!.isEnabled && axisRight!!.isDrawLabelsEnabled)
                  offsets.bottom +=
                      axisRight!!.getRequiredHeightSpace(rendererRightYAxis!!.paintAxisLabels)
            }
            else -> {}
          }
    }
  }

  override fun calculateOffsets() {
    var offsetLeft = 0f
    var offsetRight = 0f
    var offsetTop = 0f
    var offsetBottom = 0f
    calculateLegendOffsets(mOffsetsBuffer)
    offsetLeft += mOffsetsBuffer.left
    offsetTop += mOffsetsBuffer.top
    offsetRight += mOffsetsBuffer.right
    offsetBottom += mOffsetsBuffer.bottom

    // offsets for y-labels
    if (axisLeft!!.needsOffset()) {
      offsetTop += axisLeft!!.getRequiredHeightSpace(rendererLeftYAxis!!.paintAxisLabels)
    }
    if (axisRight!!.needsOffset()) {
      offsetBottom += axisRight!!.getRequiredHeightSpace(rendererRightYAxis!!.paintAxisLabels)
    }
    val xlabelwidth = xAxis.mLabelRotatedWidth.toFloat()
    if (xAxis.isEnabled) {

      // offsets for x-labels
      if (xAxis.position === XAxisPosition.BOTTOM) {
        offsetLeft += xlabelwidth
      } else if (xAxis.position === XAxisPosition.TOP) {
        offsetRight += xlabelwidth
      } else if (xAxis.position === XAxisPosition.BOTH_SIDED) {
        offsetLeft += xlabelwidth
        offsetRight += xlabelwidth
      }
    }
    offsetTop += extraTopOffset
    offsetRight += extraRightOffset
    offsetBottom += extraBottomOffset
    offsetLeft += extraLeftOffset
    val minOffset = convertDpToPixel(minOffset)
    mViewPortHandler.restrainViewPort(
        max(minOffset, offsetLeft),
        max(minOffset, offsetTop),
        max(minOffset, offsetRight),
        max(minOffset, offsetBottom))
    if (mLogEnabled) {
      Log.i(
          TAG,
          "offsetLeft: " +
              offsetLeft +
              ", offsetTop: " +
              offsetTop +
              ", offsetRight: " +
              offsetRight +
              ", offsetBottom: " +
              offsetBottom)
      Log.i(TAG, "Content: " + mViewPortHandler.contentRect.toString())
    }
    prepareOffsetMatrix()
    prepareValuePxMatrix()
  }

  override fun prepareValuePxMatrix() {
    mRightAxisTransformer!!.prepareMatrixValuePx(
        axisRight!!.mAxisMinimum, axisRight!!.mAxisRange, xAxis.mAxisRange, xAxis.mAxisMinimum)
    mLeftAxisTransformer!!.prepareMatrixValuePx(
        axisLeft!!.mAxisMinimum, axisLeft!!.mAxisRange, xAxis.mAxisRange, xAxis.mAxisMinimum)
  }

  override fun getMarkerPosition(high: Highlight): FloatArray {
    return floatArrayOf(high.drawY, high.drawX)
  }

  override fun getBarBounds(e: BarEntry, outputRect: RectF) {
    val set = data?.getDataSetForEntry(e)
    if (set == null) {
      outputRect[Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE] = Float.MIN_VALUE
      return
    }
    val y = e.y
    val x = e.x
    val barWidth = data?.barWidth ?: 0f
    val top = x - barWidth / 2f
    val bottom = x + barWidth / 2f
    val left: Float = if (y >= 0) y else 0f
    val right: Float = if (y <= 0) y else 0f
    outputRect[left, top, right] = bottom
    getTransformer(set.axisDependency).rectValueToPixel(outputRect)
  }

  override var mGetPositionBuffer = FloatArray(2)

  /**
   * Returns a recyclable MPPointF instance.
   *
   * @param e
   * @param axis
   * @return
   */
  override fun getPosition(e: Entry?, axis: AxisDependency?): MPPointF? {
    if (e == null) return null
    val vals = mGetPositionBuffer
    vals[0] = e.y
    vals[1] = e.x
    getTransformer(axis).pointValuesToPixel(vals)
    return MPPointF.getInstance(vals[0], vals[1])
  }

  /**
   * Returns the Highlight object (contains x-index and DataSet index) of the selected value at the
   * given touch point inside the BarChart.
   *
   * @param x
   * @param y
   * @return
   */
  override fun getHighlightByTouchPoint(x: Float, y: Float): Highlight? {
    return if (data == null) {
      if (mLogEnabled) Log.e(TAG, "Can't select by touch. No data set.")
      null
    } else highlighter.getHighlight(y, x) // switch x and y
  }

  override val lowestVisibleX: Float
    get() {
      getTransformer(AxisDependency.LEFT)
          .getValuesByTouchPoint(
              mViewPortHandler.contentLeft(),
              mViewPortHandler.contentBottom(),
              posForGetLowestVisibleX)
      return max(xAxis.mAxisMinimum, posForGetLowestVisibleX.y.toFloat())
    }
  override val highestVisibleX: Float
    get() {
      getTransformer(AxisDependency.LEFT)
          .getValuesByTouchPoint(
              mViewPortHandler.contentLeft(),
              mViewPortHandler.contentTop(),
              posForGetHighestVisibleX)
      return min(xAxis.mAxisMaximum, posForGetHighestVisibleX.y.toFloat())
    }

  /** ###### VIEWPORT METHODS BELOW THIS ###### */
  override fun setVisibleXRangeMaximum(maxXRange: Float) {
    val xScale = xAxis.mAxisRange / maxXRange
    mViewPortHandler.setMinimumScaleY(xScale)
  }

  override fun setVisibleXRangeMinimum(minXRange: Float) {
    val xScale = xAxis.mAxisRange / minXRange
    mViewPortHandler.setMaximumScaleY(xScale)
  }

  override fun setVisibleXRange(minXRange: Float, maxXRange: Float) {
    val minScale = xAxis.mAxisRange / minXRange
    val maxScale = xAxis.mAxisRange / maxXRange
    mViewPortHandler.setMinMaxScaleY(minScale, maxScale)
  }

  override fun setVisibleYRangeMaximum(maxYRange: Float, axis: AxisDependency) {
    val yScale = getAxisRange(axis) / maxYRange
    mViewPortHandler.setMinimumScaleX(yScale)
  }

  override fun setVisibleYRangeMinimum(minYRange: Float, axis: AxisDependency) {
    val yScale = getAxisRange(axis) / minYRange
    mViewPortHandler.setMaximumScaleX(yScale)
  }

  override fun setVisibleYRange(minYRange: Float, maxYRange: Float, axis: AxisDependency) {
    val minScale = getAxisRange(axis) / minYRange
    val maxScale = getAxisRange(axis) / maxYRange
    mViewPortHandler.setMinMaxScaleX(minScale, maxScale)
  }
}
