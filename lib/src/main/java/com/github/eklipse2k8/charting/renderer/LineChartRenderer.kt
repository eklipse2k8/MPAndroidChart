package com.github.eklipse2k8.charting.renderer

import android.graphics.*
import com.github.eklipse2k8.charting.animation.ChartAnimator
import com.github.eklipse2k8.charting.data.Entry
import com.github.eklipse2k8.charting.data.LineDataSet
import com.github.eklipse2k8.charting.highlight.Highlight
import com.github.eklipse2k8.charting.interfaces.dataprovider.LineDataProvider
import com.github.eklipse2k8.charting.interfaces.datasets.IDataSet
import com.github.eklipse2k8.charting.interfaces.datasets.ILineDataSet
import com.github.eklipse2k8.charting.utils.*
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min

class LineChartRenderer(
    @JvmField var mChart: LineDataProvider,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : LineRadarRenderer(animator, viewPortHandler) {
  /** paint for the inner circle of the value indicators */
  protected var mCirclePaintInner: Paint

  /**
   * Bitmap object used for drawing the paths (otherwise they are too long if rendered directly on
   * the canvas)
   */
  private var mDrawBitmap: WeakReference<Bitmap?>? = null

  /** on this canvas, the paths are rendered, it is initialized with the pathBitmap */
  private var mBitmapCanvas: Canvas? = null

  /** the bitmap configuration to be used */
  private var mBitmapConfig = Bitmap.Config.ARGB_8888

  private var cubicPath = Path()

  private var cubicFillPath = Path()

  override fun initBuffers() = Unit

  override fun drawData(c: Canvas) {
    val width = viewPortHandler.chartWidth.toInt()
    val height = viewPortHandler.chartHeight.toInt()
    var drawBitmap = if (mDrawBitmap == null) null else mDrawBitmap!!.get()
    if (drawBitmap == null || drawBitmap.width != width || drawBitmap.height != height) {
      if (width > 0 && height > 0) {
        drawBitmap = Bitmap.createBitmap(width, height, mBitmapConfig)
        mDrawBitmap = WeakReference(drawBitmap)
        mBitmapCanvas = Canvas(drawBitmap)
      } else return
    }
    drawBitmap!!.eraseColor(Color.TRANSPARENT)
    val lineData = mChart.lineData
    lineData?.dataSets?.forEach { set -> if (set.isVisible) drawDataSet(c, set) }
    c.drawBitmap(drawBitmap, 0f, 0f, renderPaint)
  }

  private fun drawDataSet(c: Canvas?, dataSet: ILineDataSet?) {
    if (dataSet!!.entryCount < 1) return
    renderPaint.strokeWidth = dataSet.lineWidth
    renderPaint.pathEffect = dataSet.dashPathEffect
    when (dataSet.mode) {
      LineDataSet.Mode.LINEAR, LineDataSet.Mode.STEPPED -> drawLinear(c, dataSet)
      LineDataSet.Mode.CUBIC_BEZIER -> drawCubicBezier(dataSet)
      LineDataSet.Mode.HORIZONTAL_BEZIER -> drawHorizontalBezier(dataSet)
      else -> drawLinear(c, dataSet)
    }
    renderPaint.pathEffect = null
  }

  private fun drawHorizontalBezier(dataSet: ILineDataSet?) {
    val phaseY = animator.phaseY
    val trans = mChart.getTransformer(dataSet!!.axisDependency)
    mXBounds[mChart] = dataSet
    cubicPath.reset()
    if (mXBounds.range >= 1) {
      var prev = dataSet.getEntryForIndex(mXBounds.min)
      var cur = prev

      // let the spline start
      cubicPath.moveTo(cur.x, cur.y * phaseY)
      for (j in mXBounds.min + 1..mXBounds.range + mXBounds.min) {
        prev = cur
        cur = dataSet.getEntryForIndex(j)
        val cpx = (prev.x + (cur.x - prev.x) / 2.0f)
        cubicPath.cubicTo(cpx, prev.y * phaseY, cpx, cur.y * phaseY, cur.x, cur.y * phaseY)
      }
    }

    // if filled is enabled, close the path
    if (dataSet.isDrawFilledEnabled) {
      cubicFillPath.reset()
      cubicFillPath.addPath(cubicPath)
      // create a new path, this is bad for performance
      drawCubicFill(mBitmapCanvas, dataSet, cubicFillPath, trans, mXBounds)
    }
    renderPaint.color = dataSet.color
    renderPaint.style = Paint.Style.STROKE
    trans.pathValueToPixel(cubicPath)
    mBitmapCanvas!!.drawPath(cubicPath, renderPaint)
    renderPaint.pathEffect = null
  }

  private fun drawCubicBezier(dataSet: ILineDataSet) {
    val phaseY = animator.phaseY
    val trans = mChart.getTransformer(dataSet.axisDependency)
    mXBounds[mChart] = dataSet
    val intensity = dataSet.cubicIntensity
    cubicPath.reset()

    if (mXBounds.range >= 1) {
      var prevDx: Float
      var prevDy: Float
      var curDx: Float
      var curDy: Float

      // Take an extra point from the left, and an extra from the right.
      // That's because we need 4 points for a cubic bezier (cubic=4), otherwise we get lines moving
      // and doing weird stuff on the edges of the chart.
      // So in the starting `prev` and `cur`, go -2, -1
      // And in the `lastIndex`, add +1
      val firstIndex = mXBounds.min + 1
      val lastIndex = mXBounds.min + mXBounds.range
      var prevPrev: Entry?
      var prev = dataSet.getEntryForIndex(max(firstIndex - 2, 0))
      var cur = dataSet.getEntryForIndex(max(firstIndex - 1, 0))
      var next = cur
      var nextIndex = -1

      // let the spline start
      cubicPath.moveTo(cur.x, cur.y * phaseY)
      for (j in mXBounds.min + 1..mXBounds.range + mXBounds.min) {
        prevPrev = prev
        prev = cur
        cur = if (nextIndex == j) next else dataSet.getEntryForIndex(j)
        nextIndex = if (j + 1 < dataSet.entryCount) j + 1 else j
        next = dataSet.getEntryForIndex(nextIndex)
        prevDx = (cur.x - prevPrev.x) * intensity
        prevDy = (cur.y - prevPrev.y) * intensity
        curDx = (next.x - prev.x) * intensity
        curDy = (next.y - prev.y) * intensity
        cubicPath.cubicTo(
            prev.x + prevDx,
            (prev.y + prevDy) * phaseY,
            cur.x - curDx,
            (cur.y - curDy) * phaseY,
            cur.x,
            cur.y * phaseY)
      }
    }

    // if filled is enabled, close the path
    if (dataSet.isDrawFilledEnabled) {
      cubicFillPath.reset()
      cubicFillPath.addPath(cubicPath)
      drawCubicFill(mBitmapCanvas, dataSet, cubicFillPath, trans, mXBounds)
    }
    renderPaint.color = dataSet.color
    renderPaint.style = Paint.Style.STROKE
    trans.pathValueToPixel(cubicPath)
    mBitmapCanvas?.drawPath(cubicPath, renderPaint)
    renderPaint.pathEffect = null
  }

  private fun drawCubicFill(
      c: Canvas?,
      dataSet: ILineDataSet?,
      spline: Path,
      trans: Transformer,
      bounds: XBounds
  ) {
    if (c == null) return
    val fillMin = dataSet?.fillFormatter?.getFillLinePosition(dataSet, mChart) ?: return
    spline.lineTo(dataSet.getEntryForIndex(bounds.min + bounds.range).x, fillMin)
    spline.lineTo(dataSet.getEntryForIndex(bounds.min).x, fillMin)
    spline.close()
    trans.pathValueToPixel(spline)
    val drawable = dataSet.fillDrawable
    if (drawable != null) {
      drawFilledPath(c, spline, drawable)
    } else {
      drawFilledPath(c, spline, dataSet.fillColor, dataSet.fillAlpha)
    }
  }

  private var mLineBuffer = FloatArray(4)

  /**
   * Draws a normal line.
   *
   * @param c
   * @param dataSet
   */
  private fun drawLinear(c: Canvas?, dataSet: ILineDataSet?) {
    val entryCount = dataSet?.entryCount ?: return
    val isDrawSteppedEnabled = dataSet.isDrawSteppedEnabled
    val pointsPerEntryPair = if (isDrawSteppedEnabled) 4 else 2
    val trans = mChart.getTransformer(dataSet.axisDependency)
    val phaseY = animator.phaseY
    renderPaint.style = Paint.Style.STROKE

    // if the data-set is dashed, draw on bitmap-canvas
    val canvas: Canvas? = if (dataSet.isDashedLineEnabled) {
          mBitmapCanvas
        } else {
          c
        }
    mXBounds[mChart] = dataSet

    // if drawing filled is enabled
    if (dataSet.isDrawFilledEnabled && entryCount > 0) {
      drawLinearFill(c, dataSet, trans, mXBounds)
    }

    // more than 1 color
    if (dataSet.colors.size > 1) {
      val numberOfFloats = pointsPerEntryPair * 2
      if (mLineBuffer.size <= numberOfFloats) mLineBuffer = FloatArray(numberOfFloats * 2)
      val max = mXBounds.min + mXBounds.range
      for (j in mXBounds.min until max) {
        var e = dataSet.getEntryForIndex(j)
        mLineBuffer[0] = e.x
        mLineBuffer[1] = e.y * phaseY
        if (j < mXBounds.max) {
          e = dataSet.getEntryForIndex(j + 1)
          if (isDrawSteppedEnabled) {
            mLineBuffer[2] = e.x
            mLineBuffer[3] = mLineBuffer[1]
            mLineBuffer[4] = mLineBuffer[2]
            mLineBuffer[5] = mLineBuffer[3]
            mLineBuffer[6] = e.x
            mLineBuffer[7] = e.y * phaseY
          } else {
            mLineBuffer[2] = e.x
            mLineBuffer[3] = e.y * phaseY
          }
        } else {
          mLineBuffer[2] = mLineBuffer[0]
          mLineBuffer[3] = mLineBuffer[1]
        }

        // Determine the start and end coordinates of the line, and make sure they differ.
        val firstCoordinateX = mLineBuffer[0]
        val firstCoordinateY = mLineBuffer[1]
        val lastCoordinateX = mLineBuffer[numberOfFloats - 2]
        val lastCoordinateY = mLineBuffer[numberOfFloats - 1]
        if (firstCoordinateX == lastCoordinateX && firstCoordinateY == lastCoordinateY) continue
        trans.pointValuesToPixel(mLineBuffer)
        if (!viewPortHandler.isInBoundsRight(firstCoordinateX)) break

        // make sure the lines don't do shitty things outside
        // bounds
        if (!viewPortHandler.isInBoundsLeft(lastCoordinateX) ||
            !viewPortHandler.isInBoundsTop(max(firstCoordinateY, lastCoordinateY)) ||
            !viewPortHandler.isInBoundsBottom(min(firstCoordinateY, lastCoordinateY)))
            continue

        // get the color that is set for this line-segment
        renderPaint.color = dataSet.getColor(j)
        canvas!!.drawLines(mLineBuffer, 0, pointsPerEntryPair * 2, renderPaint)
      }
    } else { // only one color per dataset
      if (mLineBuffer.size < max(entryCount * pointsPerEntryPair, pointsPerEntryPair) * 2)
          mLineBuffer = FloatArray(max(entryCount * pointsPerEntryPair, pointsPerEntryPair) * 4)
      var e1: Entry
      var e2: Entry
      var j = 0
      for (x in mXBounds.min..mXBounds.range + mXBounds.min) {
        e1 = dataSet.getEntryForIndex(if (x == 0) 0 else x - 1)
        e2 = dataSet.getEntryForIndex(x)
        mLineBuffer[j++] = e1.x
        mLineBuffer[j++] = e1.y * phaseY
        if (isDrawSteppedEnabled) {
          mLineBuffer[j++] = e2.x
          mLineBuffer[j++] = e1.y * phaseY
          mLineBuffer[j++] = e2.x
          mLineBuffer[j++] = e1.y * phaseY
        }
        mLineBuffer[j++] = e2.x
        mLineBuffer[j++] = e2.y * phaseY
      }
      if (j > 0) {
        trans.pointValuesToPixel(mLineBuffer)
        val size = Math.max((mXBounds.range + 1) * pointsPerEntryPair, pointsPerEntryPair) * 2
        renderPaint.color = dataSet.color
        canvas!!.drawLines(mLineBuffer, 0, size, renderPaint)
      }
    }
    renderPaint.pathEffect = null
  }

  private var mGenerateFilledPathBuffer = Path()

  /**
   * Draws a filled linear path on the canvas.
   *
   * @param c
   * @param dataSet
   * @param trans
   * @param bounds
   */
  private fun drawLinearFill(
      c: Canvas?,
      dataSet: ILineDataSet?,
      trans: Transformer,
      bounds: XBounds
  ) {
    if (c == null) return
    val filled = mGenerateFilledPathBuffer
    val startingIndex = bounds.min
    val endingIndex = bounds.range + bounds.min
    val indexInterval = 128
    var currentStartIndex: Int
    var currentEndIndex: Int
    var iterations = 0

    // Doing this iteratively in order to avoid OutOfMemory errors that can happen on large bounds
    // sets.
    do {
      currentStartIndex = startingIndex + iterations * indexInterval
      currentEndIndex = currentStartIndex + indexInterval
      currentEndIndex = if (currentEndIndex > endingIndex) endingIndex else currentEndIndex
      if (currentStartIndex <= currentEndIndex) {
        generateFilledPath(dataSet, currentStartIndex, currentEndIndex, filled)
        trans.pathValueToPixel(filled)
        val drawable = dataSet!!.fillDrawable
        if (drawable != null) {
          drawFilledPath(c, filled, drawable)
        } else {
          drawFilledPath(c, filled, dataSet.fillColor, dataSet.fillAlpha)
        }
      }
      iterations++
    } while (currentStartIndex <= currentEndIndex)
  }

  /**
   * Generates a path that is used for filled drawing.
   *
   * @param dataSet The dataset from which to read the entries.
   * @param startIndex The index from which to start reading the dataset
   * @param endIndex The index from which to stop reading the dataset
   * @param outputPath The path object that will be assigned the chart data.
   * @return
   */
  private fun generateFilledPath(
      dataSet: ILineDataSet?,
      startIndex: Int,
      endIndex: Int,
      outputPath: Path
  ) {
    val fillMin = dataSet!!.fillFormatter!!.getFillLinePosition(dataSet, mChart)
    val phaseY = animator.phaseY
    val isDrawSteppedEnabled = dataSet.mode === LineDataSet.Mode.STEPPED
    outputPath.reset()
    val entry = dataSet.getEntryForIndex(startIndex)
    outputPath.moveTo(entry.x, fillMin)
    outputPath.lineTo(entry.x, entry.y * phaseY)

    // create a new path
    var currentEntry: Entry? = null
    var previousEntry = entry
    for (x in startIndex + 1..endIndex) {
      currentEntry = dataSet.getEntryForIndex(x)
      if (isDrawSteppedEnabled) {
        outputPath.lineTo(currentEntry.x, previousEntry.y * phaseY)
      }
      outputPath.lineTo(currentEntry.x, currentEntry.y * phaseY)
      previousEntry = currentEntry
    }

    // close up
    if (currentEntry != null) {
      outputPath.lineTo(currentEntry.x, fillMin)
    }
    outputPath.close()
  }

  override fun drawValues(c: Canvas) {
    if (isDrawingValuesAllowed(mChart)) {
      val dataSets = mChart.lineData?.dataSets ?: return
      for (i in dataSets.indices) {
        val dataSet = dataSets[i]
        if (!shouldDrawValues(dataSet) || dataSet.entryCount < 1) continue

        // apply the text-styling defined by the DataSet
        applyValueTextStyle(dataSet)
        val trans = mChart.getTransformer(dataSet.axisDependency)

        // make sure the values do not interfear with the circles
        var valOffset = (dataSet.circleRadius * 1.75f).toInt()
        if (!dataSet.isDrawCirclesEnabled) valOffset /= 2
        mXBounds[mChart] = dataSet
        val positions =
            trans.generateTransformedValuesLine(
                dataSet, animator.phaseX, animator.phaseY, mXBounds.min, mXBounds.max)
        val iconsOffset =
          dataSet.iconsOffset?.let { MPPointF.getInstance(it) } ?: MPPointF.getInstance(0f, 0f)
        iconsOffset.x = Utils.convertDpToPixel(iconsOffset.x)
        iconsOffset.y = Utils.convertDpToPixel(iconsOffset.y)
        var j = 0
        while (j < positions.size) {
          val x = positions[j]
          val y = positions[j + 1]
          if (!viewPortHandler.isInBoundsRight(x)) break
          if (!viewPortHandler.isInBoundsLeft(x) || !viewPortHandler.isInBoundsY(y)) {
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
                x,
                y - valOffset,
                dataSet.getValueTextColor(j / 2))
          }
          if (entry.icon != null && dataSet.isDrawIconsEnabled) {
            val icon = entry.icon
            Utils.drawImage(
                c,
                icon,
                (x + iconsOffset.x).toInt(),
                (y + iconsOffset.y).toInt(),
                icon!!.intrinsicWidth,
                icon.intrinsicHeight)
          }
          j += 2
        }
        MPPointF.recycleInstance(iconsOffset)
      }
    }
  }

  override fun drawExtras(c: Canvas) {
    drawCircles(c)
  }

  /** cache for the circle bitmaps of all datasets */
  private val mImageCaches = HashMap<IDataSet<*>?, DataSetImageCache>()

  /** buffer for drawing the circles */
  private val mCirclesBuffer = FloatArray(2)

  private fun drawCircles(c: Canvas) {
    renderPaint.style = Paint.Style.FILL
    val phaseY = animator.phaseY
    mCirclesBuffer[0] = 0f
    mCirclesBuffer[1] = 0f
    val dataSets = mChart.lineData?.dataSets ?: return
    for (i in dataSets.indices) {
      val dataSet = dataSets[i]
      if (!dataSet.isVisible || !dataSet.isDrawCirclesEnabled || dataSet.entryCount == 0) continue
      mCirclePaintInner.color = dataSet.circleHoleColor
      val trans = mChart.getTransformer(dataSet.axisDependency)
      mXBounds[mChart] = dataSet
      val circleRadius = dataSet.circleRadius
      val circleHoleRadius = dataSet.circleHoleRadius
      val drawCircleHole =
          dataSet.isDrawCircleHoleEnabled &&
              circleHoleRadius < circleRadius &&
              circleHoleRadius > 0f
      val drawTransparentCircleHole =
          drawCircleHole && dataSet.circleHoleColor == ColorTemplate.COLOR_NONE
      var imageCache: DataSetImageCache?
      if (mImageCaches.containsKey(dataSet)) {
        imageCache = mImageCaches[dataSet]
      } else {
        imageCache = DataSetImageCache()
        mImageCaches[dataSet] = imageCache
      }
      val changeRequired = imageCache!!.init(dataSet)

      // only fill the cache with new bitmaps if a change is required
      if (changeRequired) {
        imageCache.fill(dataSet, drawCircleHole, drawTransparentCircleHole)
      }
      val boundsRangeCount = mXBounds.range + mXBounds.min
      for (j in mXBounds.min..boundsRangeCount) {
        val e = dataSet.getEntryForIndex(j)
        mCirclesBuffer[0] = e.x
        mCirclesBuffer[1] = e.y * phaseY
        trans.pointValuesToPixel(mCirclesBuffer)
        if (!viewPortHandler.isInBoundsRight(mCirclesBuffer[0])) break
        if (!viewPortHandler.isInBoundsLeft(mCirclesBuffer[0]) ||
            !viewPortHandler.isInBoundsY(mCirclesBuffer[1]))
            continue
        val circleBitmap = imageCache.getBitmap(j)
        if (circleBitmap != null) {
          c.drawBitmap(
              circleBitmap,
              mCirclesBuffer[0] - circleRadius,
              mCirclesBuffer[1] - circleRadius,
              null)
        }
      }
    }
  }

  override fun drawHighlighted(c: Canvas, indices: Array<Highlight?>?) {
    val lineData = mChart.lineData ?: return
    indices?.forEach { high ->
      if (high == null) return@forEach
      val set = lineData.getDataSetByIndex(high.dataSetIndex)
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
  /**
   * Returns the Bitmap.Config that is used by this renderer.
   *
   * @return
   */
  /**
   * Sets the Bitmap.Config to be used by this renderer. Default: Bitmap.Config.ARGB_8888 Use
   * Bitmap.Config.ARGB_4444 to consume less memory.
   *
   * @param config
   */
  var bitmapConfig: Bitmap.Config
    get() = mBitmapConfig
    set(config) {
      mBitmapConfig = config
      releaseBitmap()
    }

  /** Releases the drawing bitmap. This should be called when [LineChart.onDetachedFromWindow]. */
  fun releaseBitmap() {
    if (mBitmapCanvas != null) {
      mBitmapCanvas!!.setBitmap(null)
      mBitmapCanvas = null
    }
    if (mDrawBitmap != null) {
      val drawBitmap = mDrawBitmap!!.get()
      drawBitmap?.recycle()
      mDrawBitmap!!.clear()
      mDrawBitmap = null
    }
  }

  private inner class DataSetImageCache {
    private val mCirclePathBuffer = Path()

    private var circleBitmaps: Array<Bitmap?>? = null

    /**
     * Sets up the cache, returns true if a change of cache was required.
     *
     * @param set
     * @return
     */
    fun init(set: ILineDataSet?): Boolean {
      val size = set!!.circleColorCount
      var changeRequired = false
      if (circleBitmaps == null) {
        circleBitmaps = arrayOfNulls(size)
        changeRequired = true
      } else if (circleBitmaps!!.size != size) {
        circleBitmaps = arrayOfNulls(size)
        changeRequired = true
      }
      return changeRequired
    }

    /**
     * Fills the cache with bitmaps for the given dataset.
     *
     * @param set
     * @param drawCircleHole
     * @param drawTransparentCircleHole
     */
    fun fill(set: ILineDataSet?, drawCircleHole: Boolean, drawTransparentCircleHole: Boolean) {
      val colorCount = set!!.circleColorCount
      val circleRadius = set.circleRadius
      val circleHoleRadius = set.circleHoleRadius
      for (i in 0 until colorCount) {
        val conf = Bitmap.Config.ARGB_4444
        val circleBitmap =
            Bitmap.createBitmap((circleRadius * 2.1).toInt(), (circleRadius * 2.1).toInt(), conf)
        val canvas = Canvas(circleBitmap)
        circleBitmaps!![i] = circleBitmap
        renderPaint.color = set.getCircleColor(i)
        if (drawTransparentCircleHole) {
          // Begin path for circle with hole
          mCirclePathBuffer.reset()
          mCirclePathBuffer.addCircle(circleRadius, circleRadius, circleRadius, Path.Direction.CW)

          // Cut hole in path
          mCirclePathBuffer.addCircle(
              circleRadius, circleRadius, circleHoleRadius, Path.Direction.CCW)

          // Fill in-between
          canvas.drawPath(mCirclePathBuffer, renderPaint)
        } else {
          canvas.drawCircle(circleRadius, circleRadius, circleRadius, renderPaint)
          if (drawCircleHole) {
            canvas.drawCircle(circleRadius, circleRadius, circleHoleRadius, mCirclePaintInner)
          }
        }
      }
    }

    /**
     * Returns the cached Bitmap at the given index.
     *
     * @param index
     * @return
     */
    fun getBitmap(index: Int): Bitmap? {
      return circleBitmaps!![index % circleBitmaps!!.size]
    }
  }

  init {
    mCirclePaintInner = Paint(Paint.ANTI_ALIAS_FLAG)
    mCirclePaintInner.style = Paint.Style.FILL
    mCirclePaintInner.color = Color.WHITE
  }
}
