package org.wheatgenetics.onekk.fragments

import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.*
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.adapters.AnalysisAdapter
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.models.AnalysisEntity
import org.wheatgenetics.onekk.database.viewmodels.ExperimentViewModel
import org.wheatgenetics.onekk.database.viewmodels.factory.OnekkViewModelFactory
import org.wheatgenetics.onekk.databinding.FragmentAnalysisManagerBinding
import org.wheatgenetics.onekk.interfaces.AnalysisUpdateListener
import org.wheatgenetics.onekk.interfaces.OnClickAnalysis
import org.wheatgenetics.onekk.observeOnce
import org.wheatgenetics.utils.DateUtil
import org.wheatgenetics.utils.Dialogs
import org.wheatgenetics.utils.FileUtil
import java.util.*

class AnalysisFragment : Fragment(), AnalysisUpdateListener, OnClickAnalysis, CoroutineScope by MainScope() {

    private val viewModel by viewModels<ExperimentViewModel> {
        with(OnekkDatabase.getInstance(requireContext())) {
            OnekkViewModelFactory(OnekkRepository.getInstance(this.dao(), this.coinDao()))
        }
    }

    companion object {

        const val TAG = "Onekk.AnalysisFragment"

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    //variable to track if select all button deselects or selects
    private var mSelectMode = true

    private var mBinding: FragmentAnalysisManagerBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_analysis_manager, container, false)

        with(mBinding) {

            this?.recyclerView?.layoutManager = LinearLayoutManager(context).apply {
                orientation = LinearLayoutManager.VERTICAL
            }

            this?.updateUi()
        }

        //deselect all analysis before view is created
        launch {

            viewModel.updateSelectAllAnalysis(false)

        }

        mBinding?.setupTopToolbar()

        return mBinding?.root

    }

    override fun onResume() {
        super.onResume()

        mBinding?.updateUi()
    }

    private fun FragmentAnalysisManagerBinding.updateUi() {


        viewModel.getExampleImages().observeForever { uri ->

            uri?.let { _ ->

                viewModel.analysis().observeOnce(this@AnalysisFragment, {

                    this.recyclerView.adapter = AnalysisAdapter(this@AnalysisFragment)

                    (this.recyclerView.adapter as? AnalysisAdapter)?.submitList(
                            it.sortedByDescending { analyses -> analyses.date })
                })
            }
        }
    }

    private val exportSamples = registerForActivityResult(ActivityResultContracts.CreateDocument()) { it?.let { uri ->

        launch {

            withContext(Dispatchers.IO) {

                (mBinding?.recyclerView?.adapter as? AnalysisAdapter)
                    ?.currentList?.filter { analysis -> analysis.selected }?.let { data ->

                        FileUtil(requireContext()).export(uri, data)

                }
            }
        }
    }}

    private val exportSeeds = registerForActivityResult(ActivityResultContracts.CreateDocument()) { it?.let { uri ->

        (mBinding?.recyclerView?.adapter as? AnalysisAdapter)?.currentList?.filter { analysis -> analysis.selected }?.let { analysis ->

            viewModel.selectAllContours().observeOnce(viewLifecycleOwner, { contours ->

                launch {

                    withContext(Dispatchers.IO) {

                        FileUtil(requireContext()).exportSeeds(uri, analysis, contours)

                    }
                }
            })
        }
    }}

    /**
     * Uses activity results contracts to create a document and call the export function
     */
    private fun exportFile() {

        val outputFilePrefix = getString(R.string.export_file_prefix)

        val fileName = "$outputFilePrefix${DateUtil().getTime()}.csv"

        Dialogs.booleanOption(AlertDialog.Builder(requireContext()),
                getString(R.string.frag_analysis_dialog_export_title),
                getString(R.string.frag_analysis_dialog_export_samples_option),
                getString(R.string.frag_analysis_dialog_export_seeds_option),
                getString(R.string.frag_analysis_dialog_export_cancel_option)) { option ->

            if (option) {

                exportSamples.launch(fileName)

            } else {

                exportSeeds.launch(fileName)

            }
        }
    }

    private inline fun onSelectedNotEmpty(function: (List<AnalysisEntity>?) -> Unit) {

        (mBinding?.recyclerView?.adapter as? AnalysisAdapter)
            ?.currentList?.filter { it.selected }?.let { data ->

                if (data.isNotEmpty()) {

                    function(data)

                } else {

                    Toast.makeText(context, R.string.frag_analysis_no_selected_data, Toast.LENGTH_SHORT).show()

                }
            }
    }

    private fun FragmentAnalysisManagerBinding.setupTopToolbar() {

        with (toolbar) {
            findViewById<ImageButton>(R.id.importButton)?.setOnClickListener {
                findNavController().navigate(AnalysisFragmentDirections.globalActionToImport(mode = "import"))
            }
            findViewById<ImageButton>(R.id.selectAllButton)?.setOnClickListener {
                viewModel.updateSelectAllAnalysis(mSelectMode)

                mSelectMode = !mSelectMode

                mBinding?.updateUi()
            }
            findViewById<ImageButton>(R.id.exportButton)?.setOnClickListener {
                onSelectedNotEmpty {
                   exportFile()
               }
            }
            findViewById<ImageButton>(R.id.deleteButton)?.setOnClickListener {
                onSelectedNotEmpty {
                    askDelete()
                }
            }
        }
    }

    private fun askDelete() {

        Dialogs.onOk(AlertDialog.Builder(requireContext()),
                title = getString(R.string.frag_analysis_ask_delete_all),
                cancel = getString(R.string.frag_analysis_ask_delete_cancel_button),
                ok = getString(R.string.frag_analysis_ask_delete_ok_button)) {

            if (it) {

                launch {

                    viewModel.deleteSelectedAnalysis()

                    activity?.runOnUiThread {
                        mBinding?.updateUi()
                    }
                }
            }
        }
    }

    override fun onAnalysisUpdated(aid: Int, weight: Double?) {

        viewModel.updateAnalysisWeight(aid, weight)

    }

    override fun onClickCount(aid: Int) {

        findNavController().navigate(AnalysisFragmentDirections.actionToContours(aid))

    }

    override fun onClickGraph(aid: Int) {

        findNavController().navigate(AnalysisFragmentDirections.actionToGraph(aid))

    }

    override fun onSelectionSwapped(position: Int, model: AnalysisEntity, selected: Boolean) {

        viewModel.updateAnalysisSelected(model.aid!!, selected)

        (mBinding?.recyclerView?.adapter?.notifyItemChanged(position))
    }

    override fun onClick(aid: Int) {

        findNavController().navigate(AnalysisFragmentDirections.actionToScale(aid))

    }
}