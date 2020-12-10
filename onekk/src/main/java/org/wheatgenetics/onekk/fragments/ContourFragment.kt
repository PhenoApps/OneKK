package org.wheatgenetics.onekk.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_contour_list.*
import kotlinx.coroutines.*
import org.opencv.core.CvException
import org.wheatgenetics.imageprocess.DrawSelectedContour
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.adapters.ContourAdapter
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.viewmodels.ExperimentViewModel
import org.wheatgenetics.onekk.database.viewmodels.factory.OnekkViewModelFactory
import org.wheatgenetics.onekk.databinding.FragmentContourListBinding
import org.wheatgenetics.onekk.interfaces.ContourOnTouchListener
import org.wheatgenetics.utils.Dialogs
import kotlin.properties.Delegates

class ContourFragment : Fragment(), CoroutineScope by MainScope(), ContourOnTouchListener {

    private val db: OnekkDatabase by lazy {
        OnekkDatabase.getInstance(requireContext())
    }

    private val sViewModel: ExperimentViewModel by viewModels {

        OnekkViewModelFactory(
                OnekkRepository.getInstance(db.dao(), db.coinDao()))

    }

    private var mSourceBitmap: String? = null
    private var aid by Delegates.notNull<Int>()

    private var mBinding: FragmentContourListBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        aid = requireArguments().getInt("analysis", -1)

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_contour_list, null, false)

        mBinding?.let { ui ->

            ui.setupRecyclerView(aid)

            ui.setupButtons()

            updateUi(aid)

            sViewModel.getSourceImage(aid).observeForever { url ->

                mSourceBitmap = url

                imageView?.setImageBitmap(BitmapFactory.decodeFile(mSourceBitmap))
            }

            return ui.root

        }

        updateTotal()

        setHasOptionsMenu(true)

        return null
    }

    suspend fun updateImageView(x: Double, y: Double, minAxis: Double, maxAxis: Double): Deferred<Bitmap> = withContext(Dispatchers.IO) {

        async {

            val bmp = BitmapFactory.decodeFile(mSourceBitmap)

            try {

                DrawSelectedContour().process(bmp, x, y, minAxis, maxAxis)

            } catch (e: CvException) {

                e.printStackTrace()

                bmp
            }
        }
    }

    override fun onTouch(x: Double, y: Double, minAxis: Double, maxAxis: Double) {

        launch {

            mBinding?.imageView?.setImageBitmap(
                    updateImageView(x, y, minAxis, maxAxis)
                            .await())

        }

    }

    private fun updateTotal() = with(requireActivity()) {

        sViewModel.contours(aid).observeForever { contours ->

            runOnUiThread {

                val count = contours.filter { it.selected }.mapNotNull { it.contour?.count }.reduceRight { x, y ->  y + x }

                mBinding?.submitButton?.setText(getString(R.string.frag_contour_list_total) + count)

            }
        }
    }

    override fun onChoiceSwapped(id: Int, selected: Boolean) {

        launch {

            sViewModel.switchSelectedContour(aid, id, selected)

            updateTotal()
        }
    }

    private fun FragmentContourListBinding.setupButtons() {

        submitButton.setOnClickListener {


        }
    }

    private fun FragmentContourListBinding.setupRecyclerView(aid: Int) {

        singleSeedListView?.layoutManager = LinearLayoutManager(requireContext())

        singleSeedListView?.adapter = ContourAdapter(this@ContourFragment, requireContext())

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {

                return false

            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                Dialogs.onOk(AlertDialog.Builder(requireContext()),
                        getString(R.string.ask_delete_experiment),
                        getString(R.string.cancel),
                        getString(R.string.ok)) {

                    if (it) {

                        val cid = viewHolder.itemView.tag as Int

                        launch {

                            sViewModel.deleteContour(aid, cid)

                            updateUi(aid)
                        }

                    }

                    singleSeedListView?.adapter?.notifyItemChanged(viewHolder.adapterPosition)
                }
            }

        }).attachToRecyclerView(singleSeedListView)

        clusterListView?.layoutManager = LinearLayoutManager(requireContext())

        clusterListView?.adapter = ContourAdapter(this@ContourFragment, requireContext())

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {

                return false

            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                Dialogs.onOk(AlertDialog.Builder(requireContext()),
                        getString(R.string.ask_delete_experiment),
                        getString(R.string.cancel),
                        getString(R.string.ok)) {

                    if (it) {

                        val cid = viewHolder.itemView.tag as Int

                        launch {

                            sViewModel.deleteContour(aid, cid)

                            updateUi(aid)
                        }

                    }

                    clusterListView?.adapter?.notifyItemChanged(viewHolder.adapterPosition)
                }
            }

        }).attachToRecyclerView(clusterListView)
    }

    private fun updateUi(aid: Int) {

        sViewModel.contours(aid).observeForever {

            val singles = it.filter { it.contour?.count ?: 0 <= 1 }

            val clusters = it.filter { it.contour?.count ?: 0 > 1 }

            (mBinding?.singleSeedListView?.adapter as? ContourAdapter)
                    ?.submitList(singles)

            (mBinding?.clusterListView?.adapter as? ContourAdapter)
                    ?.submitList(clusters)
        }
    }
}