// src/main/java/com/screenleads/backend/app/application/service/EmailService.java
package com.screenleads.backend.app.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * Enviar email de recuperación de contraseña
     */
    public void sendPasswordResetEmail(String toEmail, String userName, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Recuperación de Contraseña - ScreenLeads");

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            String htmlContent = buildPasswordResetEmailTemplate(userName, resetLink);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Error al enviar el correo de recuperación de contraseña", e);
        }
    }

    /**
     * Template HTML para el email de reset de contraseña
     */
    private String buildPasswordResetEmailTemplate(String userName, String resetLink) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Recuperación de Contraseña</title>
                </head>
                <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
                        <tr>
                            <td align="center">
                                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                                    <!-- Header -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 40px 20px; text-align: center;">
                                            <h1 style="color: #ffffff; margin: 0; font-size: 28px;">ScreenLeads</h1>
                                            <p style="color: #ffffff; margin: 10px 0 0 0; font-size: 16px; opacity: 0.9;">Dashboard</p>
                                        </td>
                                    </tr>

                                    <!-- Body -->
                                    <tr>
                                        <td style="padding: 40px 30px;">
                                            <h2 style="color: #333333; margin: 0 0 20px 0; font-size: 24px;">Recuperación de Contraseña</h2>

                                            <p style="color: #666666; line-height: 1.6; margin: 0 0 20px 0; font-size: 16px;">
                                                Hola <strong>%s</strong>,
                                            </p>

                                            <p style="color: #666666; line-height: 1.6; margin: 0 0 20px 0; font-size: 16px;">
                                                Hemos recibido una solicitud para restablecer la contraseña de tu cuenta.
                                                Haz clic en el botón de abajo para crear una nueva contraseña:
                                            </p>

                                            <table width="100%%" cellpadding="0" cellspacing="0" style="margin: 30px 0;">
                                                <tr>
                                                    <td align="center">
                                                        <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;">
                                                            Restablecer Contraseña
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>

                                            <p style="color: #666666; line-height: 1.6; margin: 0 0 10px 0; font-size: 14px;">
                                                O copia y pega este enlace en tu navegador:
                                            </p>

                                            <p style="color: #667eea; line-height: 1.6; margin: 0 0 20px 0; font-size: 14px; word-break: break-all;">
                                                %s
                                            </p>

                                            <div style="background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0;">
                                                <p style="color: #856404; margin: 0; font-size: 14px; line-height: 1.5;">
                                                    <strong>⚠️ Importante:</strong> Este enlace expirará en <strong>1 hora</strong> por razones de seguridad.
                                                </p>
                                            </div>

                                            <p style="color: #666666; line-height: 1.6; margin: 20px 0 0 0; font-size: 14px;">
                                                Si no solicitaste restablecer tu contraseña, puedes ignorar este correo de forma segura.
                                            </p>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td style="background-color: #f8f9fa; padding: 20px 30px; text-align: center; border-top: 1px solid #e9ecef;">
                                            <p style="color: #6c757d; margin: 0; font-size: 12px; line-height: 1.5;">
                                                © 2025 ScreenLeads. Todos los derechos reservados.
                                            </p>
                                            <p style="color: #6c757d; margin: 10px 0 0 0; font-size: 12px;">
                                                Este es un correo automático, por favor no respondas a este mensaje.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
                .formatted(userName, resetLink, resetLink);
    }

    /**
     * Método genérico para enviar emails HTML
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Error al enviar el correo electrónico", e);
        }
    }
}
