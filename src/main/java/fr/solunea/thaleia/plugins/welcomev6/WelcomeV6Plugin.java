package fr.solunea.thaleia.plugins.welcomev6;

import fr.solunea.thaleia.model.Content;
import fr.solunea.thaleia.plugins.IPluginImplementation;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Locale;

public class WelcomeV6Plugin implements IPluginImplementation, Serializable {

    private static final Logger logger = Logger.getLogger(WelcomeV6Plugin.class);

    @Override
    public String getName(Locale locale) throws DetailedException {
        return "WelcomeV6Plugin";
    }

    @Override
    public String getDescription(Locale locale) throws DetailedException {
        if (locale.equals(Locale.ENGLISH)) {
            return "This plug-in will setup for you the full experience of Thaleia V6.";
        } else {
            return "Ce plugin est destiné à installer dans Thaleia l'expérience utilisateur de Thaleia v6.";
        }
    }

    @Override
    public String getVersion(Locale locale) throws DetailedException {
        return "1.0";
    }

    @Override
    public Class<?> getPage() {
        return InstallWelcomePage.class;
    }

    @Override
    public Class<?> getDetailsPage() {
        return BasePage.class;
    }

    @Override
    public byte[] getImageAsPng() {
        byte[] result = null;

        // On charge l'image comme ressource = un fichier contenu dans le Jar
        // On stocke son contenu dans un tableau d'octets.
        try (InputStream is = ThaleiaApplication.get().getApplicationSettings().getClassResolver().getClassLoader().getResourceAsStream("thaleiav6.png")) {
            if (is != null) {
                result = IOUtils.toByteArray(is);
            }
        } catch (Exception e) {
            logger.warn("Impossible de charger l'image : " + e);
        }

        return result;
    }

    @Override
    public boolean canEdit(Content content) {
        return false;
    }

    @Override
    public void onInstalled() throws DetailedException {
        // Si ce n'a pas encore été fait, on procède à l'installation des paramètres de ce plugin (pages du plugin,
        // écouteurs...).
        InstallWelcomePage.install();
    }

}
