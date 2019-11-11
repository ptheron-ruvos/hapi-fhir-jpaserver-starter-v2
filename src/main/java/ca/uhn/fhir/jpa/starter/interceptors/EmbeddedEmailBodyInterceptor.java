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
import java.util.List;

/**
 * Looks for an extension on the resource that triggered a subscription that indicates
 * the body of the email should be something specific to the resource, not something generically
 * set by the configuration of the server.
 */
@Interceptor
public class EmbeddedEmailBodyInterceptor {

  public static final String EXT_SUBSCRIPTION_EMAIL_BODY = "http://jamesagnew.github.io/hapi-fhir/StructureDefinition/ext-subscription-email-body";

  private static String getExtensionValue(List<? extends IBaseExtension<?, ?>> extensions) {
    if (extensions != null) {
      for (IBaseExtension extension : extensions) {
        if (extension.getUrl().equals(EXT_SUBSCRIPTION_EMAIL_BODY)) {
          IPrimitiveType<String> extensionValue = (IPrimitiveType<String>) extension.getValue();

          if (extensionValue != null) {
            return extensionValue.getValue();
          }
        }
      }
    }

    return null;
  }

  @Hook(value = Pointcut.SUBSCRIPTION_BEFORE_EMAIL_DELIVERY)
  public boolean handleBeforeEmailDelivery(CanonicalSubscription subscription, ResourceDeliveryMessage theMessage, EmailDetails emailDetails) {
    try {
      String emailBody = null;

      // For most resources, can check if it extends from IBaseHasExtensions
      if (theMessage.getResource() instanceof IBaseHasExtensions) {
        emailBody = getExtensionValue(((IBaseHasExtensions) theMessage.getResource()).getExtension());
      } else if (theMessage.getResource() instanceof org.hl7.fhir.dstu3.model.Bundle) {
        // For DSTU3 Bundle resources
        org.hl7.fhir.dstu3.model.Bundle dstu3Bundle = (org.hl7.fhir.dstu3.model.Bundle) theMessage.getResource();

        // Only check the first entry in the bundle
        if (dstu3Bundle.getEntry() != null && dstu3Bundle.getEntry().size() > 0 && dstu3Bundle.getEntry().get(0).getResource() != null) {
          org.hl7.fhir.dstu3.model.Resource theResource = dstu3Bundle.getEntry().get(0).getResource();

          if (theResource instanceof IBaseHasExtensions) {
            emailBody = getExtensionValue(((IBaseHasExtensions) theResource).getExtension());
          }
        }
      } else if (theMessage.getResource() instanceof org.hl7.fhir.r4.model.Bundle) {
        // For R4 Bundle resources
        org.hl7.fhir.r4.model.Bundle r4Bundle = (org.hl7.fhir.r4.model.Bundle) theMessage.getResource();

        // Only check the first entry in the bundle
        if (r4Bundle.getEntry() != null && r4Bundle.getEntry().size() > 0 && r4Bundle.getEntry().get(0).getResource() != null) {
          org.hl7.fhir.r4.model.Resource theResource = r4Bundle.getEntry().get(0).getResource();

          if (theResource instanceof IBaseHasExtensions) {
            emailBody = getExtensionValue(((IBaseHasExtensions) theResource).getExtension());
          }
        }
      } else if (theMessage.getResource() instanceof org.hl7.fhir.r5.model.Bundle) {
        // For R5 Bundle resources
        org.hl7.fhir.r5.model.Bundle r5Bundle = (org.hl7.fhir.r5.model.Bundle) theMessage.getResource();

        // Only check the first entry in the bundle
        if (r5Bundle.getEntry() != null && r5Bundle.getEntry().size() > 0 && r5Bundle.getEntry().get(0).getResource() != null) {
          org.hl7.fhir.r5.model.Resource theResource = r5Bundle.getEntry().get(0).getResource();

          if (theResource instanceof IBaseHasExtensions) {
            emailBody = getExtensionValue(((IBaseHasExtensions) theResource).getExtension());
          }
        }
      }

      if (emailBody != null && !emailBody.isEmpty()) {
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

        emailDetails.setBodyTemplate(emailBody);
      }
    } catch (Exception ex) {
      // Do nothing
    }

    return true;
  }
}
