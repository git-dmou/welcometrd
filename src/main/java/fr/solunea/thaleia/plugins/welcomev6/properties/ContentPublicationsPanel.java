package fr.solunea.thaleia.plugins.welcomev6.properties;

import fr.solunea.thaleia.model.Content;
import fr.solunea.thaleia.model.Publication;
import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.auto._Publication;
import fr.solunea.thaleia.model.dao.ContentDao;
import fr.solunea.thaleia.model.dao.PublicationDao;
import fr.solunea.thaleia.model.dao.UserDao;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.BasePage;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Présente les publications d'un contenu, la possibilité d'un créer de
 * nouvelles, de les mettre à jour.
 */
@SuppressWarnings("serial")
public abstract class ContentPublicationsPanel extends Panel {

    protected static final Logger logger = Logger.getLogger(ContentPublicationsPanel.class);

    private final int contentId;

    public ContentPublicationsPanel(String id, IModel<Content> content) {
        super(id);

        // Un contexte spécifique pour l'édition
        ObjectContext objectContext = ThaleiaSession.get().getContextService().getNewContext();
        ContentDao contentDao = new ContentDao(objectContext);
        PublicationDao publicationDao = new PublicationDao(objectContext);
        User user = new UserDao(objectContext).get(ThaleiaSession.get().getAuthenticatedUser().getObjectId());
        contentId = contentDao.getPK(content.getObject());

        setDefaultModel(new LoadableDetachableModel<Content>() {
            @Override
            protected Content load() {
                return contentDao.get(contentId);
            }
        });

        // Préparation des lignes du tableau
        LoadableDetachableModel<List<Publication>> publications = new LoadableDetachableModel<>() {
            @Override
            public List<Publication> load() {
                // On ne présente que les Publications de ce contenu sur lesquels cet utilisateur a la visibilité
                try {
                    List<Publication> result = ThaleiaSession.get().getPublicationService().find(
                            user, getPanelModel().getObject(), false);
                    // On trie du plus récent au plus vieux
                    Comparator<Publication> comparator = publicationDao.getCreationDateComparator();
                    result.sort(comparator);
                    Collections.reverse(result);
                    return result;

                } catch (DetailedException e) {
                    logger.info("Impossible de retrouver la liste des publications : " + e);
                    return new ArrayList<>();
                }
            }
        };
        PropertyListView tableau = new PropertyListView<>("objects", publications) {

            @Override
            protected void populateItem(final ListItem<Publication> item) {

                // Le titre de la publication est également le lien public vers
                // la publication
                String previewUrl = ThaleiaApplication.get().getPublishUrl() + "/" + ((_Publication) item
                        .getDefaultModelObject()).getReference();
                item.add(new ExternalLink("previewLink", previewUrl, new PropertyModel<String>(item
                        .getDefaultModelObject(), "name").getObject()).setPopupSettings(
                        new PopupSettings("_blank")));

                // Date de publication
                item.add(new Label("date", new LoadableDetachableModel<String>() {
                    @Override
                    public String load() {
                        // On formatte l'horodate, si elle
                        // existe
                        if (((_Publication) item.getDefaultModelObject()).getDate() == null) {
                            return "";
                        } else {
                            FastDateFormat format = FastDateFormat.getDateTimeInstance(FastDateFormat.SHORT,
                                    FastDateFormat.SHORT, ThaleiaSession.get().getLocale());
                            return format.format(((_Publication) item.getDefaultModelObject()).getDate());
                        }
                    }
                }));

                // Actif
                AjaxCheckBox chkActive = new AjaxCheckBox("active", new PropertyModel<>(item.getDefaultModelObject(),
                        "active")) {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        try {
                            Publication publication = (Publication) item.getDefaultModelObject();
                            publication.setActive(getModel().getObject());
                            publicationDao.save(publication, true);
                        } catch (DetailedException e) {
                            logger.warn("Impossible d'enregistrer l'objet : " + e.toString());
                        }
                        //target.appendJavaScript("event.stopImmediatePropagation();");
                    }
                };
                item.add(chkActive);

                // Accès public
                AjaxCheckBox publicAccess = new AjaxCheckBox("publicAccess", new PropertyModel<>(item
                        .getDefaultModelObject(), "publicAccess")) {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        try {
                            Publication publication = (Publication) item.getDefaultModelObject();
                            publication.setPublicAccess(getModel().getObject());
                            publicationDao.save(publication, true);
                        } catch (DetailedException e) {
                            logger.warn("Impossible d'enregistrer l'objet : " + e.toString());
                        }
                        //target.appendJavaScript("event.stopImmediatePropagation();");
                    }
                };
                item.add(publicAccess);

                // Nombre d'accès privés
                item.add(new Label("privateAccesses", new LoadableDetachableModel<String>() {
                    @Override
                    public String load() {
                        return String.valueOf(item.getModelObject().getRecipients().size());
                    }
                }));

                // L'édition
                item.add(new Link<Void>("editLink") {
                    @Override
                    public void onClick() {
                        onEdit(item.getModel());
                    }

                });

            }
        };
        addOrReplace(tableau);
    }

    private IModel<Content> getPanelModel() {
        return (IModel<Content>) getDefaultModel();
    }

    /**
     * Traitement à déclencher lors de la sélection du bouton "éditer" d'une publication.
     *
     * @param model la publication à éditer
     */
    protected abstract void onEdit(IModel<Publication> model);
}
