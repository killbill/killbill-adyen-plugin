# killbill-adyen-plugin
![Maven Central](https://img.shields.io/maven-central/v/org.kill-bill.billing.plugin.java/adyen-plugin?color=blue&label=Maven%20Central)

Plugin to use [Adyen](https://www.adyen.com/) as a gateway.

A full end-to-end integration demo is available [here](https://github.com/killbill/killbill-adyen-demo).

## Kill Bill compatibility

| Plugin version | Kill Bill version |
| -------------: | ----------------: |
| 0.1.y          | 0.14.z            |
| 0.2.y          | 0.15.z            |
| 0.3.y          | 0.16.z            |
| 0.4.y          | 0.17.z            |
| 0.5.y          | 0.18.z            |
| 0.6.y          | 0.19.z            |
| 0.7.y          | 0.20.z            |
| 0.8.y          | 0.22.z            |
| 0.9.y          | 0.22.z            |
| 0.10.y          | 0.24.z            |

**Note:** 
Version `0.8.0` of the plugin uses Adyen classic integration while version `0.9.0` supports the new Adyen checkout, including Drop-in and Components.  It uses [17.3.0 2022-04-07](https://github.com/Adyen/adyen-java-api-library) version of the Adyen SDK and version 68 of the Checkout API.


## Requirements

The plugin needs a database and does not create its tables automatically. Before starting the plugin for the first time, run the respective DDL on the Kill Bill database:

* MySQL: [ddl.sql](https://github.com/killbill/killbill-adyen-plugin/blob/master/src/main/resources/ddl.sql)
* PostgreSQL: [ddl-postgresql.sql](https://github.com/killbill/killbill-adyen-plugin/blob/master/src/main/resources/ddl-postgresql.sql)

## Build

```
mvn clean install
```

## Installation

Locally:

```
kpm install_java_plugin adyen --from-source-file target/adyen-plugin-*-SNAPSHOT.jar --destination /var/tmp/bundles
```

## Setup

In order to use the plugin, Adyen credentials are required. You can create a test Adyen account and obtain credentials as explained below:

1. Create a new Adyen test account by signing up [here](https://www.adyen.com/signup). 
2. Create a new merchant account as explained [here](https://docs.adyen.com/account/manage-account-structure#request-merchant-account).
3. Generate API key as explained [here](https://docs.adyen.com/development-resources/api-credentials#generate-api-key). Save the key for future reference.
4. Create a new webhook as explained [here](https://docs.adyen.com/development-resources/webhooks#set-up-notifications-in-your-customer-area). This includes the following steps:
    * Generate an HMAC key and store it for future use. 
    * Configure a username/password in the "Basic Authentication" section and store it for future use. 
    * Configure the server URL (This is the URL where the plugin receives notifications. Typically this should be `http://<KillBill_URL>/plugins/adyen-plugin/notification`)


## Configuration

1. Configure the Adyen plugin with the Adyen credentials obtained above as follows:

```bash
curl -v \
     -X POST \
     -u admin:password \
     -H 'X-Killbill-ApiKey: bob' \
     -H 'X-Killbill-ApiSecret: lazar' \
     -H 'X-Killbill-CreatedBy: admin' \
     -H 'Content-Type: text/plain' \
     -d 'org.killbill.billing.plugin.adyen.apiKey=test_XXX
org.killbill.billing.plugin.adyen.returnUrl=test_XXX
org.killbill.billing.plugin.adyen.merchantAccount=account_XXX
org.killbill.billing.plugin.adyen.hcmaKey=test_XXX
org.killbill.billing.plugin.adyen.captureDelayHours=XX
org.killbill.billing.plugin.adyen.enviroment=TEST
org.killbill.billing.plugin.adyen.password=xxx
org.killbill.billing.plugin.adyen.username=xxx ' \
     http://127.0.0.1:8080/1.0/kb/tenants/uploadPluginConfig/adyen-plugin
```

Where:
* apiKey: API Key generated at step 3
* returnUrl: URL the shopper is sent back to after the Drop-in payment (passed to Adyen's `/sessions` as `returnUrl`). This is your application's URL, not the notification URL
* merchantAccount: Merchant account created at step 2
* hcmaKey: HMAC Key generated at step 4a
* captureDelayHours: Desire capture delay in hours after Authorize , number must be between 0 - 168 hr
* environment: Environment to use. Possible values are `TEST`/`LIVE`. default value is `TEST`
* username / password: a Kill Bill user login (for example `admin` / `password`). The `/checkout` servlet uses these to authenticate against Kill Bill when it creates the first payment. They are not the webhook Basic Authentication credentials from step 4b; the plugin does not check those

## Payment status after an Adyen notification

Payments and refunds are recorded as `PENDING` when they are sent to Adyen. When Adyen's notification arrives (for example `AUTHORISATION` or `REFUND`), the plugin:

1. Validates the HMAC signature and updates the matching transaction in its own tables.
2. Stores the recurring token on the payment method, if Adyen sent one.
3. For a successful notification, asks Kill Bill core to reconcile the payment right away, so the transaction moves from `PENDING` to `SUCCESS` without waiting for the Janitor.

Failed notifications, and successful ones whose immediate reconciliation fails (the error is logged), are handled by the Kill Bill Janitor. Leaving failures to the Janitor keeps the payment retry schedule of invoice payments. By default the Janitor checks `PENDING` transactions 1 hour after the payment and again 1 day after that. The delays can be shortened with the `org.killbill.payment.janitor.pending.retries` property, for example:

```
org.killbill.payment.janitor.pending.retries=5m,15m,1h,1d
```

See the [payment user guide](https://docs.killbill.io/latest/userguide_payment.html) for details.

## Testing


### Test Setup

Since Adyen uses webhooks, the `http://<IP_ADDR>/plugins/adyen-plugin/notification` URL needs to be publicly accessible for Adyen to send notifications to it. Thus, testing can be done in one of the following ways:

* **Using AWS**: Test on Kill Bill running on an AWS instance. In this case, you would need to specify the AWS URL (`http://<AWS_INSTANCE_ADDR>:8080/plugins/adyen-plugin/notification`) as the webhook server URL in Adyen.
* **Using ngrok**: If you would like to test on a local installation instead of AWS, you can use a tool like [ngrok](https://ngrok.com/) to create a temporary public DNS and redirect traffic from that DNS to the local Kill Bill server. Ngrok can be set up as follows:
    * Install ngrok from [here](https://ngrok.com/download). 
    * Open a terminal window and type `ngrok http 8080` (Since Kill Bill listens on port `8080`)
    * This will display a message like `Forwarding https://39dc-117-195-30-48.in.ngrok.io -> http://localhost:8080`
    * Copy the URL displayed above (`https://39dc-117-195-30-48.in.ngrok.io`).  In this case, you would need to specify the ngrok URL (`<your-ngrok-url>/plugins/adyen-plugin/notification`) as the webhook server URL in Adyen.

### Testing
1. Build, install and configure the plugin as explained above. Note that either your AWS URL or ngrok URL needs to be specified as the webhook server URL in Adyen.
2. Test the application using the [Kill Bill Adyen Demo](https://github.com/killbill/killbill-adyen-demo) as explained [here](https://github.com/killbill/killbill-adyen-demo/#test).
3. Verify that a new account is created in Kill Bill with a payment in `PENDING` status.
4. If all goes well, Adyen would then send a notification and the plugin converts the `PENDING` payment status to `SUCCESS` in Kill Bill. 
5. Retrieve the payment as follows (`withPluginInfo=true` also triggers the [on-the-fly Janitor](https://docs.killbill.io/latest/userguide_payment.html#_on_the_fly_janitor), which is useful if the notification was not received):

```
curl \
    -u admin:password \
    -H 'X-Killbill-ApiKey: bob' \
    -H 'X-Killbill-ApiSecret: lazar' \
    -H 'Accept: application/json' \
    'http://127.0.0.1:8080/1.0/kb/payments/<paymentId>?withPluginInfo=true' 
```
6. Verify that the payment status is `SUCCESS`.

## Plugin Internals

This plugin implementation uses [Adyen Web Drop-in](https://docs.adyen.com/online-payments/web-drop-in). It creates the first payment via a servlet using the `/sessions` endpoint as explained [here](https://docs.adyen.com/online-payments/web-drop-in#create-payment-session). If the payment is recurring, it stores the token generated by Adyen so that it can be used multiples times on `/payments` as explained [here](https://docs.adyen.com/online-payments/tokenization/create-and-use-tokens#pay-one-off). After generating the session, the component (UI Drop-in) can be used to send the payment. Adyen will process the received payment and inform the plugin/killbill the result of said payment via a notification. The notification URL needs to be configured in Adyen as explained above.

## Integration

The following steps need to be followed in order to use the Adyen plugin:

1. Ensure that the plugin is installed and configured as explained above.

2. [Create](https://killbill.github.io/slate/?shell#account-create-an-account) a Kill Bill account and save the `accountId` for further use.

3. Create a Kill Bill Payment Method. Specify a `PluginProperty` corresponding to `enableRecurring` if this is going to be a recurring payment (the default value of this property is `false`). Set `isDefault` to `true` if Kill Bill should use this payment method to pay invoices automatically:

```bash
curl -v \
    -X POST \
    -u admin:password \
    -H "X-Killbill-ApiKey: bob" \
    -H "X-Killbill-ApiSecret: lazar" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -H "X-Killbill-CreatedBy: demo" \
    -H "X-Killbill-Reason: demo" \
    -H "X-Killbill-Comment: demo" \
    -d '{
  			"accountId": "2ad52f53-85ae-408a-9879-32a7e59dd03d",
  			"pluginName": "adyen-plugin",
  			"isDefault": true,
  			"pluginInfo": {
    			"isDefaultPaymentMethod": true,
    			"properties": [
      				{
        				"key": "enableRecurring",
        				"value": "true",
        				"isUpdatable": false
      				}
    			]
  			}
		}' \
    	"http://127.0.0.1:8080/1.0/kb/accounts/8785164f-b5d7-4da1-9495-33f5105e8d80/paymentMethods" 
```
4. Call `/plugins/adyen-plugin/checkout` to generate a session and create the first payment (Note that the amount needs needs to be specified in [minor units](https://docs.adyen.com/development-resources/currency-codes)):

```bash
curl -v \
     -X POST \
     -u admin:password \
     -H "X-Killbill-ApiKey: bob" \
     -H "X-Killbill-ApiSecret: lazar" \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -H "X-Killbill-CreatedBy: demo" \
     -H "X-Killbill-Reason: demo" \
     -H "X-Killbill-Comment: demo" \
     "http://127.0.0.1:8080/plugins/adyen-plugin/checkout?kbAccountId=<KB_ACCOUNT_ID>&amount=<amount>&kbPaymentMethodId=<KB_PAYMENT_METHOD_ID>"
```

Note that this creates a payment in Kill Bill for the specified amount in `PENDING` status. 

5. Set up a drop-in with the `sessionId` and `sessionData` obtained above as explained [here](https://docs.adyen.com/online-payments/web-drop-in#set-up).

6. Collect customer's payment details via the drop-in. 

7. If the payment is successful, Adyen sends a notification to Kill Bill; the plugin stores the recurring token (if `enableRecurring` was set) and moves the payment from `PENDING` to `SUCCESS`. Payments initiated by Kill Bill afterwards (for example for subscription invoices) use the stored token automatically.

## Payment methods tokenised outside the plugin

If the card is tokenised in your own Adyen `/sessions` or `/payments` call (for example before the
Kill Bill account exists), pass the token when creating the Kill Bill payment method:

* `recurringDetailReference`: the Adyen token (`storedPaymentMethodId`)
* `shopperReference`: the `shopperReference` used when Adyen stored the token

```json
"pluginInfo": {
  "properties": [
    { "key": "recurringDetailReference", "value": "K4C5QX3VQL9C5G65" },
    { "key": "shopperReference", "value": "customer-42" }
  ]
}
```

The payment method is marked as recurring and payments initiated by Kill Bill (for example for
subscription invoices) use the token right away; no first payment through `/checkout` is needed.
Adyen only accepts a token together with the `shopperReference` it was stored under: without
`shopperReference`, the plugin sends the Kill Bill account id and Adyen refuses the payment
(`800 Contract not found`).


## Credits

Code originally developed by [Wovenware](https://www.wovenware.com/).
