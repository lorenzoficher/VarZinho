package br.edu.unipampa.varzinho.ui;

import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VarZinhoIconsTest {

    @Test
    void everyInterfaceIconLoadsFromTheBundledTablerResources() {
        List<Icon> icons = List.of(VarZinhoIcons.courts(), VarZinhoIcons.archive(),
                VarZinhoIcons.operators(), VarZinhoIcons.record(), VarZinhoIcons.stop(),
                VarZinhoIcons.maintenance(), VarZinhoIcons.aim(), VarZinhoIcons.openClip(),
                VarZinhoIcons.theme());

        for (Icon icon : icons) {
            assertEquals(18, icon.getIconWidth());
            assertEquals(18, icon.getIconHeight());
        }
    }

    @Test
    void everyIconAndTheTablerLicenseShipWithTheApplication() {
        List<String> resources = List.of("building-stadium.svg", "archive.svg", "users.svg",
                "player-record.svg", "player-stop.svg", "tool.svg", "focus-2.svg",
                "external-link.svg", "sun-moon.svg", "LICENSE");

        for (String resource : resources) {
            assertNotNull(getClass().getClassLoader().getResource("icons/tabler/" + resource));
        }
    }
}
