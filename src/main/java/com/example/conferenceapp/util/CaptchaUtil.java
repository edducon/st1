package com.example.conferenceapp.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;

public class CaptchaUtil {
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RND = new SecureRandom();

    public record Captcha(Image image, String code) {}

    public static Captcha generate() {
        int width = 120, height = 50;
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();

        // фон
        g.setColor(Color.WHITE);
        g.fillRect(0,0,width,height);

        // шум
        for (int i=0;i<60;i++) {
            g.setColor(new Color(RND.nextInt(255),RND.nextInt(255),RND.nextInt(255)));
            int x1 = RND.nextInt(width), y1 = RND.nextInt(height);
            int x2 = RND.nextInt(width), y2 = RND.nextInt(height);
            g.drawLine(x1,y1,x2,y2);
        }

        // код
        g.setFont(new Font("Comic Sans MS", Font.BOLD, 28));
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<4;i++) sb.append(CHARS.charAt(RND.nextInt(CHARS.length())));
        String code = sb.toString();

        for (int i=0;i<4;i++) {
            g.setColor(new Color(RND.nextInt(150),RND.nextInt(150),RND.nextInt(150)));
            g.drawString(code.substring(i,i+1), 15 + i*25, 35 + RND.nextInt(6)-3);
        }
        g.dispose();
        return new Captcha(SwingFXUtils.toFXImage(bi, null), code);
    }
}
