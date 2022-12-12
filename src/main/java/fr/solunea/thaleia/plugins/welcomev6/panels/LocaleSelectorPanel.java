package fr.solunea.thaleia.plugins.welcomev6.panels;

import fr.solunea.thaleia.model.User;
import fr.solunea.thaleia.model.dao.LocaleDao;
import fr.solunea.thaleia.model.dao.UserDao;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.Model;
import java.util.Locale;

public class LocaleSelectorPanel extends Panel {

    public LocaleSelectorPanel(String id) {
        super(id);

        add(new Link<Object>("goEnglish") {
            @Override
            public void onClick() {
                getSession().setLocale(Locale.ENGLISH);

                // Si l'utilisateur est identifié, on met à jour sa locale préférée
                setPreferedLocale(Locale.ENGLISH);
            }
        });
        add(new Link<Object>("goFrench") {
            @Override
            public void onClick() {
                getSession().setLocale(Locale.FRENCH);

                // Si l'utilisateur est identifié, on met à jour sa locale préférée
                setPreferedLocale(Locale.FRENCH);
            }
        });
        add(new Label("currentLocale", new Model<String>() {
            @Override
            public String getObject() {
                return getSession().getLocale().getLanguage();
            }
        }));
    }

    /**
     * Assigne la locale préférée de l'utiliateur courrant.
     * @param javaLocale
     */
    private void setPreferedLocale(Locale javaLocale) {
        try {
            User authenticatedUser = ThaleiaSession.get().getAuthenticatedUser();
            ObjectContext context = authenticatedUser.getObjectContext();
            authenticatedUser.setPreferedLocale(new LocaleDao(context).getLocale(javaLocale));
            new UserDao(context).save(authenticatedUser, true);
        } catch (Exception e) {
            // On ignore les erreurs, car on peut être dans le cas d'un utilisateur non identifié.
        }
    }
}
