package com.ericversteeg.liquidocean.fragment

import android.content.Intent
import android.util.Log
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.SignInListener
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.model.StatTracker
import com.ericversteeg.liquidocean.view.ActionButtonView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.fragment_options.*
import kotlinx.android.synthetic.main.fragment_signin.*
import kotlinx.android.synthetic.main.fragment_signin.back_action
import kotlinx.android.synthetic.main.fragment_signin.back_button
import org.json.JSONObject


class SignInFragment: Fragment() {

    companion object {
        var modeSignIn = 0
        var modeSetPincode = 1
        var modeChangePincode = 2
    }

    var mode = 0

    // private lateinit var signInClient: GoogleSignInClient

    var signInListener: SignInListener? = null
    // var googleAccount: GoogleSignInAccount? = null

    // val signInRequestCode = 1000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_signin, container, false)

        // setup views here

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setModeViews()

        if (mode == modeSignIn) {
            status_text.visibility = View.INVISIBLE
        }
        else if (mode == modeSetPincode) {
            status_text.text = "Here you can set an access pincode to gain access to your account from your other devices or upon app reinstallation."
        }
        else if (mode == modeChangePincode) {
            status_text.text = "Here you can set an access pincode to gain access to your account from your other devices or upon app reinstallation."

            pincode_title.text = "New 8-digit pincode"
            pincode_2_title.text = "Repeat new 8-digit pincode"

            set_pincode_button.text = "Change Pincode"
        }

        back_button.actionBtnView = back_action
        back_action.type = ActionButtonView.Type.BACK

        back_button.setOnClickListener {
            signInListener?.onSignInBack()
        }

        set_pincode_button.setOnClickListener {
            if (mode == modeSetPincode) {
                setPincode()
            }
            else if (mode == modeChangePincode) {
                changePincode()
            }
        }

        sign_in_button_2.setOnClickListener {
            signIn()
        }

        old_pincode_input.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                set_pincode_button.isEnabled = true
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

        pincode_input.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                set_pincode_button.isEnabled = true
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

        pincode_2_input.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                set_pincode_button.isEnabled = true
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

        name_input.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                sign_in_button_2.isEnabled = true
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

        pincode_input_sign_in.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                sign_in_button_2.isEnabled = true
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

