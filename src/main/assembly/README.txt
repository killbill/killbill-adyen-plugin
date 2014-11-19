This directory has been computed by extracted all the META-INF apache cxf from the jar included in the pom (cxf-common-utilities, cxf-rt-bindings-http, cxf-rt-bindings-soap, cxf-rt-core, cxf-rt-frontend-jaxws, cxf-rt-transports-http, cxf-rt-ws-security) and making sure the assembly plugin will make them available under META-INF/cxf in the jar accessible from the Felix bundle classloader.

Note that the bus-exetnsion.txt needs to be aggregated into one file-- watch out, there are multiple entries acroos the various package and ONLY incluing one will NOT work. Took me a long time to figure that out!!!

