package org.wheatgenetics.onekk.fragments

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.fragment.findNavController
import com.danielstone.materialaboutlibrary.ConvenienceBuilder
import com.danielstone.materialaboutlibrary.MaterialAboutFragment
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.items.MaterialAboutItemOnClickAction
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import com.michaelflisar.changelog.ChangelogBuilder
import com.michaelflisar.changelog.classes.ImportanceChangelogSorter
import com.mikepenz.aboutlibraries.LibsBuilder
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.activities.IntroActivity
import org.wheatgenetics.onekk.activities.MainActivity


class AboutFragment : MaterialAboutFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context?.setTheme(R.style.AppTheme)

    }

    public override fun getMaterialAboutList(c: Context): MaterialAboutList {

        val appCardBuilder = MaterialAboutCard.Builder()

        // Add items to card
        appCardBuilder.addItem(MaterialAboutTitleItem.Builder()
                .text(getString(R.string.app_name))
                .icon(R.mipmap.ic_launcher)
                .build())
        appCardBuilder.addItem(ConvenienceBuilder.createVersionActionItem(c,
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_about_info),
                getString(R.string.about_version_title),
                false))
        appCardBuilder.addItem(MaterialAboutActionItem.Builder()
                .text(getString(R.string.changelog_title))
                .icon(R.drawable.ic_about_changelog)
                .setOnClickAction { showChangelog(managedShow = false, rateButton = false) }
                .build())
        appCardBuilder.addItem(ConvenienceBuilder.createRateActionItem(c,
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_about_rate),
                getString(R.string.about_rate),
                null
        ))
        appCardBuilder.addItem(MaterialAboutActionItem.Builder()
                .text(R.string.about_intro)
                .icon(R.drawable.ic_about_info)
                .setOnClickAction {
                    startActivity(Intent(context, IntroActivity::class.java))
                }.build()).build()
        //TODO: Add new translation project
//        appCardBuilder.addItem(MaterialAboutActionItem.Builder()
//                .text(R.string.about_help_translate_title)
//                .icon(R.drawable.ic_about_help_translate)
//                //.setOnClickAction(ConvenienceBuilder.createWebsiteOnClickAction(c, Uri.parse("https://osij6hx.oneskyapp.com/collaboration/project?id=28259")))
//                .build())
        val authorCardBuilder = MaterialAboutCard.Builder()
        authorCardBuilder.title(getString(R.string.about_project_lead_title))
        authorCardBuilder.addItem(MaterialAboutActionItem.Builder()
                .text(getString(R.string.about_developer_trife))
                .subText(getString(R.string.about_developer_trife_location))
                .icon(R.drawable.ic_nav_drawer_person)
                .build())
        authorCardBuilder.addItem(ConvenienceBuilder.createEmailItem(c,
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_about_email),
                getString(R.string.about_email_title),
                true,
                getString(R.string.about_developer_trife_email),
                "Onekk Question"))
        authorCardBuilder.addItem(ConvenienceBuilder.createWebsiteActionItem(c,
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_about_website),
                "PhenoApps.org",
                false,
                Uri.parse("http://phenoapps.org/")))
        val contributorsCardBuilder = MaterialAboutCard.Builder()
        contributorsCardBuilder.title(getString(R.string.about_contributors_title))

        //split contribution funding string into two different variables, will be easier to automatically add new app names this way.
        //can be used like: "$firstContrib ${getString(R.string.app_name)} $afterContrib"
        val firstContrib = getString(R.string.about_contributors_funding_text_first)
        val afterContrib = getString(R.string.about_contributors_funding_text_after)

        contributorsCardBuilder.addItem(ConvenienceBuilder.createWebsiteActionItem(c,
                ResourcesCompat.getDrawable(resources, R.drawable.ic_about_contributors, activity?.theme),
                getString(R.string.about_contributors_developers_title),
                false,
                Uri.parse("https://github.com/PhenoApps/Onekk/graphs/contributors")))
