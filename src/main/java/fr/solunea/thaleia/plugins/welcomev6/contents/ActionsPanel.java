package fr.solunea.thaleia.plugins.welcomev6.contents;

import fr.solunea.thaleia.model.Content;
import fr.solunea.thaleia.model.ContentVersion;
import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.model.dao.*;
import fr.solunea.thaleia.model.dao.LmsAccessDao;
import fr.solunea.thaleia.model.dao.UserDao;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.panels.confirmation.ConfirmationLink;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;

/**
 * Un panneau contenant des actions à effectuer sur un contenu : éditer,
 * supprimer, modifier...
 */
@SuppressWarnings("serial")
public abstract class ActionsPanel extends Panel {

    protected static final Logger logger = Logger.getLogger(ActionsPanel.class);
    final IModel<Locale> localeModel;
    private final IModel<Content> contentModel;
    private final ObjectId contentId;
    private final IModel<ContentVersion> contentVersionModel;
    protected ActionsOnContent actionsOnContent;
    boolean alwaysLastVersion;

    /**
     * @param locale la locale dans laquelle exporter le module le contenu à exporter
     */
    public ActionsPanel(String id,
                        final IModel<Content> content,
                        final IModel<Locale> locale,
                        final ThaleiaFeedbackPanel feedbackPanel,
                        ActionsOnContent actionsOnContent,
                        final IModel<ContentVersion> contentVersion,
                        boolean alwaysLastVersion) {
        super(id, content);

        ObjectContext objectContext = content.getObject().getObjectContext();
        ContentDao contentDao = new ContentDao(objectContext);
        LocaleDao localeDao = new LocaleDao(objectContext);
        LmsAccessDao lmsAccessDao = new LmsAccessDao(objectContext);
        UserDao userDao = new UserDao(objectContext);
        ContentVersionDao contentVersionDao = new ContentVersionDao(objectContext);

        this.alwaysLastVersion = alwaysLastVersion;
        this.actionsOnContent = actionsOnContent;
        final int contentId = contentDao.getPK(content.getObject());
        final int localeId = localeDao.getPK(locale.getObject());

        contentModel = new LoadableDetachableModel<>() {
            @Override
            protected Content load() {
                return contentDao.get(contentId);
            }
        };

        localeModel = new LoadableDetachableModel<>() {
            @Override
            protected Locale load() {
                return localeDao.get(localeId);
            }
        };

        final int contentVersionId = contentVersionDao.getPK(contentVersion.getObject());
        this.contentId = contentVersionDao.get(contentVersionId).getContent().getObjectId();

        contentVersionModel = new LoadableDetachableModel<>() {
            @Override
            protected ContentVersion load() {
                return contentVersionDao.get(contentVersionId);
            }
        };

        addEditLink();
        addPublishLink(locale, feedbackPanel);
        addUpdatePublicationLink(lmsAccessDao, userDao, locale);
        addExportLink(locale, feedbackPanel);
        addDeleteLink(contentDao);
        addPreviewLink(contentDao);

        add(new AjaxLink<Void>("editPropertiesLink") {

            @Override
            protected void onConfigure() {
                super.onConfigure();
                this.setVisible(actionsOnContent.isPropertiesEditable(contentModel));
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                onEditProperties(target, contentModel, localeModel);

            }
        });
    }

