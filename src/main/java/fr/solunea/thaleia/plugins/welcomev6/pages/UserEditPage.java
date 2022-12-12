package fr.solunea.thaleia.plugins.welcomev6.pages;

import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.plugins.welcomev6.BasePage;
import fr.solunea.thaleia.webapp.pages.admin.UserEditPanel;
import fr.solunea.thaleia.webapp.panels.ThaleiaFooterPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

/**
 * Cette page n'a plus l'air utilisée.
 * La remplaçante serait : fr/solunea/thaleia/webapp/pages/admin/UserEditPage.java
 */

@AuthorizeInstantiation("user")
// On permet à un utilisateur de présenter cette page, afin qu'il puisse
// modifier son compte.
public class UserEditPage extends BasePage {

    /**
     * La page appelée en sortie d'édition.
     */
    private final Class<? extends Page> outPageClass;

    public UserEditPage() {
        super();
        setDefaultModel(new LoadableDetachableModel<User>() {
            @Override
            protected User load() {
                return ThaleiaSession.get().getAuthenticatedUser();
            }
        });
        this.outPageClass = this.getPageClass();
        initPage();
    }

    public UserEditPage(IModel<User> model, Class<? extends Page> outPageClass) {
        super();
        setDefaultModel(model);

        this.outPageClass = outPageClass;

        initPage();
    }

    @SuppressWarnings("unchecked")
    private void initPage() {
        WebMarkupContainer editPanel = new UserEditPanel("editPanel", (IModel<User>) getDefaultModel()) {
            @Override
            protected void onOut(AjaxRequestTarget target) {
                UserEditPage.this.onOut(target);
            }
        };
        editPanel.setOutputMarkupId(true);
        add(editPanel);
    }

    /**
     * Redirection appelée à la sortie de l'édition d'un compte utilisateur.
     */
    public void onOut(AjaxRequestTarget target) {
        logger.debug("Sortie vers la page " + outPageClass);
        setResponsePage(outPageClass);
    }

}
