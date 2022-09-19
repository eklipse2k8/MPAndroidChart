package com.github.eklipse2k8.charting.renderer

import android.graphics.*
import android.graphics.Paint.Align
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.github.eklipse2k8.charting.animation.ChartAnimator
import com.github.eklipse2k8.charting.charts.PieChart
import com.github.eklipse2k8.charting.data.Entry
import com.github.eklipse2k8.charting.data.PieData
import com.github.eklipse2k8.charting.data.PieDataSet.ValuePosition
import com.github.eklipse2k8.charting.highlight.Highlight
import com.github.eklipse2k8.charting.interfaces.datasets.IPieDataSet
import com.github.eklipse2k8.charting.utils.ColorTemplate
import com.github.eklipse2k8.charting.utils.MPPointF
import com.github.eklipse2k8.charting.utils.Utils
import com.github.eklipse2k8.charting.utils.ViewPortHandler
import java.lang.ref.WeakReference
import kotlin.math.*

class PieChartRenderer(
    private var chart: PieChart,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : DataRenderer(animator, viewPortHandler) {
  /** paint for the hole in the center of the pie chart and the transparent circle */
  var paintHole: Paint
    private set

  var paintTransparentCircle: Paint
    private set

  private var valueLinePaint: Paint

  /** paint object for the text that can be displayed in the center of the chart */
  val paintCenterText: TextPaint

  /** paint object used for drwing the slice-text */
  val paintEntryLabels: Paint

  private var centerTextLayout: StaticLayout? = null
  private var centerTextLastValue: CharSequence? = null
  private val centerTextLastBounds = RectF()
  private val rectBuffer = arrayOf(RectF(), RectF(), RectF())

  /** Bitmap for drawing the center hole */
  private var drawBitmap: WeakReference<Bitmap?>? = null
  private var bitmapCanvas: Canvas? = null
  private val pathBuffer = Path()
  private val innerRectBuffer = RectF()
  private var drawCenterTextPathBuffer = Path()

  override fun initBuffers() = Unit

  override fun drawData(c: Canvas) {
    val width = viewPortHandler.chartWidth.toInt()
    val height = viewPortHandler.chartHeight.toInt()
    var bitmap = drawBitmap?.get()
    if (bitmap == null || bitmap.width != width || bitmap.height != height) {
      if (width > 0 && height > 0) {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawBitmap = WeakReference(bitmap)
        bitmapCanvas = Canvas(bitmap)
      } else return
    }
    bitmap?.eraseColor(Color.TRANSPARENT)
    chart.data?.dataSets?.forEach { set ->
      if (set.isVisible && set.entryCount > 0) drawDataSet(set)
    }
  }

  private fun calculateMinimumRadiusForSpacedSlice(
      center: MPPointF,
      radius: Float,
      angle: Float,
      arcStartPointX: Float,
      arcStartPointY: Float,
      startAngle: Float,
      sweepAngle: Float
  ): Float {
    val angleMiddle = startAngle + sweepAngle / 2f

    // Other point of the arc
    val arcEndPointX = center.x + radius * cos((startAngle + sweepAngle) * Utils.FDEG2RAD)
    val arcEndPointY = center.y + radius * sin((startAngle + sweepAngle) * Utils.FDEG2RAD)

    // Middle point on the arc
    val arcMidPointX = center.x + radius * cos(angleMiddle * Utils.FDEG2RAD)
    val arcMidPointY = center.y + radius * sin(angleMiddle * Utils.FDEG2RAD)

    // This is the base of the contained triangle
    val basePointsDistance =
        sqrt(
            (arcEndPointX - arcStartPointX).toDouble().pow(2.0) +
                (arcEndPointY - arcStartPointY).toDouble().pow(2.0))

    // After reducing space from both sides of the "slice",
    //   the angle of the contained triangle should stay the same.
    // So let's find out the height of that triangle.
    val containedTriangleHeight =
        (basePointsDistance / 2.0 * tan((180.0 - angle) / 2.0 * Utils.DEG2RAD)).toFloat()

    // Now we subtract that from the radius
    var spacedRadius = radius - containedTriangleHeight

    // And now subtract the height of the arc that's between the triangle and the outer circle
    spacedRadius -=
        sqrt(
                (arcMidPointX - (arcEndPointX + arcStartPointX) / 2f).toDouble().pow(2.0) +
                    (arcMidPointY - (arcEndPointY + arcStartPointY) / 2f).toDouble().pow(2.0))
            .toFloat()
    return spacedRadius
  }

  /**
   * Calculates the sliceSpace to use based on visible values and their size compared to the set
   * sliceSpace.
   *
   * @param dataSet
   * @return
   */
  private fun getSliceSpace(dataSet: IPieDataSet): Float {
    if (!dataSet.isAutomaticallyDisableSliceSpacingEnabled) return dataSet.sliceSpace
    val spaceSizeRatio = dataSet.sliceSpace / viewPortHandler.smallestContentExtension
    val minValueRatio: Float = dataSet.yMin / chart.data!!.yValueSum * 2
    return if (spaceSizeRatio > minValueRatio) 0f else dataSet.sliceSpace
  }

  private fun drawDataSet(dataSet: IPieDataSet) {
    var angle = 0f
    val rotationAngle = chart.rotationAngle
    val phaseX = animator.phaseX
    val phaseY = animator.phaseY
    val circleBox = chart.circleBox
    val entryCount = dataSet.entryCount
    val drawAngles = chart.drawAngles
    val center = chart.centerCircleBox
    val radius = chart.radius
    val drawInnerArc = chart.isDrawHoleEnabled && !chart.isDrawSlicesUnderHoleEnabled
    val userInnerRadius = if (drawInnerArc) radius * (chart.holeRadius / 100f) else 0f
    val roundedRadius = (radius - radius * chart.holeRadius / 100f) / 2f
    val roundedCircleBox = RectF()
    val drawRoundedSlices = drawInnerArc && chart.isDrawRoundedSlicesEnabled
    var visibleAngleCount = 0
    for (j in 0 until entryCount) {
      // draw only if the value is greater than zero
      if (abs(dataSet.getEntryForIndex(j).y) > Utils.FLOAT_EPSILON) {
        visibleAngleCount++
      }
    }
    val sliceSpace = if (visibleAngleCount <= 1) 0f else getSliceSpace(dataSet)
    for (j in 0 until entryCount) {
      val sliceAngle = drawAngles[j]
      var innerRadius = userInnerRadius
      val e: Entry = dataSet.getEntryForIndex(j)

      // draw only if the value is greater than zero
      if (abs(e.y) <= Utils.FLOAT_EPSILON) {
        angle += sliceAngle * phaseX
        continue
      }

      // Don't draw if it's highlighted, unless the chart uses rounded slices
      if (dataSet.isHighlightEnabled && chart.needsHighlight(j) && !drawRoundedSlices) {
        angle += sliceAngle * phaseX
        continue
      }
      val accountForSliceSpacing = sliceSpace > 0f && sliceAngle <= 180f
      renderPaint.color = dataSet.getColor(j)
      val sliceSpaceAngleOuter =
          if (visibleAngleCount == 1) 0f else sliceSpace / (Utils.FDEG2RAD * radius)
      val startAngleOuter = rotationAngle + (angle + sliceSpaceAngleOuter / 2f) * phaseY
      var sweepAngleOuter = (sliceAngle - sliceSpaceAngleOuter) * phaseY
      if (sweepAngleOuter < 0f) {
        sweepAngleOuter = 0f
      }
      pathBuffer.reset()
      if (drawRoundedSlices) {
        val x =
            center.x +
                (radius - roundedRadius) *
                    cos((startAngleOuter * Utils.FDEG2RAD).toDouble()).toFloat()
        val y =
            center.y +
                (radius - roundedRadius) *
                    sin((startAngleOuter * Utils.FDEG2RAD).toDouble()).toFloat()
        roundedCircleBox[x - roundedRadius, y - roundedRadius, x + roundedRadius] =
            y + roundedRadius
      }
      val arcStartPointX =
          center.x + radius * cos((startAngleOuter * Utils.FDEG2RAD).toDouble()).toFloat()
      val arcStartPointY =
          center.y + radius * sin((startAngleOuter * Utils.FDEG2RAD).toDouble()).toFloat()
      if (sweepAngleOuter >= 360f && sweepAngleOuter % 360f <= Utils.FLOAT_EPSILON) {
        // Android is doing "mod 360"
        pathBuffer.addCircle(center.x, center.y, radius, Path.Direction.CW)
      } else {
        if (drawRoundedSlices) {
          pathBuffer.arcTo(roundedCircleBox, startAngleOuter + 180, -180f)
        }
        pathBuffer.arcTo(circleBox, startAngleOuter, sweepAngleOuter)
      }

      // API < 21 does not receive floats in addArc, but a RectF
      innerRectBuffer[center.x - innerRadius, center.y - innerRadius, center.x + innerRadius] =
          center.y + innerRadius
      if (drawInnerArc && (innerRadius > 0f || accountForSliceSpacing)) {
        if (accountForSliceSpacing) {
          var minSpacedRadius =
              calculateMinimumRadiusForSpacedSlice(
                  center,
                  radius,
                  sliceAngle * phaseY,
                  arcStartPointX,
                  arcStartPointY,
                  startAngleOuter,
                  sweepAngleOuter)
          if (minSpacedRadius < 0f) minSpacedRadius = -minSpacedRadius
          innerRadius = max(innerRadius, minSpacedRadius)
        }
        val sliceSpaceAngleInner =
            if (visibleAngleCount == 1 || innerRadius == 0f) 0f
            else sliceSpace / (Utils.FDEG2RAD * innerRadius)
        val startAngleInner = rotationAngle + (angle + sliceSpaceAngleInner / 2f) * phaseY
        var sweepAngleInner = (sliceAngle - sliceSpaceAngleInner) * phaseY
        if (sweepAngleInner < 0f) {
          sweepAngleInner = 0f
        }
        val endAngleInner = startAngleInner + sweepAngleInner
        if (sweepAngleOuter >= 360f && sweepAngleOuter % 360f <= Utils.FLOAT_EPSILON) {
          // Android is doing "mod 360"
          pathBuffer.addCircle(center.x, center.y, innerRadius, Path.Direction.CCW)
        } else {
          if (drawRoundedSlices) {
            val x =
                center.x +
                    (radius - roundedRadius) *
                        cos((endAngleInner * Utils.FDEG2RAD).toDouble()).toFloat()
            val y =
                center.y +
                    (radius - roundedRadius) *
                        sin((endAngleInner * Utils.FDEG2RAD).toDouble()).toFloat()
            roundedCircleBox[x - roundedRadius, y - roundedRadius, x + roundedRadius] =
                y + roundedRadius
            pathBuffer.arcTo(roundedCircleBox, endAngleInner, 180f)
          } else
              pathBuffer.lineTo(
                  center.x +
                      innerRadius * cos((endAngleInner * Utils.FDEG2RAD).toDouble()).toFloat(),
                  center.y +
                      innerRadius * sin((endAngleInner * Utils.FDEG2RAD).toDouble()).toFloat())
          pathBuffer.arcTo(innerRectBuffer, endAngleInner, -sweepAngleInner)
        }
      } else {
        if (sweepAngleOuter % 360f > Utils.FLOAT_EPSILON) {
          if (accountForSliceSpacing) {
            val angleMiddle = startAngleOuter + sweepAngleOuter / 2f
            val sliceSpaceOffset =
                calculateMinimumRadiusForSpacedSlice(
                    center,
                    radius,
                    sliceAngle * phaseY,
                    arcStartPointX,
                    arcStartPointY,
                    startAngleOuter,
                    sweepAngleOuter)
            val arcEndPointX =
                center.x +
                    sliceSpaceOffset * cos((angleMiddle * Utils.FDEG2RAD).toDouble()).toFloat()
            val arcEndPointY =
                center.y +
                    sliceSpaceOffset * sin((angleMiddle * Utils.FDEG2RAD).toDouble()).toFloat()
            pathBuffer.lineTo(arcEndPointX, arcEndPointY)
          } else {
            pathBuffer.lineTo(center.x, center.y)
          }
        }
      }
      pathBuffer.close()
      bitmapCanvas!!.drawPath(pathBuffer, renderPaint)
      angle += sliceAngle * phaseX
    }
    MPPointF.recycleInstance(center)
  }

  override fun drawValues(c: Canvas) {
    val center = chart.centerCircleBox

    // get whole the radius
    val radius = chart.radius
    var rotationAngle = chart.rotationAngle
    val drawAngles = chart.drawAngles
    val absoluteAngles = chart.absoluteAngles
    val phaseX = animator.phaseX
    val phaseY = animator.phaseY
    val roundedRadius = (radius - radius * chart.holeRadius / 100f) / 2f
    val holeRadiusPercent = chart.holeRadius / 100f
    var labelRadiusOffset = radius / 10f * 3.6f
    if (chart.isDrawHoleEnabled) {
      labelRadiusOffset = (radius - radius * holeRadiusPercent) / 2f
      if (!chart.isDrawSlicesUnderHoleEnabled && chart.isDrawRoundedSlicesEnabled) {
        // Add curved circle slice and spacing to rotation angle, so that it sits nicely inside
        rotationAngle += (roundedRadius * 360 / (Math.PI * 2 * radius)).toFloat()
      }
    }
    val labelRadius = radius - labelRadiusOffset
    val data: PieData = chart.data!!
    val dataSets = data.dataSets
    val yValueSum = data.yValueSum
    val drawEntryLabels = chart.isDrawEntryLabelsEnabled
    var angle: Float
    var xIndex = 0
    c.save()
    val offset = Utils.convertDpToPixel(5f)
    for (i in dataSets.indices) {
      val dataSet = dataSets[i]
      val drawValues = dataSet.isDrawValuesEnabled
      if (!drawValues && !drawEntryLabels) continue
      val xValuePosition = dataSet.xValuePosition
      val yValuePosition = dataSet.yValuePosition

      // apply the text-styling defined by the DataSet
      applyValueTextStyle(dataSet)
      val lineHeight = (Utils.calcTextHeight(valuePaint, "Q") + Utils.convertDpToPixel(4f))
      val formatter = dataSet.valueFormatter
      val entryCount = dataSet.entryCount
      val isUseValueColorForLineEnabled = dataSet.isUseValueColorForLineEnabled
      val valueLineColor = dataSet.valueLineColor
      valueLinePaint.strokeWidth = Utils.convertDpToPixel(dataSet.valueLineWidth)
      val sliceSpace = getSliceSpace(dataSet)
      val iconsOffset =
          dataSet.iconsOffset?.let { MPPointF.getInstance(it) } ?: MPPointF.getInstance(0f, 0f)
      iconsOffset.x = Utils.convertDpToPixel(iconsOffset.x)
      iconsOffset.y = Utils.convertDpToPixel(iconsOffset.y)
      for (j in 0 until entryCount) {
        val entry = dataSet.getEntryForIndex(j)
        angle = if (xIndex == 0) 0f else absoluteAngles[xIndex - 1] * phaseX
        val sliceAngle = drawAngles[xIndex]
        val sliceSpaceMiddleAngle = sliceSpace / (Utils.FDEG2RAD * labelRadius)

        // offset needed to center the drawn text in the slice
        val angleOffset = (sliceAngle - sliceSpaceMiddleAngle / 2f) / 2f
        angle += angleOffset
        val transformedAngle = rotationAngle + angle * phaseY
        val value: Float =
            if (chart.isUsePercentValuesEnabled) entry.y / yValueSum * 100f else entry.y
        val entryLabel = entry.label
        val sliceXBase = cos((transformedAngle * Utils.FDEG2RAD).toDouble()).toFloat()
        val sliceYBase = sin((transformedAngle * Utils.FDEG2RAD).toDouble()).toFloat()
        val drawXOutside = drawEntryLabels && xValuePosition === ValuePosition.OUTSIDE_SLICE
        val drawYOutside = drawValues && yValuePosition === ValuePosition.OUTSIDE_SLICE
        val drawXInside = drawEntryLabels && xValuePosition === ValuePosition.INSIDE_SLICE
        val drawYInside = drawValues && yValuePosition === ValuePosition.INSIDE_SLICE
        if (drawXOutside || drawYOutside) {
          val valueLineLength1 = dataSet.valueLinePart1Length
          val valueLineLength2 = dataSet.valueLinePart2Length
          val valueLinePart1OffsetPercentage = dataSet.valueLinePart1OffsetPercentage / 100f
          var pt2x: Float
          var pt2y: Float
          var labelPtx: Float
          var labelPty: Float
          val line1Radius: Float =
              if (chart.isDrawHoleEnabled)
                  (radius - radius * holeRadiusPercent) * valueLinePart1OffsetPercentage +
                      radius * holeRadiusPercent
              else radius * valueLinePart1OffsetPercentage
          val polyline2Width =
              if (dataSet.isValueLineVariableLength)
                  labelRadius *
                      valueLineLength2 *
                      abs(sin((transformedAngle * Utils.FDEG2RAD).toDouble())).toFloat()
              else labelRadius * valueLineLength2
          val pt0x = line1Radius * sliceXBase + center.x
          val pt0y = line1Radius * sliceYBase + center.y
          val pt1x = labelRadius * (1 + valueLineLength1) * sliceXBase + center.x
          val pt1y = labelRadius * (1 + valueLineLength1) * sliceYBase + center.y
          if (transformedAngle % 360.0 in 90.0..270.0) {
            pt2x = pt1x - polyline2Width
            pt2y = pt1y
            valuePaint.textAlign = Align.RIGHT
            if (drawXOutside) paintEntryLabels.textAlign = Align.RIGHT
            labelPtx = pt2x - offset
            labelPty = pt2y
          } else {
            pt2x = pt1x + polyline2Width
            pt2y = pt1y
            valuePaint.textAlign = Align.LEFT
            if (drawXOutside) paintEntryLabels.textAlign = Align.LEFT
            labelPtx = pt2x + offset
            labelPty = pt2y
          }
          var lineColor = ColorTemplate.COLOR_NONE
          if (isUseValueColorForLineEnabled) lineColor = dataSet.getColor(j)
          else if (valueLineColor != ColorTemplate.COLOR_NONE) lineColor = valueLineColor
          if (lineColor != ColorTemplate.COLOR_NONE) {
            valueLinePaint.color = lineColor
            c.drawLine(pt0x, pt0y, pt1x, pt1y, valueLinePaint)
            c.drawLine(pt1x, pt1y, pt2x, pt2y, valueLinePaint)
          }

          // draw everything, depending on settings
          if (drawXOutside && drawYOutside) {
            drawValue(
                c, formatter!!, value, entry, 0, labelPtx, labelPty, dataSet.getValueTextColor(j))
            if (j < data.entryCount && entryLabel != null) {
              drawEntryLabel(c, entryLabel, labelPtx, labelPty + lineHeight)
            }
          } else if (drawXOutside) {
            if (j < data.entryCount && entryLabel != null) {
              drawEntryLabel(c, entryLabel, labelPtx, labelPty + lineHeight / 2f)
            }
          } else if (drawYOutside) {
            drawValue(
                c,
                formatter!!,
                value,
                entry,
                0,
                labelPtx,
                labelPty + lineHeight / 2f,
                dataSet.getValueTextColor(j))
          }
        }
        if (drawXInside || drawYInside) {
          // calculate the text position
          val x = labelRadius * sliceXBase + center.x
          val y = labelRadius * sliceYBase + center.y
          valuePaint.textAlign = Align.CENTER

          // draw everything, depending on settings
          if (drawXInside && drawYInside) {
            drawValue(c, formatter!!, value, entry, 0, x, y, dataSet.getValueTextColor(j))
            if (j < data.entryCount && entryLabel != null) {
              drawEntryLabel(c, entryLabel, x, y + lineHeight)
            }
          } else if (drawXInside) {
            if (j < data.entryCount && entryLabel != null) {
              drawEntryLabel(c, entryLabel, x, y + lineHeight / 2f)
            }
          } else if (drawYInside) {
            drawValue(
                c,
                formatter!!,
                value,
                entry,
                0,
                x,
                y + lineHeight / 2f,
                dataSet.getValueTextColor(j))
          }
        }
        if (entry.icon != null && dataSet.isDrawIconsEnabled) {
          val icon = entry.icon
          val x = (labelRadius + iconsOffset.y) * sliceXBase + center.x
          var y = (labelRadius + iconsOffset.y) * sliceYBase + center.y
          y += iconsOffset.x
          Utils.drawImage(c, icon, x.toInt(), y.toInt(), icon.intrinsicWidth, icon.intrinsicHeight)
        }
        xIndex++
      }
      MPPointF.recycleInstance(iconsOffset)
    }
    MPPointF.recycleInstance(center)
    c.restore()
  }

  /**
   * Draws an entry label at the specified position.
   *
   * @param c
   * @param label
   * @param x
   * @param y
   */
  private fun drawEntryLabel(c: Canvas, label: String?, x: Float, y: Float) {
    c.drawText(label!!, x, y, paintEntryLabels)
  }

  override fun drawExtras(c: Canvas) {
    drawHole()
    drawBitmap?.get()?.let { bitmap -> c.drawBitmap(bitmap, 0f, 0f, null) }
    drawCenterText(c)
  }

  private val holeCirclePath = Path()

  /** draws the hole in the center of the chart and the transparent circle / hole */
  private fun drawHole() {
    if (chart.isDrawHoleEnabled && bitmapCanvas != null) {
      val radius = chart.radius
      val holeRadius = radius * (chart.holeRadius / 100)
      val center = chart.centerCircleBox
      if (Color.alpha(paintHole.color) > 0) {
        // draw the hole-circle
        bitmapCanvas?.drawCircle(center.x, center.y, holeRadius, paintHole)
      }

      // only draw the circle if it can be seen (not covered by the hole)
      if (Color.alpha(paintTransparentCircle.color) > 0 &&
          chart.transparentCircleRadius > chart.holeRadius) {
        val alpha = paintTransparentCircle.alpha
        val secondHoleRadius = radius * (chart.transparentCircleRadius / 100)
        paintTransparentCircle.alpha =
            (alpha.toFloat() * animator.phaseX * animator.phaseY).toInt()

        // draw the transparent-circle
        holeCirclePath.reset()
        holeCirclePath.addCircle(center.x, center.y, secondHoleRadius, Path.Direction.CW)
        holeCirclePath.addCircle(center.x, center.y, holeRadius, Path.Direction.CCW)
        bitmapCanvas?.drawPath(holeCirclePath, paintTransparentCircle)

        // reset alpha
        paintTransparentCircle.alpha = alpha
      }
      MPPointF.recycleInstance(center)
    }
  }

  /**
   * draws the description text in the center of the pie chart makes most sense when center-hole is
   * enabled
   */
  private fun drawCenterText(c: Canvas) {
    val centerText = chart.centerText
    if (chart.isDrawCenterTextEnabled && centerText != null) {
      val center = chart.centerCircleBox
      val offset = chart.centerTextOffset
      val x = center.x + offset.x
      val y = center.y + offset.y
      val innerRadius =
          if (chart.isDrawHoleEnabled && !chart.isDrawSlicesUnderHoleEnabled)
              chart.radius * (chart.holeRadius / 100f)
          else chart.radius
      val holeRect = rectBuffer[0]
      holeRect.left = x - innerRadius
      holeRect.top = y - innerRadius
      holeRect.right = x + innerRadius
      holeRect.bottom = y + innerRadius
      val boundingRect = rectBuffer[1]
      boundingRect.set(holeRect)
      val radiusPercent = chart.centerTextRadiusPercent / 100f
      if (radiusPercent > 0.0) {
        boundingRect.inset(
            (boundingRect.width() - boundingRect.width() * radiusPercent) / 2f,
            (boundingRect.height() - boundingRect.height() * radiusPercent) / 2f)
      }
      if (centerText != centerTextLastValue || boundingRect != centerTextLastBounds) {

        // Next time we won't recalculate StaticLayout...
        centerTextLastBounds.set(boundingRect)
        centerTextLastValue = centerText
        val width = centerTextLastBounds.width()

        // If width is 0, it will crash. Always have a minimum of 1
        centerTextLayout =
            StaticLayout(
                centerText,
                0,
                centerText.length,
                paintCenterText,
                max(ceil(width.toDouble()), 1.0).toInt(),
                Layout.Alignment.ALIGN_CENTER,
                1f,
                0f,
                false)
      }

      // float layoutWidth = Utils.getStaticLayoutMaxWidth(mCenterTextLayout);
      val layoutHeight = centerTextLayout!!.height.toFloat()
      c.save()
      val path = drawCenterTextPathBuffer
      path.reset()
      path.addOval(holeRect, Path.Direction.CW)
      c.clipPath(path)
      c.translate(boundingRect.left, boundingRect.top + (boundingRect.height() - layoutHeight) / 2f)
      centerTextLayout!!.draw(c)
      c.restore()
      MPPointF.recycleInstance(center)
      MPPointF.recycleInstance(offset)
    }
  }

  private var mDrawHighlightedRectF = RectF()

  override fun drawHighlighted(c: Canvas, indices: Array<Highlight?>?) {
    /* Skip entirely if using rounded circle slices, because it doesn't make sense to highlight
     * in this way.
     * TODO: add support for changing slice color with highlighting rather than only shifting the slice
     */
    val drawInnerArc = chart.isDrawHoleEnabled && !chart.isDrawSlicesUnderHoleEnabled
    if (drawInnerArc && chart.isDrawRoundedSlicesEnabled) return
    val phaseX = animator.phaseX
    val phaseY = animator.phaseY
    var angle: Float
    val rotationAngle = chart.rotationAngle
    val drawAngles = chart.drawAngles
    val absoluteAngles = chart.absoluteAngles
    val center = chart.centerCircleBox
    val radius = chart.radius
    val userInnerRadius = if (drawInnerArc) radius * (chart.holeRadius / 100f) else 0f
    val highlightedCircleBox = mDrawHighlightedRectF
    highlightedCircleBox[0f, 0f, 0f] = 0f
    indices?.forEach { item ->
      // get the index to highlight
      val index = item?.x?.toInt() ?: return@forEach
      if (index >= drawAngles.size) return@forEach
      val set: IPieDataSet? = chart.data?.getDataSetByIndex(item.dataSetIndex)
      if (set == null || !set.isHighlightEnabled) return@forEach
      val entryCount = set.entryCount
      var visibleAngleCount = 0
      for (j in 0 until entryCount) {
        // draw only if the value is greater than zero
        if (abs(set.getEntryForIndex(j).y) > Utils.FLOAT_EPSILON) {
          visibleAngleCount++
        }
      }
      angle = if (index == 0) 0f else absoluteAngles[index - 1] * phaseX
      val sliceSpace = if (visibleAngleCount <= 1) 0f else set.sliceSpace
      val sliceAngle = drawAngles[index]
      var innerRadius = userInnerRadius
      val shift = set.selectionShift
      val highlightedRadius = radius + shift
      chart.circleBox.let { highlightedCircleBox.set(it) }
      highlightedCircleBox.inset(-shift, -shift)
      val accountForSliceSpacing = sliceSpace > 0f && sliceAngle <= 180f
      var highlightColor = set.highlightColor
      if (highlightColor == null) highlightColor = set.getColor(index)
      renderPaint.color = highlightColor
      val sliceSpaceAngleOuter =
          if (visibleAngleCount == 1) 0f else sliceSpace / (Utils.FDEG2RAD * radius)
      val sliceSpaceAngleShifted =
          if (visibleAngleCount == 1) 0f else sliceSpace / (Utils.FDEG2RAD * highlightedRadius)
      val startAngleOuter = rotationAngle + (angle + sliceSpaceAngleOuter / 2f) * phaseY
      var sweepAngleOuter = (sliceAngle - sliceSpaceAngleOuter) * phaseY
      if (sweepAngleOuter < 0f) {
        sweepAngleOuter = 0f
      }
      val startAngleShifted = rotationAngle + (angle + sliceSpaceAngleShifted / 2f) * phaseY
      var sweepAngleShifted = (sliceAngle - sliceSpaceAngleShifted) * phaseY
      if (sweepAngleShifted < 0f) {
        sweepAngleShifted = 0f
      }
      pathBuffer.reset()
      if (sweepAngleOuter >= 360f && sweepAngleOuter % 360f <= Utils.FLOAT_EPSILON) {
        // Android is doing "mod 360"
        pathBuffer.addCircle(center.x, center.y, highlightedRadius, Path.Direction.CW)
      } else {
        pathBuffer.moveTo(
            center.x + highlightedRadius * cos(startAngleShifted * Utils.FDEG2RAD),
            center.y + highlightedRadius * sin(startAngleShifted * Utils.FDEG2RAD))
        pathBuffer.arcTo(highlightedCircleBox, startAngleShifted, sweepAngleShifted)
      }
      var sliceSpaceRadius = 0f
      if (accountForSliceSpacing) {
        sliceSpaceRadius =
            calculateMinimumRadiusForSpacedSlice(
                center,
                radius,
                sliceAngle * phaseY,
                center.x + radius * cos((startAngleOuter * Utils.FDEG2RAD).toDouble()).toFloat(),
                center.y + radius * sin((startAngleOuter * Utils.FDEG2RAD).toDouble()).toFloat(),
                startAngleOuter,
                sweepAngleOuter)
      }

      // API < 21 does not receive floats in addArc, but a RectF
      innerRectBuffer[center.x - innerRadius, center.y - innerRadius, center.x + innerRadius] =
          center.y + innerRadius
      if (drawInnerArc && (innerRadius > 0f || accountForSliceSpacing)) {
        if (accountForSliceSpacing) {
          var minSpacedRadius = sliceSpaceRadius
          if (minSpacedRadius < 0f) minSpacedRadius = -minSpacedRadius
          innerRadius = max(innerRadius, minSpacedRadius)
        }
        val sliceSpaceAngleInner =
            if (visibleAngleCount == 1 || innerRadius == 0f) 0f
            else sliceSpace / (Utils.FDEG2RAD * innerRadius)
        val startAngleInner = rotationAngle + (angle + sliceSpaceAngleInner / 2f) * phaseY
        var sweepAngleInner = (sliceAngle - sliceSpaceAngleInner) * phaseY
        if (sweepAngleInner < 0f) {
          sweepAngleInner = 0f
        }
        val endAngleInner = startAngleInner + sweepAngleInner
        if (sweepAngleOuter >= 360f && sweepAngleOuter % 360f <= Utils.FLOAT_EPSILON) {
          // Android is doing "mod 360"
          pathBuffer.addCircle(center.x, center.y, innerRadius, Path.Direction.CCW)
        } else {
          pathBuffer.lineTo(
              center.x + innerRadius * cos((endAngleInner * Utils.FDEG2RAD).toDouble()).toFloat(),
              center.y + innerRadius * sin((endAngleInner * Utils.FDEG2RAD).toDouble()).toFloat())
          pathBuffer.arcTo(innerRectBuffer, endAngleInner, -sweepAngleInner)
        }
      } else {
        if (sweepAngleOuter % 360f > Utils.FLOAT_EPSILON) {
          if (accountForSliceSpacing) {
            val angleMiddle = startAngleOuter + sweepAngleOuter / 2f
            val arcEndPointX =
                center.x +
                    sliceSpaceRadius * cos((angleMiddle * Utils.FDEG2RAD).toDouble()).toFloat()
            val arcEndPointY =
                center.y +
                    sliceSpaceRadius * sin((angleMiddle * Utils.FDEG2RAD).toDouble()).toFloat()
            pathBuffer.lineTo(arcEndPointX, arcEndPointY)
          } else {
            pathBuffer.lineTo(center.x, center.y)
          }
        }
      }
      pathBuffer.close()
      bitmapCanvas?.drawPath(pathBuffer, renderPaint)
    }
    MPPointF.recycleInstance(center)
  }

  /** Releases the drawing bitmap. This should be called when [Chart.onDetachedFromWindow]. */
  fun releaseBitmap() {
    bitmapCanvas?.setBitmap(null)
    bitmapCanvas = null
    drawBitmap?.get()?.recycle()
    drawBitmap?.clear()
    drawBitmap = null
  }

  init {
    paintHole = Paint(Paint.ANTI_ALIAS_FLAG)
    paintHole.color = Color.WHITE
    paintHole.style = Paint.Style.FILL
    paintTransparentCircle = Paint(Paint.ANTI_ALIAS_FLAG)
    paintTransparentCircle.color = Color.WHITE
    paintTransparentCircle.style = Paint.Style.FILL
    paintTransparentCircle.alpha = 105
    paintCenterText = TextPaint(Paint.ANTI_ALIAS_FLAG)
    paintCenterText.color = Color.BLACK
    paintCenterText.textSize = Utils.convertDpToPixel(12f)
    valuePaint.textSize = Utils.convertDpToPixel(13f)
    valuePaint.color = Color.WHITE
    valuePaint.textAlign = Align.CENTER
    paintEntryLabels = Paint(Paint.ANTI_ALIAS_FLAG)
    paintEntryLabels.color = Color.WHITE
    paintEntryLabels.textAlign = Align.CENTER
    paintEntryLabels.textSize = Utils.convertDpToPixel(13f)
    valueLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    valueLinePaint.style = Paint.Style.STROKE
  }
}