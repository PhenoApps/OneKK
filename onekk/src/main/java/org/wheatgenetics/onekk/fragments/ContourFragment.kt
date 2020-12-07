package org.wheatgenetics.onekk.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_contour_list.*
import kotlinx.coroutines.*
import org.opencv.core.MatOfPoint
import org.wheatgenetics.imageprocess.DrawSelectedContour
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.adapters.ContourAdapter
import org.wheatgenetics.onekk.adapters.ExperimentAdapter
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.models.ContourEntity
import org.wheatgenetics.onekk.database.models.ExperimentEntity
import org.wheatgenetics.onekk.database.models.embedded.Experiment
import org.wheatgenetics.onekk.database.viewmodels.ExperimentViewModel
import org.wheatgenetics.onekk.database.viewmodels.factory.OnekkViewModelFactory
import org.wheatgenetics.onekk.databinding.FragmentContourListBinding
import org.wheatgenetics.onekk.databinding.FragmentExperimentListBinding
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
    private var eid by Delegates.notNull<Int>()
    private var aid by Delegates.notNull<Int>()

    private var mBinding: FragmentContourListBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        eid = requireArguments().getInt("experiment", -1)
        aid = requireArguments().getInt("analysis", -1)

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_contour_list, null, false)

        mBinding?.let { ui ->

            ui.setupRecyclerView(aid)

            ui.setupButtons()

            updateUi(eid, aid)

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

            DrawSelectedContour().process(BitmapFactory.decodeFile(mSourceBitmap),
                        x, y, minAxis, maxAxis)
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

                val count = contours.filter { it.selected && !(it.contour?.isCluster ?: false) }.size

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

//        inflater.inflate(R.menu.activity_main_toolbar, menu)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

//        when(item.itemId) {
//
//        }
        return super.onOptionsItemSelected(item)
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

                            updateUi(eid, aid)
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

                            updateUi(eid, aid)
                        }

                    }

                    clusterListView?.adapter?.notifyItemChanged(viewHolder.adapterPosition)
                }
            }

        }).attachToRecyclerView(clusterListView)
    }

    private fun updateUi(eid: Int, aid: Int) {

        sViewModel.contours(aid).observeForever {

            val contours = it.partition { it.contour?.isCluster ?: false }

            (mBinding?.singleSeedListView?.adapter as? ContourAdapter)
                    ?.submitList(contours.second)

            (mBinding?.clusterListView?.adapter as? ContourAdapter)
                    ?.submitList(contours.first)
        }
    }
}