        /*google_sign_in_button.setOnClickListener {
            activity?.startActivityForResult(signInClient.signInIntent, signInRequestCode)
        }*/
    }

    private fun setModeViews() {
        if (mode == modeSignIn) {
            toggleModeViews(modeSetPincode, false)
            toggleModeViews(modeChangePincode, false)
            toggleModeViews(modeSignIn, true)
        }
        else if (mode == modeSetPincode) {
            toggleModeViews(modeSetPincode, true)
            toggleModeViews(modeChangePincode, false)
        }
        else if (mode == modeChangePincode) {
            toggleModeViews(modeChangePincode, true)
            toggleModeViews(modeSignIn, false)
        }
    }

    private fun toggleModeViews(mode: Int, show: Boolean) {
        if (mode == modeSignIn) {
            if (show) {
                name_title.visibility = View.VISIBLE
            }
            else {
                name_title.visibility = View.GONE
            }

            if (show) {
                name_input.visibility = View.VISIBLE
            }
            else {
                name_input.visibility = View.GONE
            }

            if (show) {
                pincode_title_sign_in.visibility = View.VISIBLE
            }
            else {
                pincode_title_sign_in.visibility = View.GONE
            }

            if (show) {
                pincode_input_sign_in.visibility = View.VISIBLE
            }
            else {
                pincode_input_sign_in.visibility = View.GONE
            }

            if (show) {
                sign_in_button_2.visibility = View.VISIBLE
            }
            else {
                sign_in_button_2.visibility = View.GONE
            }
        }
        else if (mode == modeSetPincode) {
            if (show) {
                pincode_title.visibility = View.VISIBLE
            }
            else {
                pincode_title.visibility = View.GONE
            }

            if (show) {
                pincode_input.visibility = View.VISIBLE
            }
            else {
                pincode_input.visibility = View.GONE
            }

            if (show) {
                pincode_2_title.visibility = View.VISIBLE
            }
            else {
                pincode_2_title.visibility = View.GONE
            }

            if (show) {
                pincode_2_input.visibility = View.VISIBLE
            }
            else {
                pincode_2_input.visibility = View.GONE
            }

            if (show) {
                set_pincode_button.visibility = View.VISIBLE
            }
            else {
                set_pincode_button.visibility = View.GONE
            }
        }
        else if (mode == modeChangePincode) {
            toggleModeViews(modeSignIn, show)

            if (show) {
                old_pincode_title.visibility = View.VISIBLE
            }
            else {
                old_pincode_title.visibility = View.GONE
            }

            if (show) {
                old_pincode_input.visibility = View.VISIBLE
            }
            else {
                old_pincode_input.visibility = View.GONE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.client_id))
            .requestEmail()
            .build()

        activity?.apply {
            signInClient = GoogleSignIn.getClient(this, gso)
        }*/
    }

    override fun onStart() {
        super.onStart()

        activity?.apply {
            /*googleAccount = GoogleSignIn.getLastSignedInAccount(this)

            if (googleAccount != null || SessionSettings.instance.googleAuth) {
                status_text.text = "Account synced with Google"
                google_sign_in_button.isEnabled = false
                sendGoogleToken(googleAccount)
            }
            else {
                status_text.text = "Sync account with Google"
            }*/
        }
    }

    /*fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            googleAccount = completedTask.getResult(ApiException::class.java)
            sendGoogleToken(googleAccount)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.i("Sign In", "signInResult:failed code=" + e.statusCode)
        }
    }*/

    private fun setPincode() {
        set_pincode_button.isEnabled = false

        context?.apply {
            val requestQueue = Volley.newRequestQueue(this)

            val requestParams = HashMap<String, String>()

            if (SessionSettings.instance.displayName == "") {
                status_text.text = "You must first set a display name in Options"
                return
            }

            if (pincode_input.text.toString().isEmpty()) {
                set_pincode_button.isEnabled = true
                return
            }

            if (pincode_input.text.toString() != pincode_2_input.text.toString()) {
                status_text.text = "Pincodes don't match"
                return
            }

            if (pincode_input.text.toString().length != 8) {
                status_text.text = "Pincode length is incorrect"
                return
            }

            requestParams["pincode"] = pincode_input.text.toString()

            val paramsJson = JSONObject(requestParams as Map<String, String>)

            val request = JsonObjectRequest(
                Request.Method.POST,
                Utils.baseUrlApi + "/api/v1/devices/${SessionSettings.instance.uniqueId}",
                paramsJson,
                { response ->
                    SessionSettings.instance.pincodeSet = true

                    // update UI
                    status_text.text = "Pincode set. Go to Options -> Sign-in to access your account from any device."
                },
                { error ->
                    Toast.makeText(context, "Network error, please try again.", Toast.LENGTH_SHORT).show()
                    set_pincode_button.isEnabled = true
                })

            requestQueue.add(request)
        }
    }

    private fun changePincode() {
        set_pincode_button.isEnabled = false

        context?.apply {
            val requestQueue = Volley.newRequestQueue(this)

            val requestParams = HashMap<String, String>()

            if (SessionSettings.instance.displayName == "") {
                status_text.text = "You must first set a display name in Options"
                return
            }

            if (old_pincode_input.text.toString().isEmpty()) {
                set_pincode_button.isEnabled = true
                return
            }

            if (pincode_input.text.toString().length != 8) {
                status_text.text = "Old pincode length is incorrect"
                return
            }

            if (pincode_input.text.toString().isEmpty()) {
                set_pincode_button.isEnabled = true
                return
            }

            if (pincode_input.text.toString() != pincode_2_input.text.toString()) {
                status_text.text = "Pincodes don't match"
                return
            }

            if (pincode_input.text.toString().length != 8) {
                status_text.text = "Pincode length is incorrect"
                return
            }

            requestParams["old_pincode"] = old_pincode_input.text.toString()
            requestParams["pincode"] = pincode_input.text.toString()

            val paramsJson = JSONObject(requestParams as Map<String, String>)

            val request = JsonObjectRequest(
                Request.Method.POST,
                Utils.baseUrlApi + "/api/v1/devices/${SessionSettings.instance.uniqueId}/pincode",
                paramsJson,
                { response ->
                    SessionSettings.instance.pincodeSet = true

                    // update UI
                    status_text.text = "Pincode changed. Go to Options -> Sign-in to access your account from any device."
                },
                { error ->
                    Toast.makeText(context, "Network error, please try again.", Toast.LENGTH_SHORT).show()
                    set_pincode_button.isEnabled = true
                })

            requestQueue.add(request)
        }
    }

    private fun signIn() {
        sign_in_button_2.isEnabled = false

        context?.apply {
            val requestQueue = Volley.newRequestQueue(this)

            val requestParams = HashMap<String, String>()

            var name = name_input.text.toString()
            var pincode = pincode_input_sign_in.text.toString()

            if (name.length > 20) {
                status_text.text = "Name length is incorrect"
                status_text.visibility = View.VISIBLE
            }
            else if (name.isEmpty()) {
                status_text.text = "Please enter a display name"
                status_text.visibility = View.VISIBLE
            }

            if (pincode.isEmpty()) {
                status_text.text = "Please enter a pincode"
                status_text.visibility = View.VISIBLE
            }

            if (pincode.length != 8) {
                status_text.text = "Pincode length is incorrect"
                status_text.visibility = View.VISIBLE
            }

            requestParams["name"] = name
            requestParams["pincode"] = pincode

            val paramsJson = JSONObject(requestParams as Map<String, String>)

            val request = JsonObjectRequest(
                Request.Method.POST,
                Utils.baseUrlApi + "/api/v1/devices/pincode/auth",
                paramsJson,
                { response ->
                    if (response.has("error")) {
                        status_text.text = "Display name or password is incorrect"
                    }
                    else {
                        SessionSettings.instance.pincodeSet = true

                        SessionSettings.instance.uniqueId = response.getString("uuid")
                        SessionSettings.instance.dropsAmt = response.getInt("paint_qty")
                        SessionSettings.instance.xp = response.getInt("xp")

                        SessionSettings.instance.displayName = response.getString("name")

                        SessionSettings.instance.sentUniqueId = true

                        StatTracker.instance.numPixelsPaintedWorld = response.getInt("wt")
                        StatTracker.instance.numPixelsPaintedSingle = response.getInt("st")
                        StatTracker.instance.totalPaintAccrued = response.getInt("tp")
                        StatTracker.instance.numPixelOverwritesIn = response.getInt("oi")
                        StatTracker.instance.numPixelOverwritesOut = response.getInt("oo")

                        context?.apply {
                            StatTracker.instance.save(this)
                        }

                        // update UI
                        status_text.text = "Successfully signed in."
                    }
                    status_text.visibility = View.VISIBLE
                },
                { error ->
                    Toast.makeText(context, "Network error, please try again.", Toast.LENGTH_SHORT).show()
                    sign_in_button_2.isEnabled = true
                })

            requestQueue.add(request)
        }
    }

    /*private fun sendGoogleToken(googleAccount: GoogleSignInAccount?) {
        googleAccount?.apply {
            context?.apply {
                val requestQueue = Volley.newRequestQueue(this)

                val requestParams = HashMap<String, String>()

                googleAccount.idToken?.apply {
                    if (SessionSettings.instance.uniqueId == null) {
                        SessionSettings.instance.uniqueId == "####"
                    }

                    requestParams["token_id"] = this.substring(0, 16)

                    val paramsJson = JSONObject(requestParams as Map<String, String>)

                    val request = JsonObjectRequest(
                        Request.Method.POST,
                        Utils.baseUrlApi + "/api/v1/devices/${SessionSettings.instance.uniqueId}/google/auth",
                        paramsJson,
                        { response ->
                            SessionSettings.instance.uniqueId = response.getString("uuid")
                            SessionSettings.instance.dropsAmt = response.getInt("paint_qty")
                            SessionSettings.instance.xp = response.getInt("xp")

                            SessionSettings.instance.displayName = response.getString("name")

                            SessionSettings.instance.sentUniqueId = true

                            StatTracker.instance.numPixelsPaintedWorld = response.getInt("wt")
                            StatTracker.instance.numPixelsPaintedSingle = response.getInt("st")
                            StatTracker.instance.totalPaintAccrued = response.getInt("tp")
                            StatTracker.instance.numPixelOverwritesIn = response.getInt("oi")
                            StatTracker.instance.numPixelOverwritesOut = response.getInt("oo")

                            context?.apply {
                                StatTracker.instance.save(this)
                            }

                            SessionSettings.instance.googleAuth = true

                            // update UI
                            google_sign_in_button.isEnabled = false
                            status_text.text = "Signed in"
                        },
                        { error ->
                            Toast.makeText(context, "Network error, please try again.", Toast.LENGTH_SHORT).show()
                        })

                    requestQueue.add(request)
                }

            }
        }
    }*/
}