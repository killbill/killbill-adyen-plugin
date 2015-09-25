[![Build Status](https://travis-ci.org/killbill/killbill-adyen-plugin.svg?branch=master)](https://travis-ci.org/killbill/killbill-adyen-plugin)

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

Requirements
------------

The plugin needs a database. The latest version of the schema can be found [here](https://github.com/killbill/killbill-adyen-plugin/blob/master/src/main/resources/ddl.sql).

Configuration
-------------

The following properties are required:

* `org.killbill.billing.plugin.adyen.merchantAccount`: your merchant account(s)
* `org.killbill.billing.plugin.adyen.username`: your username(s)
* `org.killbill.billing.plugin.adyen.password`: your password(s)
* `org.killbill.billing.plugin.adyen.paymentUrl`: SOAP Payment service url (i.e. `https://pal-test.adyen.com/pal/servlet/soap/Payment` or `https://pal-live.adyen.com/pal/servlet/soap/Payment`)

The format for the merchant account(s), username(s) and password(s) is `XX#YY|XX#YY|...` where:

* `XX` is the country code (DE, FR, etc.)
* `YY` is the value (merchant account, username of the form `ws@Company.[YourCompanyAccount]` or password)

If you have a single country, omit the country code part.

To configure Hosted Payment Pages (HPP):

* `org.killbill.billing.plugin.adyen.hpp.target`: host payment page url (e.g. https://test.adyen.com/hpp/pay.shtml)
* `org.killbill.billing.plugin.adyen.hmac.secret`: your hmac secret(s)
* `org.killbill.billing.plugin.adyen.skin`: you skin code(s)

The format for secrets and skins is the same as above if you support multiple countries.

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
             "key": "ccType",
             "value": "sepadirectdebit"
           },
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

### ELV

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
             "key": "ccType",
             "value": "elv"
           },
           {
             "key": "ddHolderName",
             "value": "Bill Killson"
           },
           {
             "key": "ddNumber",
             "value": "1234567890"
           },
           {
             "key": "ddBlz",
             "value": "12345678"
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
           "key": "paymentProviderType",
           "value": "CREDITCARD"
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
* Change *paymentProviderType* to the type of payment method in the HPP (see [PaymentType.java](https://github.com/killbill/killbill-adyen-plugin/blob/master/src/main/java/org/killbill/billing/plugin/adyen/client/model/PaymentType.java))
* At this point, no payment has been created in Kill Bill. The payment will be recorded when processing the notification

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
| PaReq                    | 3D-Secure Pa Request                          |
| MD                       | 3D-Secure Message Digest                      |
| TermUrl                  | 3D-Secure Term URL                            |
