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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
    private var mSortState: Boolean = true
    private var mBinding: FragmentContourListBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        aid = requireArguments().getInt("analysis", -1)

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_contour_list, null, false)

        mBinding?.let { ui ->

            setHasOptionsMenu(true)

            ui.setupRecyclerView(aid)

            ui.setupButtons()

            updateUi(aid)

            sViewModel.getSourceImage(aid).observeForever { uri ->

                mSourceBitmap = uri

                Glide.with(requireContext()).asBitmap().load(uri).fitCenter().into(imageView)
                //imageView?.setImageBitmap(BitmapFactory.decodeFile(mSourceBitmap))

                imageView?.visibility = View.VISIBLE

                imageLoadingTextView.visibility = View.GONE
            }

            submitButton?.text = getString(R.string.frag_contour_list_button_loading)

            sViewModel.contours(aid).observeForever { contours ->

                if (contours.isNotEmpty()) {

                    val count = contours.filter { it.selected }.mapNotNull { it.contour?.count }.reduceRight { x, y ->  y + x }

                    submitButton?.text = "${getString(R.string.frag_contour_list_total)} $count"

                }
            }

            return ui.root

        }

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

            sViewModel.getAnalysis(aid).observe(viewLifecycleOwner, {

                if (it.weight == null) {

                    findNavController().navigate(ContourFragmentDirections.actionToScale(aid))

                } else {

                    findNavController().navigate(ContourFragmentDirections.actionToCamera())

                }
            })
        }
    }

    private fun FragmentContourListBinding.setupRecyclerView(aid: Int) {

        recyclerView?.layoutManager = LinearLayoutManager(requireContext())

        recyclerView?.adapter = ContourAdapter(this@ContourFragment, requireContext())

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_contour_view, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {

            //toggle the torch when the option is clicked
            R.id.action_sort -> {

                item.setIcon(when (mSortState) {

                    //ascending order by area, switch to descending
                    true -> {

                        mSortState = false

                        R.drawable.ic_sort_variant

                    }

                    //descending to ascending
                    else -> {

                        mSortState = true

                        R.drawable.ic_sort_reverse_variant

                    }
                })

            }

            else -> return super.onOptionsItemSelected(item)
        }

        updateUi(aid)

        return true
    }

    private fun updateUi(aid: Int) {

        sViewModel.contours(aid).observeForever {

            val singles = it.filter { it.contour?.count ?: 0 <= 1 }

            val clusters = it.filter { it.contour?.count ?: 0 > 1 }

            val contours = when(mSortState) {
                true -> (singles + clusters).sortedBy { it.contour?.area }
                else -> (singles + clusters).sortedByDescending { it.contour?.area }
            }

            (mBinding?.recyclerView?.adapter as? ContourAdapter)
                    ?.submitList(contours)
        }

        Handler().postDelayed({
            mBinding?.recyclerView?.scrollToPosition(0)
        }, 500)
    }
}