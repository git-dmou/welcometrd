package fr.solunea.thaleia.plugins.welcomev6.properties;

import fr.solunea.thaleia.model.Content;
import fr.solunea.thaleia.model.ContentVersion;
import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.model.Publication;
import fr.solunea.thaleia.model.dao.ContentDao;
import fr.solunea.thaleia.model.dao.ContentVersionDao;
import fr.solunea.thaleia.model.dao.LocaleDao;
import fr.solunea.thaleia.plugins.welcomev6.contents.ActionsOnContent;
import fr.solunea.thaleia.plugins.welcomev6.contents.RevisionsListPanel;
import fr.solunea.thaleia.plugins.welcomev6.messages.LocalizedMessages;
import fr.solunea.thaleia.plugins.welcomev6.panels.MenuPanel;
import fr.solunea.thaleia.utils.DateUtils;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.pages.BasePage;
import fr.solunea.thaleia.webapp.pages.ErrorPage;
import fr.solunea.thaleia.webapp.pages.ThaleiaV6MenuPage;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.*;
import org.apache.wicket.util.encoding.UrlEncoder;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class EditPropertiesPage extends ThaleiaV6MenuPage {

    public static final String MODULE_FORMAT_PROPERTY_NAME = "ModuleFormat";
    public static final String DEBUG_MODE_PROPERTY_NAME = "DebugMode";
    public static final String SCORM_COMMUNICATION_PROPERTY_NAME = "SCORMCommunication";
    public static final String MASTERYSCORE_PROPERTY_NAME = "PassageNote";
    private final ThaleiaFeedbackPanel feedbackPanel;
    private final Form<ContentVersion> form;
    private final int contentId;
    private final ActionsOnContent actionsOnContent;
    private Component title;
    private int contentVersionId;
    private boolean editedNotSaved = false;

    protected EditPropertiesPage(IModel<Content> content, IModel<Locale> locale,
                                 Class<? extends WebPage> backPageClass, IModel<String> menuLabel,
                                 ActionsOnContent actionsOnContent) {

        this.actionsOnContent = actionsOnContent;
        // On fabrique un contexte d'édition pour sauver ou annuler les modifications sans effet sur les autres objets en cours d'édition
        ObjectContext context = ThaleiaSession.get().getContextService().getChildContext(content.getObject().getObjectContext());
        ContentDao contentDao = new ContentDao(context);
        ContentVersionDao contentVersionDao = new ContentVersionDao(context);
        contentId = contentDao.getPK(content.getObject());

        // Le modèle de la page demande au DAO de reconstruire l'objet
        // Cayenne à chaque demande, car sinon on se retrouve avec des
        // objets Cayenne transients, ce qui ne permet pas d'appeler les
        // objets Cayenne qui leur sont liés.
        setDefaultModel(new CompoundPropertyModel<>(new LoadableDetachableModel<ContentVersion>() {
            @Override
            protected ContentVersion load() {
                // On tente de charger la contentVersion, si elle existe toujours dans ce contexte avec un ID temporaire
                ContentVersion result = contentVersionDao.get(contentVersionId);
                if (result == null) {
                    // On fabrique une nouvelle version dans ce contexte d'édition
                    Content content = contentDao.get(contentId);
                    try {
                        // On crée une nouvelle version
                        result = ThaleiaSession.get().getContentService().getNewVersion(content,
                                ThaleiaSession.get().getAuthenticatedUser());
                        // On stocke l'id de cette nouvelle version
                        contentVersionId = contentVersionDao.getPK(result);
                        // On marque les champs comme non encore édités
                        editedNotSaved = false;
                        // On copie les propriétés de la dernière version existante du contenu
                        ThaleiaSession.get().getContentService().copyProperties(content.getLastVersion(), result);
                    } catch (DetailedException e) {
                        logger.warn("Erreur de copie des propriétés pour l'édition d'une nouvelle version de contenu : " + e);
                        setResponsePage(ErrorPage.class);
                    }
                }
                return result;
            }
        }
        ));

        // Panneau de feedback
        feedbackPanel = (ThaleiaFeedbackPanel) new ThaleiaFeedbackPanel("feedback").setOutputMarkupId(true);
        add(feedbackPanel);

        // Le menu, qui renvoie sur la page demandée
        add(new MenuPanel("pluginMenu", backPageClass, menuLabel, new MenuPanel.MenuPanelAction() {
            @Override
            protected void run() {
                // Devrait-on supprimer le contexte Cayenne d'édition, pour libérer des ressources, ou bien le garder dans l'historique de navigation ?
                // Si on voulait détruire le contexte Cayenne, ça serait ici.
            }
        }));

        // Le titre du formulaire = le titre de la version
        title = new Label("versionTitle", new LoadableDetachableModel<String>() {
            @Override
            public String load() {
                return actionsOnContent.getContentTitle(getModel().getObject(), locale.getObject());
            }
        }).setOutputMarkupId(true);
        add(title);

        // Les boutons d'action sur ce contenu
        addButtons(locale);

        // Le formulaire
        form = new Form<>("form", getModel());
        form.setOutputMarkupId(true);

        // Les boutons Appliquer et Rétablir
        MarkupContainer saveButton = getSaveButton();
        form.add(saveButton);
        MarkupContainer cancelButton = getCancelButton();
        form.add(cancelButton);

        // Le nom de cette version de contenu
        // Si on peut enregistrer, alors on rend le bouton  Save actif.
        // Les éléments graphiques
        Component versionId = new TextField<ContentVersion>("contentIdentifier").add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                logger.debug("L'identifiant a été changé.");
                // Si on peut enregistrer, alors on rend le bouton
                // Save actif.
                onFieldChange(target);
            }
        });
        form.add(versionId.setOutputMarkupId(true));

        // La description
        addEditTextAreaPropertyPanel("descriptionEditPanel", "Description", form, locale);

        // Le suivi SCORM, si défini
        addSelectorPanelIfPropertyExists(locale, "scorm", "scorm.communication.yes", "scorm.communication.no",
                SCORM_COMMUNICATION_PROPERTY_NAME);

        // Le debugMode, si défini
        addSelectorPanelIfPropertyExists(locale, "debugMode", "debug.mode.yes", "debug.mode.no",
                DEBUG_MODE_PROPERTY_NAME);

        // Le format du module, si défini
        addSelectorPanelIfPropertyExists(locale, "format", "content.format.html", "content.format.exe",
                MODULE_FORMAT_PROPERTY_NAME);

        // Titre, si défini : texte libre
        if (isPropertyDefined("title")) {
            title = addEditTextPropertyPanel("title", "Title", form, locale);
        } else {
            title = new EmptyPanel("title").setOutputMarkupId(true);
        }
        form.add(title);

        // Note de passage, si définie
        if (isPropertyDefined("PassageNote")) {
            // Note de passage : entier entre 0 et 100
            addEditTextPropertyPanel("passingScore", "PassageNote", form, locale).setRequired(true).add((IValidator<String>) validatable -> {
                // On s'assure que la valeur est un entier
                String value = validatable.getValue();
                int score = 0;
                try {
                    score = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    error(validatable, "score.nan");
                }
                // On s'assure qu'elle est entre 0 et 100
                if (score < 0 || score > 100) {
                    error(validatable, "score.range");
                }
            });
        } else {
            form.add(new EmptyPanel("passingScore").setOutputMarkupId(true));
        }

        // Date de dernière mise à jour, non éditable
        Component lastEditDate = new TextField<>("lastEditDate", new LoadableDetachableModel<String>() {
            @Override
            public String load() {
                return DateUtils.formatDateHour(getModel().getObject().getContent().getVersion(getModel().getObject().getRevisionNumber()).getLastUpdateDate(), ThaleiaSession.get().getLocale());

            }
        }).setOutputMarkupId(true);
        form.add(lastEditDate);

        add(form);

        // Le bouton "Nouvelle publication"
        add(new AjaxLink<Void>("addPublicationLink") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                actionsOnContent.onPublish(content, locale, target, feedbackPanel);
            }

            @Override
            public boolean isVisible() {
                return actionsOnContent.isPublishable(content);
            }
        });

        // Le panneau de gestion des publications
        add(new ContentPublicationsPanel("publications", new LoadableDetachableModel<>() {
            @Override
            protected Content load() {
                return contentDao.get(contentId);
            }
        }) {
            @Override
            public boolean isVisible() {
                // On renvoie false si le plugin de publication n'est pas installé
                String publicationEditPageName = "fr.solunea.thaleia.plugins.analyze.pages.PublicationEditPage";
                try {
                    Class.forName(publicationEditPageName, true, ThaleiaSession.get().getPluginService().getClassLoader());
                } catch (Exception e) {
                    logger.debug("Impossible de retrouver une instance correcte de la page de publication : " + e);
                    return false;
                }

                // On vérifie que ce contenu a bien été publié par l'utilisateur
                try {
                    //logger.debug("On vérifie que le contenu " + contentId + " a bien été publié par l'utilisateur.");
                    return ThaleiaSession.get().getPublicationService().thisContentWasPublished(contentDao.get(contentId), ThaleiaSession.get().getAuthenticatedUser(), false);
                } catch (DetailedException e) {
                    logger.warn(e);
                    return false;
                }
            }

            @Override
            protected void onEdit(IModel<Publication> model) {
                onEditPublication(model);
            }
        });

        // Le panneau de présentation des versions du contenu
        Component versionsPanel = new RevisionsListPanel("versions", content, actionsOnContent, locale, feedbackPanel, false,
                showSourceLinks());
        add(versionsPanel.setOutputMarkupId(true));
    }

    /**
     * Si la propriété qui porte ce nom existe pour le contentType, alors ajoute au formulaire de la page un panneau
     * de sélection de deux valeurs (valeur localisée des clés choice1 et choice2 dans LocalizedMessages.properties)
     * pour la propriété qui porte ce nom.
     *
     * @param choice1 dans les LocalizedProperties, le nom de la clé pour la valeur de ce choix.
     * @param choice2 dans les LocalizedProperties, le nom de la clé pour la valeur de ce choix.
     */
    private void addSelectorPanelIfPropertyExists(IModel<Locale> locale, String scormPanelId, String choice1,
                                                  String choice2, String contentPropertyUnlocalizedName) {
        if (isPropertyDefined(contentPropertyUnlocalizedName)) {
            List<String> choices = new ArrayList<>();
            // On place comme valeurs possibles les valeurs qui sont de la même
            // locale que la ContentPropertyValue
            // On reste dans un contexte standard, car ce sont des locales pour les messages.
            LocaleDao localeDao = new LocaleDao(ThaleiaSession.get().getContextService().getContextSingleton());
            choices.add(LocalizedMessages.getMessageForLocale(choice1,
                    localeDao.getJavaLocale(locale.getObject())));
            choices.add(LocalizedMessages.getMessageForLocale(choice2,
                    localeDao.getJavaLocale(locale.getObject())));
            addEditSelectorPropertyPanel(scormPanelId, contentPropertyUnlocalizedName, form, locale, choices);
        } else {
            form.add(new EmptyPanel(scormPanelId));
        }
    }

    /**
     * @return true si la propriété demandée  est associée au type de contenu édité
     */
    private boolean isPropertyDefined(String contentPropertyUnlocalizedName) {
        return ThaleiaSession.get().getContentPropertyService().findContentProperty(getModel().getObject().getContentType(), contentPropertyUnlocalizedName)
                != null;
    }

    private void addButtons(IModel<Locale> locale) {

        IModel<Content> content = new LoadableDetachableModel<>() {
            @Override
            protected Content load() {
                return getModel().getObject().getContent();
            }
        };

        // La publication
        // **************
        add(new AjaxLink<Void>("publishLink") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                actionsOnContent.onPublish(content, locale, target, feedbackPanel);
            }

            @Override
            public boolean isVisible() {
                return actionsOnContent.isPublishable(content);
            }
        });

        // La mise à jour des publications
        // *******************************
        add(new AjaxLink<Void>("updatePublicationLink") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                actionsOnContent.onUpdatePublish(content, locale, getPage());
            }

            @Override
            public boolean isVisible() {
                return actionsOnContent.isPublicationsToUpdate(content, locale);
            }
        });

        // La fonction d'export
        // ********************
        add(new AjaxLink<Void>("exportLink") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                actionsOnContent.onExport(content, locale, target, feedbackPanel);
            }

            @Override
            public boolean isVisible() {
                return actionsOnContent.isExportable(content);
            }
        });

        // La prévisualisation
        // *******************
        add(new Link<Void>("previewLink") {
            @Override
            public void onClick() {
                actionsOnContent.onPreview((IModel<ContentVersion>) EditPropertiesPage.this.getDefaultModel(), locale);
            }
        }.setPopupSettings(new PopupSettings("")));

        // On ajoute le lien qui permet d'ouvrir la page de suivi des
        // publications de cette mission.
        add(new AjaxLink<Void>("monitorLink") {
            @Override
            public boolean isVisible() {
                return actionsOnContent.isAnalyzable(content, locale);
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                actionsOnContent.onAnalyze(content);
            }
        });

        // Télécharger la source
        // *********************
        add(new DownloadLink("sourceLink", new AbstractReadOnlyModel<>() {
            @Override
            public File getObject() {
                return actionsOnContent.getSourceFile(getModel(), locale);
            }
        }, new AbstractReadOnlyModel<>() {
            @Override
            public String getObject() {
                // Le nom du fichier dans la réponse HTTP
                String downloadFilename = getModel().getObject().getContentIdentifier() + "_v"
                        + getModel().getObject().getRevisionNumber() + ".zip";
                return UrlEncoder.QUERY_INSTANCE.encode(downloadFilename, "UTF-8");
            }
        }) {
            @Override
            public boolean isVisible() {
                return actionsOnContent.sourceAvailable(EditPropertiesPage.this.getModel(), locale);
            }

        }.setCacheDuration(Duration.NONE));

    }


    /**
     * Lancement de l'édition d'une publication.
     */
    protected abstract void onEditPublication(IModel<Publication> model);

    private void error(IValidatable<String> validatable, String errorKey) {
        ValidationError error = new ValidationError();
        error.addKey(errorKey);
        validatable.error(error);
    }

    /**
     * Ajoute un panneau d'édition de valeur d'une propriété, d'après une liste de choix.
     */
    private void addEditSelectorPropertyPanel(String id, String contentPropertyUnlocalizedName,
                                              Form<ContentVersion> form, IModel<Locale> locale, List<String> choices) {
        // Si la description est nulle, alors on fixe une valeur vide, sinon le
        // panneau d'édition de cette propriété ne s'affichera pas;
        try {
            logger.debug("Préparation d'un panneau de sélection pour la propriété '" + contentPropertyUnlocalizedName
                    + "' pour la version : " + form.getModelObject());
            // Le panneau d'édition de cette propriété.
            EditSelectorPropertyPanel panel = new EditSelectorPropertyPanel(id, contentPropertyUnlocalizedName, getModel(), locale, choices) {
                @Override
                protected void onPropertyChanged(AjaxRequestTarget target) {
                    logger.debug("Une propriété a été changée.");
                    // Si on peut enregistrer, alors on rend le bouton
                    // Save actif.
                    onFieldChange(target);
                }
            };
            form.add(panel.setOutputMarkupId(true));

        } catch (Exception e) {
            logger.warn("Impossible de présenter un panneau de sélection pour la propriété '"
                    + contentPropertyUnlocalizedName + "' : " + e);
            form.add(new EmptyPanel(id));
        }
    }

    /**
     * @param id                             l'id du panneau à ajouter
     * @param contentPropertyUnlocalizedName Le nom non localisé de la contentProperty dont on veut ajouter un panneau
     *                                       d'édition de la valeur
     * @return le textField qui a été généré, ou null si aucun n'a été généré (ce qui arrive si la propriété demandée
     * n'a pas été trouvée)
     */
    private TextField<String> addEditTextPropertyPanel(String id, String contentPropertyUnlocalizedName,
                                                       Form<ContentVersion> form, IModel<Locale> locale) {
        // Le panneau d'édition de cette propriété.
        EditTextPropertyPanel panel = new EditTextPropertyPanel(id, contentPropertyUnlocalizedName, getModel(),
                locale) {
            @Override
            protected void onPropertyChanged(AjaxRequestTarget target) {
                logger.debug("Une propriété a été changée.");
                onFieldChange(target);
            }
        };
        form.add(panel.setOutputMarkupId(true));
        return panel.getTextField();
    }

    /**
     * Ajoute un panneau d'édition de propriété, de type texte.
     */
    private void addEditTextAreaPropertyPanel(String id, String contentPropertyUnlocalizedName,
                                              Form<ContentVersion> form, IModel<Locale> locale) {
        // Le panneau d'édition de cette propriété.
        EditTextAreaPropertyPanel panel = new EditTextAreaPropertyPanel(id, contentPropertyUnlocalizedName,
                getModel(), locale) {
            @Override
            protected void onPropertyChanged(AjaxRequestTarget target) {
                logger.debug("Une propriété a été changée.");
                onFieldChange(target);
            }
        };
        form.add(panel.setOutputMarkupId(true));
    }


    /**
     * Méthode appelée pour quitter la page.
     */
    protected abstract void onOut();

    /**
     * Méthode à appeler si une valeur a été modifiée dans un champ par
     * l'utilisateur.
     */
    private void onFieldChange(AjaxRequestTarget target) {
        editedNotSaved = true;
        target.add(form);
    }

    /**
     * @return true si le bouton Enregistrer/Annuler peut être activé.
     */
    private boolean isButtonCanBeActivated() {
        try {
            // Si le contenu n'a pas été modifié, alors n'active pas le bouton
            if (!editedNotSaved) {
                return false;
            }

            // On vérifie que l'identifiant du contenu n'est pas nul
            if (getModel().getObject().getContentIdentifier() == null
                    || getModel().getObject().getContentIdentifier().isEmpty()) {
                return false;
            }

            // Si ce n'est pas la dernière version, alors on ne la modifiera pas !
            if (!Objects.equals(getModel().getObject().getContent().getLastVersion().getRevisionNumber(),
                    getModel().getObject().getRevisionNumber())) {
                return false;
            }

            // On vérifie que l'identifiant du contenu n'est pas déjà utilisé

            // On s'assure que le contenu n'a pas été perdu lors
            // d'une sérialisation / désérialisation.
            Content content = new ContentDao(getModel().getObject().getObjectContext()).get(getModel().getObject().getContent().getObjectId());

            String id = getModel().getObject().getContentIdentifier();
            try {
                if (ThaleiaSession.get().getContentService().isContentVersionNameExists(id, content, content.getDomain())) {

                    // Cet identifiant de contenu existe !
                    // Mais c'est normal dans le cas où cette modification est
                    // la modification d'un contenu existant. C'est le seul cas
                    // autorisé.
                    // On compare donc l'identifiant courant avec celui de la
                    // version précédente
                    boolean authorized;
                    int currentRevisionNumber = getModel().getObject().getRevisionNumber();
                    if (currentRevisionNumber > 1) {
                        String previousVersionId = content.getVersion(currentRevisionNumber - 1).getContentIdentifier();
                        // L'identifiant existant est celui de la version
                        // précédente : on accepte donc que cette nouvelle
                        // version porte cet identifiant.
                        authorized = previousVersionId.equals(getModel().getObject().getContentIdentifier());
                    } else {
                        // Première version, donc on est dans le cas où on a
                        // choisi un identifiant d'un autre contenu.
                        authorized = false;
                    }

                    if (!authorized) {
                        // On ajoute un message
                        StringResourceModel messageModel = new StringResourceModel("content.name.exists",
                                EditPropertiesPage.this, null);
                        error(messageModel.getString());
                        // On n'active pas le bouton Enregistrer
                        return false;
                    } else {
                        return true;
                    }
                }
            } catch (DetailedException e) {
                throw new DetailedException(e).addMessage(
                        "Erreur durant la vérification de l'unicité de " + "l'identifiant du contenu " + content + ".");
            }

            // Pas de problème pour enregistrer
            return true;

        } catch (Exception e) {
            logger.warn(e);
            logger.warn(LogUtils.getStackTrace(e.getStackTrace()));
            return false;
        }
    }

    private MarkupContainer getSaveButton() {
        return (MarkupContainer) new AjaxLink<>("save", getModel()) {

            @Override
            public boolean isEnabled() {
                return isButtonCanBeActivated();
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    ContentVersion contentVersion = getModel().getObject();
                    new ContentVersionDao(contentVersion.getObjectContext()).save(contentVersion, true);
                    // Message
                    MarkupContainer panelContainer = getParent().getParent();
                    StringResourceModel messageModel = new StringResourceModel("save.ok", panelContainer, getModel());
                    info(messageModel.getString());
                    target.add(feedbackPanel);

                    // Le contenu a été modifié au moins une fois

                    // On recharge la page avec une nouvelle version à éditer
                    contentVersionId = -1;
                    editedNotSaved = false;
                    getModel().detach();
                    target.add(form);
                    target.add(title);

                } catch (Exception e) {
                    logger.debug("Impossible d'enregistrer l'objet : " + e.toString());
                    MarkupContainer panelContainer = getParent().getParent();
                    StringResourceModel errorMessageModel = new StringResourceModel("save.error", panelContainer,
                            getModel());
                    // Affiche le message d'erreur
                    fr.solunea.thaleia.webapp.panels.PanelUtils.showErrorMessage(errorMessageModel.getString(),
                            e.toString(), feedbackPanel);
                    target.add(feedbackPanel);
                }
            }
        }.setOutputMarkupId(true);
    }

    private MarkupContainer getCancelButton() {
        return (MarkupContainer) new AjaxLink<ContentVersion>("cancel") {

            @Override
            public boolean isEnabled() {
                return isButtonCanBeActivated();
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                // On supprime la version qui a été modifiée (elle avait été créée pour cette page uniquement), et on
                // recharge une nouvelle version à éditer, basée sur la dernière existante du contenu.
                editedNotSaved = false;
                contentVersionId = -1;
                getModel().detach();

                // Pas de sortie de la page : on reste, mais on recharge le formulaire
                target.add(form);
            }

        }.setOutputMarkupId(true);
    }

    @SuppressWarnings("unchecked")
    private IModel<ContentVersion> getModel() {
        return (IModel<ContentVersion>) getDefaultModel();
    }

    /**
     * @return true s'il faut proposer les boutons de téléchargement des sources dans le panneau des versions de ce
     * contenu.
     */
    public abstract boolean showSourceLinks();

}
