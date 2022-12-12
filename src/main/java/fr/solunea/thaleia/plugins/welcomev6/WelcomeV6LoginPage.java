package fr.solunea.thaleia.plugins.welcomev6;

import org.apache.wicket.Page;

@SuppressWarnings("serial")
public class WelcomeV6LoginPage extends fr.solunea.thaleia.webapp.pages.LoginPage {

    public WelcomeV6LoginPage() {
        super();
    }

    public WelcomeV6LoginPage(String destination) {
        super(destination);
    }

    public WelcomeV6LoginPage(Page destination) {
        super(destination);
    }

    @Override
    protected void addWelcomeMessage() {
    }
}
