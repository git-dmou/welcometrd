package fr.solunea.thaleia.plugins.welcomev6.contents;

import fr.solunea.thaleia.model.Content;
import fr.solunea.thaleia.model.ContentVersion;
import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.model.dao.ContentDao;
import fr.solunea.thaleia.model.dao.ContentVersionDao;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.panels.confirmation.ConfirmationLink;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.utils.MessagesUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.util.encoding.UrlEncoder;
import org.apache.wicket.util.time.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Présente un tableau ces révisions d'un contenu.
 */
public class RevisionsListPanel extends Panel {

    protected static final Logger logger = Logger.getLogger(RevisionsListPanel.class);

    private final int contentId;
    private final ThaleiaFeedbackPanel feedbackPanel;

    public RevisionsListPanel(String id, IModel<Content> contentModel, ActionsOnContent actionsOnContent,
                              IModel<Locale> localeModel, ThaleiaFeedbackPanel feedbackPanel, boolean showLastVersion
            , boolean showSourceLink) {
        super(id, contentModel);

        ContentDao contentDao = new ContentDao(contentModel.getObject().getObjectContext());
        ContentVersionDao contentVersionDao = new ContentVersionDao(contentModel.getObject().getObjectContext());

        this.feedbackPanel = feedbackPanel;
        contentId = contentDao.getPK(contentModel.getObject());

        setDefaultModel(new LoadableDetachableModel<Content>() {
            @Override
            protected Content load() {
                return contentDao.get(contentId);
            }
        });

        // On masque éventuellement la colonne de téléchargement des sources
        add(new WebMarkupContainer("downloadHeader") {
            @Override
            public boolean isVisible() {
                return showSourceLink;
            }
        });

        // La suppression des anciennes versions
        add(new DeleteOldVersionsLink("deleteOldVersionsLink", contentModel, MessagesUtils.getLocalizedMessage("delete.confirm", RevisionsListPanel.class)));

        // Préparation des lignes du tableau
        add(new PropertyListView<>("objects", new LoadableDetachableModel<List<ContentVersion>>() {
            @Override
            public List<ContentVersion> load() {
                try {

                    // On ne garde que les versions qui sont en base : pas la
                    // nouvelle qui a été créée si on est sur un écran de nouvelle
                    // version.
                    //                    result.addAll(((Content) (getDefaultModel().getObject())).getVersions()
                    //                    .stream().filter
                    //                            (contentVersionDao::isCommitedInDatabase).collect(Collectors.toList
                    //                            ()));
                    List<ContentVersion> result = ((Content) (getDefaultModel().getObject())).getVersions().stream().filter(contentVersion -> {
                        // La version doit être commitée en base
                        if (!contentVersionDao.isCommitedInDatabase(contentVersion)) {
                            return false;
                        } else {
                            // Le contenu associé à cette version doit exister. Par exemple, si un contenu n'a
                            // été généré qu'en FR, les versions dans les autres langues n'ont  pas de contenu.
                            return actionsOnContent.isPreviewable(Model.of(contentVersion), localeModel);
                        }
                    }).collect(Collectors.toList());

                    // Si demandé, on ne présente pas la dernière version.
                    if (!showLastVersion && result.size() > 0) {
                        result.remove(result.size() - 1);
                    }

                    //On trie de la plus récente à la plus ancienne
                    result.sort((o1, o2) -> o2.getRevisionNumber().compareTo(o1.getRevisionNumber()));

                    return result;

                } catch (Exception e) {
                    logger.info("Impossible de retrouver la liste des versions : " + e);
                    return new ArrayList<>();
                }
            }
        }) {

            @Override
            protected void populateItem(final ListItem<ContentVersion> item) {

                // Titre
                item.add(new Label("title", new LoadableDetachableModel<String>() {
                    @Override
                    public String load() {
                        return actionsOnContent.getContentTitle(item.getModelObject(), localeModel.getObject());
                    }
                }));

                // Date de publication
                item.add(new Label("date", new LoadableDetachableModel<String>() {
                    @Override
                    public String load() {
                        // On présente la date dans la locale de l'IHM.
                        FastDateFormat format = FastDateFormat.getDateTimeInstance(FastDateFormat.SHORT,
                                FastDateFormat.SHORT, ThaleiaSession.get().getLocale());
                        return format.format(item.getModel().getObject().getLastUpdateDate());
                    }
                }));

                // Auteur
                item.add(new Label("author", new LoadableDetachableModel<String>() {
                    @Override
                    public String load() {
                        return item.getModelObject().getAuthor().getName();
                    }
                }));

                // Prévisualisation
                item.add(new PreviewPanel("preview", new LoadableDetachableModel<>() {
                    @Override
                    protected ContentVersion load() {
                        return item.getModel().getObject();
                    }
                }, localeModel, actionsOnContent, false) {
                });

//                previewPanel.add(new Label("previewLabel", "the string to display"));

                // Téléchargement de la source
                item.add(new DownloadLink("source", new AbstractReadOnlyModel<>() {
                    @Override
                    public File getObject() {
                        return actionsOnContent.getSourceFile(item.getModel(), localeModel);
                    }
                }, new AbstractReadOnlyModel<>() {
                    @Override
                    public String getObject() {
                        // Le nom du fichier dans la réponse HTTP
                        String downloadFilename = item.getModelObject().getContentIdentifier() + "_v"
                                + item.getModelObject().getRevisionNumber() + ".zip";

                        return UrlEncoder.QUERY_INSTANCE.encode(downloadFilename, "UTF-8");
                    }
                }) {
                    @Override
                    public boolean isVisible() {
                        return showSourceLink && actionsOnContent.sourceAvailable(item.getModel(), localeModel);
                    }

                }.setCacheDuration(Duration.NONE));
            }
        });
    }

    // Le bouton de suppression des anciennes versions
    private class DeleteOldVersionsLink extends ConfirmationLink<Content> {

        public DeleteOldVersionsLink(String id, IModel<Content> model, String text) {
            super(id, model, Model.of(text));
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            try {
                ThaleiaSession.get().getContentService().deleteOldVersions(getModelObject());

                StringResourceModel messageModel = new StringResourceModel("delete.old.ok", RevisionsListPanel.this, null);
                ThaleiaSession.get().info(messageModel.getString());

                // On recharge la page
                setResponsePage(getPage());

            } catch (DetailedException e) {
                logger.warn(
                        "Impossible de supprimer les anciennes versions du contenu " + getModelObject() + " : " + e);

                StringResourceModel messageModel = new StringResourceModel("delete.old.error", RevisionsListPanel.this,
                        null);
                ThaleiaSession.get().warn(messageModel.getString());
                target.add(feedbackPanel);
            }
        }

    }
}
