package com.example.etereatesis;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailSender {

    private String email;
    private String password;
    private Session session;

    public MailSender(String email, String password) {
        this.email = email;
        this.password = password;

        Properties props = new Properties();
        // Configuración para el servidor SMTP de Yahoo
        props.put("mail.smtp.host", "smtp.mail.yahoo.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        // Se utiliza Session.getDefaultInstance para obtener la sesión
        session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, password);
            }
        });
    }

    /**
     * Envía un correo con el asunto y cuerpo especificados.
     *
     * @param subject El asunto del correo.
     * @param body El contenido del correo.
     * @param from La dirección de correo remitente.
     * @param to La dirección de correo destinatario.
     * @throws MessagingException Si ocurre algún error al enviar el correo.
     */
    public void sendMail(String subject, String body, String from, String to) throws MessagingException {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        // Agrega el destinatario; para múltiples destinatarios se puede usar un array
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject);
        message.setText(body);
        Transport.send(message);
    }
}
