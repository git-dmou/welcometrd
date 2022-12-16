package fr.solunea.thaleia.plugins.welcomev6.properties;

import fr.solunea.thaleia.plugins.welcomev6.WelcomeV6Plugin;
import org.apache.log4j.Logger;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;


public class ModuleTranslationPanel extends Panel {

    private Boolean reloadedAngular = false;
    private static Logger logger = Logger.getLogger(ModuleTranslationPanel.class);
    private String ContentVersionId;
    private String locale;

    private static final ResourceReference thaleiaAPI = new JavaScriptResourceReference(WelcomeV6Plugin.class, "/js/thaleia_api.js");

    public ModuleTranslationPanel(String id, int contentVersionId) {
        super(id);
            // transmet contentVersionId, pour le rendre disponible sur l'écran
            // et permettre l'apel à l'API transform/translate !
            Label formContentVersionId = new Label("wContentVersionId", Model.of(contentVersionId));
            add(formContentVersionId);
    }

        @Override
        public void renderHead(IHeaderResponse response) {
            super.renderHead(response);
            response.render(JavaScriptReferenceHeaderItem.forReference(thaleiaAPI).setDefer(true));
        }




}