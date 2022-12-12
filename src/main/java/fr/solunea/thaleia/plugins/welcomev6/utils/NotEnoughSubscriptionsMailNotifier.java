package fr.solunea.thaleia.plugins.welcomev6.utils;

import fr.solunea.thaleia.model.Publication;
import fr.solunea.thaleia.model.dao.ICayenneContextService;
import fr.solunea.thaleia.service.utils.Configuration;
import fr.solunea.thaleia.service.utils.IEventListener;
import fr.solunea.thaleia.utils.DetailedException;
import org.apache.log4j.Logger;

/**
 * Cet écouteur déclenche l'envoi d'un mail indiquant que l'accès d'un apprenant à une inscription a été demandé, mais
 * est impossible en raison d'un manque de crédits d'inscriptions pour l'utilisateur.
 */
public class NotEnoughSubscriptionsMailNotifier implements IEventListener {

    private static final Logger logger = Logger.getLogger(NotEnoughSubscriptionsMailNotifier.class);

    @Override
    public void onEvent(String eventName, ICayenneContextService contextService, Configuration configuration,
                        Object... parameters) throws DetailedException {
        logger.debug("Traitement !");
        // On s'assure qu'on a bien reçu la publication concernée en paramètre
        if (parameters[0] != null && Publication.class.isAssignableFrom(parameters[0].getClass())) {
            new MailUtils(configuration).warnForSubscriptions((Publication) parameters[0]);
        }
    }
}
