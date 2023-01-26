# killbill-adyen-plugin

Plugin to use [Adyen](https://www.adyen.com/) as a gateway.

Note: This plugin supports the new Adyen checkout, including Drop-in and Components. For Adyen classic integration, you can use the `0.8.0` version.


## Kill Bill compatibility

| Plugin version | Kill Bill version  | Adyen sdk version                                         | Checkout API Version|
| -------------: | -----------------: | --------------------------------------------------------: |-------------------- |
| 0.8.1         | 0.22.z             | 17.3.0 [2022-04-07](https://github.com/Adyen/adyen-java-api-library) |Version 68|



## Requirements

The plugin needs a database. The latest version of the schema can be found [here](https://github.com/killbill/killbill-adyen-plugin/tree/master/src/main/resources).

## Build

```
mvn clean install
```

## Installation

Locally:

```
kpm install_java_plugin adyen --from-source-file target/adyen-plugin-*-SNAPSHOT.jar --destination /var/tmp/bundles
```

## Configuration

1. Create a new Adyen test account by signing up [here](https://www.adyen.com/signup). 
2. Create a new merchant account as explained [here](https://docs.adyen.com/account/manage-account-structure#request-merchant-account).
3. Generate API key as explained [here](https://docs.adyen.com/development-resources/api-credentials#generate-api-key). Save the key for future reference.
4. Create a new webhook as explained [here](https://ca-test.adyen.com/ca/ca/config/showthirdparty.shtml). This includes the following steps:
    * Generate an HMAC key and store it for future use. 
    * Configure a username/password in the "Basic Authentication" section and store it for future use. 
    * Configure the server URL (This is the URL where the plugin receives notifications. Typically this should be `http://127.0.0.1:8080/plugins/adyen-plugin/notification`)
5. Configure the Adyen plugin as follows:

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
org.killbill.billing.plugin.adyen.merchantAccount=server_url_XXX
org.killbill.billing.plugin.adyen.hcmaKey=test_XXX
org.killbill.billing.plugin.adyen.captureDelayHours=XX
org.killbill.billing.plugin.adyen.password=xxx
org.killbill.billing.plugin.adyen.username=xxx ' \
     http://127.0.0.1:8080/1.0/kb/tenants/uploadPluginConfig/adyen-plugin
```

## Testing

1. Build, install and configure the plugin as explained above.
2. Start the AdyenTestUI demo. Specify amount as `20`. 
3. Verify that a new account is created in Kill Bill with a `$20` payment in `PENDING` status.

## Plugin Internals

This plugin implementation uses [Adyen Web Drop-in](https://docs.adyen.com/online-payments/web-drop-in). It creates the first payment via a servlet using the `/sessions` endpoint as explained [here](https://docs.adyen.com/online-payments/web-drop-in#create-payment-session). If the payment is recurring, we store the token generated by Adyen so that it can be used multiples times on `/payments` as explained [here](https://docs.adyen.com/online-payments/tokenization/create-and-use-tokens#pay-one-off). After generating the session, the component (UI Drop-in) can be used to send the payment. Adyen will process the received payment and inform the plugin/killbill the result of said payment via a notification. The notification URL needs to be configured in Adyen as explained above.

## Integration

The following steps need to be followed in order to use the Adyen plugin:

1. Ensure that the plugin is installed and configured as explained above.

2. Create a Kill Bill account and Kill Bill Payment (Specify a `PluginProperty` corresponding to `enableRecurring` if this is going to be a recurrinng payment. The default value of this property is `false`):

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
    -d '{ "accountId": "2ad52f53-85ae-408a-9879-32a7e59dd03d", "pluginName": "adyen-plugin" ,"isDefault": true, "pluginInfo": { "isDefaultPaymentMethod": true, "properties": [ { "key": "enableRecurring", "value": "true", "isUpdatable": false } }' \
    "http://127.0.0.1:8080/1.0/kb/accounts/8785164f-b5d7-4da1-9495-33f5105e8d80/paymentMethods" 
```
2. Call `/plugins/adyen-plugin/checkout` to generate a session (Note that the amount needs needs to be specified in [minor units](https://docs.adyen.com/development-resources/currency-codes)):

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
This returns `sessionId` and `sessionData`. 

3. Set up a drop-in with the `sessionId` and `sessionData` obtained above as explained [here](https://docs.adyen.com/online-payments/web-drop-in#set-up).