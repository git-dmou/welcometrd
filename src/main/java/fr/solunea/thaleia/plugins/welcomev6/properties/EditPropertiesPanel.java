package fr.solunea.thaleia.plugins.welcomev6.properties;

import fr.solunea.thaleia.model.*;
import fr.solunea.thaleia.model.dao.ContentDao;
import fr.solunea.thaleia.model.dao.ContentVersionDao;
import fr.solunea.thaleia.model.dao.LocaleDao;
import fr.solunea.thaleia.model.dao.UserDao;
import fr.solunea.thaleia.plugins.welcomev6.contents.ActionsOnContent;
import fr.solunea.thaleia.plugins.welcomev6.contents.RevisionsListPanel;
import fr.solunea.thaleia.plugins.welcomev6.messages.LocalizedMessages;
import fr.solunea.thaleia.service.ContentService;
import fr.solunea.thaleia.utils.DateUtils;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.ErrorPage;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.utils.MessagesUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
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

public abstract class EditPropertiesPanel extends Panel {

    public static final String MODULE_FORMAT_PROPERTY_NAME = "ModuleFormat";
    public static final String DEBUG_MODE_PROPERTY_NAME = "DebugMode";
    public static final String SCORM_COMMUNICATION_PROPERTY_NAME = "SCORMCommunication";
    public static final String MASTERYSCORE_PROPERTY_NAME = "PassageNote";
    protected static final Logger logger = Logger.getLogger(EditPropertiesPanel.class);
    protected final Form<ContentVersion> form;
    protected final Component title;
    protected final int contentId;
    protected final ActionsOnContent actionsOnContent;
    protected final ThaleiaFeedbackPanel feedbackPanel;
    protected final MarkupContainer saveButton;
    protected final MarkupContainer cancelButton;
    protected final Component versionsPanel;

    protected Component moduleTranslationPanel;
    protected int contentVersionId;
    protected int currentContentVersionId;
    protected boolean editedNotSaved = false;
    protected final IModel<Locale> locale;

//    private ContentVersionDao contentVersionDao;


