package fr.solunea.thaleia.plugins.welcomev6.preview;

import fr.solunea.thaleia.model.ContentVersion;
import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.model.dao.ContentVersionDao;
import fr.solunea.thaleia.model.dao.LocaleDao;
import fr.solunea.thaleia.plugins.welcomev6.BasePage;
import fr.solunea.thaleia.plugins.welcomev6.panels.MenuPanel;
import fr.solunea.thaleia.plugins.welcomev6.messages.LocalizedMessages;
import fr.solunea.thaleia.service.PreviewService;
import fr.solunea.thaleia.service.utils.IPreviewHelper;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.modules.ModulesPage;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.panels.ThaleiaLazyLoadPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.wicket.Component;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.flow.RedirectToUrlException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@AuthorizeInstantiation("user")
public abstract class PreviewPage extends BasePage {

    protected IModel<Locale> locale;

    /**
     * @param showMenuActions doit-on présenter les actions du menu sur cette page ?
     */
    public PreviewPage(final ContentVersion module, final IModel<Locale> locale, boolean showMenuActions) {
        super(showMenuActions);

        initPreviewPage(module, locale, showMenuActions);
    }

    /**
     * @param locale la locale dans laquelle il faut présenter le module.
     */
    public PreviewPage(final ContentVersion module, final IModel<Locale> locale) {
        super();

        initPreviewPage(module, locale, true);
    }

    private void initPreviewPage(final ContentVersion module, final IModel<Locale> locale, boolean showMenuActions) {

        this.locale = new LoadableDetachableModel<>() {
            @Override
            protected Locale load() {
                return new LocaleDao(module.getObjectContext()).get(locale.getObject().getObjectId());
            }
        };

        // On remplace les modèles par des versions LoadableDetachable pour
        // éviter les problèmes d'objets Cayenne qui deviennent Hollow si on
        // joue avec la navigation dans la pages par l'historique du client web
        setDefaultModel(new LoadableDetachableModel<ContentVersion>() {
            @Override
            protected ContentVersion load() {
                return new ContentVersionDao(module.getObjectContext()).get(module.getObjectId());
            }
        });

        // Le menu, qui renvoie sur la page de présentation des modules
        add(new MenuPanel("pluginMenu", ModulesPage.class, new StringResourceModel("menuLabel", PreviewPage.this,
                null)).setVisible(showMenuActions));

        // On place un panel temporaire qui indique que la préparation de
        // l'aperçu est en cours. Ce panel sera remplacé par la page du contenu
        // temporaire.
        final ThaleiaLazyLoadPanel donePanel = new ThaleiaLazyLoadPanel("treatment") {

            @Override
            public Component getLazyLoadComponent(String id) {
                return new DonePanel(id);
            }

            @Override
            protected Component getLoadingComponent(String markupId) {
                return new LoadingPanel(markupId);
            }
        };
        add(donePanel);
    }

    class DonePanel extends Panel {

        public DonePanel(String id) {
            super(id);

            // Panneau de feedback
            final ThaleiaFeedbackPanel feedbackPanel = (ThaleiaFeedbackPanel) new ThaleiaFeedbackPanel("feedbackPanel"
            ).setOutputMarkupId(true);
            add(feedbackPanel);

            // On lance le traitement
            List<String> errorMessagesKeys = new ArrayList<>();
            try {
                final File file = prepareFile();

                // Préparation de l'aperçu
                PreviewService previewService = null;
                try {
                    previewService = ThaleiaSession.get().getPreviewService();
                } catch (SecurityException e) {
                    error(LocalizedMessages.getMessage("security.preview.error"));
                    setResponsePage(ThaleiaApplication.get().getApplicationSettings().getInternalErrorPage());
                }
                // L'objet qui va adapter l'archive pour permettre de la
                // prévisualiser
                IPreviewHelper previewAdapter = getPreviewHelper();
                String previewUrl = previewService.publishArchive(file, previewAdapter);

                // L'url est relative par rapport à l'hôte :
                // /nomduwar/preview/12345/index.html
                // Il faut la rendre absolue :
                previewUrl = RequestCycle.get().getUrlRenderer().renderFullUrl(Url.parse(previewUrl));

                // redirection vers la page qui présente l'aperçu
                throw new RedirectToUrlException(previewUrl);

            } catch (RedirectToUrlException e) {
                // Il ne faut pas traiter cette exception : elle est voulue pour
                // la redirection !
                throw e;

            } catch (Exception e) {
                // Pour faire moins peur, on n'envoie pas sur la
                // page d'erreur.
                logger.warn("Erreur de préparation de l'aperçu :" + e);
                logger.warn(LogUtils.getStackTrace(e.getStackTrace()));

                // On présente les erreurs transmises par le
                // traitement
                for (String key : errorMessagesKeys) {
                    error(LocalizedMessages.getMessage(key));
                }
                error(LocalizedMessages.getMessage("preview.error"));
            }

        }
    }

    /**
     * @return le fichier du contenu à prévisualiser, ou null s'il n'a pas été
     * possible de le préparer.
     */
    protected abstract File prepareFile() throws DetailedException;

    /**
     * @return l'objet qui va adapter le contenu pour la prévisualisation
     */
    protected abstract IPreviewHelper getPreviewHelper();

    class LoadingPanel extends Panel {
        public LoadingPanel(String id) {
            super(id);
            add(new Label("progressLabel", new StringResourceModel("progressLabel", PreviewPage.this, null)));
        }
    }

}
