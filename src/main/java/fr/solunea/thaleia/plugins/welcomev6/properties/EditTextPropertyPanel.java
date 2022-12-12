package fr.solunea.thaleia.plugins.welcomev6.properties;

import fr.solunea.thaleia.model.ContentVersion;
import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.model.dao.ContentPropertyValueDao;
import fr.solunea.thaleia.model.dao.LocaleDao;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

abstract class EditTextPropertyPanel extends EditPropertyPanel {

    private final TextField<String> textField;

    EditTextPropertyPanel(String id, String contentPropertyUnlocalizedName, IModel<ContentVersion> contentVersion,
                          IModel<Locale> locale) {
        super(id, contentPropertyUnlocalizedName, contentVersion, locale);

        LocaleDao localeDao = new LocaleDao(contentVersion.getObject().getObjectContext());
        ContentPropertyValueDao contentPropertyValueDao = new ContentPropertyValueDao(ThaleiaApplication.get().getConfiguration().getLocalDataDir().getAbsolutePath(),
                ThaleiaApplication.get().getConfiguration().getBinaryPropertyType(), contentVersion.getObject().getObjectContext());

        // Le nom de la propriété
        add(new Label("property.name", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                // Le nom de la propriété, dans la locale de l'IHM
                return getPanelModel().getObject().getName(localeDao.getLocale(ThaleiaSession.get().getLocale()));
            }
        }));

        // La valeur de la propriété
        textField = new TextField<>("value", new PropertyModel<String>(((getDefaultModelObject())), "value") {
            @Override
            public String getObject() {
                // Si la valeur est un fichier, on ne présente
                // que son nom, et pas le répertoire qui le
                // contient.
                return contentPropertyValueDao.getValue(getPanelModel().getObject());
            }

            @Override
            public void setObject(String object) {
                // On fixe la valeur
                // TODO : comment gérer un champ de type fichier ?
                getPanelModel().getObject().setValue(object);
            }
        }) {

            @Override
            public boolean isEnabled() {
                // Si de toutes façons il a été désactivé, alors on ne va pas plus loin
                if (!super.isEnabled()) {
                    return false;
                }

                try {
                    // Modifiable si la propriété n'est pas du type
                    // "fichier binaire"
                    return !contentPropertyValueDao.isValueDescribesAFile(getPanelModel().getObject());
                } catch (Exception e) {
                    logger.warn("Erreur durant l'analyse d'un objet : " + e + "\n" + LogUtils.getStackTrace(e
                            .getStackTrace()));
                    return false;
                }
            }
        };
        textField.add(new OnChangeAjaxBehavior() {
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

        add(textField.setOutputMarkupId(true));
    }

    protected abstract void onPropertyChanged(AjaxRequestTarget target);

    TextField<String> getTextField() {
        return textField;
    }

}
