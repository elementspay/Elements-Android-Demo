# Elements-Android-SDK Demo

The Elements Android SDK makes it quick and easy to build an excellent payment experience in your Android app. We provide powerful and customizable UI screens and elements that can be used out-of-the-box to collect your users' payment details. We also expose the low-level APIs that power those UIs so that you can build fully custom experiences.

Table of contents
=================

<!--ts-->
   * [Features](#features)
   * [Requirements](#requirements)
   * [Getting Started](#getting-started)
      * [Installation](#installation)
      * [Usage](#usage)
      * [Example](#example-app)
   * [Releases](#releases)
<!--te-->

## Features

**PCI Complaint**: We make it simple for you to collect sensitive data such as credit card numbers and remain [PCI compliant](https://stripe.com/docs/security#pci-dss-guidelines). This means the sensitive data is sent directly to Elements instead of passing through your server

**Elements API**: We provide [low-level APIs](https://stripe.dev/stripe-ios/docs/Classes/STPAPIClient.html) that correspond to objects and methods in the Elements API.

**Native UI**: (Coming soon in the future) We provide native screens and elements to collect payment details.

## Requirements

* Android 5.0 (API level 21) and above
* [Android Gradle Plugin](https://developer.android.com/studio/releases/gradle-plugin) 3.5.1
* [Gradle](https://gradle.org/releases/) 5.4.1+
* [AndroidX](https://developer.android.com/jetpack/androidx/) (as of v11.0.0)

## Getting Started

## Installation

Add `io.elements.pay:core-module` to your `build.gradle` dependencies.

### Gradle
```
dependencies {
    implementation 'io.elements.pay:core-module:1.0.1-beta01'
}
```

## Usage

### ElementsApiClient

The [ElementsApiClient] handles the low level api communications to Elements server. i.e. Card Tokenization

#### Initialize the API client.

The api client requires `clientToken` fetched from your backend server. Once you have obtained your `clientToken` you can initialize the api client in the following way.

```kotlin
// Environment -> The environment that ElementsApiClient runs.
// StripeTestKey -> Optional if you want to take fall back to Stripe tokenization
// if elements tokenization failed
val configuration = ElementsApiClientConfiguration(
  Environment.sandbox(clientToken)
)
val elementsApiClient = ElementsApiClient(configuration = configuration)
```

In order to call the tokenize api, you need to create and pass in `ElementsCardParams` object, example:

```kotlin
val cardParams = ElementsCardParams(
  cardNumber = "424242424242",
  expiryMonth = 4,
  expiryYear = 24,
  securityCode = "242",
  holderName = "Test"
)
```

Once you have created the `ElementsAPIClient` and `ElementsCardParams` you can call the following to tokenize a card.

```kotlin
elementsApiClient.tokenizeCard(cardParams, callback = object : ApiResultCallback<VaultToken> {
  override fun onSuccess(result: VaultToken) {
    Log.d(TAG, "Tokenization succeeded $result")
    result.elementsToken?.let {
      // Now you can pass this token object to your backend server
    }
    result.fallbackStripToken?.let { 
      // If Elements tokenization failed, a stripe token will be generated if you have provided the `stripePublishableKey`
      // You can pass the stripe key to your backend server as a fall back.
    }
  }

  override fun onError(e: Exception) {
    // Check ElementsException for tokenization failure.
    Log.d(TAG, "Tokenization failed $e")
  }
})
```

### ElementsToken

The `ElementsToken` model returns the response received from Elements server once tokenization succeeded. It contains the corresponded elements token matching the `ElementsCardParams` you passed in the method. It will also contain an `ElementsCard` object that has the tokenized card info.

```kotlin
ElementsToken(
  id = tok_xxxxxxxxxxxxxxxx
  card = ElementsCard(
    id = card-9a06775f-d4d7-4413-8fed-379ac610283e, 
    last4 = 4242, 
    expiryMonth= 4,
    expiryYear = 2024, 
    brand = VISA, 
    fingerprint = mhahtPiD6Yns0Nnyn82206DG1l17mxYIedFkWuQ6GUg=
  )
)
```

### 3DS2 Flow ###

`ElementsAPIClient` also supports tokenize card with 3DS2 auth flow enabled. In order to handle 3DS2 flow correctly you need to pass an `activity` param to the tokenization method. This activity will be the host of all 3DS flow activities from Elements.

```kotlin
private fun tokenizeCard(cardParams: ElementsCardParams) {
    client.tokenizeCard(cardParams, this, callback = object : ApiResultCallback<VaultToken> {
        override fun onSuccess(result: VaultToken) {
            result.elementsToken?.let {
                Log.d("Tokenization succeeded", parseElementsTokenToDisplayString(it)).show()
            }
            result.fallbackStripToken?.let {
                Log.d("Tokenization succeeded", "Stripe token $it").show()
            }
        }

        override fun onError(e: Exception) {
            Log.d(TAG, "Tokenization failed $e")
        }
    })
}
```
You will also need to listen for 3DS result from `onActivityResult` and obtain result from apiClient.
```kotlin
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
```
Now the token you obtained from `onActivityResult` will be a token validated through 3DS.

### Example App

Clone this repo and run the app. The demo app demonstrated how to use `ElementsApiClient`.

## Releases

Releases notifications will get updated here.
