This directory has been computed by extracting all the META-INF apache cxf from the jar included in the pom (cxf-common-utilities, cxf-rt-bindings-http, cxf-rt-bindings-soap, cxf-rt-core, cxf-rt-frontend-jaxws, cxf-rt-transports-http, cxf-rt-ws-security) and making sure the assembly plugin will make them available under META-INF/cxf in the jar accessible from the Felix bundle classloader.

Note that the bus-extension.txt needs to be aggregated into one file- - watch out, there are multiple entries across the various packages and ONLY including one will NOT work. Took me a long time to figure that out!!!

