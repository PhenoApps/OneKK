package org.wheatgenetics.onekk.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.BarGraphSeries
import com.jjoe64.graphview.series.DataPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.viewmodels.ExperimentViewModel
import org.wheatgenetics.onekk.database.viewmodels.factory.OnekkViewModelFactory
import org.wheatgenetics.onekk.databinding.FragmentGraphBinding
import kotlin.math.log2
import kotlin.math.pow
import kotlin.properties.Delegates

class GraphFragment : Fragment(), CoroutineScope by MainScope() {

    private val db: OnekkDatabase by lazy {
        OnekkDatabase.getInstance(requireContext())
    }

    private val sViewModel: ExperimentViewModel by viewModels {

        OnekkViewModelFactory(
                OnekkRepository.getInstance(db.dao(), db.coinDao()))

    }

    private var aid by Delegates.notNull<Int>()
    private var mBinding: FragmentGraphBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        aid = requireArguments().getInt("analysis", -1)

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_graph, null, false)

        /**
         * Setup graph refresh whenever a tab is selected.
         */
        mBinding?.graphTabLayout?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {

                tab?.let {

                    loadGraph(it.text.toString())
                }
            }
        })

        //custom support action toolbar
        with(mBinding?.toolbar) {
            this?.findViewById<ImageButton>(R.id.backButton)?.setOnClickListener {
                findNavController().popBackStack()
            }
        }

        //force area graph to be ch
        loadGraph(getString(R.string.graph_tab_area))

        return mBinding?.root
    }

    /**
     * Loads the Graph View given the variable to use as data-points. Could be area, length or width.
     */
    private fun loadGraph(variable: String) {

        val areaOption = getString(R.string.graph_tab_area)
        val widthOption = getString(R.string.graph_tab_width)
       // val lengthOption = getString(R.string.graph_tab_length)

        sViewModel.contours(aid).observe(viewLifecycleOwner, { data ->

            data?.let { sample ->

                if (sample.isNotEmpty()) {

                    //collect all contours that are not clusters
                    val nonEmpty = sample.filter { x -> (x.contour?.count ?: 0) == 1 }

                    //sort by the given variable (from the parameter)
                    val sortedData = when (variable) {

                        areaOption -> {

                            nonEmpty.mapNotNull { x -> x.contour?.area }.sortedBy { it }

                        }
                        widthOption -> {

                            nonEmpty.mapNotNull { x -> x.contour?.minAxis }.sortedBy { it }

                        }
                        else -> {

                            nonEmpty.mapNotNull { x -> x.contour?.maxAxis }.sortedBy { it }

                        }
                    }

                    //val mean = data.reduceRight { x, y -> x + y } / data.size

                    //val variance = variance(data, mean, data.size)

                    //val stdDev = sqrt(variance)

//                    val bell = areas.mapNotNull { x -> DataPoint(x, getY(x, variance, mean, stdDev)) }

                   // setViewportGrid(mBinding!!.graphView)

//                    setViewport(bell.minOf { it.x }, bell.maxOf { it.x }, bell.minOf { it.y }, bell.maxOf { it.y }, mBinding!!.graphView)

//                    setViewport(0.0, areas.size.toDouble(), areas.min() ?: 0.0, areas.max() ?: 0.0, mBinding!!.graphView)

//                    val points = areas.mapIndexed { index, d -> DataPoint(index.toDouble(), d) }

                    //renderNormal(mBinding!!.graphView, points)

                    displayHistogram(mBinding!!.graphView, BarGraphSeries(),
                        sturgeRule(data.size.toDouble()).toInt(), sortedData.toDoubleArray())
                }
            }
        })
    }

    private fun sturgeRule(n: Double) = 1 + 3.3 * log2(n)

    private fun getHistogramX(minimum: Double, histogram: Int, bins: Double): DoubleArray {

        val xValues = DoubleArray(histogram)

        for (i in 0 until histogram) {

            if (i == 0){

                xValues[i] = minimum

            } else {

                val previous = xValues[i-1]

                xValues[i] = previous + bins

            }
        }

        return xValues
    }

    //Based on https://stackoverflow.com/questions/10786465/how-to-generate-bins-for-histogram-using-apache-math-3-0-in-java
    private fun displayHistogram(graph: GraphView, series: BarGraphSeries<DataPoint>, numBins: Int, data: DoubleArray) {

        val histogram = DoubleArray(numBins)

        val distribution = org.apache.commons.math3.random.EmpiricalDistribution(numBins)

        distribution.load(data)

        distribution.binStats.forEachIndexed { index, stat ->

            histogram[index] = stat.n.toDouble()

        }

        val min = data.minByOrNull { it } ?: 0
        val max = data.maxByOrNull { it } ?: 0

        val binSize = (max.toDouble() - min.toDouble()) / numBins

        for (i in histogram.indices) {

            series.appendData(
                    DataPoint(getHistogramX(min.toDouble(), histogram.size, binSize)[i], histogram[i]
            ), false, histogram.count())
        }

        graph.removeAllSeries()

        series.color = context?.getColor(R.color.colorAccent) ?: Color.rgb(0, 255, 0)

        series.valuesOnTopColor = context?.getColor(R.color.colorPrimary) ?: Color.rgb(0, 0, 255)

        series.isDrawValuesOnTop = true

        graph.addSeries(series)

        graph.viewport.isScalable = true
        graph.viewport.isScrollable = true
    }

//    private fun setViewportGrid(graph: GraphView) = with(graph){
//
////    this.title = title
////        gridLabelRenderer.gridStyle = GridLabelRenderer.GridStyle.BOTH
////        gridLabelRenderer.isHorizontalLabelsVisible=false
////        gridLabelRenderer.isVerticalLabelsVisible=false
////    gridLabelRenderer.horizontalAxisTitle = xAxis
////    gridLabelRenderer.verticalAxisTitle = yAxis
//
//    }

//    fun setViewport(minX: Double, maxX: Double, minY: Double, maxY: Double, graph: GraphView) = with(graph) {
//
//        // set manual X bounds
//        viewport.isXAxisBoundsManual = true
//        viewport.setMinX(minX)
//        viewport.setMaxX(maxX)
//
//        // set manual Y bounds
//        viewport.isYAxisBoundsManual = true
//        viewport.setMinY(minY)
//        viewport.setMaxY(maxY)
//
//        // activate horizontal zooming and scrolling
//        viewport.setScalable(true)
//
//// activate horizontal scrolling
//        viewport.setScrollable(true)
//
//// activate horizontal and vertical zooming and scrolling
//        viewport.setScalableY(true)
//
//// activate vertical scrolling
//        viewport.setScrollableY(true)
//
//    }
//
//    fun renderNormal(graph: GraphView, data: List<DataPoint>) = with(graph) {
//
//        graph.removeAllSeries()
//
//        val plot = BarGraphSeries(data.toTypedArray())
//
//        plot.color = Color.rgb(0, 255, 0)
//
//        plot.isDrawValuesOnTop = true
//
//        plot.spacing = 50
//
//        plot.valuesOnTopColor = Color.RED
//
//        graph.addSeries(plot)
//
//    }
}