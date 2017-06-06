package org.subethamail.smtp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.subethamail.util.ExtendedTrustManager;

import com.sun.mail.util.MailSSLSocketFactory;

class TestUtil {

    private static final String PASSWORD = "password";

    static final String LOCALHOST = "127.0.0.1";
    static final String EMAIL_TO = "me@gmail.com";
    static final String EMAIL_FROM = "fred@gmail.com";
    static final int PORT = 25000;

    static SSLContext createTlsSslContext(KeyManager[] keyManagers, TrustManager[] trustManagers)
            throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, new java.security.SecureRandom());
        return sslContext;
    }

    static TrustManager[] getTrustManagers() {
        InputStream trustStore = StartTLSFullTest.class.getResourceAsStream("/trustStore.jks");
        TrustManager trustManager = new ExtendedTrustManager(trustStore, PASSWORD.toCharArray(), false);
        TrustManager[] trustManagers = new TrustManager[] { trustManager };
        return trustManagers;
    }

    static KeyManager[] getKeyManagers() throws KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException {
        InputStream keyStore = StartTLSFullTest.class.getResourceAsStream("/keyStore.jks");
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(keyStore, PASSWORD.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, PASSWORD.toCharArray());
        KeyManager[] keyManagers = kmf.getKeyManagers();
        return keyManagers;
    }

    static enum ConnectionType {
        START_TLS, PURE_TLS;
    }

    static void send(TrustManager[] trustManagers, ConnectionType connectionType) throws Exception {
        String to = EMAIL_TO;
        String from = EMAIL_FROM;
        String host = LOCALHOST;
        Properties props = new Properties();
        props.put("mail.debug", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", PORT + "");
        if (connectionType == ConnectionType.START_TLS) {
            props.put("mail.smtp.starttls.enable", "true");
        } else {
            props.put("mail.smtp.ssl.enable", "true");
        }
        // props.put("mail.transport.protocol", "smtp");
        // props.put("mail.smtp.starttls.required", "true");
        // props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        MailSSLSocketFactory sslSocketFactory = new MailSSLSocketFactory("TLSv1.2");
        sslSocketFactory.setTrustManagers(trustManagers);
        props.put("mail.smtp.ssl.socketFactory", sslSocketFactory);

        Session session = Session.getInstance(props);
        // Create a default MimeMessage object.
        Message message = new MimeMessage(session);

        // Set From: header field of the header.
        message.setFrom(new InternetAddress(from));

        InternetAddress[] toAddresses = InternetAddress.parse(to);

        // Set To: header field of the header.
        message.setRecipients(Message.RecipientType.TO, toAddresses);

        // Set Subject: header field
        message.setSubject("Testing Subject " + "\u2191");

        // Create the message part
        BodyPart messageBodyPart = new MimeBodyPart();

        // Now set the actual message
        messageBodyPart.setText("This is message body");

        // Create a multipar message
        Multipart multipart = new MimeMultipart();

        // Set text message part
        multipart.addBodyPart(messageBodyPart);

        // Part two is attachment
        messageBodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(new File("src/test/resources/man.png"));
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName("man" + "\u2191" + ".png");
        multipart.addBodyPart(messageBodyPart);

        // Send the complete message parts
        message.setContent(multipart);

        Transport.send(message);
        System.out.println("Sent message successfully....");
    }

}
