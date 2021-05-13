package com.elements.pay.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.elements.pay.demo.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.elements.pay.api.Environment
import io.elements.pay.api.client.ElementsApiClient
import io.elements.pay.api.client.ElementsApiClientConfiguration
import io.elements.pay.log.LogUtil
import io.elements.pay.log.Logger
import io.elements.pay.model.public.*
import java.util.*

class MainActivity : AppCompatActivity() {
    companion object {
        private val TAG: String = LogUtil.getTag()
    }

    private val clientToken = "TODO: Your client token fetched from backend goes here..."
    private val stripeKey = "TODO: Optional if you want to provide your Stripe publishable key as a fall back method..."

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(findViewById(R.id.toolbar))

        binding.payButton.setOnClickListener {
            tokenizeCard(ElementsCardParams(
                "4242424242424242",2, 24, "123", "!23"
            ))
        }
    }

    private fun tokenizeCard(cardParams: ElementsCardParams) {
        // Optional list if you want to provide your psp infos.
        val pspCustomers = arrayListOf(
            PspCustomer(
                pspAccount = PspAccount("STRIPE", "TODO: Your stripe account goes here..."),
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
            result += "${pspToken.pspAccount.pspType.toLowerCase(Locale.getDefault())} : ${pspToken.token}"
        }
        result += "\nElements Card\n"
        var cardDisplay = "Card id: ${token.card.id}\n"
        val brand = token.card.brand ?: "Unknown brand"
        val last4 = token.card.last4 ?: "Unknown last 4"
        cardDisplay += "Brand: ${brand}\nLast4: $last4"
        result += cardDisplay
        return result
    }
}