package fr.solunea.thaleia.plugins.welcomev6.utils;

import fr.solunea.thaleia.model.ContentPropertyValue;
import fr.solunea.thaleia.model.ContentVersion;
import fr.solunea.thaleia.model.Locale;
import fr.solunea.thaleia.service.ContentPropertyService;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;

public class ContentTypeParser {
	
	/**
	 * @param contentVersion
	 * @param locale
	 * @return true si le format de cette version est un format exécutable, dans
	 *         la locale demandée.
	 */
	public static boolean isExeContent(ContentVersion contentVersion, Locale locale) {
		ContentPropertyService contentService = ThaleiaSession.get().getContentPropertyService();
		ContentPropertyValue value = contentService.getContentPropertyValue(contentVersion, "ModuleFormat", locale);
		if (value != null) {
			// On suppose que, quelle que soit la locale, si la valeur de la
			// propriété contient la chaîne "exe", alors le format de la version
			// est le format exécutable.
			if (value.getValue().toLowerCase().contains("exe")) {
				return true;
			}
		}
		return false;
	}

}
