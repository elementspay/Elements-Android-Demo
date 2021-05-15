package com.elements.pay.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import com.elements.pay.demo.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.elements.pay.api.Environment
import io.elements.pay.api.client.ElementsApiClient
import io.elements.pay.api.client.ElementsApiClientConfiguration
import io.elements.pay.components.card.CardComponent
import io.elements.pay.components.card.CardConfiguration
import io.elements.pay.components.model.payments.request.CardPaymentMethod
import io.elements.pay.log.LogUtil
import io.elements.pay.log.Logger
import io.elements.pay.model.*
import io.elements.pay.model.paymentmethods.PaymentMethod
import io.elements.pay.model.paymentmethods.PaymentMethodSupportedData
import io.elements.pay.model.public.*
import java.util.*

class MainActivity : AppCompatActivity() {
    companion object {
        private val TAG: String = LogUtil.getTag()
    }

    private val clientToken = "TODO: Your client token fetched from backend goes here..."
    private val stripeKey = "TODO: Optional if you want to provide your Stripe publishable key as a fall back method..."
    private var currentCardParams: ElementsCardParams? = null

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
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
        val pspCustomers = arrayListOf(
            PspCustomer(
                pspAccount = PspAccount(SupportedPspType.STRIPE, "TODO: Your stripe account goes here..."),
                customerId = "TODO: Optional for customer id"
            )
        )
        val configuration = ElementsApiClientConfiguration(Environment.sandbox(clientToken), pspCustomers, stripeKey)
        val client = ElementsApiClient(configuration = configuration)
        client.tokenizeCard(cardParams, callback = object : ApiResultCallback<VaultToken> {
            override fun onSuccess(result: VaultToken) {
                result.elementsToken?.let {
                    createAlertDialog("Tokenization succeeded", parseElementsTokenToDisplayString(it)).show()
                }
                result.fallbackStripToken?.let {
                    createAlertDialog("Tokenization succeeded", "Stripe token $it").show()
                }
                Logger.d(TAG, "Tokenization succeeded $result")
            }

            override fun onError(e: Exception) {
                createAlertDialog("Tokenization failed", "$e").show()
                Logger.d(TAG, "Tokenization failed $e")
            }
        })
    }

    private fun showCardComponent() {
        val cardConfiguration = CardConfiguration.Builder(this@MainActivity, Environment.sandbox(clientToken))
            .setShowStorePaymentField(false)
            .setHolderNameRequired(true)
            .build()
        val paymentMethod = PaymentMethod()
        paymentMethod.type = "credit_cards"
        paymentMethod.supportedDataList = listOf("mc", "visa", "amex", "maestro", "cup", "diners", "discover", "jcb").map {
            val paymentMethodSupportedData = PaymentMethodSupportedData()
            paymentMethodSupportedData.brand = it
            paymentMethodSupportedData.label = it
            paymentMethodSupportedData
        }

        val cardComponent = CardComponent.PROVIDER.get(this@MainActivity, paymentMethod, cardConfiguration)
        binding.cardView.attach(cardComponent, this@MainActivity)
        cardComponent.observe(this@MainActivity, Observer {
            if (it.isValid) {
                it.data.paymentMethod?.let { paymentMethod ->
                    val json = CardPaymentMethod.SERIALIZER.serialize(paymentMethod)
                    currentCardParams = ElementsCardParams.SERIALIZER.deserialize(json)
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
        result += "Psp Tokens\n"
        for (pspToken in token.pspTokens) {
            result += "${pspToken.pspAccount.pspType.value.toLowerCase(Locale.getDefault())} token: ${pspToken.token}"
        }
        result += "\n\nElements Card\n"
        var cardDisplay = "Card id: ${token.card.id}\n"
        val brand = token.card.brand ?: "Unknown brand"
        val last4 = token.card.last4 ?: "Unknown last 4"
        cardDisplay += "Brand: ${brand}\nLast4: $last4"
        result += cardDisplay
        return result
    }
}