package com.coala.appliedpowah.integration;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** R3: GUI status lines must not use parenthetical explanations. */
class GuiLangCopyTest {

    private static String read(String name) throws Exception {
        Path p = Path.of("common/src/main/resources/assets/applied_powah/lang", name);
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    @Test
    void zhEjectLineHasNoParenExplanation() throws Exception {
        String json = read("zh_cn.json");
        assertTrue(json.contains("\"applied_powah.gui.eject.sides\": \"产物输出: 指定面\""));
        assertFalse(json.contains("产物输出: 指定面（"));
        assertFalse(json.contains("产物输出: 指定面("));
    }

    @Test
    void enEjectLineHasNoParenExplanation() throws Exception {
        String json = read("en_us.json");
        assertTrue(json.contains("\"applied_powah.gui.eject.sides\": \"Output: configured sides\""));
        assertFalse(json.contains("Output: configured sides ("));
        assertFalse(json.contains("Output: configured sides("));
    }

    @Test
    void allThreeEjectStatesExistInBothLanguages() throws Exception {
        for (String lang : new String[] {"zh_cn.json", "en_us.json"}) {
            String json = read(lang);
            for (String key : new String[] {
                    "applied_powah.gui.eject.off",
                    "applied_powah.gui.eject.sides",
                    "applied_powah.gui.eject.network",
                    "applied_powah.gui.eject.sides_config",
                    "applied_powah.gui.eject.sides_title",
                    "applied_powah.gui.page.label",
                    "applied_powah.gui.page.prev",
                    "applied_powah.gui.page.next"}) {
                assertTrue(json.contains("\"" + key + "\":"), lang + " missing " + key);
            }
        }
    }

    @Test
    void removedCardKeysAreGone() throws Exception {
        for (String lang : new String[] {"zh_cn.json", "en_us.json"}) {
            String json = read(lang);
            assertFalse(json.contains("thread_card"), lang + " still lists thread cards");
            assertFalse(json.contains("parallel_card"), lang + " still lists parallel cards");
            assertFalse(json.contains("upgrade.family"), lang + " still lists card families");
            assertFalse(json.contains("tooltip.card"), lang + " still lists card tooltips");
            assertFalse(json.contains("gui.auto_export"), lang + " still lists the old auto-export keys");
        }
    }
}
