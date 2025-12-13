package com.tanxe.widgets;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class ColorPickerDialog extends Dialog {

    private int selectedColor;
    private ColorPickerDialogListener dialogListener;

    public interface ColorPickerDialogListener {
        void onColorSelected(int color);
    }

    public ColorPickerDialog(Context context, int initialColor, ColorPickerDialogListener listener) {
        super(context);
        this.selectedColor = initialColor;
        this.dialogListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_color_picker);

        TextView colorPreviewTextView = findViewById(R.id.colorPreviewTextView);
        SeekBar redSeekBar = findViewById(R.id.redSeekBar);
        SeekBar greenSeekBar = findViewById(R.id.greenSeekBar);
        SeekBar blueSeekBar = findViewById(R.id.blueSeekBar);
        Button cancelButton = findViewById(R.id.cancelButton);
        Button selectButton = findViewById(R.id.selectButton);

        colorPreviewTextView.setBackgroundColor(selectedColor);
        redSeekBar.setProgress(Color.red(selectedColor));
        greenSeekBar.setProgress(Color.green(selectedColor));
        blueSeekBar.setProgress(Color.blue(selectedColor));

        redSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedColor = Color.rgb(progress, greenSeekBar.getProgress(), blueSeekBar.getProgress());
                colorPreviewTextView.setBackgroundColor(selectedColor);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        greenSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedColor = Color.rgb(redSeekBar.getProgress(), progress, blueSeekBar.getProgress());
                colorPreviewTextView.setBackgroundColor(selectedColor);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        blueSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedColor = Color.rgb(redSeekBar.getProgress(), greenSeekBar.getProgress(), progress);
                colorPreviewTextView.setBackgroundColor(selectedColor);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogListener.onColorSelected(selectedColor);
                dismiss();
            }
        });
    }
}
