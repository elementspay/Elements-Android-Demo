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
    implementation 'io.elements.pay:core-module:0.1.7'
}
```

## Usage

### ElementsApiClient

The [ElementsApiClient] handles the low level api communications to Elements server. i.e. Card Tokenization

#### Initialize the API client.

The api client requires `clientToken` fetched from your backend server. Once you have obtained your `clientToken` you can initialize the api client in the following way.

```kotlin

// Optional field if you want to provide your psp customer info.
val pspCustomers = arrayListOf(
  PspCustomer(
    pspAccount = PspAccount("STRIPE", "F56FGLTABXWVR"),
    customerId = "cus_JPlDyCKLEaq8mO"
  )
)
// Environment -> The environment that ElementsApiClient runs.
// PspCustomers -> Optional list if you want to provide your psp customer info.
// StripeTestKey -> Optional if you want to take fall back to Stripe tokenization
// if elements tokenization failed
val configuration = ElementsApiClientConfiguration(
  Environment.sandbox(clientToken), pspCustomers, stripeTestKey
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
    Logger.d(TAG, "Tokenization succeeded $result")
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
    Logger.d(TAG, "Tokenization failed $e")
  }
})
```

### ElementsToken

The `ElementsToken` struct returns the response received from Elements server once tokenization succeeded. It contains the corresponded tokens matching the `[PspCustomer]` you have configured in `ElementsApiClient`. It will also contains a `ElementsCard` object that has the tokenized card info.

```kotlin
ElementsToken(
  pspTokens= [
    PspToken(pspAccount = PspAccount(pspType = STRIPE, accountId=xxxxxxxx),
    customerId = null,
    token = tok_xxxxxxxxxxxxxxxx)
  ],
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

### Example App

Clone this repo and run the app. The demo app demonstrated how to use `ElementsApiClient`.

## Releases

Releases notifications will get updated here.