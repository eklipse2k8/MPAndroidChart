package com.github.eklipse2k8.charting.data

import android.graphics.DashPathEffect
import com.github.eklipse2k8.charting.interfaces.datasets.ILineScatterCandleRadarDataSet
import com.github.eklipse2k8.charting.utils.Utils.convertDpToPixel

/** Created by Philipp Jahoda on 11/07/15. */
abstract class LineScatterCandleRadarDataSet<T : Entry>(yVals: MutableList<T>, label: String?) :
    BarLineScatterCandleBubbleDataSet<T>(yVals, label), ILineScatterCandleRadarDataSet<T> {

  override var isVerticalHighlightIndicatorEnabled = true
    protected set

  override var isHorizontalHighlightIndicatorEnabled = true
    protected set

  /** the path effect for dashed highlight-lines */
  override var dashPathEffectHighlight: DashPathEffect? = null
    protected set

  /**
   * Enables / disables the horizontal highlight-indicator. If disabled, the indicator is not drawn.
   * @param enabled
   */
  fun setDrawHorizontalHighlightIndicator(enabled: Boolean) {
    isHorizontalHighlightIndicatorEnabled = enabled
  }

  /**
   * Enables / disables the vertical highlight-indicator. If disabled, the indicator is not drawn.
   * @param enabled
   */
  fun setDrawVerticalHighlightIndicator(enabled: Boolean) {
    isVerticalHighlightIndicatorEnabled = enabled
  }

  /**
   * Enables / disables both vertical and horizontal highlight-indicators.
   * @param enabled
   */
  fun setDrawHighlightIndicators(enabled: Boolean) {
    setDrawVerticalHighlightIndicator(enabled)
    setDrawHorizontalHighlightIndicator(enabled)
  }

  /**
   * Sets the width of the highlight line in dp.
   * @param width
   */
  override var highlightLineWidth: Float = convertDpToPixel(0.5f)

  fun setHighlighLineWidthPixel(width: Float) {
    highlightLineWidth = convertDpToPixel(width)
  }

  /**
   * Enables the highlight-line to be drawn in dashed mode, e.g. like this "- - - - - -"
   *
   * @param lineLength the length of the line pieces
   * @param spaceLength the length of space inbetween the line-pieces
   * @param phase offset, in degrees (normally, use 0)
   */
  fun enableDashedHighlightLine(lineLength: Float, spaceLength: Float, phase: Float) {
    dashPathEffectHighlight = DashPathEffect(floatArrayOf(lineLength, spaceLength), phase)
  }

  /** Disables the highlight-line to be drawn in dashed mode. */
  fun disableDashedHighlightLine() {
    dashPathEffectHighlight = null
  }

  /**
   * Returns true if the dashed-line effect is enabled for highlight lines, false if not. Default:
   * disabled
   *
   * @return
   */
  val isDashedHighlightLineEnabled: Boolean
    get() = dashPathEffectHighlight != null

  protected fun copyTo(dataSet: LineScatterCandleRadarDataSet<*>) {
    super.copyTo(dataSet)
    dataSet.isHorizontalHighlightIndicatorEnabled = isHorizontalHighlightIndicatorEnabled
    dataSet.isVerticalHighlightIndicatorEnabled = isVerticalHighlightIndicatorEnabled
    dataSet.highlightLineWidth = highlightLineWidth
    dataSet.dashPathEffectHighlight = dashPathEffectHighlight
  }
}
