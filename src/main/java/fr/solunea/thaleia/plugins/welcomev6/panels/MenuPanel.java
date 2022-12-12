package fr.solunea.thaleia.plugins.welcomev6.panels;

import fr.solunea.thaleia.webapp.pages.BasePage;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.io.Serializable;

@SuppressWarnings("serial")
public class MenuPanel extends Panel {

    /**
     * Une action.
     */
    public abstract static class MenuPanelAction implements Serializable {
        /**
         * La méthode appellée pour exécuter l'action.
         */
        protected abstract void run();
    }

    /**
     * @param id
     * @param backPageClass     la page à ouvrir lorsqu'on clique sur le bouton "Retour".
     * @param label             le label à présenter dans le menu
     * @param runBeforeBackPage une action à lancer avant de quitter la page par le bouton "retour"
     */
    public MenuPanel(String id, final Class<? extends WebPage> backPageClass, IModel<String> label, MenuPanelAction
            runBeforeBackPage) {
        super(id);

        add(new IndicatingAjaxLink<Void>("backLink") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                runBeforeBackPage.run();
                MenuPanel.this.getPage().setResponsePage(backPageClass);
            }

            @Override
            public boolean isVisible() {
                return backPageClass != null;
            }
        }.add(new Image("ico_backarrow_header", new PackageResourceReference(BasePage.class,
                "img/ico_backarrow_header.png"))));


        init(label);
    }

    /**
     * @param id
     * @param backPageClass la page à ouvrir lorsqu'on clique sur le bouton "Retour".
     * @param label         le label à présenter dans le menu
     */
    public MenuPanel(String id, final Class<? extends WebPage> backPageClass, IModel<String> label) {
        super(id);

        add(new IndicatingAjaxLink<Void>("backLink") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                MenuPanel.this.getPage().setResponsePage(backPageClass);
            }

            @Override
            public boolean isVisible() {
                return backPageClass != null;
            }
        }.add(new Image("ico_backarrow_header", new PackageResourceReference(BasePage.class,
                "img/ico_backarrow_header.png"))));

        init(label);
    }

    /**
     * @param id
     * @param backPage la page à ouvrir lorsqu'on clique sur le bouton "Retour".
     * @param label    le label à présenter dans le menu
     */
    public MenuPanel(String id, Page backPage, IModel<String> label) {
        super(id);

        add(new IndicatingAjaxLink<Void>("backLink") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                MenuPanel.this.getPage().setResponsePage(backPage);
            }

            @Override
            public boolean isVisible() {
                return backPage != null;
            }
        }.add(new Image("ico_backarrow_header", new PackageResourceReference(BasePage.class,
                "img/ico_backarrow_header.png"))));

        init(label);
    }

    private void init(IModel<String> label) {
        add(new Label("menuLabel", label));

    }

    /**
     * @param id
     * @param backPageName le nom de la classe de la page à ouvrir lorsqu'on clique sur le bouton "Retour".
     * @param label        le label à présenter dans le menu
     */
    public MenuPanel(String id, final String backPageName, IModel<String> label) {
        super(id);

        add(new IndicatingAjaxLink<Void>("backLink") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                ((BasePage) (MenuPanel.this.getPage())).setResponsePage(backPageName);
            }

            @Override
            public boolean isVisible() {
                return backPageName != null && !backPageName.isEmpty();
            }
        }.add(new Image("ico_backarrow_header", new PackageResourceReference(BasePage.class,
                "img/ico_backarrow_header.png"))));

        init(label);
    }

}
