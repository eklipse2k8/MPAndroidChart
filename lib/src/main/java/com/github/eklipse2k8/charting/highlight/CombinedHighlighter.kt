package com.github.eklipse2k8.charting.highlight

import androidx.annotation.NonNull
import com.github.eklipse2k8.charting.data.BarData
import com.github.eklipse2k8.charting.data.Rounding
import com.github.eklipse2k8.charting.interfaces.dataprovider.BarDataProvider
import com.github.eklipse2k8.charting.interfaces.dataprovider.CombinedDataProvider

/** Created by Philipp Jahoda on 12/09/15. */
class CombinedHighlighter(chart: CombinedDataProvider, @NonNull barChart: BarDataProvider) :
    ChartHighlighter<CombinedDataProvider>(chart), IHighlighter {
  /** bar highlighter for supporting stacked highlighting */
  protected val barHighlighter: BarHighlighter = BarHighlighter(barChart)

  override fun getHighlightsAtXValue(xVal: Float, x: Float, y: Float): List<Highlight> {
    highlightBuffer.clear()
    val dataObjects = chartView.combinedData?.allData
    dataObjects?.forEachIndexed { i, dataObject ->

      // in case of BarData, let the BarHighlighter take over
      if (dataObject is BarData) {
        val high = barHighlighter.getHighlight(x, y)
        if (high != null) {
          high.dataIndex = i
          highlightBuffer.add(high)
        }
      } else {
        var j = 0
        val dataSetCount = dataObject.dataSetCount
        while (j < dataSetCount) {
          val dataSet = dataObject.getDataSetByIndex(j) ?: continue

          // don't include datasets that cannot be highlighted
          if (!dataSet.isHighlightEnabled) {
            j++
            continue
          }
          val highs = buildHighlights(dataSet, j, xVal, Rounding.CLOSEST)
          for (high in highs!!) {
            high.dataIndex = i
            highlightBuffer.add(high)
          }
          j++
        }
      }
    }
    return highlightBuffer
  }
}
