package fr.solunea.thaleia.plugins.welcomev6.utils;

import fr.solunea.thaleia.model.BuyProcess;
import fr.solunea.thaleia.model.Licence;
import fr.solunea.thaleia.model.Publication;
import fr.solunea.thaleia.model.dao.ApplicationParameterDao;
import fr.solunea.thaleia.plugins.welcomev6.messages.LocalizedMessages;
import fr.solunea.thaleia.service.LicenceService;
import fr.solunea.thaleia.service.MailService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.log4j.Logger;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.resource.IResourceStream;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MailUtils {

    private static final Logger logger = Logger.getLogger(MailUtils.class);
    private final Configuration configuration;

    private final ApplicationParameterDao applicationParameterDao;

    private final String sender;
    private final MailService mailService;

    public MailUtils(Configuration configuration) throws DetailedException {
        applicationParameterDao = ThaleiaApplication.get().getApplicationParameterDao();
        this.configuration = configuration;
        sender = applicationParameterDao.getValue(Configuration.ACCOUNT_REQUEST_DESTINATION_MAILS, "");
        try {
            mailService = MailService.get();
        } catch (Exception e) {
            throw new DetailedException(e).addMessage("Impossible d'initiliser un MailUtils.");
        }
    }

    /**
     * Envoie un mail indiquant que le nombre d'inscriptions max est atteint, et qu'une personne ne peut pas s'inscrire
     * à cette publication à cause de cette limite.
     */
    public void warnForSubscriptions(Publication publication) {
        try {
            // On considère que ce mail est envoyé en français.
            // Pour améliorer, il faudrait stocker en base la dernière locale
            // utilisée pour l'IHM par le propriétaire de la publication...

            // On charge le contenu du mail
            String content = loadContent("notEnoughSubscriptionsMail.html", Locale.FRENCH);

            // Récupération des valeurs dynamiques à placer dans le mail
            String serverUrl = applicationParameterDao.getValue(Configuration.APPLICATION_PARAMETER_SERVER_URL, "");
            String legalPage = serverUrl + "/" + Configuration.LEGAL_SITE;
            String buyLicencesPage = serverUrl + configuration.getLicencesPageMountPoint();
            String subject = LocalizedMessages.getMessageForLocale("warn.subscriptions.mail.object", Locale.FRENCH,
                    (Object[]) null);
            String publicationUrl = ThaleiaApplication.get().getPublishUrl() + "/" + publication.getReference();

            String subscriptionsAuthorizedNumber = "0";
            try {
                // La licence courante du propriétaire de la publication
                LicenceService licenceService = ThaleiaSession.get().getLicenceService();
                subscriptionsAuthorizedNumber = String.valueOf(licenceService.getMaxPermittedRegistrations(publication.getUser()));
            } catch (Exception e) {
                logger.info(e);
            }

            content = content.replaceAll("\\$0", publication.getName());
            content = content.replaceAll("\\$1", publicationUrl);
            content = content.replaceAll("\\$2", subscriptionsAuthorizedNumber);
            content = content.replaceAll("\\$3", buyLicencesPage);
            content = content.replaceAll("\\$4", legalPage);

            // Envoi du mail
            mailService.send(sender, publication.getUser().getLogin(), content, subject);

        } catch (Exception e) {
            logger.warn(e);
        }
    }

    /**
     * Envoie un mail à l'administrateur lui indiquant que ce processus d'achat vient d'être exécuté.
     */
    public void warnAdmin(BuyProcess buyProcess, Licence licence, Locale locale) {
        try {
            // On considère que les mails pour l'admin sont envoyés en français.
            // On pourrait fixer ce paramètre dans la conf de l'application...

            // On charge le contenu du mail
            String content = loadContent("warnAdminMail.html", locale);

            // Récupération des valeurs dynamiques à placer dans le mail
            String serverUrl = applicationParameterDao.getValue(Configuration.APPLICATION_PARAMETER_SERVER_URL, "");
            String legalPage = serverUrl + "/" + Configuration.LEGAL_SITE;
            String subject = LocalizedMessages.getMessageForLocale("warn.admin.mail.object", Locale.FRENCH,
                    (Object[]) null);

            // Le formatteur de dates
            FastDateFormat format = FastDateFormat.getDateTimeInstance(FastDateFormat.SHORT, FastDateFormat.SHORT,
                    locale);

            // Le nom de la licence qui apparaîtra dans le paiment
            // "Licence Thaleia XXX - DDD days"
            LicenceService licenceService = ThaleiaSession.get().getLicenceService();
            int days = licence.getLicenceDurationDays();
            String licenceTitle = LocalizedMessages.getMessage("licenceTitleInPayment", licenceService.getDisplayName
                    (licence, locale), days);

            content = content.replaceAll("\\$1", serverUrl);
            content = content.replaceAll("\\$2", buyProcess.getPayer().getLogin());
            content = content.replaceAll("\\$3", buyProcess.getSkuGiven());
            content = content.replaceAll("\\$4", format.format(buyProcess.getExecutionDate()));
            content = content.replaceAll("\\$5", buyProcess.getAmount());
            content = content.replaceAll("\\$6", buyProcess.getPaymentExternalId());
            content = content.replaceAll("\\$7", licenceTitle);
            content = content.replaceAll("\\$8", legalPage);
            content = content.replaceAll("\\$9", buyProcess.getPayer().getName());
            content = content.replaceAll("\\$A", buyProcess.getPayer().getPostalAddress());

            // Envoi du mail
            mailService.send(sender, sender, content, subject);

        } catch (Exception e) {
            logger.warn(e);
        }

    }

    /**
     * @return la chaîne de caractère qui est dans le fichier portant ce nom.
     */
    private String loadContent(String filename, Locale locale) {
        try {
            PackageResourceReference resource = new PackageResourceReference(MailUtils.class, filename, locale, null,
                    null);
            IResourceStream stream = resource.getResource().getCacheableResourceStream();
            InputStream is = stream.getInputStream();
            String result = IOUtils.toString(is, StandardCharsets.UTF_8);
            stream.close();
            return result;
        } catch (Exception e) {
            logger.warn(e);
            return "";
        }
    }

    /**
     * Envoie un mail à l'utilisateur lui indiquant que ce processus d'achat vient d'être exécuté.
     */
    public void confirmBuy(BuyProcess buyProcess, Licence licence, Locale locale) {
        try {
            // On charge le contenu du mail
            String content = loadContent("confirmBuy.html", locale);

            // Récupération des valeurs dynamiques à placer dans le mail
            String serverUrl = applicationParameterDao.getValue(Configuration.APPLICATION_PARAMETER_SERVER_URL, "");
            String legalPage = serverUrl + "/" + Configuration.LEGAL_SITE;
            String subject = LocalizedMessages.getMessage("confirm.buy.mail.object", (Object[]) null);

            // Le formatteur de dates
            FastDateFormat format = FastDateFormat.getDateTimeInstance(FastDateFormat.SHORT, FastDateFormat.SHORT,
                    locale);

            // Le nom de la licence qui apparaîtra dans le paiment
            // "Licence Thaleia XXX - DDD days"
            Date now = Calendar.getInstance().getTime();
            LicenceService licenceService = ThaleiaSession.get().getLicenceService();
            int days = licence.getLicenceDurationDays();
            String licenceTitle = LocalizedMessages.getMessage("licenceTitleInPayment", licenceService.getDisplayName
                    (licence, locale), days);

            content = content.replaceAll("\\$1", serverUrl);
            content = content.replaceAll("\\$2", licenceTitle);
            content = content.replaceAll("\\$3", format.format(buyProcess.getExecutionDate()));
            content = content.replaceAll("\\$4", buyProcess.getAmount());
            content = content.replaceAll("\\$8", legalPage);

            // Envoi du mail
            mailService.send(sender, buyProcess.getPayer().getLogin(), content, subject);

        } catch (Exception e) {
            logger.warn(e);
        }

    }

}
