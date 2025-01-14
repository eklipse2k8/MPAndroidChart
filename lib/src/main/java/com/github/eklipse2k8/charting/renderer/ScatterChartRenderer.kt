package com.github.eklipse2k8.charting.renderer

import android.graphics.Canvas
import android.util.Log
import com.github.eklipse2k8.charting.animation.ChartAnimator
import com.github.eklipse2k8.charting.highlight.Highlight
import com.github.eklipse2k8.charting.interfaces.dataprovider.ScatterDataProvider
import com.github.eklipse2k8.charting.interfaces.datasets.IScatterDataSet
import com.github.eklipse2k8.charting.utils.MPPointF
import com.github.eklipse2k8.charting.utils.Utils
import com.github.eklipse2k8.charting.utils.ViewPortHandler
import kotlin.math.ceil
import kotlin.math.min

class ScatterChartRenderer(
    @JvmField var mChart: ScatterDataProvider,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : LineScatterCandleRadarRenderer(animator, viewPortHandler) {
  override fun initBuffers() {}

  override fun drawData(c: Canvas) {
    val scatterData = mChart.scatterData ?: return
    for (set in scatterData.dataSets) {
      if (set.isVisible) drawDataSet(c, set)
    }
  }

  var mPixelBuffer = FloatArray(2)

  private fun drawDataSet(c: Canvas?, dataSet: IScatterDataSet) {
    if (dataSet.entryCount < 1) return
    val viewPortHandler = viewPortHandler
    val trans = mChart.getTransformer(dataSet.axisDependency)
    val phaseY = animator.phaseY
    val renderer = dataSet.shapeRenderer
    if (renderer == null) {
      Log.i("MISSING", "There's no IShapeRenderer specified for ScatterDataSet")
      return
    }
    val max: Int =
        min(ceil(dataSet.entryCount.toDouble() * animator.phaseX), dataSet.entryCount.toDouble())
            .toInt()
    for (i in 0 until max) {
      val e = dataSet.getEntryForIndex(i)
      mPixelBuffer[0] = e.x
      mPixelBuffer[1] = e.y * phaseY
      trans.pointValuesToPixel(mPixelBuffer)
      if (!viewPortHandler.isInBoundsRight(mPixelBuffer[0])) break
      if (!viewPortHandler.isInBoundsLeft(mPixelBuffer[0]) ||
          !viewPortHandler.isInBoundsY(mPixelBuffer[1]))
          continue
      renderPaint.color = dataSet.getColor(i / 2)
      renderer.renderShape(
          c!!, dataSet,
        this.viewPortHandler, mPixelBuffer[0], mPixelBuffer[1], renderPaint)
    }
  }

  override fun drawValues(c: Canvas) {
    // if values are drawn
    if (isDrawingValuesAllowed(mChart)) {
      val dataSets = mChart.scatterData?.dataSets ?: return
      for (i in 0 until mChart.scatterData!!.dataSetCount) {
        val dataSet = dataSets[i]
        if (!shouldDrawValues(dataSet) || dataSet.entryCount < 1) continue

        // apply the text-styling defined by the DataSet
        applyValueTextStyle(dataSet)
        mXBounds[mChart] = dataSet
        val positions =
            mChart
                .getTransformer(dataSet.axisDependency)
                .generateTransformedValuesScatter(
                    dataSet, animator.phaseX, animator.phaseY, mXBounds.min, mXBounds.max)
        val shapeSize = Utils.convertDpToPixel(dataSet.scatterShapeSize)
        val iconsOffset =
          dataSet.iconsOffset?.let { MPPointF.getInstance(it) } ?: MPPointF.getInstance(0f, 0f)
        iconsOffset.x = Utils.convertDpToPixel(iconsOffset.x)
        iconsOffset.y = Utils.convertDpToPixel(iconsOffset.y)
        var j = 0
        while (j < positions.size) {
          if (!viewPortHandler.isInBoundsRight(positions[j])) break

          // make sure the lines don't do shitty things outside bounds
          if (!viewPortHandler.isInBoundsLeft(positions[j]) ||
              !viewPortHandler.isInBoundsY(positions[j + 1])) {
            j += 2
            continue
          }
          val entry = dataSet.getEntryForIndex(j / 2 + mXBounds.min)
          if (dataSet.isDrawValuesEnabled) {
            drawValue(
                c,
                dataSet.valueFormatter!!,
                entry.y,
                entry,
                i,
                positions[j],
                positions[j + 1] - shapeSize,
                dataSet.getValueTextColor(j / 2 + mXBounds.min))
          }
          if (entry.icon != null && dataSet.isDrawIconsEnabled) {
            val icon = entry.icon
            Utils.drawImage(
                c,
                icon,
                (positions[j] + iconsOffset.x).toInt(),
                (positions[j + 1] + iconsOffset.y).toInt(),
                icon!!.intrinsicWidth,
                icon.intrinsicHeight)
          }
          j += 2
        }
        MPPointF.recycleInstance(iconsOffset)
      }
    }
  }

  override fun drawExtras(c: Canvas) {}

  override fun drawHighlighted(c: Canvas, indices: Array<Highlight?>?) {
    val scatterData = mChart.scatterData ?: return
    indices?.forEach { high ->
      if (high == null) return@forEach
      val set = scatterData.getDataSetByIndex(high.dataSetIndex)
      if (set == null || !set.isHighlightEnabled) return@forEach
      val e = set.getEntryForXValue(high.x, high.y) ?: return@forEach
      if (!isInBoundsX(e, set)) return@forEach
      val pix =
          mChart.getTransformer(set.axisDependency).getPixelForValues(e.x, e.y * animator.phaseY)
      high.setDraw(pix.x.toFloat(), pix.y.toFloat())

      // draw the lines
      drawHighlightLines(c, pix.x.toFloat(), pix.y.toFloat(), set)
    }
  }
}
