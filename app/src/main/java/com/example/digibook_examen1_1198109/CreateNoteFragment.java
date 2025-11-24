package com.example.digibook_examen1_1198109;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

import com.example.digibook_examen1_1198109.R;

import java.util.ArrayList;
import java.util.List;

public class CreateNoteFragment extends Fragment {

    private DrawingView drawingView;
    private FrameLayout textContainer;
    private FrameLayout workArea;
    private LinearLayout brushControls, textControls, colorPalette;
    private MaterialButton btnModeDraw, btnModeText, btnModePan, btnModeEraser, btnAddText;

    private EditText activeEditText;

    // Variables para el guardado
    private NoteRepository noteRepository;
    private String currentNoteName = "NotaSinTitulo";

    private static final int MODE_DRAW = 0;
    private static final int MODE_TEXT = 1;
    private static final int MODE_PAN = 2;
    private static final int MODE_ERASER = 3;
    private int currentMode = MODE_DRAW;

    private int currentColor = Color.BLACK;
    private List<View> colorViews = new ArrayList<>();

    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 5.0f;

    private float lastTouchX, lastTouchY;
    private float globalPosX = 0f, globalPosY = 0f;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;

    private DrawingView.Stroke draggingStroke = null;
    private boolean isDraggingItem = false;

    private final String[] colors = {
            "#000000", "#545454", "#9E9E9E", "#F44336",
            "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
            "#2196F3", "#03A9F4", "#00BCD4", "#009688",
            "#4CAF50", "#8BC34A", "#CDDC39", "#FFEB3B",
            "#FFC107", "#FF9800", "#FF5722", "#795548"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_note, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar repositorio
        noteRepository = new NoteRepository(requireContext());

        // Obtener nombre de la nota de los argumentos
        if (getArguments() != null) {
            String nameArg = getArguments().getString("noteName");
            if (nameArg != null && !nameArg.isEmpty()) {
                currentNoteName = nameArg;
            }
        }

        // Actualizar título de la toolbar
        if (getActivity() instanceof AppCompatActivity && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(currentNoteName);
        }

        initViews(view);
        setupTouchLogic();
        setupControls();
        createColorButtons();

        setMode(MODE_DRAW);

        // Cargar datos si existen
        loadNoteData();
    }

    // Guardar al pausar (salir de la app o ir atrás)
    @Override
    public void onPause() {
        super.onPause();
        saveNoteData();
    }

    private void saveNoteData() {
        if (drawingView != null) {
            String jsonData = drawingView.serializeDrawing();
            noteRepository.saveNote(currentNoteName, jsonData);
            // Opcional: Toast.makeText(getContext(), "Nota guardada", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadNoteData() {
        String jsonData = noteRepository.loadNote(currentNoteName);
        if (jsonData != null && !jsonData.isEmpty()) {
            drawingView.deserializeDrawing(jsonData);
        }
    }

    private void initViews(View view) {
        workArea = view.findViewById(R.id.workArea);
        drawingView = view.findViewById(R.id.drawingView);
        textContainer = view.findViewById(R.id.textContainer);

        brushControls = view.findViewById(R.id.brushControls);
        textControls = view.findViewById(R.id.textControls);
        colorPalette = view.findViewById(R.id.colorPalette);

        btnModeDraw = view.findViewById(R.id.btnModeDraw);
        btnModeText = view.findViewById(R.id.btnModeText);
        btnModePan = view.findViewById(R.id.btnModePan);
        btnModeEraser = view.findViewById(R.id.btnModeEraser);
        btnAddText = view.findViewById(R.id.btnAddText);
    }

    private void setupTouchLogic() {
        scaleGestureDetector = new ScaleGestureDetector(requireContext(), new ScaleListener());

        drawingView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);

            float x = event.getX();
            float y = event.getY();
            float rawX = event.getRawX();
            float rawY = event.getRawY();

            final int action = event.getActionMasked();

            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    activePointerId = event.getPointerId(0);
                    lastTouchX = rawX;
                    lastTouchY = rawY;
                    isDraggingItem = false;

                    if (currentMode == MODE_ERASER) {
                        DrawingView.Stroke strokeToDelete = drawingView.findStrokeAt(x, y);
                        if (strokeToDelete != null) {
                            drawingView.deleteStroke(strokeToDelete);
                            return true;
                        }
                    }

                    if (currentMode == MODE_PAN) {
                        draggingStroke = drawingView.findStrokeAt(x, y);
                        if (draggingStroke != null) {
                            isDraggingItem = true;
                        }
                    }

                    if (currentMode == MODE_DRAW && event.getPointerCount() == 1) {
                        drawingView.startPath(x, y);
                        return true;
                    }
                    break;
                }

