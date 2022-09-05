package com.ericversteeg.radiofrost.helper

import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import okhttp3.OkHttpClient
import java.net.URISyntaxException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

// TODO: Delete this class and get an SSL certificate from a CA
object TrustAllSSLCertsDebug {
    internal const val TAG = "TrustSSLCerts"
    fun trust() {
        try {
            val trustAllCerts: Array<TrustManager> = arrayOf(
                object : X509TrustManager {
                    override fun checkClientTrusted(p0: Array<out java.security.cert.X509Certificate>?, p1: String?) {}
                    override fun checkServerTrusted(p0: Array<out java.security.cert.X509Certificate>?, p1: String?) {}
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate?> {
                        return arrayOfNulls(0)
                    }
                }
            )

            val sc: SSLContext = SSLContext.getInstance("SSL")
            sc.init(null, trustAllCerts, SecureRandom())

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        } catch (e: Exception) {

        }
    }

    fun getAllCertsIOSocket(): Socket {
        try {
            val socketUrl: String = ""
            val hostnameVerifier =
                HostnameVerifier { _, _ -> true }
            val trustAllCerts: Array<TrustManager> =
                arrayOf(object : X509TrustManager {
                    override fun checkClientTrusted(
                        chain: Array<X509Certificate?>?,
                        authType: String?
                    ) {}

                    override fun checkServerTrusted(
                        chain: Array<X509Certificate?>?,
                        authType: String?
                    ) {}

                    override fun getAcceptedIssuers(): Array<X509Certificate?> {
                        return arrayOfNulls(0)
                    }
                })
            val trustManager: X509TrustManager = trustAllCerts[0] as X509TrustManager
            val sslContext: SSLContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, null)
            val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory
            val okHttpClient = OkHttpClient.Builder()
                .hostnameVerifier(hostnameVerifier)
                .sslSocketFactory(sslSocketFactory, trustManager)
                .build()
            val opts = IO.Options()
            opts.callFactory = okHttpClient
            opts.webSocketFactory = okHttpClient

            opts.reconnectionAttempts = 3
            opts.transports = arrayOf(WebSocket.NAME)

            return IO.socket(socketUrl, opts)

        } catch (e: URISyntaxException) {

        } catch (e: NoSuchAlgorithmException) {

        } catch (e: KeyManagementException) {

        }

        return IO.socket("")
    }
}