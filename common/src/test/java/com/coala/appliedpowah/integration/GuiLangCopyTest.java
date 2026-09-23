package com.coala.appliedpowah.integration;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** R3: GUI status lines must not use parenthetical explanations. */
class GuiLangCopyTest {

    private static String read(String name) throws Exception {
        Path p = Path.of("common/src/main/resources/assets/applied_powah/lang", name);
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    @Test
    void zhAutoExportOnHasNoParenExplanation() throws Exception {
        String json = read("zh_cn.json");
        assertTrue(json.contains("\"applied_powah.gui.auto_export.on\": \"自动输出: 开\""));
        assertFalse(json.contains("自动输出: 开（"));
    }

    @Test
    void enAutoExportOnHasNoParenExplanation() throws Exception {
        String json = read("en_us.json");
        assertTrue(json.contains("\"applied_powah.gui.auto_export.on\": \"Auto-export: ON\""));
        assertFalse(json.contains("Auto-export: ON ("));
    }

    @Test
    void offLinesUnchanged() throws Exception {
        assertTrue(read("zh_cn.json").contains("\"applied_powah.gui.auto_export.off\": \"自动输出: 关\""));
        assertTrue(read("en_us.json").contains("\"applied_powah.gui.auto_export.off\": \"Auto-export: OFF\""));
    }

    @Test
    void autoExportOnIsExactShortLabel() throws Exception {
        // status reports state only — explanation lives in Guide/tooltip
        assertEquals(true, read("zh_cn.json").lines().anyMatch(l -> l.contains("自动输出: 开\"")));
    }
}
