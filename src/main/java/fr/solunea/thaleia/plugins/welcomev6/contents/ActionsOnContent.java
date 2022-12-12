package fr.solunea.thaleia.plugins.welcomev6.contents;

import fr.solunea.thaleia.model.*;
import fr.solunea.thaleia.plugins.welcomev6.download.DownloadPageWithNavigation;
import fr.solunea.thaleia.plugins.welcomev6.messages.LocalizedMessages;
import fr.solunea.thaleia.plugins.welcomev6.publish.PublicationProcess;
import fr.solunea.thaleia.service.LicenceService;
import fr.solunea.thaleia.service.PublicationService;
import fr.solunea.thaleia.service.utils.export.ExportFormat;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.utils.MessagesUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Propose des méthodes pour déterminer si des actions sur des contenus sont possibles, et si oui, comment les traiter.
 * La partie abstraite ne fait que des actions communes à tous les plugins (vérification des droits, licences, etc.) :
 * les actions spécifiques (export, etc.) sont surdéfinie pour que ce soit le plugin qui fasse les traitements
 * effectifs.
 */
public abstract class ActionsOnContent implements Serializable {

    protected static final Logger logger = Logger.getLogger(ActionsOnContent.class);
    private User authenticatedUser;

    public ActionsOnContent() {
        this.authenticatedUser = ThaleiaSession.get().getAuthenticatedUser();
    }

    /**
     * Publie ce contenu dans cette publication.
     *
     * @param callingPage la page qui appelle le traitement, pour y afficher des erreurs éventuelles.
     */
    protected void publish(IModel<ContentVersion> contentVersion, final IModel<Locale> locale, List<Publication>
            publications, Page callingPage) {

        final List<String> errorMessagesKeys = new ArrayList<>();
        try {
            PublicationProcess publicationProcess = new PublicationProcess() {
                @Override
                protected File export() throws DetailedException {
                    // Préparation du paquet dans un fichier temporaire
                    return getFileForExport(contentVersion, locale, errorMessagesKeys, ExportFormat
                            .PUBLICATION_PLATFORM_DEFINED);
                }
            };
            publicationProcess.publish(callingPage, locale, publications, contentVersion.getObject().getContent());
        } catch (Exception e) {
            logger.debug(LogUtils.getStackTrace(e.getStackTrace()));

            // On présente les erreurs transmises par le
            // traitement
            for (String key : errorMessagesKeys) {
                callingPage.error(LocalizedMessages.getMessage(key));
            }

            callingPage.error(LocalizedMessages.getMessage("publish.error"));
        }

    }

    /**
     * Prépare et retourne le fichier destiné à être envoyé lors d'une demande d'export.
     *
     * @param format le format demandé pour le résultat
     */
    protected abstract File getFileForExport(IModel<ContentVersion> version, IModel<Locale> locale, List<String>
            errorMessageKeys, ExportFormat format) throws DetailedException;

    /**
     * Le plugin Publish est il actif ?
     */

    private boolean isPublishPluginActive() {
        String publicationEditPageName = "fr.solunea.thaleia.plugins.analyze.pages.PublicationEditPage";
        try {
            Class.forName(publicationEditPageName, true, ThaleiaSession.get().getPluginService().getClassLoader());
        } catch (Exception e) {
            logger.debug("Impossible de retrouver une instance correcte de la page de publication : " + e);
            return false;
        }
        return true;
    }

    /**
     * Retourne true si il existe une version de ce contenu publié
     */

    private boolean isContentIsVersioned(IModel<Content> content) {
        return (content.getObject() != null && content.getObject().getLastVersion() != null);
    }

    /**
     * Retourne true si une publication existe pour ce contenu
     */
    private boolean isContentAlreadyPublished(IModel<Content> content, IModel<Locale> locale) {
        try {
            // On demande la liste des publications actives, sur lesquelles
            // l'utilisateur a la visibilité, pour ce contenu et cette locale.
            List<Publication> publications = ThaleiaSession.get().getPublicationService().find(
                    authenticatedUser,
                    content.getObject(),
                    locale.getObject(),
                    new PublicationService.ContentVersionAnalyzer() {
                        @Override
                        public boolean isContentAvailable(ContentVersion version, Locale locale) {
                            return isPreviewable(Model.of(version), Model.of(locale));
                        }
                    },
                    true);

            // Y-en a-t-il une à mettre à jour ?
            for (Publication publication : publications) {
                if (!ThaleiaSession.get().getPublicationService().isPublicationUpToDate(publication)) {
                    return true;
                }
            }
        } catch (DetailedException e) {
            logger.warn(e);
            return false;
        }

        return false;
    }

