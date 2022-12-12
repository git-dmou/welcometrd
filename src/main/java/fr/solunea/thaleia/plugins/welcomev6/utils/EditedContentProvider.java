package fr.solunea.thaleia.plugins.welcomev6.utils;

import fr.solunea.thaleia.model.Content;
import fr.solunea.thaleia.model.ContentType;
import fr.solunea.thaleia.model.EditedContent;
import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.model.dao.ContentTypeDao;
import fr.solunea.thaleia.model.dao.EditedContentDao;
import fr.solunea.thaleia.model.dao.LocaleDao;
import fr.solunea.thaleia.model.dao.UserDao;
import fr.solunea.thaleia.service.ContentPropertyService;
import fr.solunea.thaleia.service.EditedContentService;
import fr.solunea.thaleia.service.PreviewService;
import fr.solunea.thaleia.service.utils.IPreviewHelper;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.BasePage;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class EditedContentProvider implements Serializable {

    protected static final Logger logger = Logger.getLogger(EditedContentProvider.class);

    /**
     * @return le ContentType des Content édités par le plugin : s'il n'existe pas, il est créé.
     */
    public static ContentType getContentType(String ContentTypeName, ObjectContext context) {
        ContentTypeDao contentTypeDao = new ContentTypeDao(context);
        ContentType contentType = contentTypeDao.findByName(ContentTypeName);

        try {
            if (contentType == null) {
                contentType = contentTypeDao.get();
                contentType.setName(ContentTypeName);
                contentType.setIsModuleType(false);

                contentTypeDao.save(contentType);

            }
        } catch (DetailedException e) {
            logger.warn("Imposible d'enregistrer le ContentType pour cet éditeur : " + e);
            contentType = null;
        }

        return contentType;
    }

    /**
     * Dans le répertoire passé en paramètre, en considérant qu'il s'agit du
     * répertoire qui contient le fichier en cours d'édition dans Mission
     * (mission-model.js), on modifie le contenu de la mission pour y placer le
     * titre et la locale.
     *
     * @param locale            la locale dans laquelle sera présentée l'IHM de la mission
     * @param contentIdentifier le titre à donner à la mission
     */
    public abstract void configureDefaultContent(File directory, IModel<Locale> locale, String contentIdentifier)
            throws DetailedException;

    /**
     * Le nom du fichier ZIP du contenu par défaut, en fonction de la locale du contenu.
     */
    public abstract String getDefaultZipFilename(Locale locale);

    /**
     * Le nom du fichier ZIP de l'éditeur.
     */
    public abstract String getEditorFilename();

    /**
     * @return le nom non localisé de la propriété qui contient les données qui sont à éditer pour ce contenu.
     */
    public abstract String getContentPropertyName();

    /**
     * @return le nom non localisé de la propriété qui contient les données qui sont à éditer pour ce contenu.
     */
    public abstract String getContentTypeName();

    /**
     * S'assure de l'existence des toutes les ContentProperties pour
     * ce ContentType, définies avec ce préfixe dans les paramètres,
     * de type [prefixe].name, [prefixe].type, [prefixe].hidden, etc.
     *
     * @param binaryContentPropertyName le nom non localisé de la propriété qui contient le binaire (getContentPropertyName())
     */

    public static void prepareDefaultContentProperties(ContentType contentType, String binaryContentPropertyName) throws DetailedException {
        ContentPropertyService contentPropertyService = ThaleiaSession.get().getContentPropertyService();

        LocaleDao localeDao = new LocaleDao(contentType.getObjectContext());
        Locale fr = localeDao.findByName("fr");
        Locale en = localeDao.findByName("en");

        {
            // Nom non localisé

            Map<Locale, String> names = new HashMap<>(2);
            names.put(fr, binaryContentPropertyName);
            names.put(en, binaryContentPropertyName);

            // Type
            String valueTypeName = "localizedFile";

            // Attribut hidden
            boolean hidden = false;

            // Création en base
            contentPropertyService.createContentProperty(binaryContentPropertyName, names, valueTypeName, hidden, contentType);
        }

        {
            // Nom non localisé
            String propertyName = "Description";

            // TitleFR
            String titleFR = "Description";
            // TitleEN
            String titleEN = "Description";

            Map<Locale, String> names = new HashMap<>(2);
            names.put(fr, titleFR);
            names.put(en, titleEN);

            // Type
            String valueTypeName = "localizedString";

            // Attribut hidden
            boolean hidden = false;

            // Création en base
            contentPropertyService.createContentProperty(propertyName, names, valueTypeName, hidden, contentType);
        }

        {
            // Nom non localisé
            String contentPropertyName = "SCORMCommunication";

            // TitleFR
            String titleFR = "Communication SCORM";
            // TitleEN
            String titleEN = "SCORM Communication";

            Map<Locale, String> names = new HashMap<>(2);
            names.put(fr, titleFR);
            names.put(en, titleEN);

            // Type
            String valueTypeName = "localizedString";

            // Attribut hidden
            boolean hidden = false;

            // Création en base
            contentPropertyService.createContentProperty(contentPropertyName, names, valueTypeName, hidden, contentType);
        }
    }

    /**
     * @param locale            la locale du contenu à éditer
     * @param contentIdentifier l'identifiant à donner à la première ContentVersion créée.
     * @return une EditedContent sur un nouveau Content, créé avec le type par défaut et le contenu par défaut.
     */
    public EditedContent createEditedContent(IModel<Locale> locale, String contentIdentifier) {

        try {
            ObjectContext context = ThaleiaSession.get().getContextService().getChildContext(locale.getObject().getObjectContext());

            ContentType contentType = getContentType(getContentTypeName(), context);

            EditedContentService editedContentService = ThaleiaSession.get().getEditedContentService();

            // On s'assure que le type de contenu possède bien les propriétés
            // nécessaires
            prepareDefaultContentProperties(contentType, getContentPropertyName());
            prepareSpecificContentProperties(contentType);

            // On fabrique un nouveau Contenu à éditer
            final EditedContent editedContent;
            editedContent = editedContentService.create(
                    new UserDao(context).get(ThaleiaSession.get().getAuthenticatedUser().getObjectId()),
                    contentType,
                    getContentPropertyName(),
                    locale.getObject(),
                    getContent(getDefaultZipFilename(locale.getObject())),
                    contentIdentifier,
                    context);
            // On l'enregistre
            new EditedContentDao(context).save(editedContent, true);

            // Le répertoire local qui contient le contenu edité
            File editedContentDirectory = ThaleiaSession.get().getEditedContentService().getEditedContentDir
                    (editedContent);
            // On met à jour le contenu par défaut (titre de la mission,
            // locale), mais pas la conf LRS, qui ne sera adaptée qu'à la
            // publication ou à l'export.
            configureDefaultContent(editedContentDirectory, locale, contentIdentifier);

            return editedContent;

        } catch (DetailedException e) {
            logger.info("Impossible de créer un nouveau EditedContent depuis un modèle : " + e);
            return null;
        }

    }

    /**
     * Les propriétés dont il faut s'assurer que le contentType possède pour que l'éditeur puisse fonctionner.
     */
    protected abstract void prepareSpecificContentProperties(ContentType contentType) throws DetailedException;

    /**
     * Publie les fichiers de l'éditeur
     * du EditedContent dans une prévisualisation, et renvoie l'URL pour accéder à cette
     * prévsisualisation.
     *
     * @param page          la page où afficher les erreurs de traitement
     * @param previewHelper l'objet de traitements spécifiques pour la prévisualisation
     */
    public String publishEditorFiles(IPreviewHelper previewHelper, BasePage page) {
        // Récupération du Zip qui contient l'éditeur HTML
        File editorZip;

        try (InputStream is = ThaleiaSession.get().getPluginService().getClassLoader().getResourceAsStream(getEditorFilename())) {
            if (is == null) {
                logger.warn("Le fichier de l'éditeur HTML n'a pas été trouvé !");
                return "";
            }
            // On récupère le Zip dans un fichier temporaire
            editorZip = ThaleiaApplication.get().getTempFilesService().getTempFile(getEditorFilename());
            FileUtils.copyInputStreamToFile(is, editorZip);
        } catch (Exception e) {
            logger.warn("Le fichier de l'éditeur HTML n'a pas pu être copié : " + e);
            return "";
        }

        // On appelle le service de prévisualisation
        PreviewService previewService;
        String url = "";
        try {
            previewService = ThaleiaApplication.get().getPreviewService();

            // On prévisualise
            url = previewService.publishArchive(editorZip, previewHelper);
        } catch (Exception e) {
            logger.warn("Impossible de publier l'éditeur HTML : " + e);
            page.error(new StringResourceModel("publicationError", page, null).getString());
        }

        // On ajoute la locale de l'IHM de l'éditeur en paramètre
        return url + "?lang=" + ThaleiaSession.get().getLocale().getLanguage();
    }

    /**
     * Prépare le EditedContent pour l'édition de ce contenu
     *
     * @param locale       la locale du contenu à éditer
     * @param contentModel le contenu à éditer
     */
    public EditedContent openEditedContent(IModel<Locale> locale, IModel<Content> contentModel) {

        try {
            EditedContentService editedContentService = ThaleiaSession.get().getEditedContentService();
            Content content = contentModel.getObject();

            final EditedContent editedContent;
            if (editedContentService.isContentEditedBy(content, ThaleiaSession.get().getAuthenticatedUser())) {
                // Si le contenu est actuellement en cours d'édition par l'utilisateur : il existe déjà un
                // EditedContent en
                // base pour ce user ET ce contenu. Si c'est le cas, c'est probablement que l'utilisateur a été
                // déconnecté de  l'éditeur avant d'avoir pu enregistrer (ou non) son travail dans le EditedContent, qui
                // n'a pas pu être nettoyé.
                editedContent = editedContentService.getEditedContent(content, ThaleiaSession.get()
                        .getAuthenticatedUser());
            } else {
                // On fabrique un nouveau Contenu à éditer
                editedContent = editedContentService.open(
                        new UserDao(contentModel.getObject().getObjectContext())
                                .get(ThaleiaSession.get().getAuthenticatedUser().getObjectId()),
                        content,
                        getContentPropertyName(),
                        locale.getObject(),
                        getContent(getDefaultZipFilename(locale.getObject()))
                );
                // On l'enregistre
                new EditedContentDao(editedContent.getObjectContext()).save(editedContent, true);
            }

            return editedContent;

        } catch (DetailedException e) {
            logger.info("Impossible de créer un nouveau EditedContent pour un contenu existant : " + e);
            return null;
        }

    }

    /**
     * @return le fichier qui porte ce nom, dans le ClassLoader (c'est à dire dans les paquets JAR des plugins)
     */
    protected File getContent(String filename) throws DetailedException {

        // Récupération du Zip qui contient les fichiers par défaut
        File defaultZipedContent;

        try (InputStream is = ThaleiaSession.get().getPluginService().getClassLoader().getResourceAsStream(filename)) {
            if (is == null) {
                throw new DetailedException("Le fichier '" + filename + "' n'a pas été trouvé !");
            }

            defaultZipedContent = ThaleiaApplication.get().getTempFilesService().getTempFile(filename);
            logger.debug("Copie du fichier '" + filename + "' dans " + defaultZipedContent.getAbsolutePath() + ".");
            FileUtils.copyInputStreamToFile(is, defaultZipedContent);

        } catch (Exception e) {
            throw new DetailedException(e).addMessage("Le fichier '" + filename + "' n'a pas pu être copié.");

        }

        return defaultZipedContent;
    }
}
