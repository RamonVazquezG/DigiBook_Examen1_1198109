package com.example.digibook_examen1_1198109;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {

    // Clase interna para representar un trazo individual
    public static class Stroke {
        public Path path;
        public Paint paint;
        public int color;
        public int width;
        // Lista de puntos para poder reconstruir/guardar el trazo
        public List<PointF> points = new ArrayList<>();

        public Stroke(int color, int width) {
            this.path = new Path();
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
    private Stroke currentStroke;
    private int currentColor = Color.BLACK;
    private int currentBrushSize = 10;
    private boolean isDrawingEnabled = true;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Stroke stroke : paths) {
            canvas.drawPath(stroke.path, stroke.paint);
        }

        if (isDrawingEnabled && currentStroke != null) {
            canvas.drawPath(currentStroke.path, currentStroke.paint);
        }
    }

    public void startPath(float x, float y) {
        currentStroke = new Stroke(currentColor, currentBrushSize);
        currentStroke.path.moveTo(x, y);
        currentStroke.points.add(new PointF(x, y));
        invalidate();
    }

    public void movePath(float x, float y) {
        if (currentStroke != null) {
            currentStroke.path.lineTo(x, y);
            currentStroke.points.add(new PointF(x, y));
            invalidate();
        }
    }

    public void endPath() {
        if (currentStroke != null) {
            paths.add(currentStroke);
            currentStroke = null;
            invalidate();
        }
    }

    public Stroke findStrokeAt(float x, float y) {
        RectF bounds = new RectF();
        for (int i = paths.size() - 1; i >= 0; i--) {
            Stroke stroke = paths.get(i);
            stroke.path.computeBounds(bounds, true);
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
            // Actualizar puntos para coherencia al guardar
            for (PointF p : stroke.points) {
                p.x += dx;
                p.y += dy;
            }
            invalidate();
        }
    }

    public void setColor(String colorHex) {
        this.currentColor = Color.parseColor(colorHex);
    }

    public void setBrushSize(int size) {
        this.currentBrushSize = size;
    }

    public void setDrawingEnabled(boolean enabled) {
        this.isDrawingEnabled = enabled;
    }

    // --- SERIALIZACIÓN (Guardar y Cargar) ---

    // Convertir todos los trazos a un String JSON
    public String serializeDrawing() {
        JSONArray jsonArray = new JSONArray();
        for (Stroke stroke : paths) {
            try {
                JSONObject strokeJson = new JSONObject();
                strokeJson.put("color", stroke.color);
                strokeJson.put("width", stroke.width);

                JSONArray pointsArray = new JSONArray();
                for (PointF point : stroke.points) {
                    JSONObject pJson = new JSONObject();
                    pJson.put("x", point.x);
                    pJson.put("y", point.y);
                    pointsArray.put(pJson);
                }
                strokeJson.put("points", pointsArray);
                jsonArray.put(strokeJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return jsonArray.toString();
    }

    // Reconstruir el dibujo desde un String JSON
    public void deserializeDrawing(String jsonData) {
        paths.clear();
        try {
            JSONArray jsonArray = new JSONArray(jsonData);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject strokeJson = jsonArray.getJSONObject(i);
                int color = strokeJson.getInt("color");
                int width = strokeJson.getInt("width");

                Stroke stroke = new Stroke(color, width);
                JSONArray pointsArray = strokeJson.getJSONArray("points");

                if (pointsArray.length() > 0) {
                    JSONObject firstP = pointsArray.getJSONObject(0);
                    stroke.path.moveTo((float) firstP.getDouble("x"), (float) firstP.getDouble("y"));
                    stroke.points.add(new PointF((float) firstP.getDouble("x"), (float) firstP.getDouble("y")));

                    for (int j = 1; j < pointsArray.length(); j++) {
                        JSONObject p = pointsArray.getJSONObject(j);
                        stroke.path.lineTo((float) p.getDouble("x"), (float) p.getDouble("y"));
                        stroke.points.add(new PointF((float) p.getDouble("x"), (float) p.getDouble("y")));
                    }
                }
                paths.add(stroke);
            }
            invalidate();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}