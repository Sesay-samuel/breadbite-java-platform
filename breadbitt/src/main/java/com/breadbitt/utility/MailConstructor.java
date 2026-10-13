
package com.breadbitt.utility;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.breadbitt.domain.Order;
import com.breadbitt.domain.User;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Component
public class MailConstructor {

	@Autowired
	private Environment env;

	@Autowired
	private TemplateEngine templateEngine;

	public SimpleMailMessage constructResetTokenEmail(
			String contextPath,
			Locale locale,
			String token,
			User user,
			String password) {

		// Keep the existing method signature for compatibility.
		// Never include the password in the email.
		return constructPasswordSetupEmail(
				contextPath, token, user, false);
	}

	public SimpleMailMessage constructPasswordSetupEmail(
			String contextPath,
			String token,
			User user,
			boolean registration) {

		String baseUrl = contextPath.replaceAll("/+$", "");

		String url = baseUrl + "/resetPassword?token=" + token;

		SimpleMailMessage email = new SimpleMailMessage();

		email.setTo(user.getEmail());

		email.setSubject(
				registration
						? "Salone Breadbitt - Set Your Password"
						: "Salone Breadbitt - Reset Your Password");

		email.setText(
				"Hello " + user.getUsername() + ",\n\n"
						+ (registration
								? "Welcome to Salone Breadbitt!\n\n"
										+ "Use the following link to set your password:\n"
								: "We received a request to reset your password.\n\n"
										+ "Use the following link to choose a new password:\n")
						+ url
						+ "\n\nThis link expires in 15 minutes "
						+ "and can be used only once.\n\n"
						+ "If you did not request this, you can ignore this email.\n\n"
						+ "Salone Breadbitt Support");

		email.setFrom(env.getRequiredProperty("support.email"));

		return email;
	}

	public MimeMessagePreparator constructOrderConfirmationEmail(
			User user,
			Order order,
			Locale locale) {

		Context context = new Context(locale);

		context.setVariable("order", order);
		context.setVariable("user", user);
		context.setVariable(
				"cartItemList", order.getCartItemList());

		String text = templateEngine.process(
				"orderConfirmationEmailTemplate", context);

		return new MimeMessagePreparator() {

			@Override
			public void prepare(MimeMessage mimeMessage)
					throws Exception {

				MimeMessageHelper email = new MimeMessageHelper(mimeMessage);

				email.setTo(user.getEmail());

				email.setSubject(
						"Order Confirmation - " + order.getId());

				email.setText(text, true);

				email.setFrom(
						new InternetAddress(
								env.getRequiredProperty("support.email")));
			}
		};
	}
}