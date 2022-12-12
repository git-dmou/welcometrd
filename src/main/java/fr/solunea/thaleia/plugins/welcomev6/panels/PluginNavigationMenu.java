package fr.solunea.thaleia.plugins.welcomev6.panels;

import fr.solunea.thaleia.model.Content;
import fr.solunea.thaleia.model.Locale;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * Panneau de navigation entre les différents menus d'un module Thaleia.
 */
public abstract class PluginNavigationMenu extends Panel {

    protected static final Logger logger = Logger.getLogger(PluginNavigationMenu.class);

    private final AjaxLink<?> resourcesLink = new AjaxLink<>(Link.resources.name()) {
        @Override
        public void onClick(AjaxRequestTarget target) {
            onResourcesLinkClicked(target);
            target.appendJavaScript("activate('resources');");
            target.appendJavaScript("hideLoader();");
        }
    };
    private final AjaxLink<?> parametersLink = new AjaxLink<>(Link.parameters.name()) {
        @Override
        public void onClick(AjaxRequestTarget target) {
            onParametersLinkClicked(target);
            target.appendJavaScript("activate('parameters');");
            target.appendJavaScript("hideLoader();");
        }
    };
    private final AjaxLink<?> myModulesLink = new AjaxLink<>(Link.modules.name()) {
        @Override
        public void onClick(AjaxRequestTarget target) {
            onMyModulesLinkClicked(target);
            target.appendJavaScript("activate('modules');");
            target.appendJavaScript("hideLoader();");
        }
    };
    private final AjaxLink<?> createLink = new AjaxLink<>(Link.create.name()) {
        @Override
        public void onClick(AjaxRequestTarget target) {
            onCreateLinkClicked(target);
            target.appendJavaScript("activate('create');");
            target.appendJavaScript("hideLoader();");
        }
    };

    public PluginNavigationMenu(String id) {
        super(id);

        add(createLink.setOutputMarkupId(true).setMarkupId("create"));
        add(myModulesLink.setOutputMarkupId(true).setMarkupId("modules"));
        add(parametersLink.setOutputMarkupId(true).setMarkupId("parameters"));
        add(resourcesLink.setOutputMarkupId(true).setMarkupId("resources"));
    }

    public void clickOnEditModuleLink(AjaxRequestTarget target, IModel<Content> content, IModel<Locale> locale) {
        onEditModuleClicked(target, content, locale);
        target.add(this);
        target.appendJavaScript("activate('modules');");
    }

    /**
     * Reproduit le comportement du clic sur un des éléments du menu.
     */
    public void clickOnLink(AjaxRequestTarget target, Link link) {
        switch (link) {
            case create: onCreateLinkClicked(target);
            case modules: onMyModulesLinkClicked(target);
            case parameters: onParametersLinkClicked(target);
            case resources: onResourcesLinkClicked(target);
        }
        // Rechargement des éléments du panel
        target.add(this);
    }

    public abstract void onCreateLinkClicked(AjaxRequestTarget target);

    public abstract void onMyModulesLinkClicked(AjaxRequestTarget target);

    public abstract void onParametersLinkClicked(AjaxRequestTarget target);

    public abstract void onResourcesLinkClicked(AjaxRequestTarget target);

    public abstract void onEditModuleClicked(AjaxRequestTarget target, IModel<Content> content, IModel<Locale> locale);

    public enum Link {create, modules, parameters, resources}

}

