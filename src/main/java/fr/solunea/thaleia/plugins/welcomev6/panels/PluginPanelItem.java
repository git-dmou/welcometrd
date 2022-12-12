package fr.solunea.thaleia.plugins.welcomev6.panels;

/**
 * Bouton d'action du panneau latéral d'un plugin.
 */
public class PluginPanelItem {
    /**
     * ID du bouton
     */
    private String id;

    /**
     * Libellé du bouton
     */
    private String label;

    /**
     * Icône du bouton (doit correspondre à un SVG défini dans le document HTML)
     */
    private String icon;

    /**
     * Lien du bouton.
     */
    private String link;

    /**
     * Bouton d'action du panneau latéral d'un plugin.
     * @param id    ID du bouton
     * @param label Libellé du bouton
     * @param icon  Icône du bouton (doit correspondre à un SVG)
     * @param link  Lien du bouton.
     */
    public PluginPanelItem (String id, String label, String icon, String link) {
        this.label = label;
        this.icon = icon;
        this.link = link;
        this.id = id;
    }

}
