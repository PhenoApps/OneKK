package org.wheatgenetics.onekk.activities

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroBase
import com.github.appintro.AppIntroFragment
import org.wheatgenetics.onekk.R

class IntroActivity : AppIntro() {
    @SuppressLint("MissingSuperCall") //AS bug?
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make sure you don't call setContentView!

        // Call addSlide passing your Fragments.
        // You can use AppIntroFragment to use a pre-built fragment
        addSlide(AppIntroFragment.newInstance(
            title = getString(R.string.intro_slide_1), //Welcome to OneKK
            description = getString(R.string.intro_slide_1_description)
        ))
        addSlide(AppIntroFragment.newInstance(
            title = getString(R.string.intro_slide_2), //OneKK works with a lightbox, four coins, and a Bluetooth Ohaus scale.
            description = getString(R.string.intro_slide_2_description)
        ))
        addSlide(AppIntroFragment.newInstance(
            title = getString(R.string.intro_slide_3),
            description = getString(R.string.intro_slide_3_description),
            imageDrawable = R.drawable.sample_original
        ))
        addSlide(AppIntroFragment.newInstance(
            title = getString(R.string.intro_slide_4),
            description = getString(R.string.intro_slide_4_description)
        ))
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        // Decide what to do when the user clicks on "Skip"
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        // Decide what to do when the user clicks on "Done"
        finish()
    }
}