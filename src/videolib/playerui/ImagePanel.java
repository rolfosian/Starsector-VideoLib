package videolib.playerui;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import videolib.projector.ImagePlugin;

public class ImagePanel {
    private CustomPanelAPI panel;
    private ImagePlugin plugin;

    public ImagePanel(CustomPanelAPI panel, ImagePlugin plugin) {
        this.panel = panel;
        this.plugin = plugin;
    }

    public PositionAPI addTo(UIPanelAPI parent) {
        return parent.addComponent(panel);
    }

    public void init() {
        plugin.init(panel.getPosition(), panel);
    }
}
