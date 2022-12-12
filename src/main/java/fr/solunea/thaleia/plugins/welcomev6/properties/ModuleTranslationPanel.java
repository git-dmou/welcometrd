package fr.solunea.thaleia.plugins.welcomev6.properties;

import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.plugins.welcomev6.WelcomeV6Plugin;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;


public class ModuleTranslationPanel extends Panel {

    private Boolean reloadedAngular = false;
    private static Logger logger = Logger.getLogger(ModuleTranslationPanel.class);
    private String ContentVersionId;
    private String locale;


    private static final ResourceReference localisation = new JavaScriptResourceReference(WelcomeV6Plugin.class, "/js/Localisation.js");
    private static final ResourceReference localisation_FR = new JavaScriptResourceReference(WelcomeV6Plugin.class, "/js/Localisation_FR.js");
    private static final ResourceReference localisation_EN = new JavaScriptResourceReference(WelcomeV6Plugin.class, "/js/Localisation_EN.js");
    private static final ResourceReference thaleiaAPI = new JavaScriptResourceReference(WelcomeV6Plugin.class, "/js/thaleia_api.js");
    private static final ResourceReference cannelleCreateContent = new JavaScriptResourceReference(WelcomeV6Plugin.class, "/js/translateContent.js");
    private static final ResourceReference angularJS = new JavaScriptResourceReference(WelcomeV6Plugin.class, "/js/angular.min.js");
    private static final ResourceReference angularSanitize = new JavaScriptResourceReference(WelcomeV6Plugin.class, "/js/angular-sanitize.js");
    private static final ResourceReference appRun = new JavaScriptResourceReference(WelcomeV6Plugin.class, "/js/app-run.js");



    public ModuleTranslationPanel(String id, String ContentVersionId, IModel<Locale> locale, ObjectContext context) {
        super(id);

    }

        @Override
        public void renderHead(IHeaderResponse response) {
            super.renderHead(response);
            response.render(JavaScriptReferenceHeaderItem.forReference(localisation).setDefer(true));
            response.render(JavaScriptReferenceHeaderItem.forReference(localisation_FR).setDefer(true));
            response.render(JavaScriptReferenceHeaderItem.forReference(localisation_EN).setDefer(true));
            response.render(JavaScriptReferenceHeaderItem.forReference(thaleiaAPI).setDefer(true));
            response.render(JavaScriptReferenceHeaderItem.forReference(cannelleCreateContent).setDefer(true));
            response.render(JavaScriptReferenceHeaderItem.forReference(angularJS).setDefer(true));
            response.render(JavaScriptReferenceHeaderItem.forReference(angularSanitize).setDefer(true));
            response.render(JavaScriptReferenceHeaderItem.forReference(appRun).setDefer(true));
        }




}