    protected EditPropertiesPanel(IModel<Content> content, IModel<Locale> locale, ActionsOnContent actionsOnContent,
                                  String id, ThaleiaFeedbackPanel feedbackPanel) {
        super(id);

        logger.debug("Edition du content PK=" + content.getObject().getObjectId());

        this.locale = locale;


        ObjectContext Context = content.getObject().getObjectContext();
        ContentDao contentDao = new ContentDao(Context);
        ContentVersionDao contentVersionDao = new ContentVersionDao(Context);
        contentId = contentDao.getPK(content.getObject());
        currentContentVersionId = contentVersionDao.getPK(content.getObject().getLastVersion()) ;
        logger.debug("ID de l'objet en cours d'??dition dans le contexte local : " + contentId);

        this.actionsOnContent = actionsOnContent;

        initContentVersionModel();

        // Le formulaire
        form = new Form<>("form");
        form.setOutputMarkupId(true);

        // Panneau de feedback
        this.feedbackPanel = feedbackPanel;

        addBtnBack();

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

        // Les boutons Appliquer et R??tablir
        saveButton = getSaveButton();
        form.add(saveButton);
        cancelButton = getCancelButton();
        form.add(cancelButton);

        // Le nom de cette version de contenu
        // Si on peut enregistrer, alors on rend le bouton
        // Save actif.
        // Les ??l??ments graphiques
        Component versionId = new TextField<ContentVersion>("contentIdentifier").add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                logger.debug("L'identifiant a ??t?? chang??.");
                // Si on peut enregistrer, alors on rend le bouton  Save actif.
                onFieldChange(target);
            }
        });
        form.add(versionId.setOutputMarkupId(true));

        addOrReplacePropertiesEditorPanels();

        // Date de derni??re mise ?? jour, non ??ditable
        Component lastEditDate = new TextField<>("lastEditDate", new LoadableDetachableModel<String>() {
            @Override
            public String load() {
                return DateUtils.formatDateHour(getModel().getObject().getContent().getVersion(getModel().getObject().getRevisionNumber() - 1).getLastUpdateDate(), ThaleiaSession.get().getLocale());
            }
        }).setEnabled(false).setOutputMarkupId(true);
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
                // On renvoie false si le plugin de publication n'est pas install??
                String publicationEditPageName = "fr.solunea.thaleia.plugins.analyze.pages.PublicationEditPage";
                try {
                    Class.forName(publicationEditPageName, true, ThaleiaSession.get().getPluginService().getClassLoader());
                } catch (Exception e) {
                    logger.debug("Impossible de retrouver une instance correcte de la page de publication : " + e);
                    return false;
                }

                // On v??rifie que ce contenu a bien ??t?? publi?? par l'utilisateur
                try {
                    //logger.debug("On v??rifie que le contenu " + contentId + " a bien ??t?? publi?? par l'utilisateur.");
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

        // Le panneau de pr??sentation des versions du contenu
        versionsPanel = new RevisionsListPanel("versions", content, actionsOnContent, locale, feedbackPanel, false, showSourceLinks());
        add(versionsPanel.setOutputMarkupId(true));

       /* // Le panneau de traduction automatique du module
        moduleTranslationPanel = new ModuleTranslationPanel("moduleTranslation", currentContentVersionId).setOutputMarkupId(true);
        add(moduleTranslationPanel) ;*/

    }

    protected void addOrReplacePropertiesEditorPanels() {
        // R??cup??ration de la locale de l'IHM
        LocaleDao localeDao = new LocaleDao(ThaleiaSession.get().getContextService().getContextSingleton());
        IModel<Locale> IhmLocale = Model.of(localeDao.getLocale(ThaleiaSession.get().getLocale()));

        // La description
        addEditTextAreaPropertyPanel("descriptionEditPanel", "Description", form, locale);

        // Le suivi SCORM, si d??fini
        addSelectorPanelIfPropertyExists(IhmLocale, "scorm", "scorm.communication.yes", "scorm.communication.no",
                SCORM_COMMUNICATION_PROPERTY_NAME);

        // Le debugMode, si d??fini
        addSelectorPanelIfPropertyExists(locale, "debugMode", "debug.mode.yes", "debug.mode.no",
                DEBUG_MODE_PROPERTY_NAME);

        // Le format du module, si d??fini
        addSelectorPanelIfPropertyExists(IhmLocale, "format", "content.format.html",
                "content.format.exe", MODULE_FORMAT_PROPERTY_NAME);

        // Note de passage, si d??finie
        if (isPropertyDefined(MASTERYSCORE_PROPERTY_NAME)) {
            // Note de passage : entier entre 0 et 100
            addEditTextPropertyPanel("passingScore", MASTERYSCORE_PROPERTY_NAME, form, locale).setRequired(true).add((IValidator<String>) validatable -> {
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
            form.addOrReplace(new EmptyPanel("passingScore").setOutputMarkupId(true));
        }
    }

    /**
     * Initialise le mod??le de la ContentVersion ?? ??diter dans ce panneau, ?? partir du contentId : fabrique une ContentVersion dans un nouveau contexte temporaire.
     */
    protected void initContentVersionModel() {
        // On fabrique un contexte d'??dition pour sauver ou annuler les modifications sans effet sur les autres objets en cours d'??dition
        ObjectContext objectContext = ThaleiaSession.get().getContextService().getNewContext();

        editedNotSaved = false;
        contentVersionId = -1;

        // Le mod??le de la page demande au DAO de reconstruire l'objet
        // Cayenne ?? chaque demande, car sinon on se retrouve avec des
        // objets Cayenne transients, ce qui ne permet pas d'appeler les
        // objets Cayenne qui leur sont li??s.
        CompoundPropertyModel<ContentVersion> contentVersionCompoundPropertyModel = new CompoundPropertyModel<>(Model.of(loadModelFromContentVersionId(objectContext)));
        setDefaultModel(contentVersionCompoundPropertyModel);
    }

    protected ContentVersion loadModelFromContentVersionId(ObjectContext objectContext) {
        // On tente de charger la contentVersion, si elle existe toujours dans ce contexte avec un ID temporaire
        ContentVersionDao contentVersionDao = new ContentVersionDao(objectContext);
        ContentDao contentDao = new ContentDao(objectContext);
        logger.debug("Tentative de chargement de la contentVersionId=" + contentVersionId);
        ContentVersion result = contentVersionDao.get(contentVersionId);
        if (result == null) {
            // On fabrique une nouvelle version dans ce contexte d'??dition
            logger.debug("Cr??ation d'une nouvelle contentVersion pour le content " + contentId);
            Content content = contentDao.get(contentId);
            try {
                // On cr??e une nouvelle version
                User authenticatedUser = new UserDao(objectContext).get(ThaleiaSession.get().getAuthenticatedUser().getObjectId());
                result = ThaleiaSession.get().getContentService().getNewVersion(content, authenticatedUser);
                // On stocke l'id de cette nouvelle version
                contentVersionId = contentVersionDao.getPK(result);
                logger.debug("Nouvelle contentVersion cr????e : contentVersionId=" + contentVersionId);
                // On marque les champs comme non encore ??dit??s
                editedNotSaved = false;
                // On copie les propri??t??s de la derni??re version existante du contenu
                new ContentService(ThaleiaSession.get().getContextService(), ThaleiaApplication.get().getConfiguration()).copyProperties(content.getLastVersion(), result);
            } catch (DetailedException e) {
                logger.warn("Erreur de copie des propri??t??s pour l'??dition d'une nouvelle version de contenu : " + e);
                setResponsePage(ErrorPage.class);
            }
        }
        return result;
    }

    /**
     * Si la propri??t?? qui porte ce nom existe pour le contentType, alors ajoute au formulaire de la page un panneau
     * de s??lection de deux valeurs (valeur localis??e des cl??s choice1 et choice2 dans LocalizedMessages.properties)
     * pour la propri??t?? qui porte ce nom.
     *
     * @param choice1 dans les LocalizedProperties, le nom de la cl?? pour la valeur de ce choix.
     * @param choice2 dans les LocalizedProperties, le nom de la cl?? pour la valeur de ce choix.
     */
    protected void addSelectorPanelIfPropertyExists(IModel<Locale> locale, String scormPanelId, String choice1,
                                                  String choice2, String contentPropertyUnlocalizedName) {
        if (isPropertyDefined(contentPropertyUnlocalizedName)) {
            List<String> choices = new ArrayList<>();
            // On place comme valeurs possibles les valeurs qui sont de la m??me
            // locale que la ContentPropertyValue
            // On reste dans un contexte standard, car ce sont des locales pour les messages.
            LocaleDao localeDao = new LocaleDao(ThaleiaSession.get().getContextService().getContextSingleton());
            choices.add(LocalizedMessages.getMessageForLocale(choice1, localeDao.getJavaLocale(locale.getObject())));
            choices.add(LocalizedMessages.getMessageForLocale(choice2, localeDao.getJavaLocale(locale.getObject())));
            addEditSelectorPropertyPanel(scormPanelId, contentPropertyUnlocalizedName, form, locale, choices);
        } else {
            form.addOrReplace(new EmptyPanel(scormPanelId));
        }
    }

    /**
     * @return true si la propri??t?? demand??e  est associ??e au type de contenu ??dit??
     */
    protected boolean isPropertyDefined(String contentPropertyUnlocalizedName) {
        return ThaleiaSession.get().getContentPropertyService().findContentProperty(getModel().getObject().getContentType(), contentPropertyUnlocalizedName)
                != null;
    }

    /**
     * Retourne la liste des boutons d'action.
     *
     * @param locale Locale de l'utilisateur courant.
     */
    protected void addButtons(IModel<Locale> locale) {
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

        // La mise ?? jour des publications
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

        // La pr??visualisation
        // *******************
        add(new Link<Void>("previewLink") {
            @Override
            @SuppressWarnings("unchecked")
            public void onClick() {
                actionsOnContent.onPreview((IModel<ContentVersion>) EditPropertiesPanel.this.getDefaultModel(), locale);
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

        // T??l??charger la source
        // *********************
        add(new DownloadLink("sourceLink", new AbstractReadOnlyModel<>() {
            @Override
            public File getObject() {
                return actionsOnContent.getSourceFile(getModel(), locale);
            }
        }, new AbstractReadOnlyModel<>() {
            @Override
            public String getObject() {
                // Le nom du fichier dans la r??ponse HTTP
                String downloadFilename = getModel().getObject().getContentIdentifier() + "_v"
                        + getModel().getObject().getRevisionNumber() + ".zip";
                return UrlEncoder.QUERY_INSTANCE.encode(downloadFilename, "UTF-8");
            }
        }) {
            @Override
            public boolean isVisible() {
                return actionsOnContent.sourceAvailable(EditPropertiesPanel.this.getModel(), locale);
            }

        }.setCacheDuration(Duration.NONE));

    }


    /**
     * Lancement de l'??dition d'une publication.
     */
    protected abstract void onEditPublication(IModel<Publication> model);

    protected void error(IValidatable<String> validatable, String errorKey) {
        ValidationError error = new ValidationError();
        error.addKey(errorKey);
        validatable.error(error);
    }

    /**
     * Ajoute un panneau d'??dition de valeur d'une propri??t??, d'apr??s une liste de choix.
     */
    protected void addEditSelectorPropertyPanel(String id, String contentPropertyUnlocalizedName,
                                              Form<ContentVersion> form, IModel<Locale> locale, List<String> choices) {
        // Si la description est nulle, alors on fixe une valeur vide, sinon le
        // panneau d'??dition de cette propri??t?? ne s'affichera pas;
        try {
            // Le panneau d'??dition de cette propri??t??.
            EditSelectorPropertyPanel panel = new EditSelectorPropertyPanel(id, contentPropertyUnlocalizedName,
                    getModel(), locale, choices) {
                @Override
                protected void onPropertyChanged(AjaxRequestTarget target) {
                    logger.debug("Une propri??t?? a ??t?? chang??e.");
                    // Si on peut enregistrer, alors on rend le bouton
                    // Save actif.
                    onFieldChange(target);
                }
            };
            form.addOrReplace(panel.setOutputMarkupId(true));

        } catch (Exception e) {
            logger.warn("Impossible de pr??senter un panneau de s??lection pour la propri??t?? '"
                    + contentPropertyUnlocalizedName + "' : " + e);
            form.addOrReplace(new EmptyPanel(id));
        }

    }

    /**
     * @param id                             l'id du panneau ?? ajouter
     * @param contentPropertyUnlocalizedName Le nom non localis?? de la contentProperty dont on veut ajouter un panneau
     *                                       d'??dition de la valeur
     * @return le textField qui a ??t?? g??n??r??, ou null si aucun n'a ??t?? g??n??r?? (ce qui arrive si la propri??t?? demand??e
     * n'a pas ??t?? trouv??e)
     */
    protected TextField<String> addEditTextPropertyPanel(String id, String contentPropertyUnlocalizedName,
                                                       Form<ContentVersion> form, IModel<Locale> locale) {
        // Le panneau d'??dition de cette propri??t??.
        EditTextPropertyPanel panel = new EditTextPropertyPanel(id, contentPropertyUnlocalizedName, getModel(),
                locale) {
            @Override
            protected void onPropertyChanged(AjaxRequestTarget target) {
                logger.debug("Une propri??t?? a ??t?? chang??e.");
                onFieldChange(target);
            }
        };
        form.addOrReplace(panel.setOutputMarkupId(true));
        return panel.getTextField();
    }

    /**
     * Ajoute un panneau d'??dition de propri??t??, de type texte.
     */
    protected void addEditTextAreaPropertyPanel(String id, String contentPropertyUnlocalizedName,
                                              Form<ContentVersion> form, IModel<Locale> locale) {
        // Le panneau d'??dition de cette propri??t??.
        EditTextAreaPropertyPanel panel = new EditTextAreaPropertyPanel(id, contentPropertyUnlocalizedName,
                getModel(), locale) {
            @Override
            protected void onPropertyChanged(AjaxRequestTarget target) {
                logger.debug("Une propri??t?? a ??t?? chang??e.");
                onFieldChange(target);
            }
        };
        form.addOrReplace(panel.setOutputMarkupId(true));
    }


    /**
     * M??thode appel??e pour quitter la page.
     */
    protected abstract void onOut();

    /**
     * M??thode ?? appeler si une valeur a ??t?? modifi??e dans un champ par
     * l'utilisateur.
     */
    protected void onFieldChange(AjaxRequestTarget target) {
        logger.debug("Champ modifi??.");
        editedNotSaved = true;
        // On recharge l'??tat pr??sent?? des boutons enregistrer et annuler.
        target.add(saveButton);
        target.add(cancelButton);
    }

    /**
     * @return true si le bouton Enregistrer/Annuler peut ??tre activ??.
     */
    protected boolean isButtonCanBeActivated() {
        try {
            // Si le contenu n'a pas ??t?? modifi??, alors n'active pas le bouton
            if (!editedNotSaved) {
                return false;
            }

            // On v??rifie que l'identifiant du contenu n'est pas nul
            if (getModel().getObject().getContentIdentifier() == null || getModel().getObject().getContentIdentifier().isEmpty()) {
                return false;
            }

            // Si ce n'est pas la derni??re version, alors on ne la modifiera pas !
            if (!Objects.equals(getModel().getObject().getContent().getLastVersion().getRevisionNumber(), getModel().getObject().getRevisionNumber())) {
                return false;
            }

            // On v??rifie que l'identifiant du contenu n'est pas d??j?? utilis??

            // On s'assure que le contenu n'a pas ??t?? perdu lors
            // d'une s??rialisation / d??s??rialisation.
//            Content content = new ContentDao(getModel().getObject().getObjectContext()).get(getModel().getObject().getContent().getObjectId());
            Content content = getModel().getObject().getContent();

            String id = getModel().getObject().getContentIdentifier();
            try {
                if (ThaleiaSession.get().getContentService().isContentVersionNameExists(id, content, content.getDomain())) {

                    // Cet identifiant de contenu existe !
                    // Mais c'est normal dans le cas o?? cette modification est
                    // la modification d'un contenu existant. C'est le seul cas
                    // autoris??.
                    // On compare donc l'identifiant courant avec celui de la
                    // version pr??c??dente
                    boolean authorized;
                    int currentRevisionNumber = getModel().getObject().getRevisionNumber();
                    if (currentRevisionNumber > 1) {
                        String previousVersionId = content.getVersion(currentRevisionNumber - 1).getContentIdentifier();
                        // L'identifiant existant est celui de la version
                        // pr??c??dente : on accepte donc que cette nouvelle
                        // version porte cet identifiant.
                        authorized = previousVersionId.equals(getModel().getObject().getContentIdentifier());
                    } else {
                        // Premi??re version, donc on est dans le cas o?? on a
                        // choisi un identifiant d'un autre contenu.
                        authorized = false;
                    }

                    if (!authorized) {
                        // On ajoute un message
                        StringResourceModel messageModel = new StringResourceModel("content.name.exists",
                                EditPropertiesPanel.this, null);
                        error(messageModel.getString());
                        // On n'active pas le bouton Enregistrer
                        return false;
                    } else {
                        return true;
                    }
                }
            } catch (DetailedException e) {
                throw new DetailedException(e).addMessage(
                        "Erreur durant la v??rification de l'unicit?? de " + "l'identifiant du contenu " + content + ".");
            }

            // Pas de probl??me pour enregistrer
            return true;

        } catch (Exception e) {
            logger.warn(e);
            logger.warn(LogUtils.getStackTrace(e.getStackTrace()));
            return false;
        }
    }

    protected MarkupContainer getSaveButton() {
        return (MarkupContainer) new AjaxLink<ContentVersion>("save") {

            @Override
            public boolean isEnabled() {
                return isButtonCanBeActivated();
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    logger.debug("Enregistrement des donn??es du formulaire.");
                    ContentVersion contentVersion = EditPropertiesPanel.this.getModel().getObject();
                    new ContentVersionDao(contentVersion.getObjectContext()).save(contentVersion, true);
                    // Message
                    info(MessagesUtils.getLocalizedMessage("save.ok", EditPropertiesPanel.class, (Object) null));
                    // On recharge la page avec une nouvelle version ?? ??diter
                    initContentVersionModel();
                    addAllComponents(target);
                    addOrReplacePropertiesEditorPanels();
                    logger.debug("Version actuellement en ??dition : " + EditPropertiesPanel.this.getModel().getObject().getRevisionNumber());
                } catch (Exception e) {
                    logger.debug("Impossible d'enregistrer l'objet : " + e.toString());
                    String message = MessagesUtils.getLocalizedMessage("save.error", EditPropertiesPanel.class, (Object) null);
                    // Affiche le message d'erreur
                    fr.solunea.thaleia.webapp.panels.PanelUtils.showErrorMessage(message, e.toString(), feedbackPanel);
                    target.add(feedbackPanel);
                }
            }
        }.setOutputMarkupId(true);
    }

    /**
     * Ajoute tous les composants Wickets de la page ?? la target, afin de rafraichir tous les ??l??ments pr??sent??s dans le panneau.
     */
    protected void addAllComponents(AjaxRequestTarget target) {
        target.add(form);
        target.add(title);
        target.add(versionsPanel);
        target.add(saveButton);
        target.add(cancelButton);
        target.add(feedbackPanel);
    }

    protected MarkupContainer getCancelButton() {
        return (MarkupContainer) new AjaxLink<ContentVersion>("cancel") {

            @Override
            public boolean isEnabled() {
                return isButtonCanBeActivated();
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                // On supprime la version qui a ??t?? modifi??e (elle avait ??t?? cr????e pour cette page uniquement), et on
                // recharge une nouvelle version ?? ??diter, bas??e sur la derni??re existante du contenu.

                // On recharge la page avec une nouvelle version ?? ??diter, dans un nouveau contexte temporaire.
                initContentVersionModel();
                addAllComponents(target);
                addOrReplacePropertiesEditorPanels();
            }

        }.setOutputMarkupId(true);
    }

    @SuppressWarnings("unchecked")
    protected IModel<ContentVersion> getModel() {
        return (IModel<ContentVersion>) getDefaultModel();
    }

    /**
     * @return true s'il faut proposer les boutons de t??l??chargement des sources dans le panneau des versions de ce
     * contenu.
     */
    public abstract boolean showSourceLinks();

    /**
     * Ajout du bouton Retour.
     */
    protected void addBtnBack() {
        add(new Link<>("btnBack") {
            public void onClick() {
                onOut();
            }
        });
    }

}
