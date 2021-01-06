package org.wheatgenetics.onekk.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.dsl.genericFastAdapter
import com.polidea.rxandroidble2.helpers.ValueInterpreter
import kotlinx.coroutines.*
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.adapters.AnalysisAdapter
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.models.AnalysisEntity
import org.wheatgenetics.onekk.database.models.ContourEntity
import org.wheatgenetics.onekk.database.viewmodels.ExperimentViewModel
import org.wheatgenetics.onekk.database.viewmodels.factory.OnekkViewModelFactory
import org.wheatgenetics.onekk.databinding.FragmentAnalysisManagerBinding
import org.wheatgenetics.onekk.interfaces.AnalysisUpdateListener
import org.wheatgenetics.onekk.interfaces.BleNotificationListener
import org.wheatgenetics.onekk.interfaces.OnClickAnalysis
import org.wheatgenetics.onekk.observeOnce
import org.wheatgenetics.utils.BluetoothUtil
import org.wheatgenetics.utils.DateUtil
import org.wheatgenetics.utils.Dialogs
import org.wheatgenetics.utils.FileUtil
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

class AnalysisFragment : Fragment(), AnalysisUpdateListener, OnClickAnalysis, CoroutineScope by MainScope() {

    private val viewModel by viewModels<ExperimentViewModel> {
        with(OnekkDatabase.getInstance(requireContext())) {
            OnekkViewModelFactory(OnekkRepository.getInstance(this.dao(), this.coinDao()))
        }
    }

    companion object {

        final val TAG = "Onekk.AnalysisFragment"

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    //variable to track if select all button deselects or selects
    private var mSelectMode = true

    private var mBinding: FragmentAnalysisManagerBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_analysis_manager, container, false)

        with(mBinding) {

            this?.recyclerView?.layoutManager = LinearLayoutManager(requireContext())

            this?.updateUi()
        }

        //deselect all analysis before view is created
        launch {

            viewModel.updateSelectAllAnalysis(false)

        }

        setHasOptionsMenu(true)

        return mBinding?.root
    }

    override fun onResume() {
        super.onResume()

        mBinding?.updateUi()
    }

    private fun FragmentAnalysisManagerBinding.updateUi() {

        viewModel.getExampleImages().observeForever { uri ->

            uri?.let { images ->

                viewModel.analysis().observeOnce(this@AnalysisFragment, {

                    if (it.isEmpty()) {

                        Toast.makeText(requireContext(), R.string.frag_analysis_table_empty_message, Toast.LENGTH_SHORT).show()

                        findNavController().popBackStack()
                    }

                    this?.recyclerView?.adapter = AnalysisAdapter(this@AnalysisFragment)

                    (this.recyclerView.adapter as? AnalysisAdapter)?.submitList(
                            it.sortedByDescending { analyses -> analyses.date })
                })
            }
        }
    }

    private fun exportSamples(fileName: String, analysis: List<AnalysisEntity>) {

        registerForActivityResult(ActivityResultContracts.CreateDocument()) { it?.let { uri ->

            launch {

                withContext(Dispatchers.IO) {

                    FileUtil(requireContext()).export(uri, analysis)

                }
            }
        }}.launch(fileName)

    }

    private fun exportSeeds(fileName: String, analysis: List<AnalysisEntity>, contours: List<ContourEntity>) {

        registerForActivityResult(ActivityResultContracts.CreateDocument()) { it?.let { uri ->

            launch {

                withContext(Dispatchers.IO) {

                    FileUtil(requireContext()).exportSeeds(uri, analysis, contours)

                }
            }
        }}.launch(fileName)

    }

    /**
     * Uses activity results contracts to create a document and call the export function
     */
    private fun exportFile(analysis: List<AnalysisEntity>) {

        val outputFilePrefix = getString(R.string.export_file_prefix)

        val fileName = "$outputFilePrefix${DateUtil().getTime()}"

        Dialogs.booleanOption(AlertDialog.Builder(requireContext()),
                getString(R.string.frag_analysis_dialog_export_title),
                getString(R.string.frag_analysis_dialog_export_samples_option),
                getString(R.string.frag_analysis_dialog_export_seeds_option),
                getString(R.string.frag_analysis_dialog_export_cancel_option)) { option ->

            if (option) {

                exportSamples(fileName, analysis)

            } else {

                viewModel.selectAllContours().observeOnce(viewLifecycleOwner, {
                    exportSeeds(fileName, analysis, it)

                })
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_analysis_view, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {

            R.id.action_delete -> {

                askDelete()
            }

            R.id.action_export -> {

               (mBinding?.recyclerView?.adapter as? AnalysisAdapter)
                    ?.currentList?.filter { it.selected }?.let { data ->

                        exportFile(data)

                    }
            }

            R.id.action_select_all -> {

                viewModel.updateSelectAllAnalysis(mSelectMode)

                mSelectMode = !mSelectMode

                mBinding?.updateUi()
            }

            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    private fun askDelete() {

        Dialogs.onOk(AlertDialog.Builder(requireContext()),
                title = getString(R.string.frag_analysis_ask_delete_all),
                cancel = getString(R.string.frag_analysis_ask_delete_cancel_button),
                ok = getString(R.string.frag_analysis_ask_delete_ok_button)) {

            if (it) {

                launch {

                    async {

                        viewModel.deleteSelectedAnalysis()

                    }.await()

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