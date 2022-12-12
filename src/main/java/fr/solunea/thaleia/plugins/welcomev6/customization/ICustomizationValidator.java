package fr.solunea.thaleia.plugins.welcomev6.customization;

import fr.solunea.thaleia.utils.DetailedException;

import java.io.File;
import java.io.Serializable;

public interface ICustomizationValidator extends Serializable {

    void validate(File customizationFile) throws DetailedException;
}
