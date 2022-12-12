package fr.solunea.thaleia.plugins.welcomev6.customization;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Une propriété personnalisable, sous la forme d'une clé (stockée en base, pour
 * être retrouvé), d'un label localisée et d'une liste de valeurs possibles.
 */
public class CustomizationPropertyProposal {
    public String name;
    public Map<Locale, String> label;
    public List<String> values;
    public String defaultValue;

    /**
     * @param values       les valeurs possibles, pour un sélecteur
     * @param defaultValue la valeur par défaut à présenter si aucune n'a été définie
     */
    public CustomizationPropertyProposal(String name, Map<Locale, String> label, List<String> values, String
            defaultValue) {
        this.name = name;
        this.label = label;
        this.values = values;
        this.defaultValue = defaultValue;
    }
}