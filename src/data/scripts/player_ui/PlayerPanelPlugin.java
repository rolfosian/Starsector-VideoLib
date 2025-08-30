package data.scripts.player_ui;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.PositionAPI;

// this doesnt even do anything as far as i can tell
public class PlayerPanelPlugin extends BaseCustomUIPanelPlugin {
    private float x;
    private float y;
    private float width;
    private float height;

    @Override
    public void render(float alphaMult) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(0f, 0f, 0f, 0.7f);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();

        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    public void positionChanged(PositionAPI pos) {
        x = pos.getX();
        y = pos.getY();
        width = pos.getWidth();
        height = pos.getHeight();
    }

    public void init(PositionAPI panelPos) {
        x = panelPos.getX();
        y = panelPos.getY();
        width = panelPos.getWidth();
        height = panelPos.getHeight();
    }
}
