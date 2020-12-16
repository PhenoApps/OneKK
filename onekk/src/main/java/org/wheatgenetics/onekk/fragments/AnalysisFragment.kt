package org.wheatgenetics.onekk.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
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
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.polidea.rxandroidble2.helpers.ValueInterpreter
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
import org.wheatgenetics.onekk.interfaces.BleNotificationListener
import org.wheatgenetics.onekk.interfaces.OnClickAnalysis
import org.wheatgenetics.utils.BluetoothUtil
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

        final val TAG = "Onekk.AnalysisFragment"

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private var mBinding: FragmentAnalysisManagerBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_analysis_manager, container, false)

        with(mBinding) {

            val adapter = AnalysisAdapter(this@AnalysisFragment)
            this?.recyclerView?.adapter = adapter
            this?.recyclerView?.layoutManager = LinearLayoutManager(requireContext())

            viewModel.analysis().observe(viewLifecycleOwner, {

                if (it.isEmpty()) {

                    Toast.makeText(requireContext(), R.string.frag_analysis_table_empty_message, Toast.LENGTH_LONG).show()

                    findNavController().popBackStack()
                }
                
                adapter.submitList(it)

            })

            this?.exportButton?.setOnClickListener {

                exportFile(adapter.currentList.filter { it.selected })
            }

            this?.deleteButton?.setOnClickListener {

                Dialogs.onOk(AlertDialog.Builder(requireContext()),
                        title = getString(R.string.frag_analysis_ask_delete_all),
                        cancel = getString(R.string.frag_analysis_ask_delete_cancel_button),
                        ok = getString(R.string.frag_analysis_ask_delete_ok_button)) {

                    if (it) {

                        viewModel.deleteAllAnalysis()

                    }

                }
            }
        }

        setHasOptionsMenu(true)

        return mBinding?.root
    }

    /**
     * Uses activity results contracts to create a document and call the export function
     */
    private fun exportFile(analysis: List<AnalysisEntity>) {

        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { it?.let { uri ->

            launch {

                withContext(Dispatchers.IO) {

                    val chosenUri = Uri.parse(uri.toString())

                    val folder = DocumentFile.fromTreeUri(requireContext(), chosenUri)

                    folder?.let {

                        val docFile = it.createDirectory("Output_${DateUtil().getTime()}")

                        docFile?.let { directory ->

                            directory.createFile("text/csv", "${UUID.randomUUID()}.csv")?.let { output ->

                                FileUtil(requireContext()).export(output.uri, analysis)

                            }
                        }
                    }
                }
            }
        }}.launch(requireContext().filesDir.toUri())
    }

    override fun onClickCount(aid: Int) {

        findNavController().navigate(AnalysisFragmentDirections.actionToContours(aid))

    }

    override fun onSelectionSwapped(aid: Int, selected: Boolean) {

        viewModel.updateAnalysisSelected(aid, selected)

    }

    override fun onClick(aid: Int) {

        findNavController().navigate(AnalysisFragmentDirections.actionToScale(aid))

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_analysis_view, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {

            else -> return super.onOptionsItemSelected(item)
        }

//        return true
    }

    override fun onAnalysisUpdated(aid: Int, weight: Double?) {

        viewModel.updateAnalysisWeight(aid, weight)

    }

}