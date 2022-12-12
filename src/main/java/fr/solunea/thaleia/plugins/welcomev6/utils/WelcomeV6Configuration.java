package fr.solunea.thaleia.plugins.welcomev6.utils;

import fr.solunea.thaleia.model.ApplicationParameter;
import fr.solunea.thaleia.model.dao.ApplicationParameterDao;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.webapp.ThaleiaApplication;
import fr.solunea.thaleia.webapp.security.ThaleiaSession;

import java.util.*;

public class WelcomeV6Configuration {

    /**
     * @return les couples clé/valeur par défaut des paramètres.
     */
    private static Map<String, String> getDefaultParameters() {
        Map<String, String> defaultParameters = new HashMap<>();
        // Paramètres PayPal
        // On pourrait imaginer de stocker ces attributs avec un préfixe de type
        // "welcomeV6.paypal".
        defaultParameters.put("http.UseProxy", "false");
        defaultParameters.put("http.Retry", "1");
        defaultParameters.put("http.ReadTimeOut", "30000");
        defaultParameters.put("http.MaxConnection", "100");
        defaultParameters.put("http.GoogleAppEngine", "false");
        defaultParameters.put("service.EndPoint", "https://api.sandbox.paypal.com");
        defaultParameters.put("clientId", "clientId");
        defaultParameters.put("clientSecret", "clientSecret");
        return defaultParameters;
    }

    /**
     * @return les paramètres PayPal stockés en base
     */
    public static Properties getPayPalParameters() {
        // les noms des propriétés PayPal qui sont récupérées en base
        String[] keys = new String[]{"http.UseProxy", "http.Retry", "http.ReadTimeOut",
                "http.MaxConnection", "http.GoogleAppEngine", "service.EndPoint", "clientId", "clientSecret"};

        Properties result = new Properties();
        ApplicationParameterDao applicationParameterDao = new ApplicationParameterDao(ThaleiaSession.get().getContextService().getContextSingleton());
        for (String key : keys) {
            ApplicationParameter parameter = applicationParameterDao.findByName(key);
            if (parameter != null) {
                result.setProperty(key, parameter.getValue());
            }
        }
        return result;
    }

    /**
     * @return la liste des paramètres de l'application Thaleia existant en base
     * qui concernent le plugin WelcomeV6
     */
    public static List<ApplicationParameter> getParameters() {
        List<ApplicationParameter> result = new ArrayList<>();
        ApplicationParameterDao applicationParameterDao = new ApplicationParameterDao(ThaleiaSession.get().getContextService().getContextSingleton());

        for (String key : getDefaultParameters().keySet()) {
            ApplicationParameter parameter = applicationParameterDao.findByName(key);
            if (parameter != null) {
                result.add(parameter);
            }
        }
        return result;
    }

    /**
     * S'assure que les paramètres sont installés en base. S'ils existent, ils
     * ne sont pas modifiés. Sinon, ils sont créés avec leur valeur par défaut.
     *
     */
    public static void installDefaultParameters() throws DetailedException {
        ApplicationParameterDao applicationParameterDao = ThaleiaApplication.get().getApplicationParameterDao();
        Map<String, String> defaultParameters = getDefaultParameters();

        for (String key : defaultParameters.keySet()) {
            ApplicationParameter parameter = applicationParameterDao.findByName(key);
            if (parameter == null) {
                parameter = applicationParameterDao.get();
                parameter.setName(key);
                parameter.setValue(defaultParameters.get(key));
                try {
                    applicationParameterDao.save(parameter);
                } catch (DetailedException e) {
                    throw e.addMessage("Impossible d'enregistrer en base le paramètre WelcomeV6 " + key);
                }
            }
        }
    }

}
