package fr.solunea.thaleia.plugins.welcomev6.publish;

import fr.solunea.thaleia.model.Content;
import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.model.Publication;
import fr.solunea.thaleia.model.dao.ContentDao;
import fr.solunea.thaleia.model.dao.LocaleDao;
import fr.solunea.thaleia.model.dao.PublicationDao;
import fr.solunea.thaleia.plugins.welcomev6.messages.LocalizedMessages;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.wicket.Page;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Cette classe va effectuer les traitements de publication (export, puis
 * création d'une publication ou mise à jour d'une publication existante) dans
 * une page de "traitement en cours".
 */
@SuppressWarnings("serial")
public abstract class PublicationProcess implements Serializable {

    /**
     * Les actions d'export, qui seront traitées avant la publication, et qui
     * vont produire le fichier à publier.
     */
    protected abstract File export() throws DetailedException;

    public void publish(final Page originPage, IModel<Locale> localeModel, final List<Publication> publications, final
    Content content) {
        // On exporte, puis on publie, et enfin on ouvre la page d'édition de la publication.

        originPage.setResponsePage(new PublishPage() {
            @Override
            protected void prepareFile() throws DetailedException {

                try {
                    // Appel de la fonction d'export.
                    final File toPublish = export();

                    // On demande au plugin de publication de procéder à
                    // l'analyse du paquet et à sa publication.
                    // On ne fait pas une dépendance dans le POM (et un appel
                    // direct à la classe), mais plutôt une tentative
                    // d'instanciation dynamique, car on ne veut pas
                    // introduire de dépendance.
                    // De toutes façons, le bouton d'export n'est visible que si
                    // le plugin de publication a été installé.
                    Class<?> newPublicationPageClass = Class.forName("fr.solunea.thaleia.plugins.publish" + "" + "" +
                            ".NewPublicationPage", true, ThaleiaSession.get().getPluginService().getClassLoader());

                    // On s'assure que les objets Cayenne sont bien attachés dans un nouveau contexte
                    ObjectContext newContext = ThaleiaSession.get().getContextService().getNewContext();
                    ContentDao contentDao = new ContentDao(newContext);
                    PublicationDao publicationDao = new PublicationDao(newContext);
                    Content contentChecked = contentDao.get(contentDao.getPK(content));
                    List<Publication> publicationsChecked = new ArrayList<>();
                    for (Publication publication : publications) {
                        publicationsChecked.add(publicationDao.get(publicationDao.getPK(publication)));
                    }
                    Locale locale = new LocaleDao(newContext).get(localeModel.getObject().getObjectId());

                    // Appel dynamique de la méthode :
                    // NewPublicationPage.publish()
                    // On appelle sur l'objet null car la méthode "publish"  est statique
                    final Publication publication = (Publication) (newPublicationPageClass.getMethod("publish", File
                            .class, String.class, List.class, Content.class, Locale.class).invoke(null, toPublish,
                            toPublish.getName(), publicationsChecked, contentChecked, locale));

                    if (publication != null) {
                        // Si la méthode de publication a renvoyé une publication, on ouvre sa page d'édition.
                        logger.debug("La publication a été générée : on ouvre la page d'édition de cette publication : " + publication);

                        // On instancie une page d'édition de cette publication.
                        Class<?> publicationEditPageClass = Class.forName("fr.solunea.thaleia.plugins.analyze.pages.PublicationEditPage",
                                true, ThaleiaSession.get().getPluginService().getClassLoader());
                        Constructor<?> constructor = publicationEditPageClass.getConstructor(IModel.class, Class.class);

                        Page editPage = (Page) constructor.newInstance(new LoadableDetachableModel<Publication>() {
                            @Override
                            protected Publication load() {
                                logger.debug("Chargement de la publication : " + publication);
                                // Au moment où on charge le modèle,  il peut avoir été sérialisé, car on le transmet entre pages et panels.
                                // Donc il peut avoir été devenu hollow.
                                Publication result = publicationDao.get(publication.getObjectId());
                                logger.debug("Publication renvoyée : " + publication);
                                return result;
                            }
                        }, originPage.getClass()); // <- la page de retour en sortie de la page d'édition de la publication

                        // Redirection vers la page d'édition de la publication
                        setResponsePage(editPage);

                    } else {
                        // On ajoute un message indiquant ce qui a été fait, et on retourne sur la page d'origine pour présenter
                        // les messages
                        originPage.info(LocalizedMessages.getMessage("publication.updated", (Object[]) null));
                        setResponsePage(originPage);
                    }

                } catch (Exception e) {
                    logger.warn(LogUtils.getStackTrace(e.getStackTrace()));
                    originPage.error(LocalizedMessages.getMessage("publication.export.error", (Object[]) null));
                    throw new DetailedException(e).addMessage("Impossible d'instancier la page d'édition de la " +
                            "publication.");
                }
            }
        });
    }

}
