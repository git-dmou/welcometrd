package fr.solunea.thaleia.plugins.welcomev6.contents;

import fr.solunea.thaleia.model.ContentVersion;
import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.model.dao.ContentDao;
import fr.solunea.thaleia.model.dao.ContentVersionDao;
import fr.solunea.thaleia.model.dao.LocaleDao;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectId;
import org.apache.log4j.Logger;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 * Un panneau contenant des actions à effectuer sur un contenu : éditer,
 * supprimer, modifier...
 */
@SuppressWarnings("serial")
public abstract class PreviewPanel extends Panel {

    protected static final Logger logger = Logger.getLogger(PreviewPanel.class);
    final IModel<Locale> localeModel;
    private final IModel<ContentVersion> contentVersionModel;
    private final ObjectId contentId;
    protected ActionsOnContent actionsOnContent;

    /**
     * @param locale            la locale dans laquelle exporter le module le contenu à exporter
     * @param alwaysLastVersion si true, alors on récupère au moment du clic la dernière version de ce contenu. Si
     *                          false, alors on ne prend que la ContentVersion passée en modèle.
     */
    public PreviewPanel(String id, final IModel<ContentVersion> contentVersion, final IModel<Locale> locale, ActionsOnContent actionsOnContent, boolean alwaysLastVersion) {
        super(id, contentVersion);

        ContentVersionDao contentVersionDao = new ContentVersionDao(contentVersion.getObject().getObjectContext());
        ContentDao contentDao = new ContentDao(contentVersion.getObject().getObjectContext());
        LocaleDao localeDao = new LocaleDao(contentVersion.getObject().getObjectContext());

        this.actionsOnContent = actionsOnContent;
        final int contentVersionId = contentVersionDao.getPK(contentVersion.getObject());
        final int localeId = localeDao.getPK(locale.getObject());

        contentVersionModel = new LoadableDetachableModel<>() {
            @Override
            protected ContentVersion load() {
                return contentVersionDao.get(contentVersionId);
            }
        };

        contentId = contentVersionDao.get(contentVersionId).getContent().getObjectId();

        localeModel = new LoadableDetachableModel<>() {
            @Override
            protected Locale load() {
                return localeDao.get(localeId);
            }
        };

        // La prévisualisation
        // *******************
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
                return actionsOnContent.isPreviewable(contentVersionModel, locale);
            }

        });
    }

    @Override
    public boolean isVisible() {
        // On ne montre le bouton que s'il existe
        return actionsOnContent.isPreviewable(contentVersionModel, localeModel);
    }
}
