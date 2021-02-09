package com.ericversteeg.liquidocean.fragment

import android.content.Intent
import android.util.Log
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import kotlinx.android.synthetic.main.fragment_signin.*
import kotlinx.android.synthetic.main.fragment_signin.back_action
import kotlinx.android.synthetic.main.fragment_signin.back_button
import org.json.JSONObject


class SignInFragment: Fragment() {

    private lateinit var signInClient: GoogleSignInClient

    var signInListener: SignInListener? = null
    var googleAccount: GoogleSignInAccount? = null

    val signInRequestCode = 1000

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

        status_text.text = ""

        back_button.actionBtnView = back_action
        back_action.type = ActionButtonView.Type.BACK

        back_button.setOnClickListener {
            signInListener?.onSignInBack()
        }

        google_sign_in_button.setOnClickListener {
            activity?.startActivityForResult(signInClient.signInIntent, signInRequestCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.client_id))
            .requestEmail()
            .build()

        activity?.apply {
            signInClient = GoogleSignIn.getClient(this, gso)
        }
    }

    override fun onStart() {
        super.onStart()

        activity?.apply {
            googleAccount = GoogleSignIn.getLastSignedInAccount(this)

            if (googleAccount != null) {
                status_text.text = "Account signed in."
                google_sign_in_button.isEnabled = false
                sendGoogleToken(googleAccount)
            }
            else {
                status_text.text = "Please login:"
            }
        }
    }

    fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            googleAccount = completedTask.getResult(ApiException::class.java)
            sendGoogleToken(googleAccount)

            status_text.text = "Signed in"
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.i("Sign In", "signInResult:failed code=" + e.statusCode)
        }
    }

    private fun sendGoogleToken(googleAccount: GoogleSignInAccount?) {
        googleAccount?.apply {
            context?.apply {
                val requestQueue = Volley.newRequestQueue(this)

                val requestParams = HashMap<String, String>()

                googleAccount.idToken?.apply {
                    requestParams["token_id"] = this

                    val paramsJson = JSONObject(requestParams as Map<String, String>)

                    val request = JsonObjectRequest(
                        Request.Method.POST,
                        Utils.baseUrlApi + "/api/v1/devices/${SessionSettings.instance.uniqueId}/google/auth",
                        paramsJson,
                        { response ->
                            SessionSettings.instance.uniqueId = response.getString("uuid")
                            SessionSettings.instance.dropsAmt = response.getInt("paint_qty")
                            SessionSettings.instance.xp = response.getInt("xp")

                            StatTracker.instance.numPixelsPaintedWorld = response.getInt("wt")
                            StatTracker.instance.numPixelsPaintedSingle = response.getInt("st")
                            StatTracker.instance.totalPaintAccrued = response.getInt("tp")
                            StatTracker.instance.numPixelOverwritesIn = response.getInt("oi")
                            StatTracker.instance.numPixelOverwritesOut = response.getInt("oo")

                            context?.apply {
                                StatTracker.instance.save(this)
                            }

                            SessionSettings.instance.googleAuth = true
                        },
                        { error ->

                        })

                    requestQueue.add(request)
                }

            }
        }
    }
}