    /**
     * Ajoute le lien de prévisualisation du contenu.
     *
     */
    private void addPreviewLink(ContentDao contentDao) {
        add(new Link<Void>("previewLink") {

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                tag.put("target", "_blank");
            }

            @Override
            public void onClick() {
                if (alwaysLastVersion) {
                    actionsOnContent.onPreview(new LoadableDetachableModel<>() {
                        @Override
                        protected ContentVersion load() {
                            return contentDao.get(contentId).getLastVersion();
                        }
                    }, localeModel);
                } else {
                    actionsOnContent.onPreview(contentVersionModel, localeModel);
                }
            }

            @Override
            public boolean isEnabled() {
                return actionsOnContent.isPreviewable(contentVersionModel, localeModel);
            }

        });
    }

    /**
     * Ajoute le lien d'édition du contenu.
     */
    private void addEditLink() {
        add(new IndicatingAjaxLink<Void>("editLink") {

            @Override
            protected void onConfigure() {
                super.onConfigure();
                this.setVisible(actionsOnContent.isEditable(contentModel, showEditLink()));
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                onEdit(target, contentModel, localeModel);
            }
        });
    }

    /**
     * Ajoute le lien de publication du contenu.
     *
     */
    private void addPublishLink(IModel<Locale> locale, ThaleiaFeedbackPanel feedbackPanel) {
        add(new IndicatingAjaxLink<Void>("publishLink") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                logger.debug("Appel de la publication de " + contentModel.getObject());
                actionsOnContent.onPublish(contentModel, locale, target, feedbackPanel);
            }

            @Override
            protected void onConfigure() {
                super.onConfigure();
                this.setVisible(actionsOnContent.isPublishable(contentModel));
            }

        });
    }

    /**
     * Ajoute le lien d'exportation d'un contenu
     *
     */
    private void addExportLink(IModel<Locale> locale, ThaleiaFeedbackPanel feedbackPanel) {
        add(new IndicatingAjaxLink<Void>("exportLink") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                actionsOnContent.onExport(contentModel, locale, target, feedbackPanel);
            }

            @Override
            protected void onConfigure() {
                super.onConfigure();
                this.setVisible(actionsOnContent.isExportable(contentModel));
            }

        });
    }

    /**
     * Ajoute le lien d'éditions des propriétés du contenu.
     */
    @SuppressWarnings("unused")
    private void addEditPropertiesLink() {
        add(new AjaxLink<Void>("editPropertiesLink") {

            @Override
            protected void onConfigure() {
                super.onConfigure();
                this.setVisible(actionsOnContent.isPropertiesEditable(contentModel));
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                onEditProperties(target, contentModel, localeModel);
            }
        });
    }

    /**
     * Ajoute le lien de suppression d'un contenu.
     */
    private void addDeleteLink(ContentDao contentDao) {
        add(new ConfirmationLink<Void>("deleteLink", new StringResourceModel("delete.confirm", this, null, new
                Object[]{contentDao.getDisplayName(contentModel.getObject(), ThaleiaSession.get().getLocale())})) {

            @Override
            protected void onConfigure() {
                super.onConfigure();
                this.setVisible(actionsOnContent.isDeletable(contentModel));
            }


            @Override
            public void onClick(AjaxRequestTarget target) {
                actionsOnContent.onDelete(contentModel, target, this.getPage());
            }

        });
    }

    /**
     * Ajoute le lien de publication.
     *
     */
    private void addUpdatePublicationLink(LmsAccessDao lmsAccessDao, UserDao userDao, IModel<Locale> locale) {
        add(new IndicatingAjaxLink<Void>("updatePublicationLink") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                // Ok !
                onUpdatePublish(contentModel, localeModel);
            }

            @Override
            protected void onConfigure() {
                super.onConfigure();
                this.setVisible(actionsOnContent.isPublicationsToUpdate(contentModel, locale)
                        && lmsAccessDao.findByUserId(userDao.getPK(ThaleiaSession.get().getAuthenticatedUser()))
                        .isEmpty());
            }

        });
    }

    /**
     * L'action à effectuer lors de la demande d'édition de ce contenu.
     */
    protected abstract void onEdit(AjaxRequestTarget target, IModel<Content> content, IModel<Locale> locale);

    /**
     * L'action à effectuer lors de la demande d'édition des propriétés de ce
     * contenu.
     */
    protected abstract void onEditProperties(AjaxRequestTarget target, IModel<Content> content, IModel<Locale> locale);

    /**
     * @return doit-on présenter un lien d'édition ?
     */
    protected abstract boolean showEditLink();

    /**
     * L'action à effectuer lors de la demande de mise à jour de publication du
     * contenu.
     */
    protected abstract void onUpdatePublish(IModel<Content> content, IModel<Locale> locale);

}
