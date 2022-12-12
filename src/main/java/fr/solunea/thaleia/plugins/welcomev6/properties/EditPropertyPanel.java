package fr.solunea.thaleia.plugins.welcomev6.properties;

import fr.solunea.thaleia.model.ContentPropertyValue;
import fr.solunea.thaleia.model.ContentVersion;
import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.model.dao.ContentPropertyValueDao;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public abstract class EditPropertyPanel extends Panel {

    protected static final Logger logger = Logger.getLogger(EditPropertyPanel.class);

    private int contentPropertyValueId;

    public EditPropertyPanel(String id, String contentPropertyUnlocalizedName, IModel<ContentVersion> contentVersion, IModel<Locale> locale) {
        super(id);
        setDefaultModel(new CompoundPropertyModel<>(Model.of(getContentPropertyValue(contentPropertyUnlocalizedName, contentVersion, locale))));
    }

    private ContentPropertyValue getContentPropertyValue(String contentPropertyUnlocalizedName, IModel<ContentVersion> contentVersion, IModel<Locale> locale) {
        ContentPropertyValueDao contentPropertyValueDao = new ContentPropertyValueDao(ThaleiaApplication.get().getConfiguration().getLocalDataDir().getAbsolutePath(),
                ThaleiaApplication.get().getConfiguration().getBinaryPropertyType(), contentVersion.getObject().getObjectContext());
        // On recherche l'objet stocké par son id
        ContentPropertyValue result = contentPropertyValueDao.get(contentPropertyValueId);
        if (result != null) {
            return result;
        }

        // Si la ContentProperyValueId ne pointe plus sur un objet existant, alors on va rechercher à nouveau
        // cet objet pour cette ContentVersion.
        result = ThaleiaSession.get().getContentPropertyService().getContentPropertyValue(contentVersion
                .getObject(), contentPropertyUnlocalizedName, locale.getObject());
        logger.debug("Valeur de la propriété pour ContentVersion=" + contentVersion.getObject() + " et name="
                + contentPropertyUnlocalizedName + " locale= " + locale.getObject() + ": " + result);

        // Si la valeur est nulle, alors on crée une valeur vide, sinon le panneau d'édition de cette propriété
        // ne s'affichera pas;
        if (result == null) {
            try {
                result = ThaleiaSession.get().getContentPropertyService().setContentPropertyValue
                        (contentVersion.getObject(), contentPropertyUnlocalizedName, locale.getObject(), "");
                logger.debug("Valeur de la propriété fabriquée : " + result);
            } catch (DetailedException e) {
                logger.error(e);
            }
        }

        // On stocke l'id de cet objet.
        contentPropertyValueId = contentPropertyValueDao.getPK(result);
        // logger.debug("Appel du modèle, valeur = " + result);
        return result;
    }

    @SuppressWarnings("unchecked")
    protected IModel<ContentPropertyValue> getPanelModel() {
        return (IModel<ContentPropertyValue>) getDefaultModel();
    }
}
