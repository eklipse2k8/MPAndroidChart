package com.github.eklipse2k8.catalog

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.eklipse2k8.charting.charts.LineChart
import com.github.eklipse2k8.charting.components.Legend
import com.github.eklipse2k8.charting.components.Legend.LegendForm
import com.github.eklipse2k8.charting.components.YAxis.AxisDependency
import com.github.eklipse2k8.charting.data.Entry
import com.github.eklipse2k8.charting.data.LineData
import com.github.eklipse2k8.charting.data.LineDataSet
import com.github.eklipse2k8.charting.highlight.Highlight
import com.github.eklipse2k8.charting.listener.OnChartValueSelectedListener
import com.github.eklipse2k8.charting.utils.ColorTemplate.colorWithAlpha
import com.github.eklipse2k8.charting.utils.ColorTemplate.holoBlue
import com.github.eklipse2k8.catalog.notimportant.DemoBase

/**
 * Example of a dual axis [LineChart] with multiple data sets.
 *
 * @since 1.7.4
 * @version 3.1.0
 */
class LineChartActivity2 : DemoBase(), OnSeekBarChangeListener, OnChartValueSelectedListener {
  private lateinit var chart: LineChart
  private lateinit var seekBarX: SeekBar
  private lateinit var seekBarY: SeekBar
  private lateinit var tvX: TextView
  private lateinit var tvY: TextView
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_linechart)
    title = "LineChartActivity2"
    tvX = findViewById(R.id.tvXMax)
    tvY = findViewById(R.id.tvYMax)
    seekBarX = findViewById(R.id.seekBar1)
    seekBarX.setOnSeekBarChangeListener(this)
    seekBarY = findViewById(R.id.seekBar2)
    seekBarY.setOnSeekBarChangeListener(this)
    chart = findViewById(R.id.chart1)
    chart.setOnChartValueSelectedListener(this)

    // no description text
    chart.description.isEnabled = false

    // enable touch gestures
    chart.isTouchEnabled = true
    chart.dragDecelerationFrictionCoef = 0.9f

    // enable scaling and dragging
    chart.isDragEnabled = true
    chart.setScaleEnabled(true)
    chart.setDrawGridBackground(false)
    chart.isHighlightPerDragEnabled = true

    // if disabled, scaling can be done on x- and y-axis separately
    chart.setPinchZoom(true)

    // add data
    seekBarX.setProgress(20)
    seekBarY.setProgress(30)
    chart.animateX(1500)

    // get the legend (only possible after setting data)
    val l = chart.legend

    // modify the legend ...
    l.form = LegendForm.LINE
    l.textSize = 11f
    l.textColor = Color.WHITE
    l.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
    l.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
    l.orientation = Legend.LegendOrientation.HORIZONTAL
    l.setDrawInside(false)
    //        l.setYOffset(11f);
    val xAxis = chart.xAxis
    xAxis.textSize = 11f
    xAxis.textColor = Color.WHITE
    xAxis.setDrawGridLines(false)
    xAxis.setDrawAxisLine(false)
    val leftAxis = chart.axisLeft
    leftAxis.textColor = holoBlue
    leftAxis.axisMaximum = 200f
    leftAxis.axisMinimum = 0f
    leftAxis.setDrawGridLines(true)
    leftAxis.isGranularityEnabled = true
    val rightAxis = chart.axisRight
    rightAxis.textColor = Color.RED
    rightAxis.axisMaximum = 900f
    rightAxis.axisMinimum = -200f
    rightAxis.setDrawGridLines(false)
    rightAxis.setDrawZeroLine(false)
    rightAxis.isGranularityEnabled = false
  }

  private fun setData(count: Int, range: Float) {
    val values1 = ArrayList<Entry>()
    for (i in 0 until count) {
      val `val` = (Math.random() * (range / 2f)).toFloat() + 50
      values1.add(Entry(i.toFloat(), `val`))
    }
    val values2 = ArrayList<Entry>()
    for (i in 0 until count) {
      val `val` = (Math.random() * range).toFloat() + 450
      values2.add(Entry(i.toFloat(), `val`))
    }
    val values3 = ArrayList<Entry>()
    for (i in 0 until count) {
      val `val` = (Math.random() * range).toFloat() + 500
      values3.add(Entry(i.toFloat(), `val`))
    }
    val set1: LineDataSet?
    val set2: LineDataSet?
    val set3: LineDataSet?
    if (chart.data != null && chart.data!!.dataSetCount > 0) {
      set1 = chart.data!!.getDataSetByIndex(0) as LineDataSet?
      set2 = chart.data!!.getDataSetByIndex(1) as LineDataSet?
      set3 = chart.data!!.getDataSetByIndex(2) as LineDataSet?
      if (set1 != null) {
        set1.entries = values1
      }
      if (set2 != null) {
        set2.entries = values2
      }
      if (set3 != null) {
        set3.entries = values3
      }
      chart.data!!.notifyDataChanged()
      chart.notifyDataSetChanged()
    } else {
      // create a dataset and give it a type
      set1 = LineDataSet(values1, "DataSet 1")
      set1.axisDependency = AxisDependency.LEFT
      set1.color = holoBlue
      set1.setCircleColor(Color.WHITE)
      set1.lineWidth = 2f
      set1.circleRadius = 3f
      set1.fillAlpha = 65
      set1.fillColor = holoBlue
      set1.highLightColor = Color.rgb(244, 117, 117)
      set1.setDrawCircleHole(false)
      // set1.setFillFormatter(new MyFillFormatter(0f));
      // set1.setDrawHorizontalHighlightIndicator(false);
      // set1.setVisible(false);
      // set1.setCircleHoleColor(Color.WHITE);

      // create a dataset and give it a type
      set2 = LineDataSet(values2, "DataSet 2")
      set2.axisDependency = AxisDependency.RIGHT
      set2.color = Color.RED
      set2.setCircleColor(Color.WHITE)
      set2.lineWidth = 2f
      set2.circleRadius = 3f
      set2.fillAlpha = 65
      set2.fillColor = Color.RED
      set2.setDrawCircleHole(false)
      set2.highLightColor = Color.rgb(244, 117, 117)
      // set2.setFillFormatter(new MyFillFormatter(900f));
      set3 = LineDataSet(values3, "DataSet 3")
      set3.axisDependency = AxisDependency.RIGHT
      set3.color = Color.YELLOW
      set3.setCircleColor(Color.WHITE)
      set3.lineWidth = 2f
      set3.circleRadius = 3f
      set3.fillAlpha = 65
      set3.fillColor = colorWithAlpha(Color.YELLOW, 200)
      set3.setDrawCircleHole(false)
      set3.highLightColor = Color.rgb(244, 117, 117)

      // create a data object with the data sets
      val data = LineData(set1, set2, set3)
      data.setValueTextColor(Color.WHITE)
      data.setValueTextSize(9f)

      // set data
      chart.data = data
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.line, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.viewGithub -> {
        val i = Intent(Intent.ACTION_VIEW)
        i.data =
            Uri.parse(
                "https://github.com/PhilJay/MPAndroidChart/blob/master/MPChartExample/src/com/xxmassdeveloper/mpchartexample/LineChartActivity2.java")
        startActivity(i)
      }
      R.id.actionToggleValues -> {
        chart.data?.dataSets?.forEach { set -> set.setDrawValues(!set.isDrawValuesEnabled) }
        chart.invalidate()
      }
      R.id.actionToggleHighlight -> {
        if (chart.data != null) {
          chart.data!!.isHighlightEnabled = !chart.data!!.isHighlightEnabled
          chart.invalidate()
        }
      }
      R.id.actionToggleFilled -> {
        chart.data?.dataSets?.forEach { set -> set.setDrawFilled(!set.isDrawFilledEnabled) }
        chart.invalidate()
      }
      R.id.actionToggleCircles -> {
        chart.data?.dataSets?.forEach { set ->
          (set as LineDataSet).setDrawCircles(!set.isDrawCirclesEnabled)
        }
        chart.invalidate()
      }
      R.id.actionToggleCubic -> {
        chart.data?.dataSets?.forEach { set ->
          (set as LineDataSet).mode =
              if (set.mode === LineDataSet.Mode.CUBIC_BEZIER) LineDataSet.Mode.LINEAR
              else LineDataSet.Mode.CUBIC_BEZIER
        }
        chart.invalidate()
      }
      R.id.actionToggleStepped -> {
        chart.data?.dataSets?.forEach { set ->
          (set as LineDataSet).mode =
              if (set.mode === LineDataSet.Mode.STEPPED) LineDataSet.Mode.LINEAR
              else LineDataSet.Mode.STEPPED
        }
        chart.invalidate()
      }
      R.id.actionToggleHorizontalCubic -> {
        chart.data?.dataSets?.forEach { set ->
          (set as LineDataSet).mode =
              if (set.mode === LineDataSet.Mode.HORIZONTAL_BEZIER) LineDataSet.Mode.LINEAR
              else LineDataSet.Mode.HORIZONTAL_BEZIER
        }
        chart.invalidate()
      }
      R.id.actionTogglePinch -> {
        chart.setPinchZoom(!chart.isPinchZoomEnabled)
        chart.invalidate()
      }
      R.id.actionToggleAutoScaleMinMax -> {
        chart.isAutoScaleMinMaxEnabled = !chart.isAutoScaleMinMaxEnabled
        chart.notifyDataSetChanged()
      }
      R.id.animateX -> {
        chart.animateX(2000)
      }
      R.id.animateY -> {
        chart.animateY(2000)
      }
      R.id.animateXY -> {
        chart.animateXY(2000, 2000)
      }
      R.id.actionSave -> {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED) {
          saveToGallery()
        } else {
          requestStoragePermission(chart!!)
        }
      }
    }
    return true
  }

  override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
    tvX!!.text = seekBarX!!.progress.toString()
    tvY!!.text = seekBarY!!.progress.toString()
    setData(seekBarX!!.progress, seekBarY!!.progress.toFloat())

    // redraw
    chart.invalidate()
  }

  override fun saveToGallery() {
    saveToGallery(chart!!, "LineChartActivity2")
  }

  override fun onValueSelected(e: Entry?, h: Highlight?) {
    if (e == null || h == null) return
    Log.i("Entry selected", e.toString())
    val dependency = chart.data?.getDataSetByIndex(h.dataSetIndex)?.axisDependency
    dependency?.let { chart.centerViewToAnimated(e.x, e.y, it, 500) }
  }

  override fun onNothingSelected() {
    Log.i("Nothing selected", "Nothing selected.")
  }

  override fun onStartTrackingTouch(seekBar: SeekBar) {}
  override fun onStopTrackingTouch(seekBar: SeekBar) {}
}
