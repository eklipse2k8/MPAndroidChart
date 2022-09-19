package com.github.eklipse2k8.charting.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import com.github.eklipse2k8.charting.charts.Chart
import com.github.eklipse2k8.charting.data.Entry
import com.github.eklipse2k8.charting.highlight.Highlight
import com.github.eklipse2k8.charting.utils.FSize
import com.github.eklipse2k8.charting.utils.MPPointF
import java.lang.ref.WeakReference

/**
 * View that can be displayed when selecting values in the chart. Extend this class to provide
 * custom layouts for your markers.
 *
 * @author Philipp Jahoda
 */
class MarkerImage(mContext: Context, drawableResourceId: Int) : IMarker {
  private val drawable: Drawable =
      ResourcesCompat.getDrawable(mContext.resources, drawableResourceId, mContext.theme)
          ?: error("image not found")
  private val mOffset2 = MPPointF()
  private var mWeakChart: WeakReference<Chart<*, *, *>>? = null
  private var mSize: FSize? = FSize()
  private val mDrawableBoundsCache = Rect()

  fun setOffset(offsetX: Float, offsetY: Float) {
    offset.x = offsetX
    offset.y = offsetY
  }

  override var offset: MPPointF = MPPointF()

  var size: FSize?
    get() = mSize
    set(size) {
      mSize = size
      if (mSize == null) {
        mSize = FSize()
      }
    }

  fun setChartView(chart: Chart<*, *, *>) {
    mWeakChart = WeakReference(chart)
  }

  private val chartView: Chart<*, *, *>?
    get() = if (mWeakChart == null) null else mWeakChart?.get()

  override fun getOffsetForDrawingAtPoint(posX: Float, posY: Float): MPPointF {
    val offset = offset
    mOffset2.x = offset.x
    mOffset2.y = offset.y
    val chart = chartView
    var width = mSize!!.width
    var height = mSize!!.height
    if (width == 0f) {
      width = drawable.intrinsicWidth.toFloat()
    }
    if (height == 0f) {
      height = drawable.intrinsicWidth.toFloat()
    }
    if (posX + mOffset2.x < 0) {
      mOffset2.x = -posX
    } else if (chart != null && posX + width + mOffset2.x > chart.width) {
      mOffset2.x = chart.width - posX - width
    }
    if (posY + mOffset2.y < 0) {
      mOffset2.y = -posY
    } else if (chart != null && posY + height + mOffset2.y > chart.height) {
      mOffset2.y = chart.height - posY - height
    }
    return mOffset2
  }

  override fun refreshContent(e: Entry?, highlight: Highlight?) {}

  override fun draw(canvas: Canvas?, posX: Float, posY: Float) {
    val offset = getOffsetForDrawingAtPoint(posX, posY)
    var width = mSize!!.width
    var height = mSize!!.height
    if (width == 0f) {
      width = drawable.intrinsicWidth.toFloat()
    }
    if (height == 0f) {
      height = drawable.intrinsicHeight.toFloat()
    }
    drawable.copyBounds(mDrawableBoundsCache)
    drawable.setBounds(
        mDrawableBoundsCache.left,
        mDrawableBoundsCache.top,
        mDrawableBoundsCache.left + width.toInt(),
        mDrawableBoundsCache.top + height.toInt())
    val saveId = canvas!!.save()
    // translate to the correct position and draw
    canvas.translate(posX + offset.x, posY + offset.y)
    drawable.draw(canvas)
    canvas.restoreToCount(saveId)
    drawable.bounds = mDrawableBoundsCache
  }
}