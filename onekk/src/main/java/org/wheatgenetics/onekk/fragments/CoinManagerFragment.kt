package org.wheatgenetics.onekk.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
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
import kotlinx.android.synthetic.main.fragment_coin_manager.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.adapters.CoinManagerAdapter
import org.wheatgenetics.onekk.adapters.ExperimentAdapter
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.models.ExperimentEntity
import org.wheatgenetics.onekk.database.models.embedded.Experiment
import org.wheatgenetics.onekk.database.viewmodels.ExperimentViewModel
import org.wheatgenetics.onekk.database.viewmodels.factory.OnekkViewModelFactory
import org.wheatgenetics.onekk.databinding.FragmentCoinManagerBinding
import org.wheatgenetics.onekk.databinding.FragmentExperimentListBinding
import org.wheatgenetics.onekk.interfaces.CoinValueChangedListener
import org.wheatgenetics.utils.Dialogs

class CoinManagerFragment : Fragment(), CoinValueChangedListener, CoroutineScope by MainScope() {

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

    private var mBinding: FragmentCoinManagerBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_coin_manager, null, false)

        mBinding?.let { ui ->

            ui.setupRecyclerView(mPreferences.getString(getString(R.string.onekk_country_pref_key), "USA") ?: "USA")

            ui.setupEditTextSearch()

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

                    sViewModel.countries().observeForever {

                        it?.let { countryListResult ->

                            val searchCountry = nonNullText.toString()

                            if (searchCountry in countryListResult) {

                                updateUi(searchCountry)
                            }
                        }
                    }
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

        sViewModel.coinModels(country).observeForever { coins ->

            (recyclerView.adapter as CoinManagerAdapter).submitList(coins)

        }
    }

    override fun onCoinValueChanged(country: String, name: String, value: Double) {

        launch {

            sViewModel.updateCoinValue(country, name, value)
        }
    }
}