package com.xuanchen.auth.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 滑块验证码工具类
 *
 * @author XuanChen
 * @date 2026-06-01
 */
public class CaptchaUtil {

    private static final Logger log = LoggerFactory.getLogger(CaptchaUtil.class);

    /**
     * 滑块宽度
     */
    private static final int SLIDER_WIDTH = 50;

    /**
     * 滑块高度
     */
    private static final int SLIDER_HEIGHT = 50;

    /**
     * 图片宽度
     */
    private static final int IMAGE_WIDTH = 280;

    /**
     * 图片高度
     */
    private static final int IMAGE_HEIGHT = 150;

    /**
     * 滑块范围（X轴的最小和最大值）
     */
    private static final int MIN_OFFSET_X = 30;
    private static final int MAX_OFFSET_X = IMAGE_WIDTH - SLIDER_WIDTH - 30;

    /**
     * 生成验证码图片
     *
     * @return Map 包含:
     * - bgImage: 带缺口的背景图 Base64
     * - sliderImage: 滑块图 Base64
     * - offsetX: 缺口位置 X 坐标（只用于存储，不返回给前端）
     */
    public static Map<String, Object> generateCaptcha() {
        Map<String, Object> result = new HashMap<>();

        // 1. 生成随机缺口位置
        int offsetX = (int) (Math.random() * (MAX_OFFSET_X - MIN_OFFSET_X) + MIN_OFFSET_X);
        int offsetY = (int) (Math.random() * (IMAGE_HEIGHT - SLIDER_HEIGHT - 20) + 10);

        // 2. 创建背景图
        BufferedImage bgImage = createBackgroundImage();

        // 3. 创建滑块图（从背景图中截取）
        BufferedImage sliderImage = createSliderImage(bgImage, offsetX, offsetY);

        // 4. 在背景图上绘制缺口
        drawGap(bgImage, offsetX, offsetY);

        // 5. 转为 Base64
        String bgBase64 = imageToBase64(bgImage, "png");
        String sliderBase64 = imageToBase64(sliderImage, "png");

        result.put("bgImage", bgBase64);
        result.put("sliderImage", sliderBase64);
        result.put("offsetX", offsetX);
        result.put("offsetY", offsetY);
        result.put("imageWidth", IMAGE_WIDTH);
        result.put("imageHeight", IMAGE_HEIGHT);
        result.put("sliderWidth", SLIDER_WIDTH);
        result.put("sliderHeight", SLIDER_HEIGHT);

        return result;
    }

    /**
     * 创建背景图（随机颜色渐变）
     */
    private static BufferedImage createBackgroundImage() {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 创建渐变背景
        GradientPaint gradient = new GradientPaint(0, 0,
                new Color((int) (Math.random() * 256), (int) (Math.random() * 256), (int) (Math.random() * 256)),
                IMAGE_WIDTH, IMAGE_HEIGHT,
                new Color((int) (Math.random() * 256), (int) (Math.random() * 256), (int) (Math.random() * 256)));
        g.setPaint(gradient);
        g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        // 添加干扰线 - 增加条数和长度
        for (int i = 0; i < 10; i++) {
            g.setColor(new Color((int) (Math.random() * 256), (int) (Math.random() * 256), (int) (Math.random() * 256), 180));
            g.setStroke(new BasicStroke((float) (1 + Math.random() * 3)));
            int x1 = (int) (Math.random() * IMAGE_WIDTH);
            int y1 = (int) (Math.random() * IMAGE_HEIGHT);
            int x2 = (int) (Math.random() * IMAGE_WIDTH);
            int y2 = (int) (Math.random() * IMAGE_HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        // 添加干扰点
        for (int i = 0; i < 30; i++) {
            g.setColor(new Color((int) (Math.random() * 256), (int) (Math.random() * 256), (int) (Math.random() * 256)));
            g.fillOval((int) (Math.random() * IMAGE_WIDTH), (int) (Math.random() * IMAGE_HEIGHT), 2, 2);
        }

        // 添加文字干扰 - 增加字母数量到9个
        g.setFont(new Font("Arial", Font.BOLD, 16 + (int) (Math.random() * 10)));
        for (int i = 0; i < 9; i++) {
            g.setColor(new Color((int) (Math.random() * 256), (int) (Math.random() * 256), (int) (Math.random() * 256), 120));
            char ch = (char) ('A' + (int) (Math.random() * 26));
            int x = (int) (Math.random() * (IMAGE_WIDTH - 20));
            int y = (int) (Math.random() * (IMAGE_HEIGHT - 20)) + 20;
            g.drawString(String.valueOf(ch), x, y);
        }

        g.dispose();
        return image;
    }

    /**
     * 从背景图中截取滑块
     */
    private static BufferedImage createSliderImage(BufferedImage bgImage, int offsetX, int offsetY) {
        BufferedImage slider = new BufferedImage(SLIDER_WIDTH, SLIDER_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = slider.createGraphics();

        // 从背景图截取
        g.drawImage(bgImage.getSubimage(offsetX, offsetY, SLIDER_WIDTH, SLIDER_HEIGHT), 0, 0, null);

        // 添加边框效果
        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke(2));
        g.drawRect(0, 0, SLIDER_WIDTH - 1, SLIDER_HEIGHT - 1);

        // 添加阴影效果
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRect(SLIDER_WIDTH - 3, 3, 3, SLIDER_HEIGHT - 3);
        g.fillRect(SLIDER_WIDTH - 3, SLIDER_HEIGHT - 3, 3, 3);
        g.fillRect(3, SLIDER_HEIGHT - 3, SLIDER_WIDTH - 6, 3);

        g.dispose();
        return slider;
    }

    /**
     * 在背景图上绘制缺口
     */
    private static void drawGap(BufferedImage bgImage, int offsetX, int offsetY) {
        Graphics2D g = bgImage.createGraphics();

        // 绘制半透明缺口（模拟透明效果）
        g.setColor(new Color(255, 255, 255, 200));
        g.fillRect(offsetX, offsetY, SLIDER_WIDTH, SLIDER_HEIGHT);

        // 绘制缺口边框
        g.setColor(new Color(150, 150, 150, 150));
        g.setStroke(new BasicStroke(2));
        g.drawRect(offsetX, offsetY, SLIDER_WIDTH, SLIDER_HEIGHT);

        g.dispose();
    }

    /**
     * 将图片转为 Base64
     */
    private static String imageToBase64(BufferedImage image, String format) {
        // ByteArrayOutputStream#close 为空实现，try-with-resources 仅为统一资源写法
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            return "data:image/" + format + ";base64," + base64;
        } catch (IOException e) {
            log.error("滑块验证码图片转 Base64 失败，format={}", format, e);
            return null;
        }
    }

    /**
     * 验证滑块位置
     *
     * @param actualOffset   用户滑动的位置
     * @param expectedOffset 正确位置
     * @param tolerance      容差（像素）
     * @return 是否验证通过
     */
    public static boolean verifyOffset(double actualOffset, double expectedOffset, int tolerance) {
        return Math.abs(actualOffset - expectedOffset) <= tolerance;
    }
}
