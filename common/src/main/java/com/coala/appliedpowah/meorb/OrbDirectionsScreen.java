package com.coala.appliedpowah.meorb;

import appeng.client.gui.AESubScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.widgets.AECheckbox;
import com.coala.appliedpowah.orb.EnergizingOrbMenu;
import com.coala.appliedpowah.orb.EnergizingOrbScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Output-face configuration sub-screen (eject mode = SIDES).
 *
 * <p>Six checkboxes bound to {@link EnergizingOrbMenu#isSideAllowed(Direction)}; each
 * toggle is a client action handled on the server-side BlockEntity. The back button
 * returns to the orb GUI through AE2's {@code ISubMenuHost} protocol.</p>
 */
public class OrbDirectionsScreen extends AESubScreen<EnergizingOrbMenu, EnergizingOrbScreen> {

    private static final Direction[] SIDES = Direction.values();
    private final Map<Direction, AECheckbox> boxes = new EnumMap<>(Direction.class);

    public OrbDirectionsScreen(EnergizingOrbScreen parent) {
        super(parent, "/screens/energizing_orb_directions.json");
    }

    @Override
    protected void init() {
        super.init();
        boxes.clear();
        for (Direction side : SIDES) {
            AECheckbox box = widgets.addCheckbox(
                    "side_" + side.getName(),
                    Component.translatable("applied_powah.gui.side." + side.getName()),
                    () -> this.menu.toggleSide(side));
            boxes.put(side, box);
        }
        appeng.client.gui.implementations.AESubScreen.addBackButton(this.menu, "back", widgets);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        for (Map.Entry<Direction, AECheckbox> entry : boxes.entrySet()) {
            entry.getValue().setSelected(this.menu.isSideAllowed(entry.getKey()));
        }
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        guiGraphics.drawString(font,
                Component.translatable("applied_powah.gui.eject.sides_title"),
                8, 20, style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB(), false);
    }
}
