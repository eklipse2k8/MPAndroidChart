package com.github.eklipse2k8.charting.utils

import android.content.res.AssetManager
import android.os.Environment
import android.util.Log
import com.github.eklipse2k8.charting.data.BarEntry
import com.github.eklipse2k8.charting.data.Entry
import java.io.*

/**
 * Utilities class for interacting with the assets and the devices storage to load and save DataSet
 * objects from and to .txt files.
 *
 * @author Philipp Jahoda
 */
object FileUtils {
  private const val LOG = "MPChart-FileUtils"

  /**
   * Loads a an Array of Entries from a textfile from the sd-card.
   *
   * @param path the name of the file on the sd-card (+ path if needed)
   * @return
   */
  fun loadEntriesFromFile(path: String?): List<Entry> {
    val sdcard = Environment.getExternalStorageDirectory()

    // Get the text file
    val file = File(sdcard, path)
    val entries = mutableListOf<Entry>()
    try {
      val br = BufferedReader(FileReader(file))
      var line: String
      while (br.readLine().also { line = it } != null) {
        val split = line.split("#").toTypedArray()
        if (split.size <= 2) {
          entries.add(Entry(split[0].toFloat(), split[1].toInt().toFloat()))
        } else {
          val vals = FloatArray(split.size - 1)
          for (i in vals.indices) {
            vals[i] = split[i].toFloat()
          }
          entries.add(BarEntry(split[split.size - 1].toFloat(), yVals = vals))
        }
      }
    } catch (e: IOException) {
      Log.e(LOG, e.toString())
    }
    return entries
  }

  /**
   * Loads an array of Entries from a textfile from the assets folder.
   *
   * @param am
   * @param path the name of the file in the assets folder (+ path if needed)
   * @return
   */
  fun loadEntriesFromAssets(am: AssetManager, path: String?): List<Entry> {
    val entries: MutableList<Entry> = ArrayList()
    var reader: BufferedReader? = null
    try {
      reader = BufferedReader(InputStreamReader(am.open(path!!), "UTF-8"))
      var line = reader.readLine()
      while (line != null) {
        // process line
        val split = line.split("#").toTypedArray()
        if (split.size <= 2) {
          entries.add(Entry(split[1].toFloat(), split[0].toFloat()))
        } else {
          val vals = FloatArray(split.size - 1)
          for (i in vals.indices) {
            vals[i] = split[i].toFloat()
          }
          entries.add(BarEntry(split[split.size - 1].toFloat(), yVals = vals))
        }
        line = reader.readLine()
      }
    } catch (e: IOException) {
      Log.e(LOG, e.toString())
    } finally {
      if (reader != null) {
        try {
          reader.close()
        } catch (e: IOException) {
          Log.e(LOG, e.toString())
        }
      }
    }
    return entries
  }

  /**
   * Saves an Array of Entries to the specified location on the sdcard
   *
   * @param entries
   * @param path
   */
  fun saveToSdCard(entries: List<Entry>, path: String?) {
    val sdcard = Environment.getExternalStorageDirectory()
    val saved = File(sdcard, path)
    if (!saved.exists()) {
      try {
        saved.createNewFile()
      } catch (e: IOException) {
        Log.e(LOG, e.toString())
      }
    }
    try {
      // BufferedWriter for performance, true to set append to file flag
      val buf = BufferedWriter(FileWriter(saved, true))
      for (e in entries) {
        buf.append("${e.y}#${e.x}")
        buf.newLine()
      }
      buf.close()
    } catch (e: IOException) {
      Log.e(LOG, e.toString())
    }
  }

  fun loadBarEntriesFromAssets(am: AssetManager, path: String?): List<BarEntry> {
    val entries: MutableList<BarEntry> = ArrayList()
    var reader: BufferedReader? = null
    try {
      reader = BufferedReader(InputStreamReader(am.open(path!!), "UTF-8"))
      var line = reader.readLine()
      while (line != null) {
        // process line
        val split = line.split("#").toTypedArray()
        entries.add(BarEntry(split[1].toFloat(), split[0].toFloat()))
        line = reader.readLine()
      }
    } catch (e: IOException) {
      Log.e(LOG, e.toString())
    } finally {
      if (reader != null) {
        try {
          reader.close()
        } catch (e: IOException) {
          Log.e(LOG, e.toString())
        }
      }
    }
    return entries
  }
}
