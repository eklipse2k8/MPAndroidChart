package com.github.mikephil.charting.renderer

import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.data.DataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.interfaces.dataprovider.BarLineScatterCandleBubbleDataProvider
import com.github.mikephil.charting.interfaces.datasets.IBarLineScatterCandleBubbleDataSet
import com.github.mikephil.charting.interfaces.datasets.IDataSet
import com.github.mikephil.charting.utils.ViewPortHandler
import kotlin.math.max
import kotlin.math.min

/** Created by Philipp Jahoda on 09/06/16. */
abstract class BarLineScatterCandleBubbleRenderer(
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : DataRenderer(animator, viewPortHandler) {
  /** buffer for storing the current minimum and maximum visible x */
  @JvmField protected var mXBounds = XBounds()

  /**
   * Returns true if the DataSet values should be drawn, false if not.
   *
   * @param set
   * @return
   */
  protected fun shouldDrawValues(set: IDataSet<*>): Boolean {
    return set.isVisible && (set.isDrawValuesEnabled || set.isDrawIconsEnabled)
  }

  /**
   * Checks if the provided entry object is in bounds for drawing considering the current animation
   * phase.
   *
   * @param e
   * @param set
   * @return
   */
  protected fun <E : Entry> isInBoundsX(
      e: E?,
      set: IBarLineScatterCandleBubbleDataSet<E>
  ): Boolean {
    if (e == null) return false
    val entryIndex = set.getEntryIndex(e).toFloat()
    return entryIndex < set.entryCount * mAnimator.phaseX
  }

  /**
   * Class representing the bounds of the current viewport in terms of indices in the values array
   * of a DataSet.
   */
  protected inner class XBounds {
    /** minimum visible entry index */
    @JvmField var min = 0

    /** maximum visible entry index */
    @JvmField var max = 0

    /** range of visible entry indices */
    @JvmField var range = 0

    /**
     * Calculates the minimum and maximum x values as well as the range between them.
     *
     * @param chart
     * @param dataSet
     */
    operator fun set(
        chart: BarLineScatterCandleBubbleDataProvider,
        dataSet: IBarLineScatterCandleBubbleDataSet<Entry>
    ) {
      val phaseX = max(0f, min(1f, mAnimator.phaseX))
      val low = chart.lowestVisibleX
      val high = chart.highestVisibleX
      val entryFrom = dataSet.getEntryForXValue(low, Float.NaN, DataSet.Rounding.DOWN)
      val entryTo = dataSet.getEntryForXValue(high, Float.NaN, DataSet.Rounding.UP)
      min = dataSet.getEntryIndex(entryFrom)
      max = dataSet.getEntryIndex(entryTo)
      range = ((max - min) * phaseX).toInt()
    }
  }
}