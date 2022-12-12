package fr.solunea.thaleia.plugins.welcomev6;

import fr.solunea.thaleia.model.ApplicationParameter;
import fr.solunea.thaleia.model.dao.ApplicationParameterDao;
import fr.solunea.thaleia.plugins.welcomev6.utils.NotEnoughSubscriptionsMailNotifier;
import fr.solunea.thaleia.plugins.welcomev6.utils.WelcomeV6Configuration;
import fr.solunea.thaleia.service.events.EventNotificationService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.LoginPage;
import fr.solunea.thaleia.webapp.pages.admin.parameters.ApplicationParametersPanel;
import fr.solunea.thaleia.webapp.pages.plugins.PluginsPage;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.LoadableDetachableModel;

import java.util.List;

@SuppressWarnings("serial")
public class InstallWelcomePage extends fr.solunea.thaleia.webapp.pages.BasePage {

    public InstallWelcomePage() {
        super();

        // Panneau de feedback
        final ThaleiaFeedbackPanel feedbackPanel = new ThaleiaFeedbackPanel("feedback");
        add(feedbackPanel.setOutputMarkupId(true));

        // Le panneau de gestion des paramètres du plugin
        Panel parameters = new ApplicationParametersPanel("parametersPanel", new
                LoadableDetachableModel<List<ApplicationParameter>>() {
            @Override
            protected List<ApplicationParameter> load() {
                return WelcomeV6Configuration.getParameters();
            }
        }, true, false, false);
        parameters.setOutputMarkupId(true);
        add(parameters);

        final Component pageLabel = new Label("welcomePageId", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return ThaleiaApplication.get().getConfiguration().getDatabaseParameterValue(Configuration
                        .AUTHENTIFIED_USERS_WELCOME_PAGE, "fr.solunea.thaleia.webapp.pages.plugins.PluginsPage");
            }
        });
        add(pageLabel.setOutputMarkupId(true));

        add(new IndicatingAjaxLink<Void>("installPluginPage") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    install();
                    info("La page d'accueil de Thaleia est maintenant : " + WelcomeV6Plugin.class.getCanonicalName());
                } catch (Exception e) {
                     error("Impossible de modifier la page d'accueil : " + e);
                }

                // On installe les paramètres PayPal
                try {
                    WelcomeV6Configuration.installDefaultParameters();
                } catch (DetailedException e) {
                    logger.warn("Impossible d'enregistrer les paramètres PayPal : " + e);
                }

                // On enregistre l'écouteur pour traiter les accès refusés aux
                // publications par manque de crédit d'inscription.
                try {
                    ThaleiaSession.get().getEventService().registerListener(EventNotificationService.Event
                            .EVENT_REGISTRATION_CREDIT_REQUIRED.toString(), NotEnoughSubscriptionsMailNotifier.class);
                } catch (DetailedException e) {
                    logger.warn("Impossible d'enregistrer l'écouteur pour traiter les accès"
                            + " refusés aux  publications par manque de crédit d'inscription : " + e);
                }

                target.add(pageLabel);
                target.add(feedbackPanel);
                target.add(parameters);
            }
        });

        add(new IndicatingAjaxLink<Void>("installDefaultPage") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    setApplicationPageTo(Configuration.DEFAULT_AUTHENTIFIED_USERS_WELCOME_PAGE, Configuration
                            .AUTHENTIFIED_USERS_WELCOME_PAGE);
                    setApplicationPageTo(Configuration.DEFAULT_LOGIN_PAGE, Configuration.LOGIN_PAGE);
                    setApplicationPageTo(Configuration.DEFAULT_PUBLIC_WELCOME_PAGE, Configuration
                            .PUBLIC_USERS_WELCOME_PAGE);
                    ThaleiaApplication.get().resetHomePage();
                    info("La page d'accueil de Thaleia est maintenant : " + PluginsPage.class.getCanonicalName());
                } catch (DetailedException e) {
                    error("Impossible de modifier la page d'accueil : " + e);
                }

                target.add(pageLabel);
                target.add(feedbackPanel);
                target.add(parameters);
            }
        });

    }

    public static void install() throws DetailedException {
        try {
            setApplicationPageTo(BasePage.class.getCanonicalName(), Configuration.AUTHENTIFIED_USERS_WELCOME_PAGE);
            setApplicationPageTo(LoginPage.class.getCanonicalName(), Configuration.LOGIN_PAGE);
//            setApplicationPageTo(WelcomePublicV6Page.class.getCanonicalName(), Configuration.PUBLIC_USERS_WELCOME_PAGE);
            ThaleiaApplication.get().resetHomePage();
        } catch (DetailedException e) {
           throw e.addMessage("Impossible d'enregistrer les nouvelles pages de l'application !");
        }

        // On installe les paramètres PayPal
        try {
            WelcomeV6Configuration.installDefaultParameters();
        } catch (DetailedException e) {
            logger.warn("Impossible d'enregistrer les paramètres PayPal : " + e);
        }

        // On enregistre l'écouteur pour traiter les accès refusés aux
        // publications par manque de crédit d'inscription.
        try {
            ThaleiaApplication.get().getEventNotificationService().registerListener(
                    EventNotificationService.Event.EVENT_REGISTRATION_CREDIT_REQUIRED.toString(),
                    NotEnoughSubscriptionsMailNotifier.class
            );
        } catch (DetailedException e) {
            logger.warn("Impossible d'enregistrer l'écouteur pour traiter les accès"
                    + " refusés aux publications par manque de crédit d'inscription : " + e);
        }
    }

    private static void setApplicationPageTo(String pageName, String updatedPage) throws DetailedException {
        ApplicationParameterDao dao = ThaleiaApplication.get().getApplicationParameterDao();

        ApplicationParameter param = dao.findByName(updatedPage);

        if (param == null) {
            param = dao.get();
            param.setName(updatedPage);
        }

        param.setValue(pageName);
        dao.save(param);
    }

}
