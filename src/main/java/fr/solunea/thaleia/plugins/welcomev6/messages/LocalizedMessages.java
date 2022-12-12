package fr.solunea.thaleia.plugins.welcomev6.messages;

import fr.solunea.thaleia.webapp.security.ThaleiaSession;
import fr.solunea.thaleia.webapp.utils.MessagesUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.Session;

import java.util.Locale;

public class LocalizedMessages {

    @SuppressWarnings("unused")
    private final static Logger logger = Logger.getLogger(LocalizedMessages.class);

    /**
     * Erreur levée lors de la fonction de remplacement d'une valeur dans un écran, lors de sa génération depuis un
     * modèle.
     */
    public static final String REPLACE_FUNCTION_ON_SCREEN_ERROR = "replace.function.on.screen.error";
    /**
     * Erreur levée durant la recherche d'un paramètre, qui n'est pas trouvé.
     */
    public static final String PARAMETER_NOT_FOUND_ERROR = "parameter.not.found.error";

    /**
     * Erreur levée si un fichier n'est pas trouvé dans l'archive envoyée.
     */
    public static final String MEDIA_FILE_NOT_FOUND_ERROR = "media.not.found.error";

    /**
     * Erreur levée si l'identifiant d'écran existe déjà.
     */
    public static final String DUPLICATE_SCREEN_ID_ERROR = "duplicate.screen.id";

    /**
     * Erreur levée si l'identifiant d'écran n'est pas défini.
     */
    public static final String NULL_SCREEN_ID_ERROR = "null.screen.id";

    /**
     * Erreur levée si l'écran ne peut pas être enregistré dans les écrans Thaleia.
     */
    public static final String STORE_SCREEN_ERROR = "store.screen.error";

    /**
     * Erreur levée si l'enregistrement des propriétés de cet écran a échoué.
     */
    public static final String STORE_SCREEN_PROPERTIES_ERROR = "store.screen.properties.error";

    /**
     * Erreur levée si erreur de génération de l'écran.
     */
    public static final String SCREEN_GENERATION_ERROR = "screen.generation.error";

    /**
     * Erreur levée si aucun fichier Excel n'est trouvé dans l'archive.
     */
    public static final String NO_XLS_ERROR = "no.xls.error";

    /**
     * Erreur levée si plusieurs fichiers Excel ont été trouvés dans l'archive.
     */
    public static final String MULTIPLE_XLS_ERROR = "mulitple.xls.error";

    /**
     * Erreur levée si une feuille nécessaire au traitement n'a pas été trouvée dans le fichier Excel.
     */
    public static final String SHEET_NOT_FOUND_ERROR = "sheet.not.found.error";

    /**
     * Erreur levée si un fichier média doit être placé dans un écran, mais n'a pas été fourni dans l'archive.
     */
    public static final String FILE_NOT_FOUND_ERROR = "file.not.found.error";

    /**
     * Erreur levée une propriété utilisée pour les remplacements dans un modèle d'écran n'a pas été définie, ce qui ne
     * permet pas d'effectuer le remplacement.
     */
    public static final String REPLACE_VALUE_IN_SCREEN_ERROR = "replace.value.undefined";

    /**
     * Erreur levée si un média est décrit par une URL externe, mais qui n'est pas identifée comme telle lors du
     * traitement de l'écran : elle est donc traitée comme un nom de média, mais ce média n'existe pas.
     */
    public static final String URL_ERROR = "url.not.recognized";

    /**
     * Erreur levée si une question de QRU/QRM est associée à une correction qui n'est pas reconnue parmi les
     * corrections possibles. Par exemple, on attend "Vrai" ou "Faux", et on a la correction "Correct".
     */
    public static final String QRM_CORRECTION_NOT_VALID = "qrm.correction.not.valid";

    /**
     * Erreur levée si un fichier est associé à une ressource du module, mais n'a pas été trouvé dans l'archive.
     */
    public static final String RESOURCE_FILE_NOT_FOUND = "resource.file.not.found";

    /**
     * Recherche la valeur de la chaîne localisée (dans la locale de la session) dans le fichier
     * LocalizedMessages_XX.properties (où XX est la locale de la session).
     *
     * @param resourceKey le code du message à rechercher
     * @param parameters  si besoin, des paramètres pour appliquer des interprétations de valeurs dans la chaîne de
     *                    caractères ${0}, ${1}...
     * @return le message d'erreur localisé
     */
    public static String getMessage(String resourceKey, Object... parameters) {

        Locale locale = Session.exists() ? ThaleiaSession.get().getLocale() : Locale.getDefault();

        return getMessageForLocale(resourceKey, locale, parameters);

    }

    /**
     * Identique à {@link #getMessage}, mais en utilisant la locale demandée.
     */
    public static String getMessageForLocale(String resourceKey, Locale locale, Object... parameters) {
        return getMessageForLocaleAndClass(resourceKey, LocalizedMessages.class, locale, parameters);

    }

    /**
     * Intique à {@link #getMessageForLocale}, mais en recherchant dans les fichiers de localisation associés à cette
     * classe.
     */
    public static String getMessageForLocaleAndClass(String resourceKey, Class<?> clazz, Locale locale, Object...
            parameters) {
        return MessagesUtils.getLocalizedMessage(resourceKey, clazz, locale, parameters);
    }

}
