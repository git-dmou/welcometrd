package fr.solunea.thaleia.plugins.welcomev6.customization;

import fr.solunea.thaleia.model.CustomizationFile;
import fr.solunea.thaleia.model.dao.CustomizationFileDao;
import fr.solunea.thaleia.plugins.welcomev6.utils.PanelUtils;
import fr.solunea.thaleia.service.utils.ZipUtils;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.BasePage;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.panels.UploadFormPanelArchiveV6;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.time.Duration;

import java.io.File;
import java.util.List;

public class CustomizationPanel extends Panel {

    protected static final Logger logger = Logger.getLogger(BasePage.class);
    WebMarkupContainer colDownload;
    private String id;
    private List<CustomizationPropertyProposal> customizationProperties;
    private WebPage returnPage;
    private String graphicCustomizationName;
    private String defaultGraphicCustomizationFile;
    private String defaultScormValue;
    private ICustomizationValidator customizationValidator;
    private ThaleiaFeedbackPanel feedbackPanel;

    /**
     * @param customizationProperties         la liste des propriétés (de type String) personnalisables. Si les valeurs
     *                                        sont nulles ou vides, alors la personnalisation sera un champ texte.
     * @param returnPage                      la page de retour en sortie de la page de personnalisation graphique.
     * @param graphicCustomizationName        le nom de la propriété de personnalisation graphique.
     * @param defaultGraphicCustomizationFile le nom du fichier de personnalisation graphique par défaut, en tant que
     *                                        ressource dans le jar.
     * @param defaultScormValue               La valeur par défaut de version SCORM des exports. Si null, on ne propose
     *                                        pas de sélecteur.
     */
    public CustomizationPanel(String id, List<CustomizationPropertyProposal> customizationProperties, WebPage returnPage,
                              String graphicCustomizationName, String defaultGraphicCustomizationFile, String defaultScormValue,
                              ICustomizationValidator customizationValidator) {
        super(id);

        setId(id);
        setCustomizationProperties(customizationProperties);
        setReturnPage(returnPage);
        setGraphicCustomizationName(graphicCustomizationName);
        setDefaultGraphicCustomizationFile(defaultGraphicCustomizationFile);
        setDefaultScormValue(defaultScormValue);
        setCustomizationValidator(customizationValidator);

        addDownloadActiveCustomization();
        addUploadYourCustomization();
    }

    @Override
    public String getId() {
        return id;
    }

    private void setId(String id) {
        this.id = id;
    }

    public List<CustomizationPropertyProposal> getCustomizationProperties() {
        return customizationProperties;
    }

    private void setCustomizationProperties(List<CustomizationPropertyProposal> customizationProperties) {
        this.customizationProperties = customizationProperties;
    }

    public WebPage getReturnPage() {
        return returnPage;
    }

    private void setReturnPage(WebPage returnPage) {
        this.returnPage = returnPage;
    }

    private void setGraphicCustomizationName(String graphicCustomizationName) {
        this.graphicCustomizationName = graphicCustomizationName;
    }

    private void setDefaultGraphicCustomizationFile(String defaultGraphicCustomizationFile) {
        this.defaultGraphicCustomizationFile = defaultGraphicCustomizationFile;
    }

    private void setDefaultScormValue(String defaultScormValue) {
        this.defaultScormValue = defaultScormValue;
    }

    public ICustomizationValidator getCustomizationValidator() {
        return customizationValidator;
    }

    private void setCustomizationValidator(ICustomizationValidator customizationValidator) {
        this.customizationValidator = customizationValidator;
    }

    /**
     * @param customizationProperties         la liste des propriétés (de type String) personnalisables. Si les valeurs
     *                                        sont nulles ou vides, alors la personnalisation sera un champ texte.
     * @param returnPage                      la page de retour en sortie de la page de personnalisation graphique.
     * @param graphicCustomizationName        le nom de la propriété de personnalisation graphique.
     * @param defaultGraphicCustomizationFile le nom du fichier de personnalisation graphique par défaut, en tant que
     *                                        ressource dans le jar.
     * @param defaultScormValue               La valeur par défaut de version SCORM des exports. Si null, on ne propose
     *                                        pas de sélecteur.
     */
    public CustomizationPanel (String id, List<CustomizationPropertyProposal> customizationProperties, WebPage returnPage,
                               String graphicCustomizationName, String defaultGraphicCustomizationFile, String defaultScormValue,
                               ICustomizationValidator customizationValidator, ThaleiaFeedbackPanel feedbackPanel) {
        super(id);

        this.feedbackPanel = feedbackPanel;

        setId(id);
        setCustomizationProperties(customizationProperties);
        setReturnPage(returnPage);
        setGraphicCustomizationName(graphicCustomizationName);
        setDefaultGraphicCustomizationFile(defaultGraphicCustomizationFile);
        setDefaultScormValue(defaultScormValue);
        setCustomizationValidator(customizationValidator);

        addDownloadActiveCustomization();
        addUploadYourCustomization();
    }



