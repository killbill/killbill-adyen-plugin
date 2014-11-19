# killbill-adyen-plugin

## What is it?

This is a Kill Bill plugin to connect to Adyen (to be used as a payment gateway)

## Status

Currently this is just a partial implementation, that was used to demo the use of 3DS call on top of Adyen.

However, the heavy lifting work required to make OSGI work with org.apache.cxf (used to provide all the glue to make SOAP calls) is working correctly. At this stage this is just a matter to implement the remaing calls (credit, capture, similarly to what has been done to authorization).

## Configuration

Build the plugin using `mvn clean install`

Add the configuration parameters for Adyen plugin:
* org.killbill.billing.plugin.adyen.merchantAccounts=...
* org.killbill.billing.plugin.adyen.userNames=...
* org.killbill.billing.plugin.adyen.passwords=...
* org.killbill.billing.plugin.adyen.paymentUrl=...



Then install it under `<PLUGIN_INSTALL>/plugins/java/killbill-adyen/<VERSION>`, where:
* `PLUGIN_INSTALL` is by default `/var/tmp/bundles` or the value of the system property `org.killbill.osgi.bundle.install.dir`
* `VERSION` is a string that should match the version of your plugin

For e.g: `/var/tmp/bundles/plugins/java/killbill-adyen/0.0.1-SNAPSHOT/adyen-plugin-0.0.1-SNAPSHOT-jar-with-dependencies.jar`

## Start KIll Bill

You can use `./bin/start-server -s` from the main [killbill repo](https://github.com/killbill/killbill), or use the [executable war] (http://killbill.io/downloads/)

