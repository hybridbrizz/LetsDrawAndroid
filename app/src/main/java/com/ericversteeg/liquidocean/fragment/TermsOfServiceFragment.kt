package com.ericversteeg.liquidocean.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
import androidx.fragment.app.Fragment
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.activity.InteractiveCanvasActivity
import com.ericversteeg.liquidocean.model.Server
import com.ericversteeg.liquidocean.model.SessionSettings

class TermsOfServiceFragment: Fragment() {

    lateinit var server: Server

    private val termsOfService = "Terms of Service\n\n" +
            "This software, “service” or “app” is provided by Matrix Warez Limited (\"developer\", \"publisher\") as is, whereby any bug or issue with the app is not the responsibility of the developers or publishers of the service to withhold or upkeep. You must be at least 13 years of age to use this service.\n\n" +
            "(1) User Generated Content\n" +
            "Any pixel(s) written onto any canvas are the sole responsibility of the person who places the pixels. There is a chance on occasion that a person or number of persons may place pixels onto a canvas with this app that may be seen or flagged as copyrighted content. If you have a copyright complaint please send an email to matrixwarez@gmail.com citing what content you would like removed (erased) from a canvas and which canvas you are referring to plus full proof of ownership and delegation over the content. Please allow up to (4) weeks from the date the notice is received for the content to be fully removed.\n\n" +
            "(2) Speech and Symbols\n" +
            "On occasion a person may be harassed or assaulted by another person or any number of persons through the medium of pixels on a canvas. This may involve written or depicted hate speech (words) or symbols that a person may find offensive to them in one or many ways. Please understand that by using the service you agree to a certain risk of yourself becoming the victim of such hateful actions and that if reported or seen, the culprits will be banned to the full extent that it is possible. The app developers or publishers are in no way responsible for any hate or harm that comes to another person while using the service. Any emotional or mental damage imposed upon any person by any number of persons using the service is their own responsibility. You may pursue these user(s) directly, however no name or other personal information with the exception of IP addresses are stored, so it will be challenging to figure out exactly who it was. The app developers and publishers are in no way condoning hateful acts by not keeping record of most personal data. If you are being harassed please leave the canvas immediately and report the case to matrixwarez@gmail.com.\n\n" +
            "(3) Copied Content\n" +
            "Within the app any person may choose to select, export, and share any number of pixels that together make up an image that may or may not contain sensitive, offensive, or copyrighted content, or content depicting hateful acts or speech. Please note that once content (pixels) are exported from the app, as soon as those pixels in the form of an image leave the app via a file stored on a person's device or shared via text or another app or service, that it is no longer the responsibility of the developers or publishers of this app and that while we give our best effort to tend to copyright notices and other claims, sometimes we will not get to it in time before it is copied over and leaves the app.\n\n" +
            "(4) Personal Information\n" +
            "If any person on any canvas uses pixels to give out personal information about themselves, they will be banned. Alternatively do not solicit any person to give away any personal information about themselves. If in a rare case such interactions lead to anything happening as a result in real life (outside the app), it is not the responsibility of the developers or publishers of the service."

    private var scrolledToBottom = false
        set(value) {
            if (value) {
                val agreeText = requireView().findViewById<TextView>(R.id.text_agree)
                agreeText.setTextColor(Color.BLACK)

                val agreeCheckbox = requireView().findViewById<CheckBox>(R.id.checkbox_agree)
                agreeCheckbox.isEnabled = true
                agreeCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        SessionSettings.instance.agreedToTermOfService = true
                        SessionSettings.instance.saveAgreeToTerms(requireContext())
                        (requireActivity() as InteractiveCanvasActivity).showInteractiveCanvasFragment(server)
                    }
                }
            }

            field = value
        }

    private var viewDestroyed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_terms_of_service, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val termsText = view.findViewById<TextView>(R.id.text_terms_of_service)
        termsText.text = termsOfService

        val scrollView = view.findViewById<ScrollView>(R.id.scroll_view_terms_of_service)
        scrollView.viewTreeObserver.addOnScrollChangedListener(object: ViewTreeObserver.OnScrollChangedListener {
            override fun onScrollChanged() {
                if (viewDestroyed) return

                if (scrollView.getChildAt(0).bottom <= scrollView.height + scrollView.scrollY) {
                    scrolledToBottom = true
                    scrollView.viewTreeObserver.removeOnScrollChangedListener(this)
                }
            }
        })

        scrollView.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (viewDestroyed) return

                if (scrollView.getChildAt(0).bottom <= scrollView.height + scrollView.scrollY) {
                    scrolledToBottom = true
                    scrollView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewDestroyed = true
    }
}