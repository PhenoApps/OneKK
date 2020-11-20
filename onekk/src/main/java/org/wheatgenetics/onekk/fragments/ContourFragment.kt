package org.wheatgenetics.onekk.fragments

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.adapters.ContourAdapter
import org.wheatgenetics.onekk.adapters.ExperimentAdapter
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.models.ExperimentEntity
import org.wheatgenetics.onekk.database.models.embedded.Experiment
import org.wheatgenetics.onekk.database.viewmodels.ExperimentViewModel
import org.wheatgenetics.onekk.database.viewmodels.factory.OnekkViewModelFactory
import org.wheatgenetics.onekk.databinding.FragmentContourListBinding
import org.wheatgenetics.onekk.databinding.FragmentExperimentListBinding
import org.wheatgenetics.utils.Dialogs

class ContourFragment : Fragment(), CoroutineScope by MainScope() {

    private val db: OnekkDatabase by lazy {
        OnekkDatabase.getInstance(requireContext())
    }

    private val sViewModel: ExperimentViewModel by viewModels {

        OnekkViewModelFactory(
                OnekkRepository.getInstance(db.dao(), db.coinDao()))

    }

    private var mBinding: FragmentContourListBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val eid: Int = requireArguments().getInt("experiment", -1)
        val aid: Int = requireArguments().getInt("analysis", -1)

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_contour_list, null, false)

        mBinding?.let { ui ->

            ui.setupRecyclerView(eid, aid)

            setupButtons()

            updateUi(eid, aid)

            return ui.root

        }

        setHasOptionsMenu(true)

        return null
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

    private fun setupButtons() {


    }

    private fun FragmentContourListBinding.setupRecyclerView(eid: Int, aid: Int) {

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        recyclerView.adapter = ContourAdapter(requireContext())

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

                        }

                    } else  mBinding?.recyclerView?.adapter?.notifyItemChanged(viewHolder.adapterPosition)

                }
            }

        }).attachToRecyclerView(recyclerView)
    }

    private fun updateUi(eid: Int, aid: Int) {

        sViewModel.contours(aid).observeForever {

            (mBinding?.recyclerView?.adapter as? ContourAdapter)
                    ?.submitList(it)

            //queueScroll()
        }
    }

    private fun queueScroll() {

        mBinding?.let { ui ->

            Handler().postDelayed({

                ui.recyclerView.scrollToPosition(0)

            }, 250)
        }
    }
}