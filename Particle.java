package main.kyjko.zsirkreta;

import java.awt.*;

public class Particle {

    float x, y;
    float w, h;
    //might be overkill, depends on fps really
    public static float vel = 0.5f;

    Color color = new Color(0, 200, 118, 55);

    public Particle(float x, float y) {
        this.x = x;
        this.y = y;
        this.w = 10;
        this.h = 30;
        //this.vel *= (float)(Math.random() + 0.001f);
    }

    public void render(Graphics g) {
        g.setColor(color);
        g.fillRect((int)x, (int)y, (int)w, (int)h);

        y -=vel;

        if(y < 0) {
            y = (float)Math.random()*Main.HEIGHT/2 + Main.HEIGHT;
            x = (float)Math.random()*Main.WIDTH;
        }
    }
}
