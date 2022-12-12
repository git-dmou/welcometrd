package fr.solunea.thaleia.plugins.welcomev6.publish;

import fr.solunea.thaleia.model.ContentVersion;
import fr.solunea.thaleia.plugins.welcomev6.messages.LocalizedMessages;
import fr.solunea.thaleia.utils.DetailedException;
import fr.solunea.thaleia.utils.LogUtils;
import fr.solunea.thaleia.webapp.pages.ThaleiaPageV6;
import fr.solunea.thaleia.webapp.panels.ThaleiaFeedbackPanel;
import fr.solunea.thaleia.webapp.panels.ThaleiaLazyLoadPanel;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public abstract class PublishPage extends ThaleiaPageV6 {

	public PublishPage() {
		super(false);

		// l'aperçu est en cours. Ce panel sera remplacé par la page du contenu
		// temporaire.
		final ThaleiaLazyLoadPanel donePanel = new ThaleiaLazyLoadPanel(
				"treatment") {

			@SuppressWarnings("unchecked")
			@Override
			public Component getLazyLoadComponent(String id) {
				return new DonePanel(id,
						(IModel<ContentVersion>) PublishPage.this
								.getDefaultModel());
			}

			@Override
			protected Component getLoadingComponent(String markupId) {
				return new LoadingPanel(markupId);
			}
		};
		add(donePanel);
	}

	protected abstract void prepareFile() throws DetailedException;

	class DonePanel extends Panel {

		public DonePanel(String id, IModel<ContentVersion> module) {
			super(id);

			// Panneau de feedback
			add(new ThaleiaFeedbackPanel("feedbackPanel")
					.setOutputMarkupId(true));

			// On déclenche la préparation du fichier maitenant, pour que durant
			// l'instanciation de ce pannel, le pannel de traitement en cours
			// reste présenté.
			List<String> errorMessagesKeys = new ArrayList<>();
			try {
				prepareFile();

			} catch (Exception e) {
				// Pour faire moins peur, on n'envoie pas sur la
				// page d'erreur.
				logger.warn("Erreur de publication du contenu :" + e);
				logger.warn(LogUtils.getStackTrace(e.getStackTrace()));

				// On présente les erreurs transmises par le
				// traitement
				for (String key : errorMessagesKeys) {
					error(LocalizedMessages.getMessage(key));
				}
				error(LocalizedMessages.getMessage("publish.error"));

			}
		}
	}

	class LoadingPanel extends Panel {
		public LoadingPanel(String id) {
			super(id);
			add(new Label("progressLabel", new StringResourceModel(
					"progressLabel", PublishPage.this, null)));
		}
	}

}
