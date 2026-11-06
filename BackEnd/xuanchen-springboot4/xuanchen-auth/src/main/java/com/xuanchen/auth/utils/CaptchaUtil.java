package com.xuanchen.auth.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.List;

/**
 * 滑块验证码工具类
 *
 * @author XuanChen
 * @date 2026-06-01
 */
public class CaptchaUtil {

    private static final Logger log = LoggerFactory.getLogger(CaptchaUtil.class);

    private static final int SLIDER_WIDTH = 50;
    private static final int SLIDER_HEIGHT = 50;
    private static final int IMAGE_WIDTH = 280;
    private static final int IMAGE_HEIGHT = 150;
    private static final int GAP_MARGIN = 10;

    /**
     * 13 种缺口形状，每次随机抽 3 个（1 真 2 假）
     */
    private static final String[] GAP_SHAPES = {
            "circle", "square", "rectangle", "hexagon", "parallelogram",
            "trapezoid", "triangle", "ellipse", "pentagon", "octagon",
            "diamond", "sector", "semicircle"
    };

    public static Map<String, Object> generateCaptcha() {
        return generateCaptcha("dynamic");
    }

    /**
     * 生成验证码图片
     *
     * @param captchaType 背景图类型：dynamic=动态生成，static=预置图片
     * @return Map 包含:
     * - bgImage: 带缺口的背景图 Base64
     * - sliderImage: 滑块图 Base64
     * - offsetX / offsetY: 真实缺口位置（只用于存储校验，不返回给前端）
     */
    public static Map<String, Object> generateCaptcha(String captchaType) {
        Map<String, Object> result = new HashMap<>();

        // 1. 创建背景图
        BufferedImage bgImage = "static".equals(captchaType)
                ? loadStaticBackgroundImage()
                : createBackgroundImage();

        // 2. 随机抽取 3 种不重复的形状
        String[] picked = pickRandomShapes();

        // 3. 随机选一个作为真实缺口（其余两个为干扰）
        int realIndex = (int) (Math.random() * 3);

        // 4. 计算 3 个不重叠的位置（分区放置）
        int[][] positions = generateGapPositions();

        // 5. 先从干净背景图中截取真实缺口区域的形状滑块（避免缺口填充污染滑块内容）
        Shape realShape = createShape(picked[realIndex], positions[realIndex][0], positions[realIndex][1]);
        BufferedImage sliderImage = createSliderImage(bgImage, realShape,
                positions[realIndex][0], positions[realIndex][1]);

        // 6. 绘制 2 个假缺口 + 真实缺口
        for (int i = 0; i < 3; i++) {
            if (i == realIndex) continue;
            Shape fakeShape = createShape(picked[i], positions[i][0], positions[i][1]);
            drawFakeGap(bgImage, fakeShape);
        }
        drawRealGap(bgImage, realShape);

        // 7. 转为 Base64
        result.put("bgImage", imageToBase64(bgImage, "png"));
        result.put("sliderImage", imageToBase64(sliderImage, "png"));
        result.put("offsetX", positions[realIndex][0]);
        result.put("offsetY", positions[realIndex][1]);
        result.put("imageWidth", IMAGE_WIDTH);
        result.put("imageHeight", IMAGE_HEIGHT);
        result.put("sliderWidth", SLIDER_WIDTH);
        result.put("sliderHeight", SLIDER_HEIGHT);

        return result;
    }

    /**
     * 从 13 种形状中随机抽取 3 个不重复的形状
     */
    private static String[] pickRandomShapes() {
        List<String> pool = new ArrayList<>(Arrays.asList(GAP_SHAPES));
        Collections.shuffle(pool);
        return new String[]{pool.get(0), pool.get(1), pool.get(2)};
    }

    /**
     * 将图片水平分为 3 个区域，每区随机取一个 X，确保缺口不重叠
     */
    private static int[][] generateGapPositions() {
        int zoneWidth = (IMAGE_WIDTH - 2 * GAP_MARGIN) / 3;
        int maxY = IMAGE_HEIGHT - SLIDER_HEIGHT - GAP_MARGIN;
        int[][] positions = new int[3][2];
        for (int i = 0; i < 3; i++) {
            int zoneStart = GAP_MARGIN + i * zoneWidth;
            int maxXInZone = zoneStart + zoneWidth - SLIDER_WIDTH;
            positions[i][0] = zoneStart + (int) (Math.random() * Math.max(1, maxXInZone - zoneStart));
            positions[i][1] = GAP_MARGIN + (int) (Math.random() * Math.max(1, maxY - GAP_MARGIN));
        }
        return positions;
    }

