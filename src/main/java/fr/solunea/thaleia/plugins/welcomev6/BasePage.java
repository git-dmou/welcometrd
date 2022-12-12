package fr.solunea.thaleia.plugins.welcomev6;

import fr.solunea.thaleia.webapp.pages.ThaleiaV6MenuPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Surchage de la BasePage afin de remplir les objectifs suivants :
 * - Permettre d'avoir l'architecture HTML de base pour welcomev6
 * - Ne pas casser l'existent dans welcomev6
 * Cette page HTML sert à définir le <head>
 */
public class BasePage extends ThaleiaV6MenuPage {

    public BasePage(IModel<?> model) {
        super(model);
    }

    public BasePage() {
        super();
    }

    public BasePage(boolean showMenuActions) {
        super(showMenuActions);
    }

    public BasePage(PageParameters parameters) {
        super(parameters);
    }

}
