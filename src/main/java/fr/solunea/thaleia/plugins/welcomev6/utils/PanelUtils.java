package fr.solunea.thaleia.plugins.welcomev6.utils;

import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.time.Duration;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;

public class PanelUtils {

    private final static Logger logger = Logger.getLogger(PanelUtils.class);

    /**
     * @param id            l'identifiant du DownloadLink
     * @param filenameModel le nom du fichier, qui sera recherché dans le classloader, et
     *                      renvoyé à l'utilisateur.
     * @return un lien de téléchargement de ce fichier, retrouvé dans les
     * resources de l'application.
     */
    @SuppressWarnings("serial")
    public static WebMarkupContainer getDownloadLink(String id, final IModel<String> filenameModel) {
        return new DownloadLink(id, new AbstractReadOnlyModel<>() {

            @Override
            public File getObject() {

                // On récupère le binaire dans un fichier temporaire.
                // Il sera supprimé grâce à .setDeleteAfterDownload(true)
                logger.debug("Recherche du fichier à transmettre : " + filenameModel.getObject());

                InputStream is = null;
                try {
                    is = ThaleiaSession.get().getPluginService().getClassLoader().getResourceAsStream(filenameModel
                            .getObject());
                } catch (DetailedException e1) {
                    logger.debug("Impossible d'accéder aux fichiers du plugin : " + e1);
                }

                if (is == null) {
                    logger.warn("Le fichier '" + filenameModel.getObject() + "' n'a pas été trouvé !");
                    return null;
                }

                try {
                    File result = ThaleiaApplication.get().getTempFilesService().getTempFile(filenameModel.getObject());
                    FileUtils.copyInputStreamToFile(is, result);

                    // Le nom du fichier temporaire n'est plus
                    // filenameModel, mais c'est le DownloadLink qui
                    // associera le bon nom (voir constructeur).

                    return result;

                } catch (Exception e) {
                    logger.warn("Le fichier '" + filenameModel + "' n'a pas pu être copié : " + e);
                    return null;

                } finally {
                    IOUtils.closeQuietly(is);
                }
            }

        }, filenameModel) {
            @Override
            public void onClick() {
                logger.debug("Traitement du clic pour le fichier : " + filenameModel.getObject());
                super.onClick();
            }

        }.setCacheDuration(Duration.NONE).setDeleteAfterDownload(true);
        // On ne met rien en cache, car sinon un éventuel changement de filenameModel se sera pas pris en compte : ce
        // sera le bon modèle côté serveur, mais côté client le clic conduira à appeler dans le cache le binaire
        // précédement téléchargé (du filename précédent).
    }

    /**
     * @param id       l'identifiant du DownloadLink
     * @param filename le nom du fichier, qui sera recherché dans le classloader, et
     *                 renvoyé à l'utilisateur.
     * @return un lien de téléchargement de ce fichier, retrouvé dans les
     * resources de l'application.
     */
    @SuppressWarnings("serial")
    public static MarkupContainer getDownloadModuleLink(String id, final File file, String filename) {
        return new DownloadLink(id, new AbstractReadOnlyModel<File>() {

            @Override
            public File getObject() {
                return (file);
            }

        }, filename).setDeleteAfterDownload(true);
    }

    /**
     * @param id       l'id du panel
     * @param backPage le nom de la classe à appeler lors d'un clic sur le bouton
     *                 retour
     * @return le panneau qui contient le menu à présenter, ou un panneau vide
     * si rien n'a été trouvé.
     */
    public static Component getMenuPanel(String id, Page backPage, IModel<String> label) {
        // On recherche le menu dans le plugin qui contient les IHM communes aux
        // plugins Thaleia v5

        ClassLoader classLoader = ThaleiaApplication.get().getApplicationSettings().getClassResolver().getClassLoader();

        // Le nom de la classe de menu dans le plugin des IHM
        String menuClassName = "fr.solunea.thaleia.plugins.welcomev5.MenuPanel";

        try {
            @SuppressWarnings("unchecked") Class<? extends Component> menuClass = (Class<? extends Component>)
                    classLoader.loadClass(menuClassName);

            Constructor<? extends Component> constructor = menuClass.getConstructor(String.class, Page.class, IModel
                    .class);

            return constructor.newInstance(id, backPage, label);

        } catch (Exception e) {
            logger.debug("Impossible d'obtenir le menu : " + e);
            return new EmptyPanel(id);
        }

    }

    /**
     * @param id           l'id du panel
     * @param backPageName le nom de la classe à appeler lors d'un clic sur le bouton
     *                     retour
     * @return le panneau qui contient le menu à présenter, ou un panneau vide
     * si rien n'a été trouvé.
     */
    public static Component getMenuPanel(String id, String backPageName, IModel<String> label) {
        // On recherche le menu dans le plugin qui contient les IHM communes aux
        // plugins Thaleia v5

        ClassLoader classLoader = ThaleiaApplication.get().getApplicationSettings().getClassResolver().getClassLoader();

        // Le nom de la classe de menu dans le plugin des IHM
        String menuClassName = "fr.solunea.thaleia.plugins.welcomev5.MenuPanel";

        try {
            @SuppressWarnings("unchecked") Class<? extends Component> menuClass = (Class<? extends Component>)
                    classLoader.loadClass(menuClassName);

            Constructor<? extends Component> constructor = menuClass.getConstructor(String.class, String.class,
                    IModel.class);

            return constructor.newInstance(id, backPageName, label);

        } catch (Exception e) {
            logger.debug("Impossible d'obtenir le menu : " + e);
            return new EmptyPanel(id);
        }

    }

}