    /**
     * Le contenu peut-il être publié ?
     */
    final public boolean isPublishable(IModel<Content> content) {

        return isPublishPluginActive() && isContentIsVersioned(content);
    }


    /**
     * Y-a-t'il des publications à mettre à jour ?
     */
    final public boolean isPublicationsToUpdate(IModel<Content> content, IModel<Locale> locale) {
        return isContentAlreadyPublished(content, locale) && isPublishPluginActive();
    }

    /**
     * Le contenu peut-il être exporté ?
     */

    final public boolean isExportable(IModel<Content> content) {
        // On ne le présente que s'il existe une version à ce contenu
        return isContentIsVersioned(content);
    }

    /**
     * Peut-on accéder aux stats du contenu ?
     */
    final public boolean isAnalyzable(IModel<Content> content, IModel<Locale> locale) {
        return isPublishPluginActive() && isContentAlreadyPublished(content, locale);
    }

    /**
     * Le contenu peut-il être prévisualisé ?
     */
    protected abstract boolean isPreviewable(IModel<ContentVersion> contentVersion, IModel<Locale> locale);

    /**
     * Si editable est false, alors on renvoie false. Mais sinon, on teste d'autres conditions.
     *
     * @param content  le contenu
     * @param editable editable par défaut ?
     * @return le contenu est-il éditable ?
     */
    final boolean isEditable(IModel<Content> content, Boolean editable) {
        // Si on a demandé de le cacher, on le cache.
        // On ne le présente que s'il existe une version à ce contenu
        return editable && isContentIsVersioned(content);
    }

    /**
     * Peut-on modifier les propriétés du contenu ?
     */
    final boolean isPropertiesEditable(IModel<Content> content) {
        // On ne le présente que s'il existe une version à ce contenu
        return isContentIsVersioned(content);
    }

    /**
     * Le contenu peut-il être supprimé ?
     */
    final boolean isDeletable(IModel<Content> content) {
        // Dans tous les cas, un admin peut effectuer une suppression
        if (authenticatedUser.getIsAdmin()) {
            return true;
        }

        // On ne propose pas la suppression s'il existe une publication
        // de ce contenu (active ou non)
        try {
            PublicationService publicationService = ThaleiaSession.get().getPublicationService();
            List<Publication> publications = publicationService.listContentPublications(content.getObject(),
                    authenticatedUser, false);
            return publications.isEmpty();

        } catch (DetailedException e) {
            logger.warn(e);
            return false;
        }
    }

    /**
     * Vérification si la publication est possible, puis appel à onSpecificPublish si ok.
     */
    final public void onPublish(IModel<Content> content, IModel<Locale> locale, AjaxRequestTarget target,
                                ThaleiaFeedbackPanel feedbackPanel) {
        // logger.debug("Clic sur le bouton de publication.");

        LicenceService licenceService;
        try {
            licenceService = ThaleiaSession.get().getLicenceService();
        } catch (DetailedException e) {
            // Message d'erreur
            ThaleiaSession.get().error(LocalizedMessages.getMessage("creation.error", (Object[]) null));
            if (target != null && feedbackPanel != null) {
                target.add(feedbackPanel);
            }
            return;
        }

        // On commence par vérifier s'il reste des publications disponibles permises par la licence
        if (licenceService.isLicencePermitsPublications(authenticatedUser)) {
            // Ok !
            // logger.debug("Appel de la fonction de publication du plugin concerné.");
            Page page = null;
            if (feedbackPanel != null) {
                page = feedbackPanel.getPage();
            }
            contentPublish(content, locale, new ArrayList<>(), page);

        } else {
            // Pas assez de publications dans la licence !

            // Les publications permises par la licence
            int maxPublications = licenceService.getMaxPermittedPublications(authenticatedUser);

            if (ThaleiaApplication.get().getConfiguration().isOnlineLicenceBuyActivated()) {
                // Message d'erreur qui propose d'acheter une licence
                String licencesUrl = ThaleiaApplication.get().getApplicationRootUrl()
                        + ThaleiaApplication.get().getConfiguration().getLicencesPageMountPoint();

                if (maxPublications == 0) {
                    ThaleiaSession.get().error(LocalizedMessages.getMessage("noPublishCredit.error", maxPublications));
                } else {
                    ThaleiaSession.get().error(LocalizedMessages.getMessage("toomuch.publications.error.buy",
                            maxPublications, licencesUrl));
                }
            } else {
                // Message d'erreur
                if (maxPublications == 0) {
                    ThaleiaSession.get().error(LocalizedMessages.getMessage("noPublishCredit.error", maxPublications));
                } else {
                    ThaleiaSession.get().error(LocalizedMessages.getMessage("toomuch.publications.error", maxPublications));
                }
            }
            if (target != null && feedbackPanel != null) {
                target.add(feedbackPanel);
            }
        }
    }

