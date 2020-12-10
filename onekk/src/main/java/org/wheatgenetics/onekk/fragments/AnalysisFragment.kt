package org.wheatgenetics.onekk.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.adapters.AnalysisAdapter
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.viewmodels.ExperimentViewModel
import org.wheatgenetics.onekk.database.viewmodels.factory.OnekkViewModelFactory
import org.wheatgenetics.onekk.databinding.FragmentAnalysisBinding

class AnalysisFragment : Fragment(), CoroutineScope by MainScope() {

    private val viewModel by viewModels<ExperimentViewModel> {
        with(OnekkDatabase.getInstance(requireContext())) {
            OnekkViewModelFactory(OnekkRepository.getInstance(this.dao(), this.coinDao()))
        }
    }

    companion object {

        final val TAG = "Onekk.AnalysisFragment"

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private var mBinding: FragmentAnalysisBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_analysis, container, false)

        with(mBinding) {

            val adapter = AnalysisAdapter()
            this?.recyclerView?.adapter = adapter
            this?.recyclerView?.layoutManager = LinearLayoutManager(requireContext())

//            viewModel.analysis(ExperimentEntity(Experiment("Test"), 1))
//                    .observe(viewLifecycleOwner, {
//
//                        adapter.submitList(it)
//                    })
        }

        return mBinding?.root
    }

}