package com.github.mikephil.charting.charts

import android.content.Context
import android.util.AttributeSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.renderer.LineChartRenderer

/**
 * Chart that draws lines, surfaces, circles, ...
 *
 * @author Philipp Jahoda
 */
class LineChart
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    BarLineChartBase<LineData, ILineDataSet, Entry>(context, attrs, defStyleAttr),
    LineDataProvider {

  init {
    mRenderer = LineChartRenderer(this, mAnimator, mViewPortHandler)
  }

  override val lineData: LineData?
    get() = data

  override fun onDetachedFromWindow() {
    // releases the bitmap in the renderer to avoid oom error
    if (mRenderer != null && mRenderer is LineChartRenderer) {
      (mRenderer as LineChartRenderer).releaseBitmap()
    }
    super.onDetachedFromWindow()
  }
}