    final public void onExport(IModel<Content> content, IModel<Locale> locale, AjaxRequestTarget target,
                               ThaleiaFeedbackPanel feedbackPanel) {
        // D'abord, vérification de ce que permet la licence
        try {
            LicenceService licenceService = ThaleiaSession.get().getLicenceService();
            if (licenceService.isLicencePermitsExport(authenticatedUser, content.getObject().getLastVersion().getContentType())) {

                // On appelle la fonction d'export du plugin
                launchExportPage(content, locale, feedbackPanel.getPage());

            } else {
                // Message d'erreur
                // Export interdit !

                if (ThaleiaApplication.get().getConfiguration().isOnlineLicenceBuyActivated()) {
                    // Message d'erreur qui propose d'acheter une
                    // licence
                    String licencesUrl = ThaleiaApplication.get().getApplicationRootUrl()
                            + ThaleiaApplication.get().getConfiguration().getLicencesPageMountPoint();
                    ThaleiaSession.get().error(LocalizedMessages.getMessage("export.not.permited.error.buy",
                            licencesUrl));
                } else {
                    // Message d'erreur sans proposer d'acheter une
                    // licence
                    ThaleiaSession.get().error(LocalizedMessages.getMessage("export.not.permited.error"));
                }

                target.add(feedbackPanel);
            }
        } catch (DetailedException e) {
            logger.warn(e);
        }
    }

    /**
     * Lance la page qui traite l'export de ce contenu, et présente les résultats de cet export (fichier ou erreurs).
     */
    public void launchExportPage(IModel<Content> contentModel, IModel<Locale> localeModel, Page page) {
        page.setResponsePage(new DownloadPageWithNavigation(false, page) {

            @Override
            protected File prepareFile() throws DetailedException {
                List<String> errorMessagesKeys = new ArrayList<>();

                File result;
                try {
                    result = getFileForExport(Model.of(contentModel.getObject().getLastVersion()), localeModel,
                            errorMessagesKeys, ExportFormat.USER_DEFINED);

                } catch (DetailedException e) {

                    // On présente les erreurs transmises par le
                    // traitement
                    for (String key : errorMessagesKeys) {
                        error(LocalizedMessages.getMessage(key));
                    }

                    throw e.addMessage("Impossible de préparer le fichier de l'export pour ce plugin.");
                }

                return result;
            }

            @Override
            protected String getFileName() {
                return contentModel.getObject().getLastVersion().getContentIdentifier();
            }

        });
    }

    /**
     * Procède aux actions de publication de ce contenu dans ces publications, et place des messages d'erreurs dans la
     * page si besoin.
     */
    private void contentPublish(final IModel<Content> content, final IModel<Locale> locale, List<Publication>
            publications, Page page) {
        final List<String> errorMessagesKeys = new ArrayList<>();
        try {
            PublicationProcess publicationProcess = new PublicationProcess() {
                @Override
                protected File export() throws DetailedException {
                    // Préparation du paquet dans un fichier temporaire
                    // On force un format SCORM 2004 car notre lecteur ne
                    // comprend pas le format SCORM 1.2
                    logger.debug("Export pour la publication de la version " + content.getObject().getLastVersion());
                    return getFileForExport(Model.of(content.getObject().getLastVersion()), locale,
                            errorMessagesKeys, ExportFormat.PUBLICATION_PLATFORM_DEFINED);
                }
            };
            logger.debug("Publication du contenu " + content.getObject());
            publicationProcess.publish(page, locale, publications, content.getObject());

        } catch (Exception e) {
            logger.debug(LogUtils.getStackTrace(e.getStackTrace()));

            // On présente les erreurs transmises par le
            // traitement
            for (String key : errorMessagesKeys) {
                ThaleiaSession.get().error(LocalizedMessages.getMessage(key));
            }

            ThaleiaSession.get().error(LocalizedMessages.getMessage("publish.error"));
        }
    }

