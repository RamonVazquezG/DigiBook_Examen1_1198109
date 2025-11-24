package com.example.digibook_examen1_1198109;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {

    // Clase interna para representar un trazo individual
    public static class Stroke {
        public Path path;
        public Paint paint;
        public int color;
        public int width;

        public Stroke(Path path, int color, int width) {
            this.path = path;
            this.color = color;
            this.width = width;
            this.paint = new Paint();
            this.paint.setAntiAlias(true);
            this.paint.setColor(color);
            this.paint.setStyle(Paint.Style.STROKE);
            this.paint.setStrokeJoin(Paint.Join.ROUND);
            this.paint.setStrokeCap(Paint.Cap.ROUND);
            this.paint.setStrokeWidth(width);
        }
    }

    private List<Stroke> paths = new ArrayList<>();
    private Path currentPath;
    private int currentColor = Color.BLACK;
    private int currentBrushSize = 10;
    private boolean isDrawingEnabled = true;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Redibujar todos los trazos guardados
        for (Stroke stroke : paths) {
            canvas.drawPath(stroke.path, stroke.paint);
        }

        // Dibujar el trazo que se está haciendo actualmente
        if (isDrawingEnabled && currentPath != null) {
            Paint currentPaint = new Paint();
            currentPaint.setAntiAlias(true);
            currentPaint.setColor(currentColor);
            currentPaint.setStyle(Paint.Style.STROKE);
            currentPaint.setStrokeJoin(Paint.Join.ROUND);
            currentPaint.setStrokeCap(Paint.Cap.ROUND);
            currentPaint.setStrokeWidth(currentBrushSize);
            canvas.drawPath(currentPath, currentPaint);
        }
    }

    // --- Métodos de Dibujo ---

    public void startPath(float x, float y) {
        currentPath = new Path();
        currentPath.moveTo(x, y);
        invalidate();
    }

    public void movePath(float x, float y) {
        if (currentPath != null) {
            currentPath.lineTo(x, y);
            invalidate();
        }
    }

    public void endPath() {
        if (currentPath != null) {
            paths.add(new Stroke(currentPath, currentColor, currentBrushSize));
            currentPath = null;
            invalidate();
        }
    }

    // --- Métodos de Manipulación (Mover/Borrar) ---

    /**
     * Busca un trazo que esté cerca de las coordenadas (x, y).
     * Usa los límites (bounds) del Path para una detección rápida.
     */
    public Stroke findStrokeAt(float x, float y) {
        RectF bounds = new RectF();
        // Iteramos al revés para seleccionar el más reciente (el de arriba)
        for (int i = paths.size() - 1; i >= 0; i--) {
            Stroke stroke = paths.get(i);
            stroke.path.computeBounds(bounds, true);

            // Añadimos un margen de tolerancia basado en el grosor del pincel
            float tolerance = Math.max(stroke.width, 40f);
            bounds.inset(-tolerance, -tolerance);

            if (bounds.contains(x, y)) {
                return stroke;
            }
        }
        return null;
    }

    public void deleteStroke(Stroke stroke) {
        paths.remove(stroke);
        invalidate();
    }

    public void moveStroke(Stroke stroke, float dx, float dy) {
        if (stroke != null) {
            stroke.path.offset(dx, dy);
            invalidate();
        }
    }

    // --- Configuración ---

    public void setColor(String colorHex) {
        this.currentColor = Color.parseColor(colorHex);
    }

    public void setBrushSize(int size) {
        this.currentBrushSize = size;
    }

    public void setDrawingEnabled(boolean enabled) {
        this.isDrawingEnabled = enabled;
    }
}