    // ==================== 形状创建 ====================

    /**
     * 按类型和位置创建几何形状（返回的 Shape 包围盒左上角为 (x, y)，宽高约 SLIDER_WIDTH × SLIDER_HEIGHT）
     */
    private static Shape createShape(String type, int x, int y) {
        int w = SLIDER_WIDTH;
        int h = SLIDER_HEIGHT;
        switch (type) {
            case "circle":
                return new Ellipse2D.Double(x, y, w, h);
            case "square":
                int side = Math.min(w, h);
                return new Rectangle(x, y, side, side);
            case "rectangle":
                return new Rectangle(x, y, w, h);
            case "hexagon": {
                GeneralPath p = new GeneralPath();
                double cx = x + w / 2.0, cy = y + h / 2.0, r = Math.min(w, h) / 2.0;
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(60.0 * i - 30);
                    double px = cx + r * Math.cos(angle);
                    double py = cy + r * Math.sin(angle);
                    if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
                }
                p.closePath();
                return p;
            }
            case "parallelogram": {
                GeneralPath p = new GeneralPath();
                int skew = 12;
                p.moveTo(x + skew, y);
                p.lineTo(x + w, y);
                p.lineTo(x + w - skew, y + h);
                p.lineTo(x, y + h);
                p.closePath();
                return p;
            }
            case "trapezoid": {
                GeneralPath p = new GeneralPath();
                int inset = 10;
                p.moveTo(x + inset, y);
                p.lineTo(x + w - inset, y);
                p.lineTo(x + w, y + h);
                p.lineTo(x, y + h);
                p.closePath();
                return p;
            }
            case "triangle": {
                GeneralPath p = new GeneralPath();
                p.moveTo(x + w / 2.0, y);
                p.lineTo(x + w, y + h);
                p.lineTo(x, y + h);
                p.closePath();
                return p;
            }
            case "ellipse":
                return new Ellipse2D.Double(x, y, w, h);
            case "pentagon": {
                GeneralPath p = new GeneralPath();
                double cx = x + w / 2.0, cy = y + h / 2.0, r = Math.min(w, h) / 2.0;
                for (int i = 0; i < 5; i++) {
                    double angle = Math.toRadians(72.0 * i - 90);
                    double px = cx + r * Math.cos(angle);
                    double py = cy + r * Math.sin(angle);
                    if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
                }
                p.closePath();
                return p;
            }
            case "octagon": {
                GeneralPath p = new GeneralPath();
                double cx = x + w / 2.0, cy = y + h / 2.0, r = Math.min(w, h) / 2.0;
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(45.0 * i - 22.5);
                    double px = cx + r * Math.cos(angle);
                    double py = cy + r * Math.sin(angle);
                    if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
                }
                p.closePath();
                return p;
            }
            case "diamond": {
                GeneralPath p = new GeneralPath();
                p.moveTo(x + w / 2.0, y);
                p.lineTo(x + w, y + h / 2.0);
                p.lineTo(x + w / 2.0, y + h);
                p.lineTo(x, y + h / 2.0);
                p.closePath();
                return p;
            }
            case "sector":
                return new Arc2D.Double(x, y, w, h, 0, 90, Arc2D.PIE);
            case "semicircle":
                return new Arc2D.Double(x, y, w, h, 0, 180, Arc2D.PIE);
            default:
                return new Rectangle(x, y, w, h);
        }
    }

    // ==================== 滑块 & 缺口绘制 ====================

    /**
     * 从背景图中截取滑块区域，使用形状裁剪（setClip + 坐标平移）
     */
    private static BufferedImage createSliderImage(BufferedImage bgImage, Shape shape, int offsetX, int offsetY) {
        BufferedImage slider = new BufferedImage(SLIDER_WIDTH, SLIDER_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = slider.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 将形状平移到本地坐标系 (0,0)
        AffineTransform tx = AffineTransform.getTranslateInstance(-offsetX, -offsetY);
        Shape localShape = tx.createTransformedShape(shape);

        // 裁剪到形状区域，绘制背景图对应部分
        g.setClip(localShape);
        g.drawImage(bgImage, -offsetX, -offsetY, null);

        g.dispose();
        return slider;
    }

    /**
     * 绘制真实缺口：仅深色填充，无边框
     */
    private static void drawRealGap(BufferedImage bgImage, Shape shape) {
        Graphics2D g = bgImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0, 0, 0, 180));
        g.fill(shape);
        g.dispose();
    }

    /**
     * 绘制假缺口：样式与真实缺口一致，仅靠形状区分
     */
    private static void drawFakeGap(BufferedImage bgImage, Shape shape) {
        Graphics2D g = bgImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0, 0, 0, 180));
        g.fill(shape);
        g.dispose();
    }

    // ==================== 背景图 ====================

    private static BufferedImage createBackgroundImage() {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        GradientPaint gradient = new GradientPaint(0, 0,
                new Color((int) (Math.random() * 256), (int) (Math.random() * 256), (int) (Math.random() * 256)),
                IMAGE_WIDTH, IMAGE_HEIGHT,
                new Color((int) (Math.random() * 256), (int) (Math.random() * 256), (int) (Math.random() * 256)));
        g.setPaint(gradient);
        g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        for (int i = 0; i < 10; i++) {
            g.setColor(new Color((int) (Math.random() * 256), (int) (Math.random() * 256), (int) (Math.random() * 256), 180));
            g.setStroke(new BasicStroke((float) (1 + Math.random() * 3)));
            int x1 = (int) (Math.random() * IMAGE_WIDTH);
            int y1 = (int) (Math.random() * IMAGE_HEIGHT);
            int x2 = (int) (Math.random() * IMAGE_WIDTH);
            int y2 = (int) (Math.random() * IMAGE_HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        for (int i = 0; i < 30; i++) {
            g.setColor(new Color((int) (Math.random() * 256), (int) (Math.random() * 256), (int) (Math.random() * 256)));
            g.fillOval((int) (Math.random() * IMAGE_WIDTH), (int) (Math.random() * IMAGE_HEIGHT), 2, 2);
        }

        g.setFont(new Font("Arial", Font.BOLD, 16 + (int) (Math.random() * 10)));
        for (int i = 0; i < 9; i++) {
            g.setColor(new Color((int) (Math.random() * 256), (int) (Math.random() * 256), (int) (Math.random() * 256), 120));
            char ch = (char) ('A' + (int) (Math.random() * 26));
            int xPos = (int) (Math.random() * (IMAGE_WIDTH - 20));
            int yPos = (int) (Math.random() * (IMAGE_HEIGHT - 20)) + 20;
            g.drawString(String.valueOf(ch), xPos, yPos);
        }

        g.dispose();
        return image;
    }

    private static BufferedImage loadStaticBackgroundImage() {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = cl.getResources("captcha/");
            List<URL> imageUrls = new ArrayList<>();
            while (resources.hasMoreElements()) {
                URL dirUrl = resources.nextElement();
                if ("file".equals(dirUrl.getProtocol())) {
                    java.io.File dir = new java.io.File(dirUrl.toURI());
                    if (dir.isDirectory()) {
                        java.io.File[] files = dir.listFiles((d, name) -> {
                            String lower = name.toLowerCase();
                            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
                        });
                        if (files != null) {
                            for (java.io.File f : files) {
                                imageUrls.add(f.toURI().toURL());
                            }
                        }
                    }
                }
            }
            if (imageUrls.isEmpty()) {
                log.warn("captcha/ 目录无预置图片，降级为动态生成");
                return createBackgroundImage();
            }
            URL picked = imageUrls.get((int) (Math.random() * imageUrls.size()));
            try (InputStream is = picked.openStream()) {
                BufferedImage original = ImageIO.read(is);
                if (original == null) {
                    log.warn("无法解析图片: {}，降级为动态生成", picked);
                    return createBackgroundImage();
                }
                BufferedImage scaled = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = scaled.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(original, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, null);
                g.dispose();
                return scaled;
            }
        } catch (Exception e) {
            log.warn("加载预置验证码背景失败，降级为动态生成", e);
            return createBackgroundImage();
        }
    }

    // ==================== 工具方法 ====================

    private static String imageToBase64(BufferedImage image, String format) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            return "data:image/" + format + ";base64," + base64;
        } catch (IOException e) {
            log.error("滑块验证码图片转 Base64 失败，format={}", format, e);
            return null;
        }
    }

    public static boolean verifyOffset(double actualOffset, double expectedOffset, int tolerance) {
        return Math.abs(actualOffset - expectedOffset) <= tolerance;
    }
}
