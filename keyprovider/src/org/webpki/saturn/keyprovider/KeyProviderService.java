/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.webpki.saturn.keyprovider;

import java.io.IOException;
import java.io.InputStream;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;

import java.security.PublicKey;

import java.security.cert.X509Certificate;

import java.util.Enumeration;
import java.util.Vector;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.CustomCryptoProvider;

import org.webpki.json.JSONDecoderCache;

import org.webpki.keygen2.CredentialDiscoveryResponseDecoder;
import org.webpki.keygen2.InvocationResponseDecoder;
import org.webpki.keygen2.KeyCreationResponseDecoder;
import org.webpki.keygen2.ProvisioningFinalizationResponseDecoder;
import org.webpki.keygen2.ProvisioningInitializationResponseDecoder;

import org.webpki.net.HTTPSWrapper;

import org.webpki.util.ArrayUtil;
import org.webpki.util.Base64;
import org.webpki.util.MIMETypedObject;

import org.webpki.saturn.common.KeyStoreEnumerator;
import org.webpki.saturn.common.PayerAccountTypes;

import org.webpki.webutil.InitPropertyReader;

public class KeyProviderService extends InitPropertyReader implements ServletContextListener {

    static Logger logger = Logger.getLogger(KeyProviderService.class.getCanonicalName());
    
    static final String LOGOTYPE              = "logotype";

    static final String VERSION_CHECK         = "version_check";

    static final String KEYSTORE_PASSWORD     = "key_password";

    static final String BANK_HOST             = "bank_host";
    
    static final String KEYPROV_HOST          = "keyprov_host";

    static final String KEYPROV_KMK           = "keyprov_kmk";
    
    static final String SERVER_PORT_MAP       = "server_port_map";
    
    static final String[] CREDENTIALS         = {"paycred1", "paycred2", "paycred3"};
    
    static final String BOUNCYCASTLE_FIRST    = "bouncycastle_first";

    static KeyStoreEnumerator keyManagemenentKey;
    
    static String keygen2EnrollmentUrl;
    
    static String successImageAndMessage;
    
    static Integer serverPortMapping;

    static JSONDecoderCache keygen2JSONCache;
    
    static X509Certificate tlsCertificate;

    static String grantedVersions[];

    static class PaymentCredential {
        KeyStoreEnumerator signatureKey;
        String accountType;
        String accountId;
        boolean cardFormatted;
        String authorityUrl;
        MIMETypedObject cardImage;
        PublicKey encryptionKey;
    }

    static Vector<PaymentCredential> paymentCredentials = new Vector<PaymentCredential>();

    public static String logotype;