//        contributorsCardBuilder.addItem(MaterialAboutActionItem.Builder()
//                .text(getString(R.string.about_translators_title))
//                .subText(getString(R.string.about_translators_text))
//                .icon(R.drawable.ic_about_translators)
//                .build())
        contributorsCardBuilder.addItem(MaterialAboutActionItem.Builder()
                .text(getString(R.string.about_contributors_funding_title))
                .subText("$firstContrib ${getString(R.string.app_name)} $afterContrib")
                .icon(R.drawable.ic_about_funding)
                .build())
        val technicalCardBuilder = MaterialAboutCard.Builder()
        technicalCardBuilder.title(getString(R.string.about_technical_title))
        technicalCardBuilder.addItem(MaterialAboutActionItem.Builder()
                .text(R.string.about_github_title)
                .icon(R.drawable.ic_about_github)
                .setOnClickAction(ConvenienceBuilder.createWebsiteOnClickAction(c, Uri.parse("https://github.com/PhenoApps/Onekk")))
                .build())
        technicalCardBuilder.addItem(MaterialAboutActionItem.Builder()
                .text(R.string.libraries_title)
                .icon(R.drawable.ic_about_libraries)
                .setOnClickAction {
                    LibsBuilder()
                        .withAutoDetect(true)
                            .withCheckCachedDetection(false)
                        .withActivityTitle(getString(R.string.libraries_title))
                        .withLicenseShown(true)
                        .withVersionShown(true)
                        .start(requireContext())
                }
                .build())
        val otherAppsCardBuilder = MaterialAboutCard.Builder()
        otherAppsCardBuilder.title(getString(R.string.about_title_other_apps))
        otherAppsCardBuilder.addItem(MaterialAboutActionItem.Builder()
                .text("Field-Book")
                .icon(R.drawable.other_ic_field_book)
                .setOnClickAction(openAppOrStore("com.fieldbook.tracker", c))
                .build())
        otherAppsCardBuilder.addItem(MaterialAboutActionItem.Builder()
                .text("Coordinate")
                .icon(R.drawable.other_ic_coordinate)
                .setOnClickAction(openAppOrStore("org.wheatgenetics.coordinate", c))
                .build())
        otherAppsCardBuilder.addItem(MaterialAboutActionItem.Builder()
                .text("Inventory")
                .icon(R.drawable.other_ic_inventory)
                .setOnClickAction(openAppOrStore("org.wheatgenetics.inventory", c))
                .build())
        otherAppsCardBuilder.addItem(MaterialAboutActionItem.Builder()
                .text("Verify")
                .icon(R.drawable.other_ic_verify)
                .setOnClickAction(openAppOrStore("org.phenoapps.verify", c))
                .build())

        return MaterialAboutList(appCardBuilder.build(), authorCardBuilder.build(), contributorsCardBuilder.build(), otherAppsCardBuilder.build(), technicalCardBuilder.build())
    }

    private fun showChangelog(managedShow: Boolean, rateButton: Boolean) {
        ChangelogBuilder()
            .withUseBulletList(true) // true if you want to show bullets before each changelog row, false otherwise
            .withManagedShowOnStart(managedShow) // library will take care to show activity/dialog only if the changelog has new infos and will only show this new infos
            .withRateButton(rateButton) // enable this to show a "rate app" button in the dialog => clicking it will open the play store; the parent activity or target fragment can also implement IChangelogRateHandler to handle the button click
            .withSummary(false, true) // enable this to show a summary and a "show more" button, the second paramter describes if releases without summary items should be shown expanded or not
            .withTitle(getString(R.string.changelog_title)) // provide a custom title if desired, default one is "Changelog <VERSION>"
            .withOkButtonLabel(requireContext().getString(R.string.zxing_button_ok)) // provide a custom ok button text if desired, default one is "OK"
            .withSorter(ImportanceChangelogSorter())
                .buildAndShowDialog(activity as AppCompatActivity?, false)
    }

//    override fun getActivityTitle(): CharSequence? {
//        return getString(R.string.mal_title_about)
//    }

    private fun openAppOrStore(packageName: String, c: Context): MaterialAboutItemOnClickAction {
        val packageManager = requireContext().packageManager
        return try {
            packageManager.getPackageInfo(packageName, 0)
            MaterialAboutItemOnClickAction {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                launchIntent?.let { intent ->
                    startActivity(intent)
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            ConvenienceBuilder.createWebsiteOnClickAction(c, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
        }
    }
}