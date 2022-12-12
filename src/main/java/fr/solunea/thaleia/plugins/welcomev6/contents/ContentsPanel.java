package fr.solunea.thaleia.plugins.welcomev6.contents;

import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.model.*;
import fr.solunea.thaleia.model.dao.*;
import fr.solunea.thaleia.service.ContentPropertyService;
import fr.solunea.thaleia.utils.DateUtils;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.pages.ErrorPage;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.*;

import java.io.Serializable;
import java.util.*;

@AuthorizeInstantiation("user")
public abstract class ContentsPanel extends Panel {

    protected static final Logger logger = Logger.getLogger(ContentsPanel.class);
    /**
     * Les colonnes du tableau
     */
    private final List<IColumn<TitledContent, String>> columns = new ArrayList<>();
    /**
     * La valeur à appliquer comme filtre sur les éléments du tableau.
     */
    private final IModel<String> filterModel = Model.of("");
    /**
     * Les contenus présentés
     */
    protected IModel<List<TitledContent>> contentsModel;
    protected ContentPropertyService contentPropertyService;
    protected LocaleDao localeDao;
    private IModel<Locale> contentsLocale = null;
    /**
     * Le ContentType des modules qui sont présentés dans ce tableau.
     */
    private IModel<ContentType> contentTypeModel = null;
    /**
     * Le tableau de présentation des éléments.
     */
    private ModulesDataTable<TitledContent, String> datatable;

