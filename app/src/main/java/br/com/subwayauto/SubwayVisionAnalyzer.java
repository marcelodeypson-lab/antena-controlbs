package br.com.subwayauto;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Primeira camada de visão: mede textura, contraste e saturação em três
 * corredores da pista. É um detector heurístico, não um modelo treinado.
 */
public final class SubwayVisionAnalyzer {
    private SubwayVisionAnalyzer() {}

    public static Result analyze(Bitmap bitmap) {
        if (bitmap == null || bitmap.getWidth() < 120 || bitmap.getHeight() < 160) {
            return new Result("Imagem inválida", 0, 0, 0, -1);
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        // Faixa inferior da pista: região onde obstáculos próximos tendem a aparecer.
        int top = (int)(height * 0.56f);
        int bottom = (int)(height * 0.88f);
        float[] scores = new float[3];

        for (int lane = 0; lane < 3; lane++) {
            int left = (int)(width * (lane / 3.0f + 0.08f));
            int right = (int)(width * ((lane + 1) / 3.0f - 0.08f));
            long edge = 0;
            long saturation = 0;
            long contrast = 0;
            int count = 0;
            int previous = -1;

            // Amostragem reduzida para manter o processamento leve no Moto G60s.
            for (int y = top; y < bottom; y += 6) {
                for (int x = left; x < right; x += 6) {
                    int pixel = bitmap.getPixel(x, y);
                    int r = Color.red(pixel);
                    int g = Color.green(pixel);
                    int b = Color.blue(pixel);
                    int luminance = (r * 299 + g * 587 + b * 114) / 1000;
                    int max = Math.max(r, Math.max(g, b));
                    int min = Math.min(r, Math.min(g, b));
                    saturation += max - min;
                    if (previous >= 0) {
                        edge += Math.abs(luminance - previous);
                        contrast += Math.abs(luminance - 110);
                    }
                    previous = luminance;
                    count++;
                }
            }
            if (count > 0) {
                float n = Math.max(1, count - 1);
                float texture = edge / n;
                float color = saturation / (float)count;
                float sceneContrast = contrast / n;
                scores[lane] = texture * 0.55f + color * 0.20f + sceneContrast * 0.25f;
            }
        }

        int safest = 0;
        if (scores[1] < scores[safest]) safest = 1;
        if (scores[2] < scores[safest]) safest = 2;

        String summary = String.format(java.util.Locale.US,
            "Pista visual L%.0f C%.0f R%.0f | menor sinal: %s",
            scores[0], scores[1], scores[2],
            safest == 0 ? "esquerda" : safest == 1 ? "centro" : "direita");
        return new Result(summary, scores[0], scores[1], scores[2], safest);
    }

    public static final class Result {
        public final String summary;
        public final float leftScore;
        public final float centerScore;
        public final float rightScore;
        public final int lowestSignalLane;

        Result(String summary, float left, float center, float right, int lane) {
            this.summary = summary;
            this.leftScore = left;
            this.centerScore = center;
            this.rightScore = right;
            this.lowestSignalLane = lane;
        }
    }
}