    /**
     * Ajoute le téléchargement de la personnalisation active sous réserve,que celle-ci existe.
     */
    private void addDownloadActiveCustomization() {
        // L'élément de téléchargement de la personnalisation actuelle
        colDownload = (WebMarkupContainer) new WebMarkupContainer("downloadActiveCustomizationHTMLElement") {
            @Override
            public boolean isVisible() {
                // On recherche les personnalisation Cannelle existantes
                // dans le domaine de sécurité de l'utilisateur.
                List<CustomizationFile> customizationFiles = new CustomizationFileDao(ThaleiaSession.get().getContextService().getContextSingleton())
                        .find(graphicCustomizationName, null, ThaleiaSession.get().getAuthenticatedUser().getDomain());

                return !customizationFiles.isEmpty();
            }
        }.setOutputMarkupPlaceholderTag(true).setOutputMarkupId(true);

        // Le lien de téléchargement de la personnalisation actuelle
        DownloadLink dlLink = (DownloadLink) new DownloadLink("downloadActiveCustomization", new AbstractReadOnlyModel<>() {

            @Override
            public File getObject() {
                // On recherche un fichier de personnalisation Cannelle
                // existant dans le domaine de sécurité de l'utilisateur.
                // Ceci peut renvoyer null : on suppose que le lien de
                // téléchargement n'est pas présenté en cas d'absence de
                // personnalisation.
                return getCustomizationFile(graphicCustomizationName);
            }

        }) {
        }.setCacheDuration(Duration.NONE).setOutputMarkupId(true);
        colDownload.add(dlLink.setOutputMarkupId(true));
        add(colDownload);
    }


    private void addUploadYourCustomization() {
        try {
            // On associe un fichier temporaire pour récolter le binaire uploadé
            final UploadFormPanelArchiveV6 uploadPanel = new UploadFormPanelArchiveV6(
                    "uploadForm",
                    Model.of(ThaleiaApplication.get().getTempFilesService().getTempFile()),
                    true) {
                @Override
                public void onUpload(File uploadedFile, String filename, AjaxRequestTarget target) {

                    try {
                        // Si ce n'est pas une archive, on couine
                        if (!ZipUtils.isAnArchive(uploadedFile)) {
                            error(new StringResourceModel("upload.notanarchive", CustomizationPanel.this,
                                    null, new Object[]{filename}).getString());
                            throw new DetailedException("Le fichier reçu n'est pas une archive.");
                        }

                        // On valide
                        try {
                            customizationValidator.validate(uploadedFile);
                        } catch (DetailedException e) {
                            error(new StringResourceModel("invalid.customization", CustomizationPanel.this,
                                    null, new Object[]{e.getMessage()}).getString());
                            throw e;
                        }

                        // On enregistre le fichier uploadé comme la nouvelle personnalisation pour le domaine de sécurité de l'utilisateur
                        ObjectContext tempContext = ThaleiaSession.get().getContextService().getNewContext();
                        ThaleiaSession.get().getCustomizationFilesService().setCustomizationFile(
                                graphicCustomizationName, null, ThaleiaSession.get().getAuthenticatedUser().getDomain(),
                                uploadedFile, filename, tempContext);
                        tempContext.commitChanges();

                        info(new StringResourceModel("upload.ok", CustomizationPanel.this,
                                null, new Object[]{filename}).getString());

                        target.add(colDownload);
                        target.add(feedbackPanel);
                    } catch (DetailedException e) {
                        logger.warn("Erreur d'enregistrement du fichier de personnalisation :" + e);
                        StringResourceModel errorMessageModel = new StringResourceModel("upload.error",
                                CustomizationPanel.this, null);
                        Session.get().error(errorMessageModel.getString());
                        error(errorMessageModel);
                        target.add(feedbackPanel);
                    }
                }
            };
            add(uploadPanel.setOutputMarkupId(true));

        } catch (Exception e) {
            logger.warn("Impossible de générer un fichier temporaire pour l'upload d'une personnalisation : " + e);
            add(new EmptyPanel("uploadForm").setOutputMarkupId(true));
        }
    }


    /**
     * @return le fichier de personnalisation Cannelle existant dans le domaine
     * de sécurité de l'utilisateur, ou null s'il n'y en a pas.
     */
    private File getCustomizationFile(String customizationName) {
        // On recherche un fichier de personnalisation Cannelle  existant dans le domaine de sécurité de  l'utilisateur.
        File customizationFile;
        try {
            customizationFile = ThaleiaSession.get().getCustomizationFilesService().getCustomizationFile(
                    customizationName, null, ThaleiaSession.get().getAuthenticatedUser().getDomain(),
                    ThaleiaSession.get().getContextService().getContextSingleton());
        } catch (DetailedException e) {
            logger.warn(e);
            return null;
        }
        return customizationFile;
    }

}
