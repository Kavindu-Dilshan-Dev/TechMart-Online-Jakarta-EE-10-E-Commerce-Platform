package com.kavindu.techmart.ejb.mdb;

import com.kavindu.techmart.common.exception.CircuitOpenException;
import com.kavindu.techmart.ejb.session.singleton.CircuitBreakerBean;
import com.kavindu.techmart.ejb.session.singleton.SystemConfigBean;
import com.kavindu.techmart.ejb.util.JmsConstants;
import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.MessageDrivenContext;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.mail.Authenticator;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@MessageDriven(name = "EmailMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup",
                propertyValue = JmsConstants.EMAIL_QUEUE),
        @ActivationConfigProperty(propertyName = "destinationType",
                propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "maxSession",
                propertyValue = "5")
})
public class EmailMDB implements MessageListener {

    private static final Logger LOG = Logger.getLogger(EmailMDB.class.getName());
    private static final String SERVICE = "email_smtp";

    @EJB
    private SystemConfigBean systemConfig;

    @EJB
    private CircuitBreakerBean circuitBreaker;

    @Resource
    private MessageDrivenContext mdc;

    @Override
    public void onMessage(Message message) {
        String toEmail = null;
        try {
            String emailType = message.getStringProperty(JmsConstants.PROP_EMAIL_TYPE);
            toEmail = message.getStringProperty(JmsConstants.PROP_TO_EMAIL);
            String body = (message instanceof TextMessage tm) ? tm.getText() : "";
            final String recipient = toEmail;

            circuitBreaker.callWithBreaker(SERVICE, () -> {
                sendEmail(emailType, recipient, body);
                return null;
            });
        } catch (CircuitOpenException coe) {

            LOG.warning("Email circuit OPEN; will redeliver email to " + toEmail);
            mdc.setRollbackOnly();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error sending email; redelivering", e);
            mdc.setRollbackOnly();
        }
    }

    private void sendEmail(String emailType, String toEmail, String body) throws MessagingException {
        if (systemConfig.getBooleanConfig("email.simulate.failure", false)) {
            throw new IllegalStateException("Simulated SMTP failure");
        }
        if (!systemConfig.isEmailEnabled()) {
            LOG.info("[EMAIL/SIMULATED] type=" + emailType + " to=" + toEmail + " body=\"" + body + "\"");
            return;
        }

        MimeMessage msg = new MimeMessage(buildMailSession());
        msg.setFrom(new InternetAddress(systemConfig.getSmtpFrom()));
        msg.setRecipients(jakarta.mail.Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
        msg.setSubject(subjectFor(emailType), "UTF-8");
        msg.setText(body == null ? "" : body, "UTF-8");
        msg.setSentDate(new Date());

        Transport.send(msg);
        LOG.info("[EMAIL/SMTP] sent type=" + emailType + " to=" + toEmail
                + " via " + systemConfig.getSmtpHost() + ":" + systemConfig.getSmtpPort());
    }

    private Session buildMailSession() {
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", systemConfig.getSmtpHost());
        props.put("mail.smtp.port", String.valueOf(systemConfig.getSmtpPort()));
        props.put("mail.smtp.starttls.enable", String.valueOf(systemConfig.isSmtpStartTls()));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        if (systemConfig.isSmtpAuth()) {
            props.put("mail.smtp.auth", "true");
            final String user = systemConfig.getSmtpUsername();
            final String pass = systemConfig.getSmtpPassword();
            return Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });
        }
        props.put("mail.smtp.auth", "false");
        return Session.getInstance(props);
    }

    private static String subjectFor(String emailType) {
        if (emailType == null) {
            return "TechMart notification";
        }
        return switch (emailType) {
            case JmsConstants.EMAIL_ORDER_CONFIRMATION -> "Your TechMart order is confirmed";
            case JmsConstants.EMAIL_STOCK_BACK -> "An item you wanted is back in stock";
            default -> "TechMart notification";
        };
    }
}
