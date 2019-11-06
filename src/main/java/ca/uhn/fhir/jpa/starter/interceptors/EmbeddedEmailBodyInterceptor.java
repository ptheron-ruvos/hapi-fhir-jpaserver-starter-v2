package ca.uhn.fhir.jpa.starter.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.subscription.module.CanonicalSubscription;
import ca.uhn.fhir.jpa.subscription.module.subscriber.ResourceDeliveryMessage;
import ca.uhn.fhir.jpa.subscription.module.subscriber.email.EmailDetails;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import javax.mail.BodyPart;
import javax.mail.internet.MimeBodyPart;

/**
 * Looks for an extension on the resource that triggered a subscription that indicates
 * the body of the email should be something specific to the resource, not something generically
 * set by the configuration of the server.
 */
@Interceptor
public class EmbeddedEmailBodyInterceptor {

  public static final String EXT_SUBSCRIPTION_EMAIL_BODY = "http://jamesagnew.github.io/hapi-fhir/StructureDefinition/ext-subscription-email-body";

  @Hook(value = Pointcut.SUBSCRIPTION_BEFORE_EMAIL_DELIVERY)
  public boolean handleBeforeEmailDelivery(CanonicalSubscription subscription, ResourceDeliveryMessage theMessage, EmailDetails emailDetails) {
    try {
      if (theMessage.getResource() instanceof IBaseHasExtensions) {
        IBaseHasExtensions theResource = (IBaseHasExtensions) theMessage.getResource();

        if (theResource.getExtension() != null) {
          for (IBaseExtension extension : theResource.getExtension()) {
            if (extension.getUrl().equals(EXT_SUBSCRIPTION_EMAIL_BODY)) {
              if (emailDetails.getAttachments().size() == 0) {
                BodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.setContent(emailDetails.getBodyTemplate(), emailDetails.getBodyContentType());

                if (emailDetails.getBodyContentType().endsWith("/xml")) {
                  attachmentPart.setFileName("resource.xml");
                } else if (emailDetails.getBodyContentType().endsWith("/json")) {
                  attachmentPart.setFileName("resource.json");
                }

                emailDetails.getAttachments().add(attachmentPart);
              }

              IPrimitiveType<String> value = (IPrimitiveType<String>) extension.getValue();
              emailDetails.setBodyTemplate(value.getValue());
            }
          }
        }
      }
    } catch (Exception ex) {
      // Do nothing
    }

    return true;
  }
}
