package fr.solunea.thaleia.plugins.welcomev6.utils;

import fr.solunea.thaleia.model.Content;
import fr.solunea.thaleia.model.EditedContent;
import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.model.dao.ContentDao;
import fr.solunea.thaleia.model.dao.EditedContentDao;
import fr.solunea.thaleia.model.dao.LocaleDao;
import fr.solunea.thaleia.service.utils.IPreviewHelper;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.pages.BasePage;
import fr.solunea.thaleia.webapp.pages.ThaleiaV6MenuPage;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

public abstract class BasicEditContentPage extends BasePage {

    protected static final Logger logger = Logger.getLogger(BasicEditContentPage.class);
    protected int editedContentId;

    /**
     * Ouvre la page d'édition du binaire de ce contenu, dans la locale
     * demandée.
     *
     * @param editorWindowName le nom de la fenêtre dans laquelle on doit ouvrir l'éditeur
     */
    @SuppressWarnings("unused")
    public BasicEditContentPage(final IModel<Locale> locale, final IModel<Content> contentModel, String
            editorWindowName) {
        super();

        ObjectContext context = ThaleiaSession.get().getContextService().getNewContext();
        final IModel<Locale> localeModel = new LoadableDetachableModel<>() {
            @Override
            protected Locale load() {
                return new LocaleDao(context).get(locale.getObject().getObjectId());
            }
        };
        final IModel<Content> loadableContentModel = new LoadableDetachableModel<>() {
            @Override
            protected Content load() {
                return new ContentDao(context).get(contentModel.getObject().getObjectId());
            }
        };

        // On initialise la page, en lui fournissant un EditdContent qui est
        // fabriqué par l'ouverture du Content demandé.
        initPage(getEditedContentProvider().openEditedContent(localeModel, loadableContentModel), editorWindowName);
    }

    /**
     * Crée un nouveau contenu, l'initialise avec le contenu par défaut, et
     * ouvre l'éditeur.
     *
     * @param locale            la locale dans laquelle sera éditée ce nouveau contenu.
     * @param editorWindowName  le nom de la fenêtre dans laquelle on doit ouvrir l'éditeur
     * @param contentIdentifier le nom à donner comme identifiant de la première version créée. Si null, alors généré
     *                          automatiquement.
     */
    @SuppressWarnings("unused")
    public BasicEditContentPage(final IModel<Locale> locale, String editorWindowName, final String contentIdentifier) {
        super();

        // On initialise la page, en lui fournissant un nouveau EditedContent
        // qui est fabriqué avec le contenu par défaut.
        initPage(getEditedContentProvider().createEditedContent(locale, contentIdentifier), editorWindowName);
    }

    protected abstract EditedContentProvider getEditedContentProvider();

    protected abstract Class<? extends ThaleiaV6MenuPage> getBackPage();

    protected abstract IPreviewHelper getPreviewHelper(IModel<EditedContent> editedContent);

    /**
     * @param editedContent    le EditedContent avec lequel initialiser la page.
     * @param editorWindowName le nom de la fenêtre dans laquelle on doit ouvrir l'éditeur
     */
    private void initPage(EditedContent editedContent, final String editorWindowName) {

        // Bouton retour, invisible par défaut
        // Ne sera rendu visible qu'en cas d'erreur
        Component back = new Link<Void>("back") {
            @Override
            public void onClick() {
                setResponsePage(getBackPage());
            }
        }.setVisible(false);

        try {
            if (editedContent == null) {
                throw new DetailedException("Pas de EditedContent trouvé à éditer !");
            }

            // On stocke l'ID de l'objet existant en base.
            editedContentId = new EditedContentDao(editedContent.getObjectContext()).getPK(editedContent);

            // Pas d'éditeur sur cette page
            add(new EmptyPanel("editor"));

            // Après chargement de la page, on ouvre l'éditeur visuel dans
            // une pop-up
            add(new AjaxEventBehavior("onload") {
                @Override
                protected void onEvent(final AjaxRequestTarget target) {
                    // On publie l'éditeur, dans lequel on va écrire la version initiale du A7 à ouvrir et présenter.
                    ObjectContext context = ThaleiaSession.get().getContextService().getNewContext();
                    IModel<EditedContent> myEditedContent = new LoadableDetachableModel<>() {
                        @Override
                        protected EditedContent load() {
                            return new EditedContentDao(editedContent.getObjectContext()).get(editedContent.getObjectId());
                        }
                    };
                    String url = getEditedContentProvider().publishEditorFiles(getPreviewHelper(myEditedContent), BasicEditContentPage.this);

                    target.appendJavaScript("window.open('" + url + "','" + editorWindowName + "');");
                }
            });

        } catch (Exception e) {
            logger.info("Impossible de préparer le panneau d'édition : " + e + "\n" + LogUtils.getStackTrace(e.getStackTrace()));
            error("Impossible de préparer le panneau d'édition.");
            add(new EmptyPanel("editor"));

            // On active le bouton de retour à la page principale du plugin
            back.setVisible(true);
        }

        // Le panneau des messages
        ThaleiaFeedbackPanel feedbackPanel = new ThaleiaFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        // Bouton retour
        add(back);
    }
}
