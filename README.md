killbill-adyen-plugin
=====================

Plugin to use [Adyen](https://www.adyen.com/home/) as a gateway.

Release builds are available on [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.kill-bill.billing.plugin.java%22%20AND%20a%3A%22adyen-plugin%22) with coordinates `org.kill-bill.billing.plugin.java:adyen-plugin`.

Kill Bill compatibility
-----------------------

| Plugin version | Kill Bill version |
| -------------: | ----------------: |
| 0.1.y          | 0.14.z            |
| 0.2.y          | 0.15.z            |
| 0.3.y          | 0.16.z            |
| 0.4.y          | 0.17.z            |
| 0.5.y          | 0.18.z            |
| 0.6.y          | 0.19.z            |
| 0.7.y          | 0.20.z            |

Requirements
------------

The plugin needs a database. The latest version of the schema can be found [here](https://github.com/killbill/killbill-adyen-plugin/blob/master/src/main/resources/ddl.sql).

Configuration
-------------

The following properties are required:

* `org.killbill.billing.plugin.adyen.merchantAccount`: your merchant account(s)
* `org.killbill.billing.plugin.adyen.username`: your username(s)
* `org.killbill.billing.plugin.adyen.password`: your password(s)
* `org.killbill.billing.plugin.adyen.paymentUrl`: SOAP Payment service url (i.e. `https://pal-test.adyen.com/pal/servlet/Payment/v12` or `https://pal-live.adyen.com/pal/servlet/Payment/v12`)

The following properties are optional:

* `org.killbill.billing.plugin.adyen.paymentConnectionTimeout`: Connection time-out in milliseconds for calls to Adyen SOAP Payment Service
* `org.killbill.billing.plugin.adyen.paymentReadTimeout`: Read time-out in milliseconds for calls to Adyen SOAP Payment Service
* `org.killbill.billing.plugin.adyen.recurringConnectionTimeout`: Connection time-out in milliseconds for calls to Adyen SOAP Recurring Service
* `org.killbill.billing.plugin.adyen.recurringReadTimeout`: Read time-out in milliseconds for calls to Adyen SOAP Recurring Service
* `org.killbill.billing.plugin.adyen.proxyServer`: Proxy server address
* `org.killbill.billing.plugin.adyen.proxyPort`: Proxy server port
* `org.killbill.billing.plugin.adyen.proxyType`: Proxy server type (HTTP or SOCKS)
* `org.killbill.billing.plugin.adyen.trustAllCertificates`: Whether to disable SSL certificates validation
* `org.killbill.billing.plugin.adyen.sensitiveProperties`: A list of sensitive property keys; if specified, they won't be persisted in the additional field of Adyen hpp request table.
* `org.killbill.billing.plugin.adyen.paymentProcessorAccountIdToMerchantAccount`: Mappings from the `paymentProcessorAccountId` to Adyen merchant accounts. The `paymentProcessorAccountId`, if exists in the plugin property, is a `String` set by the upstream logic to specify the merchant account used in the transaction.

Only needed for the Tests:

* `org.killbill.billing.plugin.adyen.recurringUrl`: SOAP Recurring Service URL (i.e. `https://pal-test.adyen.com/pal/servlet/Recurring/v12`)

The format for the merchant account(s), username(s) and password(s) is `XX#YY|XX#YY|...` where:

* `XX` is the country code (DE, FR, etc.)
* `YY` is the value (merchant account, username of the form `ws@Company.[YourCompanyAccount]` or password)

Notes:

* If you have a single country, omit the country code part
* If you have several merchant accounts per country, you can also specify:
  * `XX#YY` in the username property where `XX` is the merchant account
  * `XX#YY` in the password property where `XX` is the username
  * `XX#YY` in the skin property where `XX` is the merchant account
  * `XX#YY` in the hmac secret property where `XX` is the skin name
* You can also configure a FALLBACK merchant account like `FALLBACK#FallBackMerchantAccount`, which will be chosen if no matched merchant account is found based on the country code.

To configure Hosted Payment Pages (HPP):

* `org.killbill.billing.plugin.adyen.hpp.target`: host payment page url (e.g. https://test.adyen.com/hpp/pay.shtml)
* `org.killbill.billing.plugin.adyen.hmac.secret`: your hmac secret(s)
* `org.killbill.billing.plugin.adyen.skin`: you skin code(s)
* `org.killbill.billing.plugin.adyen.directoryUrl`: directory lookup url (e.g. https://test.adyen.com/hpp/directory.shtml)

The format for secrets and skins is the same as above if you support multiple countries.

The URLs can also be configured on a per region basis by specifying the region as a prefix, e.g. `XX.org.killbill.billing.plugin.adyen.paymentUrl=...` where `XX` is the region (value matching the `org.killbill.server.region` property).

These properties can be specified globally via System Properties or on a per tenant basis:

```
curl -v \
     -X POST \
     -u admin:password \
     -H 'X-Killbill-ApiKey: bob' \
     -H 'X-Killbill-ApiSecret: lazar' \
     -H 'X-Killbill-CreatedBy: admin' \
     -H 'Content-Type: text/plain' \
     -d 'org.killbill.billing.plugin.adyen.paymentUrl=WWW
org.killbill.billing.plugin.adyen.merchantAccount=XXX
org.killbill.billing.plugin.adyen.username=YYY
org.killbill.billing.plugin.adyen.password=ZZZ' \
     http://127.0.0.1:8080/1.0/kb/tenants/uploadPluginConfig/killbill-adyen
```

### Kill Bill

To avoid runtime errors (such as `ClassCastException`), starting Kill Bill with the System Property `com.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize=true` is recommended.

Usage
-----

A full end-to-end integration demo is also available [here](https://github.com/killbill/killbill-adyen-demo).

### Credit cards

Add a payment method:

```
curl -v \
     -u admin:password \
     -H "X-Killbill-ApiKey: bob" \
     -H "X-Killbill-ApiSecret: lazar" \
     -H "Content-Type: application/json" \
     -H "X-Killbill-CreatedBy: demo" \
     -X POST \
     --data-binary '{
       "pluginName": "killbill-adyen",
       "pluginInfo": {
         "properties": [
           {
             "key": "ccLastName",
             "value": "KillBill"
           },
           {
             "key": "ccExpirationMonth",
             "value": 8
           },
           {
             "key": "ccExpirationYear",
             "value": 2018
           },
           {
             "key": "ccNumber",
             "value": 4111111111111111
           },
           {
             "key": "ccVerificationValue",
             "value": 737
           }
         ]
       }
     }' \
     "http://127.0.0.1:8080/1.0/kb/accounts/<ACCOUNT_ID>/paymentMethods?isDefault=true"
```

Notes:
* Make sure to replace *ACCOUNT_ID* with the id of the Kill Bill account
* Details for working payment methods are available here: https://www.adyen.com/home/support/knowledgebase/implementation-articles.html

To trigger a payment:

```
curl -v \
     -u admin:password \
     -H "X-Killbill-ApiKey: bob" \
     -H "X-Killbill-ApiSecret: lazar" \
     -H "Content-Type: application/json" \
     -H "X-Killbill-CreatedBy: demo" \
     -X POST \
     --data-binary '{"transactionType":"AUTHORIZE","amount":"5","currency":"EUR","transactionExternalKey":"INV-'$(uuidgen)'-PURCHASE"}' \
    "http://127.0.0.1:8080/1.0/kb/accounts/<ACCOUNT_ID>/payments?pluginProperty=country=DE"
```

Notes:
* Make sure to replace *ACCOUNT_ID* with the id of the Kill Bill account
* The country plugin property will be used to retrieve your merchant account

At this point, the payment will be in *PENDING* state, until we receive a notification from Adyen. You can verify the state of the transaction by listing the payments:

```
curl -v \
     -u admin:password \
     -H "X-Killbill-ApiKey: bob" \
     -H "X-Killbill-ApiSecret: lazar" \
     -H "Content-Type: application/json" \
     -H "X-Killbill-CreatedBy: demo" \
    "http://127.0.0.1:8080/1.0/kb/accounts/<ACCOUNT_ID>/payments?withPluginInfo=true"
```

You can simulate a notification from Adyen as follows:

```
curl -v \
     -u admin:password \
     -H "X-Killbill-ApiKey: bob" \
     -H "X-Killbill-ApiSecret: lazar" \
     -H "Content-Type: application/json" \
     -H "X-Killbill-CreatedBy: demo" \
     -X POST \
     --data-binary '<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <soap:Body>
    <ns1:sendNotification xmlns:ns1="http://notification.services.adyen.com">
      <ns1:notification>
        <live xmlns="http://notification.services.adyen.com">true</live>
        <notificationItems xmlns="http://notification.services.adyen.com">
          <NotificationRequestItem>
            <additionalData xsi:nil="true"/>
            <amount>
              <currency xmlns="http://common.services.adyen.com">EUR</currency>
              <value xmlns="http://common.services.adyen.com">2995</value>
            </amount>
            <eventCode>AUTHORISATION</eventCode>
            <eventDate>2013-04-15T06:59:22.278+02:00</eventDate>
            <merchantAccountCode>TestMerchant</merchantAccountCode>
            <merchantReference>325147059</merchantReference>
            <operations>
              <string>CANCEL</string>
              <string>CAPTURE</string>
              <string>REFUND</string>
            </operations>
            <originalReference xsi:nil="true"/>
            <paymentMethod>visa</paymentMethod>
            <pspReference>4823660019473428</pspReference>
            <reason>111647:7629:5/2014</reason>
            <success>true</success>
          </NotificationRequestItem>
        </notificationItems>
      </ns1:notification>
    </ns1:sendNotification>
  </soap:Body>
</soap:Envelope>' \
    "http://127.0.0.1:8080/1.0/kb/paymentGateways/notification/killbill-adyen"
```

Notes:
* Make sure to replace *pspReference* with the psp reference of your payment (see the *adyen_responses* table)
* If *success* is true, the payment transaction state will be *SUCCESS* and the payment state *AUTH_SUCCESS*
* If *success* is false, the payment transaction state will be *PAYMENT_FAILURE* and the payment state *AUTH_FAILED*

### SEPA

The APIs are similar to the Credit Card use-case. Here is an example payload for the add payment method call:

```
curl -v \
     -u admin:password \
     -H "X-Killbill-ApiKey: bob" \
     -H "X-Killbill-ApiSecret: lazar" \
     -H "Content-Type: application/json" \
     -H "X-Killbill-CreatedBy: demo" \
     -X POST \
     --data-binary '{
       "pluginName": "killbill-adyen",
       "pluginInfo": {
         "properties": [
           {
             "key": "ddHolderName",
             "value": "A. Schneider"
           },
           {
             "key": "ddNumber",
             "value": "DE87123456781234567890"
           },
           {
             "key": "ddBic",
             "value": "TESTDE01XXX"
           }
         ]
       }
     }' \
     "http://127.0.0.1:8080/1.0/kb/accounts/<ACCOUNT_ID>/paymentMethods?isDefault=true"
```

### HPP

To generate an HPP url:

```
curl -v \
     -u admin:password \
     -H "X-Killbill-ApiKey: bob" \
     -H "X-Killbill-ApiSecret: lazar" \
     -H "Content-Type: application/json" \
     -H "X-Killbill-CreatedBy: demo" \
     -X POST \
     --data-binary '{
       "formFields": [
         {
           "key": "country",
           "value": "DE"
         },
         {
           "key": "serverUrl",
           "value": "http://killbill.io"
         },
         {
           "key": "resultUrl",
           "value": "?q=test+adyen+redirect+success"
         },
         {
           "key": "amount",
           "value": 10
         },
         {
           "key": "currency",
           "value": "USD"
         }
       ]
     }' \
     "http://127.0.0.1:8080/1.0/kb/paymentGateways/hosted/form/<ACCOUNT_ID>"
```

Notes:
* Make sure to replace *ACCOUNT_ID* with the id of the Kill Bill account
* *country* is used to retrieve the skin and the merchant account
* *customerLocale* (e.g. *es_CO*) can be used to specify Adyen's *countryCode* parameter (to override the filtering of payment methods based on IP address). This will also be used to specify the *shopperLocale* parameter
* *serverUrl* and *resultUrl* are used to redirect the user after the completion of the payment flow (success or failure)
* At this point, no payment has been created in Kill Bill. The payment will be recorded when processing the notification

### Recurring

For [Adyen's Recurring Functionality](https://docs.adyen.com/display/TD/Recurring+Manual) the following recurring types are provided (see [RecurringType.java](https://github.com/killbill/killbill-adyen-plugin/blob/master/src/main/java/org/killbill/billing/plugin/adyen/client/model/RecurringType.java))
* `ONECLICK`
* `RECURRING`

There are 3 different use cases:

1. Use Adyen's recurring payments feature with contract `RECURRING`: CVV is not required (it's an implicit `contAuth`)
2. Use Adyen's recurring payment feature with contract `ONECLICK`: CVV is always required
3. Use your own card-on-file system + `contAuth` to simulate option 1. Instead of providing Adyen's `recurringDetailId`, the merchant retrieves stored payment data from its store and populates the fields like a normal payment request. `contAuth` is needed to turn Adyen's (not needed) validations off.

#### Client-Side Encryption (CSE)

Take a look at the end-to-end [demo](https://github.com/killbill/killbill-adyen-demo) to understand how to use CSE. At a high level, the steps are:

1. Create account

```
curl -v \
     -u admin:password \
     -H "X-Killbill-ApiKey: bob" \
     -H "X-Killbill-ApiSecret: lazar" \
     -H "Content-Type: application/json" \
     -H "X-Killbill-CreatedBy: demo" \
     -X POST \
     --data-binary '{}' \
     "http://127.0.0.1:8080/1.0/kb/accounts"
```

2. Create an empty payment method

```
curl -v \
     -u admin:password \
     -H "X-Killbill-ApiKey: bob" \
     -H "X-Killbill-ApiSecret: lazar" \
     -H "Content-Type: application/json" \
     -H "X-Killbill-CreatedBy: demo" \
     -X POST \
     --data-binary '{
       "pluginName": "killbill-adyen",
       "pluginInfo": {
         "properties": []
       }
     }' \
     "http://127.0.0.1:8080/1.0/kb/accounts/<ACCOUNT_ID>/paymentMethods?isDefault=true&pluginProperty=skip_gw=true"
```

3. Trigger a $1 auth using the encrypted JSON (Adyen requires a payment to tokenize the card)

```
curl -v \
     -u admin:password \
     -H "X-Killbill-ApiKey: bob" \
     -H "X-Killbill-ApiSecret: lazar" \
     -H "Content-Type: application/json" \
     -H "X-Killbill-CreatedBy: demo" \
     -X POST \
     --data-binary '{
       "transactionType":"AUTHORIZE",
       "amount":"1",
       "currency":"USD",
       "properties": [
         {
           "key": "recurringType",
           "value": "RECURRING"
         },
         {
           "key": "contAuth",
           "value": "false"
         },
         {
           "key": "encryptedJson",
           "value": <ENCRYPTED_JSON>
         }
       ]
    }' \
    "http://127.0.0.1:8080/1.0/kb/accounts/<ACCOUNT_ID>/payments"
```

4. Void the auth

```
curl -v \
     -u admin:password \
     -H "X-Killbill-ApiKey: bob" \
     -H "X-Killbill-ApiSecret: lazar" \
     -H "Content-Type: application/json" \
     -H "X-Killbill-CreatedBy: demo" \
     -X DELETE \
     --data-binary '{}' \
     "http://127.0.0.1:8080/1.0/kb/payments/<PAYMENT_ID>"
```

5. Sync the payment methods to get the freshly created Adyen token

```
curl -v \
     -u admin:password \
     -H "X-Killbill-ApiKey: bob" \
     -H "X-Killbill-ApiSecret: lazar" \
     -H "Content-Type: application/json" \
     -H "X-Killbill-CreatedBy: demo" \
     -X POST \
     --data-binary '{}' \
    "http://127.0.0.1:8080/1.0/kb/accounts/<ACCOUNT_ID>/paymentMethods/refresh"

```

At this point, the payment method is ready for recurring payments.

Plugin properties
-----------------

| Key                      | Description                                   |
| -----------------------: | :-------------------------------------------- |
| ccNumber                 | Credit card number                            |
| ccType                   | Credit card brand                             |
| ccFirstName              | Credit card holder first name                 |
| ccLastName               | Credit card holder last name                  |
| ccExpirationMonth        | Credit card expiration month                  |
| ccExpirationYear         | Credit card expiration year                   |
| ccStartMonth             | Credit card start month                       |
| ccStartYear              | Credit card start year                        |
| ccVerificationValue      | CVC/CVV/CVN                                   |
| encryptedJson            | Encrypted JSON (EE)                           |
| dccAmount                | Payable amount                                |
| dccCurrency              | The three-character ISO currency code         |
| ddNumber                 | Direct Debit card number                      |
| ddHolderName             | Direct Debit holder name                      |
| ddBic                    | Direct Debit bank identification code (SEPA)  |
| ddBlz                    | Direct Debit Bankleitzahl (ELV)               |
| email                    | Purchaser email                               |
| address1                 | Billing address first line                    |
| address2                 | Billing address second line                   |
| city                     | Billing address city                          |
| zip                      | Billing address zip code                      |
| state                    | Billing address state                         |
| country                  | Billing address country                       |
| sepaCountryCode          | Billing address country code for SEPA requests. If absent, it will use country instead |
| PaReq                    | 3D-Secure Pa Request                          |
| PaRes                    | 3D-Secure Pa Response                         |
| MD                       | 3D-Secure Message Digest                      |
| TermUrl                  | 3D-Secure Term URL                            |
| threeDThreshold          | Minimum amount for triggering 3D-Secure       |
| userAgent                | User-Agent for 3D-Secure Browser Info         |
| acceptHeader             | Accept-Header for 3D-Secure Browser Info      |
| contAuth                 | Continuous authentication enabled (boolean)   |
| recurringDetailId        | ID of payment details stored at Adyen         |
| recurringType            | Contract to be used for Recurring             |
| createPendingPayment     | Whether to create a PENDING payment for HPP   |
| authMode                 | Create an auth instead of purchase for HPP    |
| paymentExternalKey       | HPP payment external key                      |
| acquirer                 | Value of Adyen's acquirerCode field           |
| acquirerMID              | Value of Adyen's authorisationMid field       |
| selectedBrand            | Value of Adyen's selectedBrand field          |
| lookupDirectory          | If true, query the directory (HPP flow)       |