    InputStream getResource(String name) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(name);
        if (is == null) {
            throw new IOException("Resource fail for: " + name);
        }
        return is;
    }

    String getURL (String inUrl) throws IOException {
        URL url = new URL(inUrl);
        if (!url.getHost().equals("localhost")) {
            return inUrl;
        }
        String autoHost = null;
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        int foundAddresses = 0;
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            if (networkInterface.isUp() && !networkInterface.isVirtual() && !networkInterface.isLoopback() &&
                networkInterface.getDisplayName().indexOf("VMware") < 0) {  // Well.... 
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress instanceof Inet4Address) {
                        foundAddresses++;
                        autoHost = inetAddress.getHostAddress();
                    }
                }
            }
        }
        if (foundAddresses != 1) throw new IOException("Couldn't determine network interface");
        logger.info("Host automagically set to: " + autoHost);
        return new URL(url.getProtocol(),
                       autoHost,
                       url.getPort(),
                       url.getFile()).toExternalForm();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        initProperties (event);
        try {
            CustomCryptoProvider.forcedLoad(getPropertyBoolean(BOUNCYCASTLE_FIRST));

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Logotype
            ////////////////////////////////////////////////////////////////////////////////////////////
            logotype = new String(
                ArrayUtil.getByteArrayFromInputStream(getResource(getPropertyString(LOGOTYPE))), "UTF-8");

                ////////////////////////////////////////////////////////////////////////////////////////////
            // Optional check
            ////////////////////////////////////////////////////////////////////////////////////////////
            if (getPropertyString(VERSION_CHECK).length() != 0) {
                grantedVersions = getPropertyStringList(VERSION_CHECK);
            }
 
            ////////////////////////////////////////////////////////////////////////////////////////////
            // Show a sign that the user succeeded getting Saturn credentials
            ////////////////////////////////////////////////////////////////////////////////////////////
            successImageAndMessage = new StringBuffer("<img src=\"data:image/png;base64,")
                .append(new Base64(false).getBase64StringFromBinary(
                    ArrayUtil.getByteArrayFromInputStream(
                        event.getServletContext().getResourceAsStream("/images/paywith-saturn.png"))))
                .append("\" title=\"Payment Credentials\"><br>&nbsp;" +
                        "<br><b>Enrollment Succeeded!</b>").toString();

            ////////////////////////////////////////////////////////////////////////////////////////////
            // KeyGen2
            ////////////////////////////////////////////////////////////////////////////////////////////
            keygen2JSONCache = new JSONDecoderCache ();
            keygen2JSONCache.addToCache (InvocationResponseDecoder.class);
            keygen2JSONCache.addToCache (ProvisioningInitializationResponseDecoder.class);
            keygen2JSONCache.addToCache (CredentialDiscoveryResponseDecoder.class);
            keygen2JSONCache.addToCache (KeyCreationResponseDecoder.class);
            keygen2JSONCache.addToCache (ProvisioningFinalizationResponseDecoder.class);

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Credentials
            ////////////////////////////////////////////////////////////////////////////////////////////
            String bankHost = getPropertyString(BANK_HOST);
            for (String credentialEntry : CREDENTIALS) {
                final String[] arguments = getPropertyStringList(credentialEntry);
                PaymentCredential paymentCredential = new PaymentCredential();
                paymentCredentials.add(paymentCredential);
                paymentCredential.authorityUrl = bankHost + "/" + arguments[5] + "/authority";
                paymentCredential.signatureKey =
                    new KeyStoreEnumerator(getResource(arguments[0]),
                                           getPropertyString(KEYSTORE_PASSWORD));
                paymentCredential.accountType = PayerAccountTypes.valueOf(arguments[1]).getTypeUri();
                boolean cardFormatted = true;
                if (arguments[2].charAt(0) == '!') {
                    cardFormatted = false;
                    arguments[2] = arguments[2].substring(1);
                }
                paymentCredential.accountId = arguments[2];
                paymentCredential.cardFormatted = cardFormatted;
                paymentCredential.cardImage = new MIMETypedObject() {
                    @Override
                    public byte[] getData() throws IOException {
                        return ArrayUtil.getByteArrayFromInputStream(getResource(arguments[3]));
                    }
                    @Override
                    public String getMimeType() throws IOException {
                        return "image/svg+xml";
                    }
                };
                paymentCredential.encryptionKey =
                    CertificateUtil.getCertificateFromBlob(
                        ArrayUtil.getByteArrayFromInputStream(getResource(arguments[4]))).getPublicKey();
            }


            ////////////////////////////////////////////////////////////////////////////////////////////
            // SKS key management key
            ////////////////////////////////////////////////////////////////////////////////////////////
            keyManagemenentKey = new KeyStoreEnumerator(getResource(getPropertyString(KEYPROV_KMK)),
                                                                    getPropertyString(KEYSTORE_PASSWORD));

            if (getPropertyString(SERVER_PORT_MAP).length () > 0) {
                serverPortMapping = getPropertyInt(SERVER_PORT_MAP);
            }

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Get KeyGen2 protocol entry
            ////////////////////////////////////////////////////////////////////////////////////////////
            keygen2EnrollmentUrl = getURL(getPropertyString(KEYPROV_HOST)) + "/getkeys";

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Get TLS server certificate (if necessary)
            ////////////////////////////////////////////////////////////////////////////////////////////
            if (keygen2EnrollmentUrl.startsWith("https")) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            HTTPSWrapper wrapper = new HTTPSWrapper();
                            String url = keygen2EnrollmentUrl;
                            wrapper.setRequireSuccess(false);
                            if (serverPortMapping != null) {
                                URL url2 = new URL(url);
                                url = new URL(url2.getProtocol(),
                                              url2.getHost(),
                                              serverPortMapping,
                                              url2.getFile()).toExternalForm();
                            }
                            wrapper.makeGetRequest(url);
                            tlsCertificate = wrapper.getServerCertificate();
                            logger.info("TLS cert: " + tlsCertificate.getSubjectX500Principal().getName());
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, "********\n" + e.getMessage() + "\n********", e);
                        }
                    }
                }.start();
            }
            
            logger.info("Saturn KeyProvider-server initiated");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "********\n" + e.getMessage() + "\n********", e);
        }
    }

    static boolean isDebug() {
        return true;
    }
}
