package com.bosonshiggs.focuscomponents;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.runtime.util.YailList;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff;
import android.graphics.Path;
import android.graphics.RectF;

import android.content.Context;

import android.widget.TextView;
import android.widget.FrameLayout;

import android.util.DisplayMetrics;
import android.util.Log;

import android.os.Handler;
import android.os.Build;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;


@DesignerComponent(
    version = 1,
    description = "Extensão para focar em componentes específicos para tutoriais.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "images/extension.png"
)
@SimpleObject(external = true)
public class FocusComponents extends AndroidNonvisibleComponent {
    
    private List<View> allComponents = new ArrayList<>();
    private List<View> focusComponents = new ArrayList<>();
    private Map<View, String> componentTextMap = new HashMap<>();
    private Map<View, float[]> componentCoordinatesMap = new HashMap<>();
    
    private int currentIndex = 0;
    private FocusOverlayView overlay;
    private TextView label;
    private ComponentContainer container;
    private int focusColor = Color.parseColor("#A6000000"); // Cor de desfoque padrão
    private boolean isLastComponent;
    
    private String LOG_NAME = "FocusComponents";
    private boolean flagLog = false;
    
    private float paddingLeft = 15;
    private float paddingTop = 50;
    private float paddingRight = 0;
    private float paddingBottom = -35;

    public FocusComponents(ComponentContainer container) {
        super(container.$form());
        this.container = container;
        
        label = new TextView(container.$context());
        label.setBackgroundColor(Color.TRANSPARENT);
        label.setTextColor(Color.WHITE);
        label.setTextSize(18);

        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, 
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        label.setLayoutParams(labelParams);
        label.setPadding(10, 10, 10, 10);

        overlay = new FocusOverlayView(container.$context());
        FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.MATCH_PARENT
        );

        ViewGroup rootView = (ViewGroup) container.$form().findViewById(android.R.id.content);
        rootView.addView(overlay, overlayParams);
        rootView.addView(label, labelParams);

        overlay.setVisibility(View.GONE);
        label.setVisibility(View.GONE);

        overlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLastComponent) {
                    endPresentation();
                } else {
                    MoveToNextComponent();
                }
            }
        });
    }
    

    public float GetComponentX(View view) {
        int[] location = new int[2];
        try {
            view.getLocationOnScreen(location);
            if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                location[0] += layoutParams.leftMargin;
            }
            return location[0];
        } catch (Exception e) {
            if (flagLog) Log.e(LOG_NAME, "GetComponentX - Error: " + e.getMessage(), e);
            return 0; // Retorna 0 em caso de erro
        }
    }

    public float GetComponentY(View view) {
        int[] location = new int[2];
        try {
            view.getLocationOnScreen(location);
            if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                location[1] += layoutParams.topMargin;
            }
            return location[1];
        } catch (Exception e) {
        	if (flagLog) Log.e(LOG_NAME, "GetComponentY - Error: " + e.getMessage(), e);
            return 0; // Retorna 0 em caso de erro
        }
    }

    @SimpleFunction(description = "Focuses on a list of components with explanatory text.")
    public void FocusOnComponentsWithText(YailList componentsList, YailList textsList) {
        allComponents.clear();
        focusComponents.clear();
        componentTextMap.clear();
        componentCoordinatesMap.clear(); // Limpa o mapa de coordenadas

        final Object[] componentsArray = componentsList.toArray();
        final Object[] textsArray = textsList.toArray();
        
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
        
		        for (int i = 0; i < componentsArray.length; i++) {
		            if (componentsArray[i] instanceof AndroidViewComponent) {
		                View componentView = ((AndroidViewComponent) componentsArray[i]).getView();
		                String text = (i < textsArray.length) ? textsArray[i].toString() : "";
		
		                float x = GetComponentX(componentView);
		                float y = GetComponentY(componentView);
		                if (flagLog) Log.i("FocusComponents", "FocusOnComponentsWithText: (" + x + ", " + y + ")");
		
		                focusComponents.add(componentView);
		                componentTextMap.put(componentView, text);
		                componentCoordinatesMap.put(componentView, new float[]{x, y}); // Armazena as coordenadas
		                registerComponentView(componentView);
		            }
		        }

                currentIndex = 0;
                showFocus();
                
                StartPresentation();
            }
        }, 300); // Atraso de 2000ms

    }

    private void registerComponentView(View view) {
        if (!allComponents.contains(view)) {
            allComponents.add(view);
        }
    }
   
    /*
    private void showFocus() {
        if (currentIndex < focusComponents.size()) {
            View currentComponent = focusComponents.get(currentIndex);
            focusComponent(currentComponent);

            // Obtenha as coordenadas salvas do componente
            float[] coordinates = componentCoordinatesMap.get(currentComponent);
            float x = coordinates[0];
            float y = coordinates[1];
            
            label.setText(componentTextMap.get(currentComponent));
            positionLabelNearComponent(currentComponent);
            label.setVisibility(View.VISIBLE);
            overlay.setFocusRectFromView(currentComponent, x, y); // 20 é o padding ao redor do componente
            overlay.setVisibility(View.VISIBLE);
            isLastComponent = currentIndex == focusComponents.size() - 1;
        }
    }*/
    
    private void showFocus() {
        if (currentIndex < focusComponents.size()) {
            View currentComponent = focusComponents.get(currentIndex);
            focusComponent(currentComponent);

            // Obtenha as coordenadas salvas do componente
            float[] coordinates = componentCoordinatesMap.get(currentComponent);
            float x = coordinates[0];
            float y = coordinates[1];
            
            float diffY = 0;
            
            if (flagLog) Log.i(LOG_NAME, "showFocus - coordenadas salvas: (" + x + ", " + y + ")");
            		
            // Ajusta a posição X e Y com base no componente pai
            View parent = (View) currentComponent.getParent();
            if (parent != null) {
                int[] parentCoordinates = new int[2];
                parent.getLocationOnScreen(parentCoordinates);
                //x -= parentCoordinates[0]; // Ajusta X relativo ao pai
                diffY = Math.max((y - parentCoordinates[1]), 0); // Usa Y do componente pai
                y -= Math.max(diffY, 0);
                if (flagLog) Log.i(LOG_NAME, "showFocus - coordenadas pai: (" + parentCoordinates[0] + ", " + parentCoordinates[1] + ")");
                if (flagLog) Log.i(LOG_NAME, "showFocus - coordenadas relativa: (" + x + ", " + y + ")");
            }

            label.setText(componentTextMap.get(currentComponent));
            positionLabelNearComponent(currentComponent, x, y);
            label.setVisibility(View.VISIBLE);
            overlay.setFocusRectFromView(currentComponent, x, y); // 10 é o padding ao redor do componente
            overlay.setVisibility(View.VISIBLE);
            isLastComponent = currentIndex == focusComponents.size() - 1;
        }
    }

    
    private String getComponentName(View view) {
        // Percorra o mapa de componentes e encontre o nome correspondente à View
        for (Map.Entry<View, String> entry : componentTextMap.entrySet()) {
            if (entry.getKey() == view) {
                return entry.getValue();
            }
        }
        return "Componente não encontrado"; // Retorna uma mensagem de erro se o componente não for encontrado
    }

    private void focusComponent(View view) {
        // Aplicar um efeito visual para destacar o componente
        // Exemplo: Aplicar uma sombra ou contorno
        /*
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setOutlineProvider(ViewOutlineProvider.PADDED_BOUNDS);
            view.setClipToOutline(true);
            view.setElevation(10f);
        }
        */

        // Removido o bringToFront para evitar reposicionar os componentes
        // view.bringToFront();

        // Se necessário, traga o overlay e o label para frente sem alterar a ordem dos outros componentes
        overlay.bringToFront();
        label.bringToFront();
    }


    private void positionLabelNearComponent(View component, float xPos, float yPos) {
        int x = 0; //(int) coordinates[0];
        int y = (int) yPos; //(int) coordinates[1];

        // Obter a altura da tela usando DisplayMetrics
        DisplayMetrics metrics = new DisplayMetrics();
        container.$form().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenHeight = metrics.heightPixels;
        if (flagLog) Log.i(LOG_NAME, "showFocus - screenHeight: " + screenHeight + "px");
        
        // Calcular a posição do texto
        if (y < screenHeight / 2) {
            // Se o buraco estiver acima da metade da tela, coloque o texto abaixo
            label.setY(y + component.getHeight() + 50);
            if (flagLog) Log.i(LOG_NAME, "showFocus - half: y: " + y + " and y + half: " + (y + component.getHeight() + 50));
        } else {
            // Se o buraco estiver abaixo da metade da tela, coloque o texto acima
            label.setY(y - label.getHeight() - 50);
            if (flagLog) Log.i(LOG_NAME, "showFocus - no half: y: " + y + " and y  - half: " + (y - component.getHeight() - 50));
        }

        label.setX(x);
    }

    //@SimpleFunction(description = "Moves to the next component in the tutorial.")
    public void MoveToNextComponent() {
        if (currentIndex < focusComponents.size() - 1) {
            currentIndex++;
            showFocus();
        }
    }

    //@SimpleFunction(description = "Moves to the previous component in the tutorial.")
    public void MoveToPreviousComponent() {
        if (currentIndex > 0) {
            currentIndex--;
            showFocus();
        }
    }

    @SimpleFunction(description = "Sets the focus color.")
    public void SetFocusColor(int argb) {
        focusColor = argb;
    }
    
    @SimpleFunction(description = "Sets the padding of the highlighted area.")
    public void SetPadding(float paddingLeft, float paddingTop, float paddingRight, float paddingBottom) {
    	this.paddingTop = paddingTop;
        this.paddingBottom = paddingBottom;
        this.paddingLeft = paddingLeft;
        this.paddingRight = paddingRight;
    }
    

    private void endPresentation() {
        overlay.setVisibility(View.GONE);
        label.setVisibility(View.GONE);
        unfocusAllComponents();
        EndPresentation();
    }

    private void unfocusAllComponents() {
        for (View view : allComponents) {
            if (!focusComponents.contains(view)) {
                view.getBackground().setColorFilter(focusColor, PorterDuff.Mode.DARKEN);
            }
        }
    }

    private class FocusOverlayView extends View {
        private Rect focusRect;
        private Paint darkPaint;
        private Path darkPath;

        public FocusOverlayView(Context context) {
            super(context);
            focusRect = new Rect();
            darkPaint = new Paint();
            darkPaint.setColor(focusColor);
            darkPaint.setStyle(Paint.Style.FILL);
            darkPath = new Path();
        }
        
        public void setFocusRectFromView(View view, float x, float y) {
            // Calcular a posição real do componente em relação à janela
            focusRect.set(
            		(int)(x - paddingLeft), 
            		(int)(y - paddingTop),
            		(int)(x + view.getWidth() + paddingRight), 
            		(int)(y + view.getHeight() + paddingBottom)
            		);

            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            darkPath.reset();
            darkPath.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);

            // Converter Rect para RectF
            RectF focusRectF = new RectF(focusRect);
            darkPath.addRect(focusRectF, Path.Direction.CCW);

            canvas.drawPath(darkPath, darkPaint);
        }
    }
    
    @SimpleEvent(description = "Report an error with a custom message")
    public void ReportError(String errorMessage) {
        EventDispatcher.dispatchEvent(this, "ReportError", errorMessage);
    }
    
    @SimpleEvent(description = "End of presentation")
    public void EndPresentation() {
        EventDispatcher.dispatchEvent(this, "EndPresentation");
    }
    
    @SimpleEvent(description = "Start of presentation")
    public void StartPresentation() {
        EventDispatcher.dispatchEvent(this, "StartPresentation");
    }
}