    /**
     * Le lancement d'une fenêtre de prévisualisation de cette version de ce contenu.
     */
    public abstract void onPreview(final IModel<ContentVersion> contentVersion, final IModel<Locale> locale);

    /**
     * Le lancement d'une fenêtre de présentation des statistiques d'utilisation des publications de ce contenu.
     */
    public abstract void onAnalyze(IModel<Content> content);

    /**
     * Lancement du rafraîchissement du modèle des contenus après la suppression de l'un d'eux.
     */
    public abstract void refreshModelAfterDelete();

    /**
     * L'action à traiter lors de la demande d'édition d'un contenu.
     */
    public abstract void onEdit(AjaxRequestTarget target, IModel<Content> content, IModel<Locale> locale);

    final public void onDelete(IModel<Content> content, AjaxRequestTarget target, Page page) {
        try {
            // On ne supprime pas les publications de ce contenu, mais on supprime le lien de ce contenu comme source
            // de la publication
            List<Publication> publications = ThaleiaSession.get().getPublicationService().listContentPublications
                    (content.getObject(), authenticatedUser, true);
            for (Publication publication : publications) {
                publication.setSourceVersion(null);
            }

            // Suppression des contenus
            ThaleiaSession.get().getContentService().deleteContentsAndAllVersions(Collections.singletonList(content.getObject()));
            // Enregistrement
            content.getObject().getObjectContext().commitChanges();

            // Message de réussite
            String message = MessagesUtils.getLocalizedMessage("delete.ok", ActionsOnContent.class);
            ThaleiaSession.get().info(message);

            // On appelle le rafraîchissement des données de la table qui
            // présentait le contenu supprimé
            // On s'assure que les contenus de la table
            // seront rechargés, pour ne plus présenter ceux
            // supprimés. C'est l'intérêt du
            // LoadableDetachableModel.
            refreshModelAfterDelete();

            // On recharge la page.
            page.setResponsePage(target.getPage());

        } catch (DetailedException e) {
            logger.warn("Impossible de supprimer les contenus sélectionnés : " + e.toString());

            // Message d'erreur
            String message = MessagesUtils.getLocalizedMessage("delete.error", ActionsOnContent.class);
            ThaleiaSession.get().error(message);
        }
    }

    /**
     * Met à jour toutes les publications de ce contenu avec la dernière version de ce contenu.
     */
    final public void onUpdatePublish(IModel<Content> content, IModel<Locale> locale, Page page) {
        try {
            List<Publication> publications = ThaleiaSession.get().getPublicationService().listContentPublications
                    (content.getObject(), authenticatedUser, true);

            logger.debug("Publications à mettre à jour : " + publications);

            contentPublish(content, locale, publications, page);

        } catch (DetailedException e) {
            logger.warn(e);
            ThaleiaSession.get().error(LocalizedMessages.getMessage("publish.update.error"));
        }
    }

    /**
     * Télécharge le fichier ZIP qui contient la source de ce contenu, par exmeple le fichier Excel d'un contenu
     * Cannelle.
     */
    public abstract File getSourceFile(IModel<ContentVersion> version, IModel<Locale> locale);

    /**
     * Est-ce qu'un fichier ZIP qui contient la source de ce contenu (par exmeple le fichier Excel d'un contenu
     * Cannelle) est disponible ?
     */
    public abstract boolean sourceAvailable(IModel<ContentVersion> version, IModel<Locale> locale);

    /**
     * L'intitulé de ce cette version.
     *
     * @return le titre de ce contenu. Il faut implémenter la recherche du titre présenté dans les contentProperties du
     * contenu.
     */
    public abstract String getContentTitle(ContentVersion contentVersion, Locale locale);

}
