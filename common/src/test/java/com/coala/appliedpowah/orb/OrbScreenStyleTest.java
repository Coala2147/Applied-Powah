package com.coala.appliedpowah.orb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the multi-page ScreenStyle JSONs of the orb GUIs (0.6.2):
 *
 * <ul>
 *   <li>every page has its own slot semantic with the same grid origin — pages
 *       overlap and only the active one is shown;</li>
 *   <li>one shared MACHINE_OUTPUT slot at the texture's output hole;</li>
 *   <li>rod slots only on Advanced / ME (vertical column);</li>
 *   <li>player inventory / hotbar positions override the AE2 common include so
 *       they match the 176×199 orb texture.</li>
 * </ul>
 */
class OrbScreenStyleTest {

    private static final Path SCREENS =
            Path.of("common/src/main/resources/assets/ae2/screens");

    private static JsonObject load(String name) throws Exception {
        String text = Files.readString(SCREENS.resolve(name), StandardCharsets.UTF_8);
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private static JsonObject slot(JsonObject style, String semantic) {
        JsonObject slots = style.getAsJsonObject("slots");
        assertTrue(slots.has(semantic), "missing slot entry: " + semantic);
        return slots.getAsJsonObject(semantic);
    }

    @Test
    @DisplayName("fluix tier: single page + output + player inventory")
    void fluixStyle() throws Exception {
        JsonObject style = load("fluix_energizing_orb.json");
        JsonObject slots = style.getAsJsonObject("slots");
        assertEquals(1, slots.keySet().stream().filter(k -> k.startsWith("ap_orb_page_")).count());
        assertEquals(0, slots.keySet().stream().filter(k -> k.equals("ap_orb_rod")).count());

        JsonObject page = slot(style, "ap_orb_page_1");
        assertEquals(25, page.get("left").getAsInt());
        assertEquals(30, page.get("top").getAsInt());
        assertEquals("BREAK_AFTER_2COLS", page.get("grid").getAsString());

        JsonObject output = slot(style, "MACHINE_OUTPUT");
        assertEquals(120, output.get("left").getAsInt());
        assertEquals(44, output.get("top").getAsInt());

        JsonObject inv = slot(style, "PLAYER_INVENTORY");
        assertEquals(8, inv.get("left").getAsInt());
        assertEquals(115, inv.get("top").getAsInt());
        assertEquals("BREAK_AFTER_9COLS", inv.get("grid").getAsString());

        JsonObject hotbar = slot(style, "PLAYER_HOTBAR");
        assertEquals(8, hotbar.get("left").getAsInt());
        assertEquals(173, hotbar.get("top").getAsInt());

        JsonObject background = style.getAsJsonObject("background");
        assertEquals("applied_powah:textures/gui/fluix_energizing_orb.png",
                background.get("texture").getAsString());
        assertEquals(176, background.getAsJsonArray("srcRect").get(2).getAsInt());
        assertEquals(199, background.getAsJsonArray("srcRect").get(3).getAsInt());
    }

    @Test
    @DisplayName("advanced / ME tiers: 4 overlapping pages + rod column")
    void advancedAndMeStyles() throws Exception {
        for (String name : new String[] {"advanced_energizing_orb.json", "me_energizing_orb.json"}) {
            JsonObject style = load(name);
            JsonObject slots = style.getAsJsonObject("slots");

            long pages = slots.keySet().stream().filter(k -> k.startsWith("ap_orb_page_")).count();
            assertEquals(4, pages, name + " must define 4 page semantics");

            for (String semantic : slots.keySet()) {
                if (semantic.startsWith("ap_orb_page_")) {
                    JsonObject page = slots.getAsJsonObject(semantic);
                    assertEquals(14, page.get("left").getAsInt(), semantic);
                    assertEquals(30, page.get("top").getAsInt(), semantic);
                    assertEquals("BREAK_AFTER_2COLS", page.get("grid").getAsString(), semantic);
                }
            }

            JsonObject output = slot(style, "MACHINE_OUTPUT");
            assertEquals(109, output.get("left").getAsInt());
            assertEquals(44, output.get("top").getAsInt());

            JsonObject rod = slot(style, "ap_orb_rod");
            assertEquals(153, rod.get("left").getAsInt());
            assertEquals(22, rod.get("top").getAsInt());
            assertEquals("VERTICAL", rod.get("grid").getAsString());

            JsonObject textures = style.getAsJsonObject("images");
            assertTrue(textures.has("progressBar"), "progress bar image");
            assertTrue(textures.has("powerAlert"), "power alert image");
            JsonObject widgets = style.getAsJsonObject("widgets");
            assertTrue(widgets.has("progressBar"), "progress bar widget");
            assertTrue(widgets.has("powerAlert"), "power alert widget");
            assertTrue(widgets.has("patternPage"), "pattern page widget");

            // The player inventory must sit at the orb texture's holes, not AE2's
            // common 82/24 offsets (which would be 2px off for a 199px panel).
            assertEquals(115, slot(style, "PLAYER_INVENTORY").get("top").getAsInt());
            assertEquals(173, slot(style, "PLAYER_HOTBAR").get("top").getAsInt());
        }
    }

    @Test
    @DisplayName("directions sub-screen has 6 face checkboxes + back button")
    void directionsStyle() throws Exception {
        JsonObject style = load("energizing_orb_directions.json");
        JsonObject widgets = style.getAsJsonObject("widgets");
        for (String side : new String[] {"down", "up", "north", "south", "west", "east"}) {
            assertTrue(widgets.has("side_" + side), "missing checkbox side_" + side);
        }
        assertTrue(widgets.has("back"), "missing back button");
        JsonObject background = style.getAsJsonObject("background");
        assertNotNull(background);
        // the 6 face checkboxes must not overlap each other
        long distinctX = widgets.keySet().stream()
                .filter(k -> k.startsWith("side_"))
                .map(k -> widgets.getAsJsonObject(k).get("left").getAsInt())
                .distinct()
                .count();
        assertEquals(3, distinctX, "3 checkbox columns");
    }
}
