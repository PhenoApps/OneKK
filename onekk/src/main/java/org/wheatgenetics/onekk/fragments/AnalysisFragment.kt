package org.wheatgenetics.onekk.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.polidea.rxandroidble2.helpers.ValueInterpreter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.adapters.AnalysisAdapter
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.viewmodels.ExperimentViewModel
import org.wheatgenetics.onekk.database.viewmodels.factory.OnekkViewModelFactory
import org.wheatgenetics.onekk.databinding.FragmentAnalysisManagerBinding
import org.wheatgenetics.onekk.interfaces.AnalysisUpdateListener
import org.wheatgenetics.onekk.interfaces.BleNotificationListener
import org.wheatgenetics.onekk.interfaces.OnClickAnalysis
import org.wheatgenetics.utils.BluetoothUtil
import org.wheatgenetics.utils.Dialogs

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

    private var mLastReadWeight: Double = 0.0

    private var mBinding: FragmentAnalysisManagerBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_analysis_manager, container, false)

        with(mBinding) {

            val adapter = AnalysisAdapter(this@AnalysisFragment)
            this?.recyclerView?.adapter = adapter
            this?.recyclerView?.layoutManager = LinearLayoutManager(requireContext())

            viewModel.analysis().observe(viewLifecycleOwner, {

                adapter.submitList(it)

            })
        }

        setHasOptionsMenu(true)

        return mBinding?.root
    }

    override fun onClickCount(aid: Int) {

        findNavController().navigate(AnalysisFragmentDirections.actionToContours(aid))

    }

    override fun onClick(aid: Int) {

        findNavController().navigate(AnalysisFragmentDirections.actionToScale(aid))

//        viewModel.getSourceImage(aid).observe(viewLifecycleOwner, {
//
//            showAnalysisImage(aid, BitmapFactory.decodeFile(it))
//
//        })
    }

    private fun showAnalysisImage(aid: Int, bmp: Bitmap) = with(requireActivity()) {

        runOnUiThread {

            Dialogs.updateAnalysisDialog(aid, mLastReadWeight.toString(),
                    this,
                    AlertDialog.Builder(this),
                    "",
                    srcBitmap = bmp, dstBitmap = bmp) { aid, weight ->

                viewModel.updateAnalysisWeight(aid, weight)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_analysis_view, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {

            R.id.action_delete_all -> {

                Dialogs.onOk(AlertDialog.Builder(requireContext()),
                        title = getString(R.string.frag_analysis_ask_delete_all),
                        cancel = getString(R.string.frag_analysis_ask_delete_cancel_button),
                        ok = getString(R.string.frag_analysis_ask_delete_ok_button)) {

                            if (it) {

                                viewModel.deleteAllAnalysis()

                            }

                        }

            }

            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onAnalysisUpdated(aid: Int, weight: Double?) {

        viewModel.updateAnalysisWeight(aid, weight)

    }

}