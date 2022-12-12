package fr.solunea.thaleia.plugins.welcomev6.customization;

import fr.solunea.thaleia.model.CustomizationFile;
import fr.solunea.thaleia.model.dao.CustomizationFileDao;
import fr.solunea.thaleia.plugins.welcomev6.panels.MenuPanel;
import fr.solunea.thaleia.plugins.welcomev6.utils.PanelUtils;
import fr.solunea.thaleia.service.utils.ZipUtils;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.BasePage;
import fr.solunea.thaleia.webapp.pages.ThaleiaV6MenuPage;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.panels.UploadFormPanelArchive;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.time.Duration;

import java.io.File;
import java.util.List;

/**
 * Une page de personnalisation graphique des contenus. Permet de récupérer et
 * de modifier le ZIP qui sera utilisé pour la personnalisation graphique des
 * contenus.
 */
@SuppressWarnings("serial")
public class GraphicCustomizationPage extends ThaleiaV6MenuPage {

    WebMarkupContainer colUpload;
    WebMarkupContainer colDownload;
    Component feedbackPanel;

    /**
     * @param graphicCustomizationName        Le nom de la personnalisation Cannelle utilisée pour
     *                                        personnaliser un fichier (customizationFile.name) à
     *                                        stocker/rechercher en base..
     * @param defaultGraphicCustomizationFile le nom de la resource (dans le jar) qui contient le fichier
     *                                        Zip de personnalisation par défaut.
     * @param returnPage                      la page de destination du bouton retour
     */
    public GraphicCustomizationPage(String graphicCustomizationName, String defaultGraphicCustomizationFile,
                                    WebPage returnPage, ICustomizationValidator customizationValidator) {
        super();

        // Menu du plugin
        add(new MenuPanel("pluginMenu", returnPage,
                new StringResourceModel("menuLabel", returnPage, null)));

        // Feedback
        feedbackPanel = new ThaleiaFeedbackPanel("feedbackPanel").setOutputMarkupId(true);
        add(feedbackPanel);

        // Le lien de téléchargement du modèle de personnalisation
        // On suppose que la personnalisation est la même en FR et EN.
        // Ce fichier est dans cannelle-resources, à la racine de
        // Resources_Cannelle
        add(PanelUtils.getDownloadLink("downloadDefault", Model.of(defaultGraphicCustomizationFile)).add(new Image(
                "ico_action_export", new PackageResourceReference(BasePage.class, "/img/ico_action_export.png"))));

        // La div de téléchargement de la personnalisation actuelle
        colDownload = (WebMarkupContainer) new WebMarkupContainer("colDownload") {
            @Override
            public boolean isVisible() {
                // On recherche les personnalisation Cannelle existantes dans le domaine de sécurité de l'utilisateur.
                List<CustomizationFile> customizationFiles = new CustomizationFileDao(ThaleiaSession.get().getContextService().getContextSingleton())
                        .find(graphicCustomizationName, null, ThaleiaSession.get().getAuthenticatedUser().getDomain());

                return !customizationFiles.isEmpty();
            }
        }.setOutputMarkupPlaceholderTag(true).setOutputMarkupId(true);

        // Le lien de téléchargement de la personnalisation actuelle
        DownloadLink dlLink = (DownloadLink) new DownloadLink("downloadBtn", new AbstractReadOnlyModel<>() {

            @Override
            public File getObject() {
                // On recherche un fichier de personnalisation Cannelle
                // existant dans le domaine de sécurité de
                // l'utilisateur.
                // Ceci peut renvoyer null : on suppose que le lien de
                // téléchargement n'est pas présenté en cas d'absence de
                // personnalisation.
                return getCustomizationFile(graphicCustomizationName);
            }

        }) {
        }.setCacheDuration(Duration.NONE).setOutputMarkupId(true);
        colDownload.add(dlLink.setOutputMarkupId(true));
        add(colDownload);

        dlLink.add(new Image("ico_action_export2",
                new PackageResourceReference(BasePage.class, "/img/ico_action_export.png")));

        // Le lien d'upload
        colUpload = new WebMarkupContainer("colUpload");
        colUpload.setOutputMarkupId(true);
        try {
            // On associe un fichier temporaire pour récolter le
            // binaire uploadé
            final UploadFormPanelArchive uploadPanel = new UploadFormPanelArchive("uploadForm",
                    Model.of(ThaleiaApplication.get().getTempFilesService().getTempFile()), true) {
                @Override
                public void onUpload(File uploadedFile, String filename, AjaxRequestTarget target) {

                    try {
                        // Si ce n'est pas une archive, on couine
                        if (!ZipUtils.isAnArchive(uploadedFile)) {
                            error(new StringResourceModel("upload.notanarchive", GraphicCustomizationPage.this, null,
                                    new Object[]{filename}).getString());
                            throw new DetailedException("Le fichier reçu n'est pas une archive.");
                        }

                        // On valide
                        try {
                            customizationValidator.validate(uploadedFile);
                        } catch (DetailedException e) {
                            error(new StringResourceModel("invalid.customization", GraphicCustomizationPage.this, null,
                                    new Object[]{e.getMessage()}).getString());
                            throw e;
                        }

                        // On enregistre le fichier uploadé comme la nouvelle personnalisation pour le domaine de sécurité de l'utilisateur
                        ObjectContext tempContext = ThaleiaSession.get().getContextService().getNewContext();
                        ThaleiaSession.get().getCustomizationFilesService().setCustomizationFile(
                                graphicCustomizationName, null, ThaleiaSession.get().getAuthenticatedUser().getDomain(),
                                uploadedFile, filename, tempContext);
                        tempContext.commitChanges();

                        info(new StringResourceModel("upload.ok", GraphicCustomizationPage.this, null,
                                new Object[]{filename}).getString());

                        target.add(feedbackPanel);
                        target.add(colDownload);

                    } catch (DetailedException e) {
                        logger.warn("Erreur d'enregistrement du fichier de personnalisation :" + e);
                        StringResourceModel errorMessageModel = new StringResourceModel("upload.error",
                                GraphicCustomizationPage.this, null);
                        Session.get().error(errorMessageModel.getString());
                        target.add(feedbackPanel);
                    }
                }
            };
            colUpload.add(uploadPanel.setOutputMarkupId(true));

        } catch (Exception e) {
            logger.warn("Impossible de générer un fichier temporaire pour l'upload d'une personnalisation : " + e);
            colUpload.add(new EmptyPanel("uploadForm").setOutputMarkupId(true));
        }
        add(colUpload);

    }

    /**
     * @return le fichier de personnalisation Cannelle existant dans le domaine
     * de sécurité de l'utilisateur, ou null s'il n'y en a pas.
     */
    private File getCustomizationFile(String customizationName) {
        // On recherche un fichier de personnalisation Cannelle
        // existant dans le domaine de sécurité de
        // l'utilisateur.
        File customizationFile;
        try {
            customizationFile = ThaleiaSession.get().getCustomizationFilesService().getCustomizationFile(
                    customizationName, null, ThaleiaSession.get().getAuthenticatedUser().getDomain(),
                    ThaleiaSession.get().getAuthenticatedUser().getObjectContext());
        } catch (DetailedException e) {
            logger.warn(e);
            return null;
        }
        return customizationFile;
    }

    public static class SelectOption {
        private String key;
        private String value;

        public SelectOption(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

}
