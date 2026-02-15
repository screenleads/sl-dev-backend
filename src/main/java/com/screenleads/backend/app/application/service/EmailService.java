// src/main/java/com/screenleads/backend/app/application/service/EmailService.java
package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.NotificationChannel;
import com.screenleads.backend.app.domain.model.NotificationTemplate;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.service.NotificationTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final NotificationTemplateService notificationTemplateService;

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
     * Enviar email de invitación de usuario
     */
    public void sendUserInvitationEmail(String toEmail, String inviterName, String companyName, 
                                       String roleName, String token, String customMessage) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(inviterName + " te invita a unirte a " + companyName + " en ScreenLeads");

            String acceptLink = frontendUrl + "/accept-invitation?token=" + token;
            String htmlContent = buildInvitationEmailTemplate(toEmail, inviterName, companyName, 
                                                              roleName, acceptLink, customMessage);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("User invitation email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send invitation email to: {}", toEmail, e);
            throw new RuntimeException("Error al enviar el correo de invitación", e);
        }
    }

    /**
     * Template HTML para el email de invitación de usuario
     */
    private String buildInvitationEmailTemplate(String email, String inviterName, String companyName,
                                               String roleName, String acceptLink, String customMessage) {
        String messageSection = "";
        if (customMessage != null && !customMessage.isBlank()) {
            messageSection = """
                    <div style="background-color: #f8f9fa; padding: 15px; border-radius: 8px; margin: 20px 0;">
                        <p style="margin: 0; color: #495057; font-style: italic;">
                            <strong>Mensaje del invitador:</strong><br>
                            "%s"
                        </p>
                    </div>
                    """.formatted(customMessage);
        }

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px; text-align: center; border-radius: 10px 10px 0 0;">
                        <h1 style="color: white; margin: 0; font-size: 28px;">📧 Invitación a ScreenLeads</h1>
                    </div>
                    
                    <div style="background-color: #ffffff; padding: 30px; border: 1px solid #e0e0e0; border-top: none; border-radius: 0 0 10px 10px;">
                        <p style="font-size: 16px; margin-bottom: 20px;">Hola,</p>
                        
                        <p style="font-size: 16px; margin-bottom: 20px;">
                            <strong>%s</strong> te ha invitado a unirte a <strong>%s</strong> en ScreenLeads.
                        </p>
                        
                        %s
                        
                        <div style="background-color: #e3f2fd; padding: 20px; border-radius: 8px; margin: 25px 0;">
                            <p style="margin: 0 0 10px 0; color: #1976d2;"><strong>📧 Email de registro:</strong></p>
                            <p style="margin: 0 0 15px 0; font-size: 16px; font-weight: bold;">%s</p>
                            
                            <p style="margin: 0 0 10px 0; color: #1976d2;"><strong>🎭 Rol asignado:</strong></p>
                            <p style="margin: 0; font-size: 16px; font-weight: bold;">%s</p>
                        </div>
                        
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" 
                               style="display: inline-block; padding: 15px 40px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); 
                                      color: white; text-decoration: none; border-radius: 8px; font-weight: bold; 
                                      font-size: 16px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                                ✅ Aceptar Invitación
                            </a>
                        </div>
                        
                        <div style="background-color: #fff3e0; padding: 15px; border-radius: 8px; margin-top: 25px;">
                            <p style="margin: 0; font-size: 14px; color: #e65100;">
                                ⚠️ <strong>Importante:</strong> Debes registrarte con el email <strong>%s</strong>
                            </p>
                        </div>
                        
                        <p style="font-size: 14px; color: #666; margin-top: 25px;">
                            Este enlace expira en 7 días. Si no solicitaste esta invitación, puedes ignorar este email.
                        </p>
                        
                        <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 30px 0;">
                        
                        <p style="font-size: 14px; color: #999; text-align: center; margin: 0;">
                            Si el botón no funciona, copia y pega este enlace en tu navegador:<br>
                            <a href="%s" style="color: #667eea; word-break: break-all;">%s</a>
                        </p>
                        
                        <p style="font-size: 12px; color: #999; text-align: center; margin-top: 20px;">
                            © 2026 ScreenLeads. Todos los derechos reservados.
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(inviterName, companyName, messageSection, email, roleName, 
                            acceptLink, email, acceptLink, acceptLink);
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

    /**
     * Enviar email de bienvenida al nuevo usuario
     * Busca una plantilla personalizada de bienvenida o usa una plantilla por defecto
     */
    public void sendWelcomeEmail(User user) {
        try {
            log.info("Sending welcome email to user: {} ({})", user.getUsername(), user.getEmail());
            
            // Intentar obtener plantilla personalizada de la compañía
            List<NotificationTemplate> templates = notificationTemplateService
                    .getTemplatesByCompanyAndChannel(user.getCompany().getId(), NotificationChannel.EMAIL);
            
            NotificationTemplate welcomeTemplate = templates.stream()
                    .filter(t -> t.getName().equalsIgnoreCase("WELCOME_EMAIL") || 
                                 t.getName().contains("Bienvenida") ||
                                 t.getName().contains("Welcome"))
                    .findFirst()
                    .orElse(null);
            
            String subject;
            String htmlContent;
            
            if (welcomeTemplate != null) {
                // Usar plantilla personalizada
                log.info("Using custom welcome template: {}", welcomeTemplate.getName());
                
                Map<String, String> variables = new HashMap<>();
                variables.put("userName", user.getName());
                variables.put("userFullName", user.getName() + " " + user.getLastName());
                variables.put("userEmail", user.getEmail());
                variables.put("companyName", user.getCompany().getName());
                variables.put("roleName", user.getRole().getDescription() != null ? 
                    user.getRole().getDescription() : user.getRole().getRole());
                variables.put("dashboardUrl", frontendUrl);
                
                subject = notificationTemplateService.renderTemplateSubject(welcomeTemplate, variables);
                
                // Si la plantilla tiene HTML body, usarlo; sino usar el body normal
                String bodyTemplate = welcomeTemplate.getHtmlBody() != null ? 
                        welcomeTemplate.getHtmlBody() : welcomeTemplate.getBody();
                htmlContent = renderVariables(bodyTemplate, variables);
                
                // Incrementar contador de uso de la plantilla
                notificationTemplateService.incrementUsageCount(welcomeTemplate.getId());
            } else {
                // Usar plantilla predeterminada
                log.info("Using default welcome template");
                subject = "¡Bienvenido a " + user.getCompany().getName() + " en ScreenLeads!";
                htmlContent = buildDefaultWelcomeEmailTemplate(user);
            }
            
            // Enviar el email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", user.getEmail());
            
        } catch (Exception e) {
            // No lanzar excepción para no bloquear el registro del usuario
            // Solo loguear el error
            log.error("Failed to send welcome email to: {} - Error: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * Renderizar variables en formato {{variable}} dentro de un texto
     */
    private String renderVariables(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            result = result.replace(placeholder, entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    /**
     * Enviar email de bienvenida a nuevo cliente (consumidor final)
     * Busca una plantilla personalizada de bienvenida para clientes o usa una plantilla por defecto
     */
    public void sendCustomerWelcomeEmail(com.screenleads.backend.app.domain.model.Customer customer, Long companyId) {
        try {
            log.info("Sending customer welcome email to: {} ({})", customer.getFirstName(), customer.getEmail());
            
            // Intentar obtener plantilla personalizada de la compañía
            List<NotificationTemplate> templates = notificationTemplateService
                    .getTemplatesByCompanyAndChannel(companyId, NotificationChannel.EMAIL);
            
            NotificationTemplate welcomeTemplate = templates.stream()
                    .filter(t -> t.getName().equalsIgnoreCase("CUSTOMER_WELCOME_EMAIL") || 
                                 t.getName().contains("Bienvenida Cliente") ||
                                 t.getName().contains("Customer Welcome"))
                    .findFirst()
                    .orElse(null);
            
            String subject;
            String htmlContent;
            
            if (welcomeTemplate != null) {
                // Usar plantilla personalizada
                log.info("Using custom customer welcome template: {}", welcomeTemplate.getName());
                
                Map<String, String> variables = new HashMap<>();
                variables.put("customerName", customer.getFirstName());
                variables.put("customerFullName", 
                    (customer.getFirstName() != null ? customer.getFirstName() : "") + " " + 
                    (customer.getLastName() != null ? customer.getLastName() : ""));
                variables.put("customerEmail", customer.getEmail());
                variables.put("customerPhone", customer.getPhone() != null ? customer.getPhone() : "");
                variables.put("frontendUrl", frontendUrl);
                
                subject = notificationTemplateService.renderTemplateSubject(welcomeTemplate, variables);
                
                // Si la plantilla tiene HTML body, usarlo; sino usar el body normal
                String bodyTemplate = welcomeTemplate.getHtmlBody() != null ? 
                        welcomeTemplate.getHtmlBody() : welcomeTemplate.getBody();
                htmlContent = renderVariables(bodyTemplate, variables);
                
                // Incrementar contador de uso de la plantilla
                notificationTemplateService.incrementUsageCount(welcomeTemplate.getId());
            } else {
                // Usar plantilla predeterminada para clientes
                log.info("Using default customer welcome template");
                subject = "¡Bienvenido a ScreenLeads!";
                htmlContent = buildDefaultCustomerWelcomeEmailTemplate(customer);
            }
            
            // Enviar el email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(customer.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Customer welcome email sent successfully to: {}", customer.getEmail());
            
        } catch (Exception e) {
            // No lanzar excepción para no bloquear el registro del cliente
            // Solo loguear el error
            log.error("Failed to send customer welcome email to: {} - Error: {}", 
                     customer.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * Renderizar variables en formato {{variable}} dentro de un texto
     */
    private String renderVariables(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            result = result.replace(placeholder, entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    /**
     * Plantilla HTML predeterminada para email de bienvenida de clientes
     */
    private String buildDefaultCustomerWelcomeEmailTemplate(com.screenleads.backend.app.domain.model.Customer customer) {
        String customerName = customer.getFirstName() != null ? customer.getFirstName() : "Cliente";
        
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Bienvenido</title>
                </head>
                <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
                        <tr>
                            <td align="center">
                                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                                    <!-- Header -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 50px 20px; text-align: center;">
                                            <h1 style="color: #ffffff; margin: 0; font-size: 32px;">🎉 ¡Bienvenido!</h1>
                                            <p style="color: #ffffff; margin: 15px 0 0 0; font-size: 18px; opacity: 0.95;">Gracias por registrarte</p>
                                        </td>
                                    </tr>

                                    <!-- Body -->
                                    <tr>
                                        <td style="padding: 40px 30px;">
                                            <h2 style="color: #333333; margin: 0 0 20px 0; font-size: 24px;">Hola, %s 👋</h2>

                                            <p style="color: #666666; line-height: 1.6; margin: 0 0 20px 0; font-size: 16px;">
                                                ¡Nos alegra tenerte con nosotros! Tu registro ha sido completado exitosamente 
                                                y ya puedes disfrutar de todas nuestras promociones y ofertas especiales.
                                            </p>

                                            <!-- Benefits Card -->
                                            <div style="background: linear-gradient(135deg, #f8f9ff 0%%, #f0f4ff 100%%); border-left: 4px solid #667eea; padding: 20px; border-radius: 8px; margin: 25px 0;">
                                                <h3 style="color: #667eea; margin: 0 0 15px 0; font-size: 18px;">✨ Beneficios de ser cliente</h3>
                                                <ul style="color: #666; line-height: 2; margin: 0; padding-left: 20px; font-size: 15px;">
                                                    <li>Acceso a promociones exclusivas</li>
                                                    <li>Descuentos especiales en comercios asociados</li>
                                                    <li>Notificaciones de ofertas cercanas a tu ubicación</li>
                                                    <li>Acumulación de puntos por tus canjes</li>
                                                    <li>Recompensas por fidelidad</li>
                                                </ul>
                                            </div>

                                            <p style="color: #666666; line-height: 1.6; margin: 25px 0 20px 0; font-size: 16px;">
                                                Mantente atento a tus notificaciones para no perderte ninguna oferta.
                                            </p>

                                            <!-- Info Section -->
                                            <div style="background-color: #e8f5e9; border-left: 4px solid #4caf50; padding: 15px; border-radius: 8px; margin-top: 25px;">
                                                <p style="margin: 0; color: #2e7d32; font-size: 14px; line-height: 1.6;">
                                                    <strong>💡 ¿Necesitas ayuda?</strong><br>
                                                    Si tienes alguna pregunta, no dudes en contactarnos. Estamos aquí para ayudarte.
                                                </p>
                                            </div>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td style="background-color: #f8f9fa; padding: 30px; text-align: center; border-top: 1px solid #e9ecef;">
                                            <p style="color: #6c757d; margin: 0 0 10px 0; font-size: 14px;">
                                                <strong>ScreenLeads</strong> - Tu plataforma de promociones
                                            </p>
                                            <p style="color: #6c757d; margin: 0; font-size: 12px; line-height: 1.5;">
                                                © 2026 ScreenLeads. Todos los derechos reservados.
                                            </p>
                                            <p style="color: #adb5bd; margin: 10px 0 0 0; font-size: 11px;">
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
                .formatted(customerName);
    }

    /**
     * Plantilla HTML predeterminada para email de bienvenida
     */
    private String buildDefaultWelcomeEmailTemplate(User user) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Bienvenido a ScreenLeads</title>
                </head>
                <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
                        <tr>
                            <td align="center">
                                <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                                    <!-- Header -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 50px 20px; text-align: center;">
                                            <h1 style="color: #ffffff; margin: 0; font-size: 32px;">🎉 ¡Bienvenido!</h1>
                                            <p style="color: #ffffff; margin: 15px 0 0 0; font-size: 18px; opacity: 0.95;">Tu cuenta ha sido creada exitosamente</p>
                                        </td>
                                    </tr>

                                    <!-- Body -->
                                    <tr>
                                        <td style="padding: 40px 30px;">
                                            <h2 style="color: #333333; margin: 0 0 20px 0; font-size: 24px;">Hola, %s 👋</h2>

                                            <p style="color: #666666; line-height: 1.6; margin: 0 0 20px 0; font-size: 16px;">
                                                Te damos la bienvenida a <strong style="color: #667eea;">%s</strong> en ScreenLeads.
                                                Tu cuenta ha sido configurada y ya puedes empezar a usar nuestra plataforma.
                                            </p>

                                            <!-- User Info Card -->
                                            <div style="background: linear-gradient(135deg, #f8f9ff 0%%, #f0f4ff 100%%); border-left: 4px solid #667eea; padding: 20px; border-radius: 8px; margin: 25px 0;">
                                                <h3 style="color: #667eea; margin: 0 0 15px 0; font-size: 18px;">📋 Información de tu cuenta</h3>
                                                <table width="100%%" cellpadding="8" cellspacing="0">
                                                    <tr>
                                                        <td style="color: #666; font-size: 14px; width: 40%%;">
                                                            <strong>Usuario:</strong>
                                                        </td>
                                                        <td style="color: #333; font-size: 14px;">
                                                            %s
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td style="color: #666; font-size: 14px;">
                                                            <strong>Email:</strong>
                                                        </td>
                                                        <td style="color: #333; font-size: 14px;">
                                                            %s
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td style="color: #666; font-size: 14px;">
                                                            <strong>Empresa:</strong>
                                                        </td>
                                                        <td style="color: #333; font-size: 14px;">
                                                            %s
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td style="color: #666; font-size: 14px;">
                                                            <strong>Rol:</strong>
                                                        </td>
                                                        <td style="color: #333; font-size: 14px;">
                                                            %s
                                                        </td>
                                                    </tr>
                                                </table>
                                            </div>

                                            <p style="color: #666666; line-height: 1.6; margin: 25px 0 20px 0; font-size: 16px;">
                                                Accede al dashboard para comenzar a gestionar tus campañas y analizar los resultados:
                                            </p>

                                            <!-- CTA Button -->
                                            <table width="100%%" cellpadding="0" cellspacing="0" style="margin: 30px 0;">
                                                <tr>
                                                    <td align="center">
                                                        <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: #ffffff; text-decoration: none; padding: 16px 45px; border-radius: 8px; font-size: 16px; font-weight: bold; box-shadow: 0 4px 6px rgba(102, 126, 234, 0.4);">
                                                            🚀 Ir al Dashboard
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>

                                            <!-- Features Section -->
                                            <div style="background-color: #f8f9fa; padding: 25px; border-radius: 8px; margin: 30px 0;">
                                                <h3 style="color: #333; margin: 0 0 15px 0; font-size: 18px;">✨ Qué puedes hacer en ScreenLeads:</h3>
                                                <ul style="color: #666; line-height: 2; margin: 0; padding-left: 20px; font-size: 15px;">
                                                    <li>Gestionar y rastrear campañas de marketing</li>
                                                    <li>Analizar el comportamiento de tus visitantes</li>
                                                    <li>Capturar y gestionar leads en tiempo real</li>
                                                    <li>Crear notificaciones personalizadas</li>
                                                    <li>Generar reportes y análisis detallados</li>
                                                </ul>
                                            </div>

                                            <!-- Help Section -->
                                            <div style="background-color: #e8f5e9; border-left: 4px solid #4caf50; padding: 15px; border-radius: 8px; margin-top: 25px;">
                                                <p style="margin: 0; color: #2e7d32; font-size: 14px; line-height: 1.6;">
                                                    <strong>💡 ¿Necesitas ayuda?</strong><br>
                                                    Si tienes alguna pregunta o necesitas asistencia, no dudes en contactar con tu administrador o visitar nuestra documentación.
                                                </p>
                                            </div>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td style="background-color: #f8f9fa; padding: 30px; text-align: center; border-top: 1px solid #e9ecef;">
                                            <p style="color: #6c757d; margin: 0 0 10px 0; font-size: 14px;">
                                                <strong>ScreenLeads</strong> - Tu plataforma de gestión de leads
                                            </p>
                                            <p style="color: #6c757d; margin: 0; font-size: 12px; line-height: 1.5;">
                                                © 2026 ScreenLeads. Todos los derechos reservados.
                                            </p>
                                            <p style="color: #adb5bd; margin: 10px 0 0 0; font-size: 11px;">
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
                .formatted(
                    user.getName(),
                    user.getCompany().getName(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getCompany().getName(),
                    user.getRole().getDescription() != null ? 
                        user.getRole().getDescription() : user.getRole().getRole(),
                    frontendUrl
                );
    }
}
