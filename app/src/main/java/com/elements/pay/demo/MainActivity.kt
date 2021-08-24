package com.elements.pay.demo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import com.elements.pay.demo.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.elements.pay.api.Environment
import io.elements.pay.api.client.ElementsApiClient
import io.elements.pay.api.client.ElementsApiClientConfiguration
import io.elements.pay.model.internalmodel.paymentmethods.ElementsCardParams
import io.elements.pay.model.paymentmethods.PaymentMethod
import io.elements.pay.model.paymentmethods.PaymentMethodSupportedData
import io.elements.pay.model.publicinterface.ApiResultCallback
import io.elements.pay.model.publicinterface.VaultToken
import io.elements.pay.modules.actions.driver.ElementsActionDriver
import io.elements.pay.model.publicinterface.ElementsToken
import io.elements.pay.modules.actions.redirect.RedirectUtil
import io.elements.pay.modules.card.CardConfiguration
import io.elements.pay.modules.card.CardElement
import java.lang.ref.WeakReference
import android.util.Log

class MainActivity : AppCompatActivity(), ElementsActionDriver.ActionCompletionListener {

    private val clientToken = "TODO: Your client token fetched from backend goes here..."
    private val stripeKey = "TODO: Optional if you want to provide your Stripe publishable key as a fall back method..."
    private var currentCardParams: ElementsCardParams? = null

    private lateinit var actionDriver: ElementsActionDriver
    private lateinit var binding: ActivityMainBinding
    private lateinit var client: ElementsApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionDriver = ElementsActionDriver(
            this,
            Environment.sandbox(clientToken),
            WeakReference(this)
        )
        binding = ActivityMainBinding.inflate(layoutInflater)
        val configuration = ElementsApiClientConfiguration(Environment.sandbox(clientToken))
        client = ElementsApiClient(configuration = configuration)
        handleIntent(intent)
        setContentView(binding.root)
        setSupportActionBar(findViewById(R.id.toolbar))

        showCardComponent()

        binding.payButton.setOnClickListener {
            currentCardParams?.let {
                tokenizeCard(it)
            }
        }
    }

    private fun tokenizeCard(cardParams: ElementsCardParams) {
        client.tokenizeCard(cardParams, this, callback = object : ApiResultCallback<VaultToken> {
            override fun onSuccess(result: VaultToken) {
                result.elementsToken?.let {
                    createAlertDialog("Tokenization succeeded", parseElementsTokenToDisplayString(it)).show()
                }
                result.fallbackStripToken?.let {
                    createAlertDialog("Tokenization succeeded", "Stripe token $it").show()
                }
                Log.d(TAG, "Tokenization succeeded $result")
            }

            override fun onError(e: Exception) {
                createAlertDialog("Tokenization failed", "$e").show()
                Log.d(TAG, "Tokenization failed $e")
            }
        })
    }

    private fun showCardComponent() {
        val cardConfiguration = CardConfiguration.Builder(this@MainActivity, Environment.production(clientToken))
            .setShowStorePaymentField(false)
            .setHolderNameRequired(true)
            .build()
        val paymentMethod = PaymentMethod()
        paymentMethod.type = "card"
        paymentMethod.supportedDataList = listOf("mc", "visa", "amex", "maestro", "cup", "diners", "discover", "jcb").map {
            val paymentMethodSupportedData = PaymentMethodSupportedData()
            paymentMethodSupportedData.brand = it
            paymentMethodSupportedData.label = it
            paymentMethodSupportedData
        }

        val cardComponent = CardElement.PROVIDER.get(this@MainActivity, paymentMethod, cardConfiguration)
        binding.cardView.attach(cardComponent, this@MainActivity)
        cardComponent.observe(this@MainActivity, Observer {
            if (it.isValid) {
                it.data.paymentMethod?.let { paymentMethod ->
                    currentCardParams = ElementsCardParams(
                        cardNumber = paymentMethod.cardNumber ?: "",
                        expiryMonth = paymentMethod.expiryMonth?.toInt(),
                        expiryYear = paymentMethod.expiryYear?.toInt(),
                        securityCode = paymentMethod.securityCode,
                        holderName = paymentMethod.holderName,
                    )
                }
            } else {
                currentCardParams = null
            }
        })
    }

    private fun createAlertDialog(title: String, message: String): AlertDialog {
        return MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setNeutralButton(android.R.string.ok, null)
            .create()
    }

    private fun parseElementsTokenToDisplayString(token: ElementsToken): String {
        var result = "Elements Token Object\n"
        result += "Token ${token.id}\n"
        result += "\n\nElements Card\n"
        var cardDisplay = "Card id: ${token.card.id}\n"
        val brand = token.card.brand ?: "Unknown brand"
        val last4 = token.card.last4 ?: "Unknown last 4"
        cardDisplay += "Brand: ${brand}\nLast4: $last4"
        result += cardDisplay
        return result
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            // Redirect response
            Intent.ACTION_VIEW -> {
                val data = intent.data
                Log.d(TAG, "scheme: ${RedirectUtil.REDIRECT_RESULT_SCHEME}")
                if (data != null && data.toString().startsWith(RedirectUtil.REDIRECT_RESULT_SCHEME)) {
                    actionDriver.handleRedirectResponse(data)
                } else {
                    Log.d(TAG, "Unexpected response from ACTION_VIEW - ${intent.data}")
                }
            }
            else -> {
                Log.d(TAG, "Unable to find action")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        client.onTokenizationSetupResult(requestCode, data, object : ApiResultCallback<ElementsToken> {
            override fun onSuccess(result: ElementsToken) {
                Log.d(TAG, "Tokenization succeeded $result")
            }

            override fun onError(e: Exception) {
                Log.d(TAG, "Tokenization failed $e")
            }
        })
    }

    override fun elementsActionFailed(error: Exception) {
        Log.d(TAG, "Something went wrong failed $error")
    }

    override fun elementsActionSucceeded(chargeToken: String?) {
        Log.d(TAG, "Action succeeded token: $chargeToken")
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}