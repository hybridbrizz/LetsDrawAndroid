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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.databinding.FragmentSigninBinding
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.SignInListener
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.model.StatTracker
import com.ericversteeg.liquidocean.view.ActionButtonView
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
    
    private var _binding: FragmentSigninBinding? = null
    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSigninBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setModeViews()

        binding.statusText.setTextColor(ActionButtonView.altGreenPaint.color)

        if (mode == modeSignIn) {
            binding.statusText.visibility = View.INVISIBLE

            binding.signInTitle.type = ActionButtonView.Type.SIGNIN

            val layoutParams = binding.signInTitle.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.width = Utils.dpToPx(context, 165)

            binding.signInTitle.type = ActionButtonView.Type.SIGNIN
        }
        else if (mode == modeSetPincode) {
            binding.statusText.text = "Here you can set an access pincode to gain access to your account from your other devices or upon app reinstallation."

            binding.signInTitle.type = ActionButtonView.Type.PINCODE
        }
        else if (mode == modeChangePincode) {
            binding.statusText.text = "Here you can set an access pincode to gain access to your account from your other devices or upon app reinstallation."

            binding.pincodeTitle.text = "New 8-digit pincode"
            binding.pincode2Title.text = "Repeat new 8-digit pincode"

            binding.setPincodeButton.text = "Change Pincode"

            val layoutParams = binding.pincodeTitle.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.topMargin = Utils.dpToPx(context, 40)
            binding.pincodeTitle.layoutParams = layoutParams

            binding.signInTitle.type = ActionButtonView.Type.PINCODE
        }

        binding.signInTitle.isStatic = true

        binding.backButton.actionBtnView = binding.backAction
        binding.backAction.type = ActionButtonView.Type.BACK_SOLID

        binding.backButton.setOnClickListener {
            signInListener?.onSignInBack()
        }

        binding.setPincodeButton.setOnClickListener {
            if (mode == modeSetPincode) {
                setPincode()
            }
            else if (mode == modeChangePincode) {
                changePincode()
            }
        }

        binding.signInButton2.setOnClickListener {
            signIn()
        }

        binding.oldPincodeInput.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                binding.setPincodeButton.isEnabled = true
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

        binding.pincodeInput.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                binding.setPincodeButton.isEnabled = true
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

        binding.pincode2Input.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                binding.setPincodeButton.isEnabled = true
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

        binding.nameInput.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                binding.signInButton2.isEnabled = true
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

        binding.pincodeInputSignIn.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                binding.signInButton2.isEnabled = true
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
                binding.nameTitle.visibility = View.VISIBLE
            }
            else {
                binding.nameTitle.visibility = View.GONE
            }

            if (show) {
                binding.nameInput.visibility = View.VISIBLE
            }
            else {
                binding.nameInput.visibility = View.GONE
            }

            if (show) {
                binding.pincodeTitleSignIn.visibility = View.VISIBLE
            }
            else {
                binding.pincodeTitleSignIn.visibility = View.GONE
            }

            if (show) {
                binding.pincodeInputSignIn.visibility = View.VISIBLE
            }
            else {
                binding.pincodeInputSignIn.visibility = View.GONE
            }

            if (show) {
                binding.signInButton2.visibility = View.VISIBLE
            }
            else {
                binding.signInButton2.visibility = View.GONE
            }
        }
        else if (mode == modeSetPincode) {
            if (show) {
                binding.pincodeTitle.visibility = View.VISIBLE
            }
            else {
                binding.pincodeTitle.visibility = View.GONE
            }

            if (show) {
                binding.pincodeInput.visibility = View.VISIBLE
            }
            else {
                binding.pincodeInput.visibility = View.GONE
            }

            if (show) {
                binding.pincode2Title.visibility = View.VISIBLE
            }
            else {
                binding.pincode2Title.visibility = View.GONE
            }

            if (show) {
                binding.pincode2Input.visibility = View.VISIBLE
            }
            else {
                binding.pincode2Input.visibility = View.GONE
            }

            if (show) {
                binding.setPincodeButton.visibility = View.VISIBLE
            }
            else {
                binding.setPincodeButton.visibility = View.GONE
            }
        }
        else if (mode == modeChangePincode) {
            toggleModeViews(modeSignIn, show)

            if (show) {
                binding.pincodeTitle.visibility = View.VISIBLE
            }
            else {
                binding.pincodeTitle.visibility = View.GONE
            }

            if (show) {
                binding.oldPincodeInput.visibility = View.VISIBLE
            }
            else {
                binding.oldPincodeInput.visibility = View.GONE
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
                binding.statusText.text = "Account synced with Google"
                google_sign_in_button.isEnabled = false
                sendGoogleToken(googleAccount)
            }
            else {
                binding.statusText.text = "Sync account with Google"
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
        binding.setPincodeButton.isEnabled = false

        val requestQueue = Volley.newRequestQueue(context)

        val requestParams = HashMap<String, String>()

        if (SessionSettings.instance.displayName == "") {
            binding.statusText.text = "You must first set a display name in Options"
            return
        }

        if (binding.pincodeInput.text.toString().isEmpty()) {
            binding.setPincodeButton.isEnabled = true
            return
        }

        if (binding.pincodeInput.text.toString() != binding.pincode2Input.text.toString()) {
            binding.statusText.text = "Pincodes don't match"
            return
        }

        if (binding.pincodeInput.text.toString().length != 8) {
            binding.statusText.text = "Pincode length is incorrect"
            return
        }

        requestParams["pincode"] = binding.pincodeInput.text.toString()

        val paramsJson = JSONObject(requestParams as Map<String, String>)

        val request = object: JsonObjectRequest(
            Request.Method.POST,
            Utils.baseUrlApi + "/api/v1/devices/${SessionSettings.instance.uniqueId}",
            paramsJson,
            { response ->
                SessionSettings.instance.pincodeSet = true

                // update UI
                binding.statusText.text = "Pincode set. Go to Options -> Sign-in to access your account from any device."
            },
            { error ->
                Toast.makeText(context, "Network error, please try again.", Toast.LENGTH_SHORT).show()
                binding.setPincodeButton.isEnabled = true
            }) {

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json; charset=utf-8"
                headers["key1"] = Utils.key1
                return headers
            }
        }

        requestQueue.add(request)
    }

    private fun changePincode() {
        binding.setPincodeButton.isEnabled = false

        val requestQueue = Volley.newRequestQueue(context)

        val requestParams = HashMap<String, String>()

        if (SessionSettings.instance.displayName == "") {
            binding.statusText.text = "You must first set a display name in Options"
            return
        }

        if (binding.oldPincodeInput.text.toString().isEmpty()) {
            binding.setPincodeButton.isEnabled = true
            return
        }

        if (binding.oldPincodeInput.text.toString().length != 8) {
            binding.statusText.text = "Old pincode length is incorrect"
            return
        }

        if (binding.pincodeInput.text.toString().isEmpty()) {
            binding.setPincodeButton.isEnabled = true
            return
        }

        if (binding.pincodeInput.text.toString() != binding.pincode2Input.text.toString()) {
            binding.statusText.text = "Pincodes don't match"
            return
        }

        if (binding.pincodeInput.text.toString().length != 8) {
            binding.statusText.text = "Pincode length is incorrect"
            return
        }

        requestParams["old_pincode"] = binding.oldPincodeInput.text.toString()
        requestParams["pincode"] = binding.pincodeInput.text.toString()

        val paramsJson = JSONObject(requestParams as Map<String, String>)

        val request = object: JsonObjectRequest(
            Request.Method.POST,
            Utils.baseUrlApi + "/api/v1/devices/${SessionSettings.instance.uniqueId}/pincode",
            paramsJson,
            { response ->
                if (response.has("error")) {
                    binding.statusText.text = "The pincode you entered is incorrect"
                }
                else {
                    SessionSettings.instance.pincodeSet = true

                    // update UI
                    binding.statusText.text = "Pincode changed. Go to Options -> Sign-in to access your account from any device."
                }

            },
            { error ->
                Toast.makeText(context, "Network error, please try again.", Toast.LENGTH_SHORT).show()
                binding.setPincodeButton.isEnabled = true
            }) {

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json; charset=utf-8"
                headers["key1"] = Utils.key1
                return headers
            }
        }

        requestQueue.add(request)
    }

    private fun signIn() {
        binding.signInButton2.isEnabled = false

        val requestQueue = Volley.newRequestQueue(context)

        val requestParams = HashMap<String, String>()

        var name = binding.nameInput.text.toString()
        var pincode = binding.pincodeInputSignIn.text.toString()

        if (name.length > 20) {
            binding.statusText.text = "Name length is incorrect"
            binding.statusText.visibility = View.VISIBLE
            return
        }
        else if (name.isEmpty()) {
            binding.statusText.text = "Please enter a display name"
            binding.statusText.visibility = View.VISIBLE
            return
        }

        if (pincode.isEmpty()) {
            binding.statusText.text = "Please enter a pincode"
            binding.statusText.visibility = View.VISIBLE
            return
        }

        if (pincode.length != 8) {
            binding.statusText.text = "Pincode length is incorrect"
            binding.statusText.visibility = View.VISIBLE
            return
        }

        requestParams["name"] = name
        requestParams["pincode"] = pincode

        val paramsJson = JSONObject(requestParams as Map<String, String>)

        val request = object: JsonObjectRequest(
            Request.Method.POST,
            Utils.baseUrlApi + "/api/v1/devices/pincode/auth",
            paramsJson,
            { response ->
                if (response.has("error")) {
                    binding.statusText.text = "Display name or password is incorrect"
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
                    binding.statusText.text = "Successfully signed in"
                }
                binding.statusText.visibility = View.VISIBLE
            },
            { error ->
                Toast.makeText(context, "Network error, please try again.", Toast.LENGTH_SHORT).show()
                binding.signInButton2.isEnabled = true
            }) {

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json; charset=utf-8"
                headers["key1"] = Utils.key1
                return headers
            }
        }

        requestQueue.add(request)
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
                            binding.statusText.text = "Signed in"
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