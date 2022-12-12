package fr.solunea.thaleia.plugins.welcomev6.customization;

import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import org.apache.cayenne.ObjectContext;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Panneau pour sélectionner la valeur d'un paramètre de personnalisation, parmi
 * les valeurs proposées.
 */
@SuppressWarnings("serial")
public class CustomizationPropertySelectorPanel extends Panel {

    protected static final Logger logger = Logger.getLogger(CustomizationPropertySelectorPanel.class.getName());

    /**
     * @param id                       l'id du pannel
     * @param customizationName        le nom du paramètre de personnalisation
     * @param customizationClassLabel  les libellés des valeurs possibles
     * @param customizationClassValues les valeurs possibles
     * @param defaultValue la version SCORM par défaut pour les exports, si la propriété est SCORM_PROPERTY
     */
    public CustomizationPropertySelectorPanel(String id, String customizationName, Map<Locale, String>
            customizationClassLabel, List<String> customizationClassValues, String defaultValue) {
        super(id);

        // Permet de recharger l'ensemble lorsque la valeur sélectionnée est
        // changée.
        WebMarkupContainer main = new WebMarkupContainer("main");
        main.setOutputMarkupId(true);
        add(main);

        // Le label de la propriété
        add(new Label("customizationLabel", new Model<String>() {
            @Override
            public String getObject() {
                String result = customizationClassLabel.get(ThaleiaSession.get().getLocale());
                if (result == null) {
                    logger.error("Mauvaise configuration du plugin : le libellé du paramètre " + customizationName +
                            "" + " est mal localisé !");
                }
                return result;
            }
        }));

        // Les valeurs possibles
        Form<Void> form = new Form<>("customizationClass_form");
        main.add(form);

        // La valeur sélectionnée
        IModel<String> selectedOption = new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                try {
                    String optionValue = ThaleiaSession.get().getCustomizationFilesService()
                            .getCustomizationPropertyValue(customizationName, null, ThaleiaSession.get()
                                    .getAuthenticatedUser().getDomain());
                    // Si La valeur de personnalisation n'a pas encore été définie, on présente celle par défaut
                    if (optionValue == null) {
                        optionValue = defaultValue;
                    }
                    return optionValue;
                } catch (DetailedException e) {
                    logger.warn(e);
                    return "";
                }
            }
        };
        @SuppressWarnings("unchecked") DropDownChoice<String> ddc = (DropDownChoice<String>) new
                DropDownChoice<>("customizationClass_options", selectedOption, customizationClassValues).add
                (new AjaxFormComponentUpdatingBehavior("onchange") {
            protected void onUpdate(AjaxRequestTarget target) {
                String selected = this.getComponent().getDefaultModelObjectAsString();
                logger.debug("Sélection : " + selected);
                try {
                    ObjectContext tempContext = ThaleiaSession.get().getContextService().getNewContext();
                    ThaleiaSession.get().getCustomizationFilesService().setCustomizationPropertyValue
                            (customizationName, null, ThaleiaSession.get().getAuthenticatedUser().getDomain(),
                                    selected, tempContext);
                    tempContext.commitChanges();
                } catch (DetailedException e) {
                    logger.warn(e);
                }
                // Retour ajax commenté car le composant affiche par lui
                // même la valeur choisie par l'utilisateur
                // target.add(main);
            }
        });
        form.add(ddc.add(AttributeModifier.append("placeholder", selectedOption)));
    }

}
