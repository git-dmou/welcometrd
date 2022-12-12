package fr.solunea.thaleia.plugins.welcomev6.properties;

import fr.solunea.thaleia.model.ContentVersion;
import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.model.dao.ContentPropertyValueDao;
import fr.solunea.thaleia.model.dao.LocaleDao;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

@SuppressWarnings("serial")
public abstract class EditSelectorPropertyPanel extends EditPropertyPanel {

    private final DropDownChoice<String> dropDownChoice;

    public EditSelectorPropertyPanel(String id, String contentPropertyUnlocalizedName, IModel<ContentVersion>
            contentVersion, IModel<Locale> locale, List<String> choices) {
        super(id, contentPropertyUnlocalizedName, contentVersion, locale);

        LocaleDao localeDao = new LocaleDao(contentVersion.getObject().getObjectContext());
        ContentPropertyValueDao contentPropertyValueDao = new ContentPropertyValueDao(ThaleiaApplication.get().getConfiguration().getLocalDataDir().getAbsolutePath(),
                ThaleiaApplication.get().getConfiguration().getBinaryPropertyType(), contentVersion.getObject().getObjectContext());

        // Le nom de la propriété
        add(new Label("property.name", new Model<String>() {
            @Override
            public String getObject() {
                // Le nom de la propriété, dans la locale de l'IHM
                return getPanelModel().getObject().getName(localeDao.getLocale(ThaleiaSession.get().getLocale()));
            }
        }));

        // La sélection par défaut est la valeur actuelle de la propriété.
        IModel<String> selected = new PropertyModel<>(getPanelModel().getObject(), "value") {
            @Override
            public String getObject() {
                return contentPropertyValueDao.getValue(getPanelModel().getObject());
            }

            @Override
            public void setObject(String object) {
                // On fixe la valeur
                getPanelModel().getObject().setValue(object);
            }
        };

        dropDownChoice = new DropDownChoice<>("value", selected, choices);
        dropDownChoice.setRequired(true);

        dropDownChoice.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                // On remonte l'information de mise à jour
                onPropertyChanged(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, RuntimeException e) {
                // C'est par ici que le traitement passe en cas d'erreur de
                // validation du champ.

                // On remonte l'information de mise à jour
                onPropertyChanged(target);
            }
        });

        add(dropDownChoice.setOutputMarkupId(true));
    }

    protected abstract void onPropertyChanged(AjaxRequestTarget target);

    public DropDownChoice<String> getDropDownChoice() {
        return dropDownChoice;
    }

}
