package com.elements.pay.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.elements.pay.demo.databinding.ActivityMainBinding
import io.elements.pay.api.Environment
import io.elements.pay.api.client.ElementsApiClient
import io.elements.pay.api.client.ElementsApiClientConfiguration
import io.elements.pay.components.card.CardComponent
import io.elements.pay.components.card.CardConfiguration
import io.elements.pay.components.model.paymentmethods.PaymentMethod
import io.elements.pay.components.model.paymentmethods.PaymentMethodSupportedData
import io.elements.pay.log.LogUtil
import io.elements.pay.log.Logger
import io.elements.pay.model.*

class MainActivity : AppCompatActivity() {
    companion object {
        private val TAG: String = LogUtil.getTag()
    }

    private val clientKey = "eyJraWQiOiJlbnYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE2MjA1MTMwOTcsIm1lcmNoYW50X2lkIjoiRzQ3R0RWWEhWSlRTRyIsImN1c3RvbWVyX2lkIjoiTTNMSzVWV0MyNVhSWCIsImV4dGVybmFsX2N1c3RvbWVyX2lkIjoiMzQ1NTFiNTQtZDg3ZS00Y2Y3LWJhODAtMDEwYmYyZDEwODE1Iiwic2NvcGUiOiJ3cml0ZSIsImV4cGlyZXNfaW4iOm51bGx9.BKdGYy0BLP7HJ4w2iY_OEN5IDI70zX0WIrAjb-3ZUe8YR7AoGkMjVG1BF7q-uByQSrGnfaROa_Vj79F1bXuY_iGvOjCfkfLaSNNaGjqjIXBGENphgWxFChgWn81J7luRe-2dKVGcLckFdIJ_COLDChBu2m1m2gkKvzRULdnXLrueR9ORAO-T5mkF9hMkeTNUIu0xNIc8Rp2O5sPdjTlxAEAFyLu-Myoj9qK7OASs69HJO-rSzSn6sXJ6GM9qj2gs3y4qquIMB-5JSi4rIB52EpoN9h-8v7CZH-rF1EB3RTtsumH8rUYQ3cmbP-DWtFNw34hfgdI4AKOSi4i9Q_pCuA"
    private val stripeKey = "pk_test_51HLcaZGIxBPZ7rpaxvAYG4JXt96FrFl5u1T7S4wQh6gKPmNmKsl3tCAARba2Jrce60qolY321XmZuDN3slduuU9900wmEXbYu0"

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //tokenizeCard()
        showCardComponent()
    }

    private fun tokenizeCard() {
        val pspCustomers = arrayListOf(
            PspCustomer(
                pspAccount = PspAccount("STRIPE", "F56FGLTABXWVR"),
                customerId = "cus_JPlDyCKLEaq8mO"
            )
        )
        val configuration = ElementsApiClientConfiguration(Environment.sandbox(clientKey), pspCustomers, stripeKey)
        val client = ElementsApiClient(configuration = configuration)
        val cardParams = ElementsCardParams(
            "424242424241",
            4,
            24,
            "242",
            "Marvin"
        )
        client.tokenizeCard(cardParams, callback = object : ApiResultCallback<VaultToken> {
            override fun onSuccess(result: VaultToken) {
                Logger.d(TAG, "Tokenization succeeded $result")
            }

            override fun onError(e: Exception) {
                Logger.d(TAG, "Tokenization failed $e")
            }
        })
    }

    private fun showCardComponent() {
        val cardConfiguration = CardConfiguration.Builder(this@MainActivity, Environment.sandbox(clientKey)).build()
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
    }
}