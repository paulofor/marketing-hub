package com.marketinghub.settings.email;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EmailSmtpMailFactory {

    public JavaMailSenderImpl create(EmailSmtpSettingsData settings) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.host());
        sender.setPort(settings.port());
        sender.setProtocol("smtp");
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        boolean authEnabled = Boolean.TRUE.equals(settings.authEnabled());
        if (authEnabled) {
            sender.setUsername(settings.username());
            sender.setPassword(settings.password());
        }
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", Boolean.toString(authEnabled));
        props.put("mail.smtp.starttls.enable", Boolean.toString(Boolean.TRUE.equals(settings.useStartTls())));
        props.put("mail.smtp.ssl.enable", Boolean.toString(Boolean.TRUE.equals(settings.useSsl())));
        applyTimeout(props, "mail.smtp.connectiontimeout", settings.connectionTimeoutMs());
        applyTimeout(props, "mail.smtp.timeout", settings.readTimeoutMs());
        applyTimeout(props, "mail.smtp.writetimeout", settings.writeTimeoutMs());
        if (!StringUtils.hasText(sender.getUsername())) {
            props.remove("mail.smtp.auth");
        }
        return sender;
    }

    private void applyTimeout(Properties props, String key, Integer value) {
        if (value == null) {
            props.remove(key);
            return;
        }
        props.put(key, Integer.toString(value));
    }
}
