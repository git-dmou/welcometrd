package fr.solunea.thaleia.plugins.welcomev6.download;

import fr.solunea.thaleia.plugins.welcomev6.panels.MenuPanel;
import fr.solunea.thaleia.webapp.pages.modules.ModulesPage;
import org.apache.wicket.Page;
import org.apache.wicket.model.StringResourceModel;

@SuppressWarnings("serial")
public abstract class DownloadPageWithNavigation extends DownloadPage {

    /**
     * @param showMenuActions doit-on présenter les actions du menu sur cette page ?
     * @param backPage        la destination pour le lien de retour présenté avec le bouton
     *                        de téléchargement.
     */
    public DownloadPageWithNavigation(boolean showMenuActions, Page backPage) {
        super(showMenuActions, backPage);
        initDownloadPage(showMenuActions);
    }

    public DownloadPageWithNavigation() {
        super(true, null);
        initDownloadPage(true);
    }

    private void initDownloadPage(boolean showMenuActions) {
        // Le menu, qui renvoie sur la page de présentation des modules
        add(new MenuPanel("pluginMenu", ModulesPage.class, new StringResourceModel("menuLabel",
                DownloadPageWithNavigation.this, null)).setVisible(showMenuActions));
    }
}
