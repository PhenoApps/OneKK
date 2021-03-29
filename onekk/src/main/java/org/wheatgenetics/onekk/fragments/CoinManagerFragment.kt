package org.wheatgenetics.onekk.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.*
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.activities.MainActivity
import org.wheatgenetics.onekk.adapters.CoinManagerAdapter
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.viewmodels.ExperimentViewModel
import org.wheatgenetics.onekk.database.viewmodels.factory.OnekkViewModelFactory
import org.wheatgenetics.onekk.databinding.FragmentCoinManagerBinding
import org.wheatgenetics.onekk.interfaces.CoinValueChangedListener
import org.wheatgenetics.onekk.observeOnce
import org.wheatgenetics.utils.Dialogs

class CoinManagerFragment : Fragment(), CoinValueChangedListener, CoroutineScope by MainScope() {

    private val db: OnekkDatabase by lazy {
        OnekkDatabase.getInstance(requireContext())
    }

    private val mPreferences by lazy {
        requireContext().getSharedPreferences(getString(R.string.onekk_preference_key), Context.MODE_PRIVATE)
    }

    private val viewModel by viewModels<ExperimentViewModel> {
        with(db) {
            OnekkViewModelFactory(OnekkRepository.getInstance(this.dao(), this.coinDao()))
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private var mBinding: FragmentCoinManagerBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_coin_manager, null, false)

        mBinding?.let { ui ->

            ui.setupRecyclerView(mPreferences.getString(getString(R.string.onekk_country_pref_key), "USA") ?: "USA")

            ui.setupEditTextSearch()

        }

        setHasOptionsMenu(true)

        viewModel.countries().observeOnce(viewLifecycleOwner, {

            it?.let { countryListResult ->

                activity?.runOnUiThread {

                    mBinding?.countrySearchEditText?.setAdapter(
                            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1,
                                    countryListResult))
                }
            }
        })

        //custom support action toolbar
        with(mBinding?.toolbar) {
            this?.findViewById<ImageButton>(R.id.backButton)?.setOnClickListener {
                findNavController().popBackStack()
            }
            this?.findViewById<ImageButton>(R.id.connectButton)?.setOnClickListener {
                resetCoinDatabase()
            }
        }

        return mBinding?.root
    }


    private fun resetCoinDatabase() {

        activity?.assets?.let { assets ->

            scope.launch {

                val adapter = assets.open("coin_database.csv").use {
                    ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1,
                        viewModel.diffCoinDatabase(it)
                            .await().toTypedArray())
                }

                activity?.runOnUiThread {

                    Dialogs.showCoinDiffDialog(adapter,
                        AlertDialog.Builder(requireContext()),
                        getString(R.string.frag_coin_manager_diff_title_dialog)) {

                        scope.launch {

                            assets.open("coin_database.csv").use {

                                viewModel.loadCoinDatabase(it).await()

                            }
                            activity?.runOnUiThread {

                                findNavController().navigate(SettingsFragmentDirections.globalActionToSettings())

                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Whenever the search text changes, check if the searched text is in the database
     * and update the recycler view with the coins available in this country.
     */
    private fun FragmentCoinManagerBinding.setupEditTextSearch() {

        countrySearchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(newText: Editable?) {
                newText?.let { nonNullText ->

                    viewModel.countries().observeOnce(viewLifecycleOwner, {

                        it?.let { countryListResult ->

                            val searchCountry = nonNullText.toString()

                            if (searchCountry in countryListResult) {

                                updateUi(searchCountry)
                            }
                        }
                    })
                }
            }
        })
    }

    private fun FragmentCoinManagerBinding.setupRecyclerView(country: String) {

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        recyclerView.adapter = CoinManagerAdapter(this@CoinManagerFragment, requireContext())

        updateUi(country)
    }

    private fun updateUi(country: String) {

        viewModel.coinModels(country).observeForever { coins ->

            (mBinding?.recyclerView?.adapter as? CoinManagerAdapter)?.submitList(coins)

        }
    }

    override fun onCoinValueChanged(country: String, name: String, value: Double) {

        scope.launch {

            viewModel.updateCoinValue(country, name, value)
        }
    }
}