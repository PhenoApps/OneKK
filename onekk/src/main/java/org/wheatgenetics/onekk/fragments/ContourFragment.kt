package org.wheatgenetics.onekk.fragments

import android.app.ActionBar
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.marginStart
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.evrencoskun.tableview.listener.ITableViewListener
import kotlinx.android.synthetic.main.fragment_contour_list.*
import kotlinx.coroutines.*
import org.opencv.core.CvException
import org.wheatgenetics.imageprocess.DrawSelectedContour
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.activities.MainActivity
import org.wheatgenetics.onekk.adapters.ContourAdapter
import org.wheatgenetics.onekk.adapters.ContourListAdapter
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.models.ContourEntity
import org.wheatgenetics.onekk.database.viewmodels.ExperimentViewModel
import org.wheatgenetics.onekk.database.viewmodels.factory.OnekkViewModelFactory
import org.wheatgenetics.onekk.databinding.FragmentContourListBinding
import org.wheatgenetics.onekk.interfaces.ContourOnTouchListener
import org.wheatgenetics.onekk.observeOnce
import kotlin.properties.Delegates

class ContourFragment : Fragment(), CoroutineScope by MainScope(), ContourOnTouchListener,
    ITableViewListener {

    private val db: OnekkDatabase by lazy {
        OnekkDatabase.getInstance(requireContext())
    }

    private val mPreferences by lazy {
        requireContext().getSharedPreferences(getString(R.string.onekk_preference_key), Context.MODE_PRIVATE)
    }

    private val sViewModel: ExperimentViewModel by viewModels {

        OnekkViewModelFactory(
                OnekkRepository.getInstance(db.dao(), db.coinDao()))

    }

    private var mContours: List<ContourEntity>? = null
    private var mSourceBitmap: String? = null
    private var aid by Delegates.notNull<Int>()
    private var mSortState: Boolean = true
    private var mSortMode: Int = 0
    private var mBinding: FragmentContourListBinding? = null

    private var mAdapter: ContourListAdapter? = null

    /***
     * Polymorphism class structure to serve different cell types to the grid.
     */
    open class BlockData(open val code: String) {
        override fun hashCode(): Int {
            return code.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as BlockData

            if (code != other.code) return false

            return true
        }
    }

    data class HeaderData(val name: String, override val code: String) : BlockData(code)
    data class CellData(val value: String?, override val code: String, val color: Int = Color.GREEN, val onClick: View.OnClickListener? = null): BlockData(code)
    class EmptyCell(override val code: String): BlockData(code)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        aid = requireArguments().getInt("analysis", -1)

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_contour_list, null, false)

        setHasOptionsMenu(true)

        //custom support action toolbar
        with(mBinding?.toolbar) {
            //create back button listener in toolbar
            this?.findViewById<ImageButton>(R.id.backButton)?.setOnClickListener {
                findNavController().popBackStack()
            }
        }

        mBinding?.fragContourListTableView?.selectionHandler?.isShadowEnabled = false

        sViewModel.getSourceImage(aid).observeOnce(viewLifecycleOwner, { uri ->

            mSourceBitmap = uri

            try {

                Glide.with(requireContext()).asBitmap().load(uri).fitCenter().into(imageView)
                //imageView?.setImageBitmap(BitmapFactory.decodeFile(mSourceBitmap))

            } catch (e: Exception) {
                e.printStackTrace()
            }

            mBinding?.imageView?.visibility = View.VISIBLE

        })

        sViewModel.contours(aid).observeOnce(viewLifecycleOwner, { data ->

            mContours = data

            activity?.runOnUiThread {

                updateUi()

            }
        })

        mAdapter = ContourListAdapter(0, 0, this)

        mBinding?.setupButtons()

        return mBinding?.root
    }

    private suspend fun updateImageViewAsync(x: Double, y: Double, cluster: Boolean, minAxis: Double, maxAxis: Double): Deferred<Bitmap> = withContext(Dispatchers.IO) {

        async {

            val bmp = BitmapFactory.decodeFile(mSourceBitmap)

            try {

                DrawSelectedContour(context).process(bmp, x, y, cluster, minAxis, maxAxis)

            } catch (e: CvException) {

                e.printStackTrace()

                bmp
            }
        }
    }

    private var mLastSelectedContourId = -1
    /**
     * Interface function that returns the cropped image around the chosen contour.
     * Uses a global variable to track if the same contour was clicked, if it was then show the
     * original image; otherwise, show the new contour region.
     */
    override fun onTouch(cid: Int, x: Double, y: Double, cluster: Boolean, minAxis: Double, maxAxis: Double) {

        try {
            launch {

                mLastSelectedContourId = when (mLastSelectedContourId) {

                    cid -> {

                        mBinding?.imageView?.setImageBitmap(BitmapFactory.decodeFile(mSourceBitmap))

                        -1
                    }
                    else -> {

                        mBinding?.imageView?.setImageBitmap(
                                updateImageViewAsync(x, y, cluster, minAxis, maxAxis)
                                        .await())

                        cid
                    }
                }
            }

        } catch (e: Exception) {

            e.printStackTrace()

        }
    }

    //whenever a user-adjustment occurs, update the title.
    //this isn't saved in the database until the submit button is pressed.
    private fun updateTotal() = try {

        mContours?.let { contours ->

            //update the count in the toolbar
            if (contours.isNotEmpty()) {

                //use the update map to adjust count when the save button is pressed
                val count = contours.filter { it.selected }.mapNotNull {
                    it.contour?.count
                }.reduceRight { x, y -> y + x }

                mBinding?.toolbar?.findViewById<TextView>(R.id.countTextView)?.text = "$count"
            }
        }

    } catch (e: Exception) {

        e.printStackTrace()

    }

    //interface listener for the contour adapter
    override fun onCountEdited(cid: Int, count: Int) {

        activity?.let { act ->

            val et = EditText(act).apply {
                setText(count.toString())
                gravity = Gravity.CENTER_HORIZONTAL
            }

            AlertDialog.Builder(act)
                .setTitle(R.string.frag_contour_dialog_update_count_title)
                .setView(et)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->

                    val n = et.text.toString().toIntOrNull()
                    if (et.text.isNotBlank() && n != null) {

                        mContours?.find { it.cid == cid }?.contour?.count = n

                        updateUi()
                    }

                    dialog.dismiss()

                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ ->

                    dialog.dismiss()

                }.show()
        }
    }

    override fun onChoiceSwapped(id: Int, selected: Boolean) {

        mContours?.find { it.cid == id }?.selected = selected

        updateUi()

    }

    private fun FragmentContourListBinding.setupButtons() {

        submitButton.setOnClickListener {

            mContours?.let { contours ->

                //update selection status and count in db
                launch {
                    contours.forEach {
                        sViewModel.switchSelectedContour(it.aid, it.cid ?: -1, it.selected)
                        sViewModel.updateContourCount(it.cid ?: -1, it.contour?.count ?: 0)
                    }
                }

                //aggregate the count
                val count = contours.filter { it.selected }.map {
                    it.contour?.count ?: 0
                }.reduceRight { x, y ->  y + x }

                mBinding?.toolbar?.findViewById<TextView>(R.id.countTextView)?.text = "$count"

                try {

                    sViewModel.updateAnalysisCount(aid, count)

                    when (mPreferences.getString(getString(R.string.onekk_preference_mode_key), "1")) {

                        "1", "2" -> findNavController().popBackStack()

                        else -> findNavController().navigate(ContourFragmentDirections.actionToScale(aid))
                    }

                } catch (e: Exception) {

                    e.printStackTrace()

                }
            }
        }
    }

    private fun updateUi() {

        val headers = arrayOf("Counted", "Area", "Length", "Width", "Count")

        try {

            mContours?.let { data ->

                val singles = data.filter { it.contour?.count ?: 0 <= 1 }

                val clusters = data.filter { it.contour?.count ?: 0 > 1 }

                val contours = (singles + clusters)

                //uses the two different modes to sort (ascending/descending) vs (area/l/w/count)
                val sorted = sortByState(contours)

                val dataMap = arrayListOf<List<CellData>>()

                sorted.forEach {
                    val dataList = arrayListOf<CellData>()
                    val id = it.cid?.toString() ?: "-1"
                    val selected = if (it.selected) "true" else "false"
                    dataList.add(CellData(selected, id))
                    dataList.add(CellData(it.contour?.area?.toString(), id))
                    dataList.add(CellData(it.contour?.minAxis?.toString(), id))
                    dataList.add(CellData(it.contour?.maxAxis?.toString(), id))
                    dataList.add(CellData(it.contour?.count?.toString(), id))
                    dataMap.add(dataList)
                }

                activity?.runOnUiThread {

                    updateTotal()

                    mBinding?.fragContourListTableView?.apply {
                        setHasFixedWidth(true)
                        tableViewListener = this@ContourFragment
                        isShowHorizontalSeparators = false
                        isShowVerticalSeparators = false
                        setAdapter(mAdapter)
                    }

                    mAdapter?.setAllItems(
                        headers.map { HeaderData(it, it) },
                        null,
                        dataMap.toList())
                }
            }

        } catch (e: Exception) {

            e.printStackTrace()

        }
    }

    private fun sortByState(contours: List<ContourEntity>): List<ContourEntity> {

        return when(mSortState) {

            //sort by ascending
            true -> sortByMode(true, contours)

            //sort by descending
            else -> sortByMode(false, contours)
        }

    }

    private fun sortByMode(ascending: Boolean, contours: List<ContourEntity>): List<ContourEntity> {

        return when (mSortMode) {

            0 -> {

                if (ascending) contours.sortedBy { it.contour?.area }
                else contours.sortedByDescending { it.contour?.area }

            }
            1 -> {

                if (ascending) contours.sortedBy { it.contour?.maxAxis }
                else contours.sortedByDescending { it.contour?.maxAxis }

            }
            2 -> {

                if (ascending) contours.sortedBy { it.contour?.minAxis }
                else contours.sortedByDescending { it.contour?.minAxis }

            }
            else -> {

                if (ascending) contours.sortedBy { it.contour?.count }
                else contours.sortedByDescending { it.contour?.count }

            }
        }
    }

    override fun onCellClicked(cellView: RecyclerView.ViewHolder, column: Int, row: Int) {
        mAdapter?.getCellItem(column, row)?.let { cell ->

           mContours?.find { it.cid?.toString() == cell.code }?.let { x ->

               if (column == 0) {

                   onChoiceSwapped(cell.code.toInt(), !x.selected)

               } else {
                   with (x.contour) {
                       onTouch(cell.code.toInt(),
                           this?.x ?: 0.0,
                           this?.y ?: 0.0,
                           this?.count ?: 0 > 1,
                           this?.minAxis ?: 0.0,
                           this?.maxAxis ?: 0.0)
                   }
               }
           }
        }
    }

    override fun onColumnHeaderClicked(columnHeaderView: RecyclerView.ViewHolder, column: Int) {
        when (column) {
            1 -> {
                if (mSortMode == 0) mSortState = !mSortState
                mSortMode = 0
                updateUi()
            }
            2 -> {
                if (mSortMode == 1) mSortState = !mSortState
                mSortMode = 1
                updateUi()
            }
            3 -> {
                if (mSortMode == 2) mSortState = !mSortState
                mSortMode = 2
                updateUi()
            }
            4 -> {
                if (mSortMode == 3) mSortState = !mSortState
                mSortMode = 3
                updateUi()
            }
        }
    }

    override fun onCellDoubleClicked(cellView: RecyclerView.ViewHolder, column: Int, row: Int) {}
    override fun onCellLongPressed(cellView: RecyclerView.ViewHolder, column: Int, row: Int) {}
    override fun onColumnHeaderDoubleClicked(columnHeaderView: RecyclerView.ViewHolder, column: Int) {}
    override fun onColumnHeaderLongPressed(columnHeaderView: RecyclerView.ViewHolder, column: Int) {}
    override fun onRowHeaderClicked(rowHeaderView: RecyclerView.ViewHolder, row: Int) {}
    override fun onRowHeaderDoubleClicked(rowHeaderView: RecyclerView.ViewHolder, row: Int) {}
    override fun onRowHeaderLongPressed(rowHeaderView: RecyclerView.ViewHolder, row: Int) {}
}