                case MotionEvent.ACTION_MOVE: {
                    if (activePointerId == MotionEvent.INVALID_POINTER_ID) break;

                    float dx = rawX - lastTouchX;
                    float dy = rawY - lastTouchY;

                    if (currentMode == MODE_DRAW && !scaleGestureDetector.isInProgress() && event.getPointerCount() == 1) {
                        drawingView.movePath(x, y);
                        lastTouchX = rawX;
                        lastTouchY = rawY;
                        return true;
                    }

                    if (currentMode == MODE_PAN && isDraggingItem && draggingStroke != null) {
                        drawingView.moveStroke(draggingStroke, dx / scaleFactor, dy / scaleFactor);
                        lastTouchX = rawX;
                        lastTouchY = rawY;
                        return true;
                    }

                    if ((currentMode == MODE_PAN && !isDraggingItem) || scaleGestureDetector.isInProgress() || event.getPointerCount() > 1) {
                        globalPosX += dx;
                        globalPosY += dy;
                        applyTransformations();
                        lastTouchX = rawX;
                        lastTouchY = rawY;
                        return true;
                    }
                    break;
                }

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (currentMode == MODE_DRAW && !isDraggingItem && event.getPointerCount() == 1) {
                        drawingView.endPath();
                    }

                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                    draggingStroke = null;
                    isDraggingItem = false;
                    break;
                }
            }

            if (currentMode == MODE_ERASER || currentMode == MODE_PAN) return true;

            return false;
        });
    }

    private void setupControls() {
        btnModeDraw.setOnClickListener(v -> setMode(MODE_DRAW));
        btnModeText.setOnClickListener(v -> setMode(MODE_TEXT));
        btnModePan.setOnClickListener(v -> setMode(MODE_PAN));
        btnModeEraser.setOnClickListener(v -> setMode(MODE_ERASER));
        btnAddText.setOnClickListener(v -> addDraggableText());

        Slider sliderBrush = getView().findViewById(R.id.sliderBrushSize);
        Slider sliderText = getView().findViewById(R.id.sliderTextSize);

        sliderBrush.addOnChangeListener((slider, value, fromUser) -> drawingView.setBrushSize((int) value));
        sliderText.addOnChangeListener((slider, value, fromUser) -> {
            if (activeEditText != null) activeEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, value);
        });
    }

    private void applyTransformations() {
        workArea.setScaleX(scaleFactor);
        workArea.setScaleY(scaleFactor);
        workArea.setTranslationX(globalPosX);
        workArea.setTranslationY(globalPosY);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_ZOOM, Math.min(scaleFactor, MAX_ZOOM));
            applyTransformations();
            return true;
        }
    }

    private void setMode(int mode) {
        currentMode = mode;

        drawingView.setDrawingEnabled(mode == MODE_DRAW);

        brushControls.setVisibility(mode == MODE_DRAW ? View.VISIBLE : View.GONE);
        textControls.setVisibility(mode == MODE_TEXT ? View.VISIBLE : View.GONE);
        btnAddText.setVisibility(mode == MODE_TEXT ? View.VISIBLE : View.GONE);

        updateButtonStyle(btnModeDraw, mode == MODE_DRAW);
        updateButtonStyle(btnModeText, mode == MODE_TEXT);
        updateButtonStyle(btnModePan, mode == MODE_PAN);
        updateButtonStyle(btnModeEraser, mode == MODE_ERASER);

        if (mode != MODE_TEXT && activeEditText != null) {
            activeEditText.setBackgroundResource(0);
            activeEditText.clearFocus();
            activeEditText = null;
        }
    }

    private void updateButtonStyle(MaterialButton btn, boolean active) {
        if (active) {
            btn.setStrokeWidth(4);
            btn.setStrokeColor(android.content.res.ColorStateList.valueOf(getResources().getColor(com.google.android.material.R.color.design_default_color_primary)));
        } else {
            btn.setStrokeWidth(0);
        }
    }

    private void addDraggableText() {
        EditText et = new EditText(requireContext());
        et.setText("Texto");
        et.setTextColor(currentColor);
        et.setTextSize(24);
        et.setBackgroundResource(0);
        et.setPadding(16, 16, 16, 16);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;
        et.setLayoutParams(params);

        et.setOnTouchListener((view, event) -> {
            if (currentMode == MODE_ERASER) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    textContainer.removeView(view);
                    if (activeEditText == view) activeEditText = null;
                }
                return true;
            }

            if (currentMode == MODE_DRAW) return false;

            if (currentMode == MODE_PAN || currentMode == MODE_TEXT) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastTouchX = event.getRawX();
                        lastTouchY = event.getRawY();
                        if (currentMode == MODE_TEXT) setActiveText((EditText) view);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - lastTouchX;
                        float dy = event.getRawY() - lastTouchY;

                        view.setX(view.getX() + (dx / scaleFactor));
                        view.setY(view.getY() + (dy / scaleFactor));

                        lastTouchX = event.getRawX();
                        lastTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (currentMode == MODE_TEXT) {
                            view.performClick();
                            view.requestFocus();
                            return false;
                        }
                        return true;
                }
            }
            return false;
        });

        et.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && currentMode == MODE_TEXT) {
                setActiveText((EditText) v);
            } else {
                v.setBackgroundResource(0);
            }
        });

        textContainer.addView(et);
        setActiveText(et);
    }

    private void setActiveText(EditText et) {
        if (activeEditText != null && activeEditText != et) {
            activeEditText.setBackgroundResource(0);
        }
        this.activeEditText = et;
        if (currentMode == MODE_TEXT) {
            et.setBackgroundResource(R.drawable.bg_text_selection);
        }
    }

    private void createColorButtons() {
        colorPalette.removeAllViews();
        colorViews.clear();
        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

        for (String colorHex : colors) {
            int parsedColor = Color.parseColor(colorHex);
            View colorView = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            colorView.setLayoutParams(params);
            colorView.setTag(parsedColor);
            updateColorViewBackground(colorView, parsedColor, parsedColor == currentColor);

            colorView.setOnClickListener(v -> {
                int selectedColor = (int) v.getTag();
                currentColor = selectedColor;
                drawingView.setColor(String.format("#%06X", (0xFFFFFF & selectedColor)));
                if (activeEditText != null) activeEditText.setTextColor(currentColor);
                refreshColorSelection();
            });
            colorViews.add(colorView);
            colorPalette.addView(colorView);
        }
    }

    private void refreshColorSelection() {
        for (View view : colorViews) {
            int color = (int) view.getTag();
            updateColorViewBackground(view, color, color == currentColor);
        }
    }

    private void updateColorViewBackground(View view, int color, boolean isSelected) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(color);
        if (isSelected) {
            int borderColor = (color == Color.WHITE) ? Color.BLACK : Color.DKGRAY;
            shape.setStroke(8, borderColor);
        } else {
            shape.setStroke(2, Color.LTGRAY);
        }
        view.setBackground(shape);
    }
}