    /**
     * Constructeur du tableau listant les contenus pédagogique.
     *
     * @param id            ID Wicket du panel.
     * @param locale        Locale dans laquelle présenter les valeurs des propriétés des modules.
     * @param feedbackPanel Panneau de feedback de Thaleia
     */
    public ContentsPanel(String id, final IModel<Locale> locale, final ThaleiaFeedbackPanel feedbackPanel) {
        super(id);

        // final ObjectContext objectContext = locale.getObject().getObjectContext();
        final ObjectContext objectContext = ThaleiaSession.get().getContextService().getNewContext();
        // Locale des contenus.
        try {
            contentPropertyService = ThaleiaSession.get().getContentPropertyService();
            localeDao = new LocaleDao(objectContext);
        } catch (Exception e) {
            logger.warn("Impossible d'initialiser la locale des contenus : " + e);
            logger.warn(LogUtils.getStackTrace(e.getStackTrace()));
            setResponsePage(ErrorPage.class);
        }

        try {
            contentsLocale = locale;

            contentTypeModel = new LoadableDetachableModel<>() {
                @Override
                protected ContentType load() {
                    return new ContentTypeDao(objectContext).findByName(getPresentedContentTypeName());
                }
            };

            contentsModel = new LoadableDetachableModel<>() {
                @Override
                protected List<TitledContent> load() {
                    // logger.debug("Récupération des contenus à présenter dans
                    // la liste, dans la locale " + contentsLocale.getObject());
                    List<TitledContent> result;
                    result = new ArrayList<>();
                    // Si le contentType n'existe pas, alors liste vide
                    if (contentTypeModel.getObject() != null) {
                        List<Content> contents = ThaleiaSession.get().getContentService().getContents(
                                new UserDao(objectContext).get(ThaleiaSession.get().getAuthenticatedUser().getObjectId()),
                                contentTypeModel.getObject().getIsModuleType(),
                                contentTypeModel.getObject(),
                                getContentPropertyThatMustExists(),
                                contentsLocale.getObject());
                        for (Content content : contents) {
                            result.add(new TitledContent(content));
                        }
                    }

                    // Par défaut on trie de la plus récente à la plus vieille version de ces contenus
                    //                        Collections.sort(result, (o1, o2) -> Long.compare
                    // (o1.getLastUpdateDateAsLong(),
                    //                                o2.getLastUpdateDateAsLong()));
                    // logger.debug("Nombre de contenus à présenter dans la liste : "
                    // + result.size());
                    return result;
                }
            };

            // Initialisation des colonnes du tableau
            try {
                // On vide les colonnes existantes
                columns.clear();

                // La colonne du titre du contenu, triable
                columns.add(new PropertyColumn<>(new LoadableDetachableModel<String>() {
                    // Le modèle qui renvoie le libellé de l'en-tête de
                    // cette colonne

                    @Override
                    public String load() {
                        // logger.debug("valeur de columnContentIdentifier...");
                        // Recherche de la clé columnContentTitle dans le
                        // fichier .properties
                        return new StringResourceModel("columnContentTitle", ContentsPanel.this, null).getString();
                    }

                }, "contentTitle", "contentTitle") {

                    @Override
                    public void populateItem(final Item<ICellPopulator<TitledContent>> item, final String
                            componentId, final IModel<TitledContent> rowModel) {
                        // On place le titre du contenu présenté sur cette
                        // ligne
                        item.add(new Label(componentId, new PropertyModel<TitledContent>(rowModel, "contentTitle")));
                    }

                });

                // La colonne de date de dernière mise à jour
                columns.add(new PropertyColumn<>(new LoadableDetachableModel<>() {
                    // Le modèle qui renvoie le libellé de l'en-tête de
                    // cette colonne

                    @Override
                    public String load() {
                        // Recherche de la clé lastUpdateDate dans le fichier .properties
                        return new StringResourceModel("columnLastUpdateDate", ContentsPanel.this, null).getString();
                    }
                }, "lastUpdateDate", "lastUpdateDate"));

                // Une colonne qui contient les actions à effectuer sur le
                // contenu, non triable.
                columns.add(new AbstractColumn<>(new LoadableDetachableModel<String>() {
                    // Le modèle qui renvoie le libellé de l'en-tête de
                    // cette colonne

                    @Override
                    public String load() {
                        // Recherche de la clé contentIdentifier dans le fichier .properties
                        return new StringResourceModel("columnActionsLabel", ContentsPanel.this, null).getString();
                    }

                }) {

                    @Override
                    public void populateItem(final Item<ICellPopulator<TitledContent>> item, final String
                            componentId, final IModel<TitledContent> rowModel) {
                        item.add(getActionsPanel(componentId, rowModel, feedbackPanel, contentsLocale));
                    }

                });

            } catch (Exception e) {
                logger.warn("Impossible d'initialiser les colonnes : " + e.toString());
                logger.warn(LogUtils.getStackTrace(e.getStackTrace()));
                setResponsePage(ErrorPage.class);
            }


            // la table des éléments
            datatable = new ModulesDataTable<>("datatable", columns, new ContentProvider(objectContext),
                    getItemsPerPage()) {

                @Override
                protected void onConfigure() {
                    super.onConfigure();
                    this.setVisible(contentTypeModel.getObject() != null && !contentsModel.getObject().isEmpty());
                }
            };
            datatable.setOutputMarkupId(true);
            datatable.setOutputMarkupPlaceholderTag(true);
            add(datatable);

            // Le filtre de recherche
            try {
                Form<?> form = new Form<Void>("form");
                add(form);
                TextField<String> filter = new TextField<>("filter", filterModel);

                // Placeholder du textField
                IModel mPlaceHolder = new StringResourceModel("filter.placeholder", this, null);
                filter.add(new AttributeModifier("placeholder", mPlaceHolder));

                filter.add(new OnChangeAjaxBehavior() {
                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        // On recharge le tableau des éléments
                        target.add(datatable);
                    }
                });
                form.add(filter);
                form.add(new AjaxButton("search") {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        // On recharge le tableau des éléments
                        target.add(datatable);
                    }
                });

                // On stocke la locale dans un modèle détachable, par défaut la
                // locale stockée en session = la dernière sélectionnée.
                // C'est la locale sélectionnée pour l'import, et pour le
                // téléchargement du modèle Excel.
                IModel<Locale> contentsLocaleModel = new Model<>() {
                    @Override
                    public Locale getObject() {
                        Locale locale = localeDao.get(ThaleiaSession.get().getLastContentLocale().getObjectId());
                        // logger.debug("On renvoie la locale du dernier contenu édité : " + locale);
                        return locale;
                    }

                    @Override
                    public void setObject(Locale object) {
                        super.setObject(object);
                        // logger.debug("Nouvelle locale de la page : " + object);
                        ThaleiaSession.get().setLastContentLocale(object);
                    }
                };

                // Le bouton de sélection de locale des contenus
                final Label currentSelectedLang = new Label("currentSelectedLang", new Model<String>() {
                    @Override
                    public String getObject() {
                        return localeDao.getDisplayName(contentsLocaleModel.getObject(), ThaleiaSession.get().getLocale());
                    }
                });
                currentSelectedLang.setOutputMarkupId(true);
                form.add(currentSelectedLang);

                final ListView<Locale> listView = new ListView<>("langSelectorRow", new LocaleDao(objectContext).find()) {
                    @Override
                    public void populateItem(final ListItem<Locale> item) {
                        final AjaxLink<?> lnk = new AjaxLink<>("langSelectorLink", Model.of(item.getModelObject())) {
                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                Locale selectedLocale = localeDao.get(item.getModelObject().getObjectId());
                                contentsLocaleModel.setObject(selectedLocale);
                                target.add(currentSelectedLang);
                                target.add(datatable);

                                onSelectedLocaleChanged(target);
                            }
                        };
                        lnk.add(new Label("linktext", new Model<String>() {
                            @Override
                            public String getObject() {
                                return localeDao.getDisplayName(localeDao.get(item.getModelObject().getObjectId()), ThaleiaSession.get().getLocale());
                            }
                        }));
                        item.add(lnk.setOutputMarkupId(true));
                    }
                };
                form.add(listView.setOutputMarkupId(true));


            } catch (Exception e) {
                logger.warn("Impossible d'initialiser le filtre de recherche : " + e.toString());
                logger.warn(LogUtils.getStackTrace(e.getStackTrace()));
                setResponsePage(ErrorPage.class);
            }


        } catch (Exception e) {
            contentsModel = new IModel<>() {

                @Override
                public void detach() {
                }

                @Override
                public List<TitledContent> getObject() {
                    return new ArrayList<>();
                }

                @Override
                public void setObject(List<TitledContent> object) {
                }

            };
            logger.warn("Impossible de préparer le panel : " + e + "\n" + LogUtils.getStackTrace(e.getStackTrace()));
        }
    }

    public void detachContentsModel() {
        contentsModel.detach();
    }

    /**
     * @return la ContentProperty pour laquelle il doit exister une valeur (dans la locale demandée) pour que le contenu
     * soit présent dans le tableau.
     */
    private ContentProperty getContentPropertyThatMustExists() {
        ContentProperty result;
        ContentType contentType = new ContentTypeDao(ThaleiaSession.get().getContextService().getContextSingleton()).findByName(getPresentedContentTypeName());
        if (contentType == null) {
            result = null;
        } else {
            result = ThaleiaSession.get().getContentPropertyService().findContentProperty(contentType,
                    getContentPropertyNameThatMustExists());
        }
        // logger.debug("Filtre sur la valeur de la propriété : " + result);
        return result;
    }

    /**
     * @return le nom de la ContentProperty pour laquelle il doit exister une valeur (dans la locale demandée) pour que
     * le contenu soit présent dans le tableau.
     */
    protected abstract String getContentPropertyNameThatMustExists();

    @Override
    protected void onConfigure() {
        super.onConfigure();
        this.setVisible(contentTypeModel.getObject() != null && !contentsModel.getObject().isEmpty());
    }

    /**
     * @return doit-on présenter un lien d'édition ?
     */
    protected abstract boolean showEditLink();

    /**
     * @return le nombre d'items de la liste des contenus présentés dans une page.
     */
    protected abstract int getItemsPerPage();

    protected abstract String getPresentedContentTypeName();

    /**
     * Le panneau des actions à effectuer sur chacun des éléments du tableau des contenus existants.
     */
    protected ActionsPanel getActionsPanel(final String componentId, final IModel<TitledContent> rowModel, final
    ThaleiaFeedbackPanel feedbackPanel, final IModel<Locale> locale) {
        return new ActionsPanel(componentId, new LoadableDetachableModel<>() {
            @Override
            protected Content load() {
                return rowModel.getObject().getContent();
            }
        }, locale, feedbackPanel, getActionsOnContent(contentsModel), new LoadableDetachableModel<>() {
            @Override
            protected ContentVersion load() {
                return rowModel.getObject().getContent().getLastVersion();
            }
        }, true) {

            @Override
            protected void onEdit(AjaxRequestTarget target, IModel<Content> content, IModel<Locale> locale) {
                actionsOnContent.onEdit(target, content, locale);
            }

            @Override
            protected void onEditProperties(AjaxRequestTarget target, IModel<Content> content, IModel<Locale> locale) {
                ContentsPanel.this.onEditProperties(target, content, locale);
            }

            @Override
            protected boolean showEditLink() {
                return ContentsPanel.this.showEditLink();
            }

            @Override
            protected void onUpdatePublish(IModel<Content> content, IModel<Locale> locale) {
                actionsOnContent.onUpdatePublish(content, locale, getPage());
            }
        };
    }

    /**
     * @param modelToRefreshAfterDelete le modèle de contenus à détacher pour une mise à jour après la suppression d'un
     *                                  de ces contenus.
     */
    protected abstract ActionsOnContent getActionsOnContent(IModel<?> modelToRefreshAfterDelete);

    /**
     * L'action à traiter lors de la demande d'édition des propriétés d'un
     * contenu.
     */
    protected abstract void onEditProperties(AjaxRequestTarget target, IModel<Content> content, IModel<Locale> locale);

    /**
     * Permet d'accéder aux propriétés de la dernière version du module, par une
     * propriété.
     */
    @SuppressWarnings("unused") // des méthodes sont appelées par réflexion
    protected class TitledContent implements Serializable {
        private final Content content;

        public TitledContent(Content content) {
            this.content = content;
        }

        public Long getLastUpdateDateAsLong() {
            return content.getLastVersion().getLastUpdateDate().getTime();
        }

        public String getLastUpdateDate() {
            // Cette méthode est appelée par réflexion par les PropertyModel de Cayenne.

            Date date = content.getLastVersion().getLastUpdateDate();
            // logger.debug("Date au format local...");
            // La date au format de la locale, puis l'heure
            // au format de la locale. Par exemple :
            // FR : 29/10/13 16:39
            // EN : 10/29/13 4:39 PM
            return DateUtils.formatDateHour(date, localeDao.getJavaLocale(contentsLocale.getObject()));
        }

        public String getContentTitle() {
            // Cette méthode est appelée par réflexion par les PropertyModel de Cayenne.
            return getActionsOnContent(contentsModel).getContentTitle(content.getLastVersion(), contentsLocale
                    .getObject());
        }

        public Content getContent() {
            return content;
        }
    }

    protected class ContentProvider extends SortableDataProvider<TitledContent, String> {

        private final SortableDataProviderComparator comparator = new SortableDataProviderComparator();
        private final ObjectContext objectContext;

        public ContentProvider(ObjectContext objectContext) {
            this.objectContext = objectContext;
            // On fixe le tri par défaut, afin qu'il ne soit juste pas nul.
            setSort("lastUpdateDate", SortOrder.DESCENDING);
        }

        @Override
        public Iterator<? extends TitledContent> iterator(long first, long count) {

            // On copie la liste des contenus
            List<TitledContent> contents = new ArrayList<>(getContents());

            // On trie cette liste
            contents.sort(comparator);

            // On pagine le résultat
            return contents.subList(Long.valueOf(first).intValue(), Long.valueOf(first + count).intValue()).iterator();
        }

        /**
         * @return les contenus, filtrés si l'utilisateur a demandé d'appliquer un filtre
         */
        private List<TitledContent> getContents() {

            if (filterModel.getObject() != null && filterModel.getObject().length() > 0) {

                // On filtre
                List<TitledContent> filtered = new ArrayList<>();
                for (TitledContent content : contentsModel.getObject()) {
                    if (ThaleiaSession.get().getContentService().containsInPropertyValue(content.getContent()
                            .getLastVersion(), filterModel.getObject())) {
                        filtered.add(content);
                    }
                }

                // On renvoie les éléments filtrés
                return filtered;

            } else {
                // Pas de filtre : on renvoie les objets du modèle
                return contentsModel.getObject();
            }
        }

        @Override
        public long size() {
            return getContents().size();
        }

        @Override
        public IModel<TitledContent> model(final TitledContent object) {
            return new AbstractReadOnlyModel<>() {
                @Override
                public TitledContent getObject() {
                    return new TitledContent(new ContentDao(objectContext).get((object.getContent()).getObjectId()));
                }
            };
        }

        class SortableDataProviderComparator implements Comparator<TitledContent>, Serializable {

            private final ContentPropertyDao contentPropertyDao = new ContentPropertyDao(objectContext);
            private final ContentPropertyValueDao contentPropertyValueDao = new ContentPropertyValueDao(
                    ThaleiaApplication.get().getConfiguration().getLocalDataDir().getAbsolutePath(),
                    ThaleiaApplication.get().getConfiguration().getBinaryPropertyType(), objectContext);

            @SuppressWarnings({"rawtypes", "unchecked"})
            public int compare(final TitledContent o1, final TitledContent o2) {

                ContentVersion version1 = o1.getContent().getLastVersion();
                ContentVersion version2 = o2.getContent().getLastVersion();

                if (version1 == null || version2 == null) {
                    logger.debug("Comparaison de " + version1 + " avec " + version2 + " : comparaison impossible "
                            + "d'objets nuls.");
                    return 0;
                }

                // La propriété sur laquelle effectuer le tri
                String sortProperty = getSort().getProperty();

                // On rechercher les valeurs à comparer pour les deux objets
                // Content
                Comparable value1;
                Comparable value2;
                int result;

                if ("contentTitle".equals(sortProperty)) {
                    // Cas particulier : contentIdentifier
                    value1 = version1.getContentIdentifier();
                    value2 = version2.getContentIdentifier();

                } else if ("lastUpdateDate".equals(sortProperty)) {
                    // Cas particulier : on compare des dates
                    value1 = version1.getLastUpdateDate();
                    value2 = version2.getLastUpdateDate();

                } else {
                    // Cas général : c'est le nom d'une ContentProperty

                    // La ContentProperty correspondante
                    ContentProperty contentProperty = contentPropertyDao.findByName
                            (sortProperty);

                    // Récupération des valeurs
                    try {
                        value1 = version1.getPropertyValue(contentProperty, contentsLocale.getObject(), "", contentPropertyValueDao);
                        value2 = version2.getPropertyValue(contentProperty, contentsLocale.getObject(), "", contentPropertyValueDao);
                    } catch (Exception e) {
                        // On ignore l'erreur
                        value1 = "";
                        value2 = "";
                    }
                }

                // Comparaison n'est pas raison
                try {
                    result = value1.compareTo(value2);
                } catch (Exception e) {
                    // Peut arriver si identifier est nul
                    logger.debug("Problème de comparaison ? " + e);
                    result = 0;
                }

                // Prise en compte de l'ordre demandé
                if (!getSort().isAscending()) {
                    result = -result;
                }

                return result;
            }
        }
    }

    // TODO : à supprimer ??
    protected void onSelectedLocaleChanged(AjaxRequestTarget target) {
        datatable = new ModulesDataTable<>("datatable", columns, new ContentProvider(contentsLocale.getObject().getObjectContext()),
                getItemsPerPage()) {

            @Override
            protected void onConfigure() {
                super.onConfigure();
                this.setVisible(contentTypeModel.getObject() != null && !contentsModel.getObject().isEmpty());
            }
        };
        datatable.setOutputMarkupId(true);
        datatable.setOutputMarkupPlaceholderTag(true);
        target.add(datatable);
    }
}
