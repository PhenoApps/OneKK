package org.wheatgenetics.onekk.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.activities.MainActivity
import org.wheatgenetics.onekk.database.viewmodels.BarcodeSharedViewModel
import org.wheatgenetics.onekk.databinding.FragmentBarcodeBinding

class BarcodeScanFragment: Fragment() {

    private val mSharedViewModel: BarcodeSharedViewModel by activityViewModels()

    private lateinit var mCallback: BarcodeCallback

    private var mBinding: FragmentBarcodeBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_barcode, container, false)

        mCallback = object : BarcodeCallback {

            override fun barcodeResult(result: BarcodeResult) {

                if (result.text == null) return // || result.text == lastText) return

                mSharedViewModel.lastScan.postValue(result.text.toString())

                findNavController().popBackStack()
            }

            override fun possibleResultPoints(resultPoints: List<ResultPoint>) {

            }
        }

        mBinding?.zxingBarcodeScanner?.barcodeView.apply {

            this?.cameraSettings?.isContinuousFocusEnabled = true

            this?.cameraSettings?.isAutoTorchEnabled = true

            this?.cameraSettings?.isAutoFocusEnabled = true

            this?.cameraSettings?.isBarcodeSceneModeEnabled = true

            this?.decodeSingle(mCallback)
        }

        //hide default toolbar
        (activity as? MainActivity)?.supportActionBar?.hide()

        return mBinding?.root

    }

    override fun onResume() {
        super.onResume()

        mBinding?.zxingBarcodeScanner?.resume()
    }

    override fun onPause() {
        super.onPause()

        mBinding?.zxingBarcodeScanner?.pause()
    }

}