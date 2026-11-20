package com.samp.mobile.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

final class PcInputConfig {
    static final int MOVE_FORWARD = 0;
    static final int MOVE_BACKWARD = 1;
    static final int MOVE_LEFT = 2;
    static final int MOVE_RIGHT = 3;
    static final int SPRINT = 4;
    static final int JUMP = 5;
    static final int CROUCH = 6;
    static final int ATTACK = 7;
    static final int AIM = 8;
    static final int ENTER_EXIT_VEHICLE = 9;
    static final int HORN = 10;
    static final int YES = 11;
    static final int NO = 12;
    static final int SCOREBOARD = 13;
    static final int WALK = 14;
    static final int WEAPON_PREVIOUS = 15;
    static final int WEAPON_NEXT = 16;
    static final int CHAT = 17;
    static final int ACCEPT = 18;
    static final int TOGGLE_MOUSE = 19;
    static final int ACTION_COUNT = 20;

    interface Listener {
        void onSettingsChanged(boolean enabled, int[] bindings);
        void onMouseSettingsChanged(float sensitivityX, float sensitivityY,
                                    boolean invertX, boolean invertY);
        void onVisibilityChanged(boolean visible);
    }

    private static final String[] ACTION_NAMES = {
            "Mover para frente",
            "Mover para trás",
            "Mover para esquerda",
            "Mover para direita",
            "Correr",
            "Pular",
            "Agachar",
            "Atacar / disparar (tecla; padrão: botão esquerdo)",
            "Mirar (tecla; padrão: botão direito)",
            "Entrar / sair do veículo",
            "Buzina / ação H",
            "Sim / Y",
            "Não / N",
            "Placar / Tab",
            "Andar / Alt",
            "Arma anterior (tecla; padrão: roda)",
            "Próxima arma (tecla; padrão: roda)",
            "Abrir chat",
            "Confirmar / Enter",
            "Alternar câmera / cursor / Esc"
    };

    private static final int[] DEFAULT_BINDINGS = {
            KeyEvent.KEYCODE_W,
            KeyEvent.KEYCODE_S,
            KeyEvent.KEYCODE_A,
            KeyEvent.KEYCODE_D,
            KeyEvent.KEYCODE_SHIFT_LEFT,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_C,
            KeyEvent.KEYCODE_UNKNOWN,
            KeyEvent.KEYCODE_UNKNOWN,
            KeyEvent.KEYCODE_F,
            KeyEvent.KEYCODE_H,
            KeyEvent.KEYCODE_Y,
            KeyEvent.KEYCODE_N,
            KeyEvent.KEYCODE_TAB,
            KeyEvent.KEYCODE_ALT_LEFT,
            KeyEvent.KEYCODE_UNKNOWN,
            KeyEvent.KEYCODE_UNKNOWN,
            KeyEvent.KEYCODE_T,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_ESCAPE
    };

    private final Activity activity;
    private final Listener listener;
    private final SharedPreferences preferences;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int[] bindings = DEFAULT_BINDINGS.clone();
    private final Set<Integer> receivedKeys = new HashSet<>();
    private final TextView[] bindingViews = new TextView[ACTION_COUNT];
    private final Runnable stateRefresh = new Runnable() {
        @Override
        public void run() {
            updateInputState();
            if (dialog != null && dialog.isShowing()) {
                handler.postDelayed(this, 100);
            }
        }
    };

    private AlertDialog dialog;
    private TextView stateView;
    private Switch enabledSwitch;
    private CheckBox invertXBox;
    private CheckBox invertYBox;
    private SeekBar sensitivityXBar;
    private SeekBar sensitivityYBar;
    private TextView sensitivityXLabel;
    private TextView sensitivityYLabel;
    private int captureAction = -1;
    private int mouseButtons;
    private int lastKeyCode = KeyEvent.KEYCODE_UNKNOWN;
    private boolean enabled = true;
    private float sensitivityX = 1.0f;
    private float sensitivityY = 1.0f;
    private boolean invertX;
    private boolean invertY;

    PcInputConfig(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        this.preferences = activity.getSharedPreferences("pc_input", Activity.MODE_PRIVATE);
        load();
    }

    boolean isEnabled() {
        return enabled;
    }

    int getBinding(int action) {
        return action >= 0 && action < ACTION_COUNT ? bindings[action] : KeyEvent.KEYCODE_UNKNOWN;
    }

    float getSensitivityX() {
        return sensitivityX;
    }

    float getSensitivityY() {
        return sensitivityY;
    }

    boolean isInvertX() {
        return invertX;
    }

    boolean isInvertY() {
        return invertY;
    }

    boolean isVisible() {
        return dialog != null && dialog.isShowing();
    }

    void dismiss() {
        if (isVisible()) {
            dialog.dismiss();
        }
    }

    void show() {
        if (isVisible()) {
            dialog.getWindow().getDecorView().bringToFront();
            return;
        }

        ScrollView scrollView = new ScrollView(activity);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(18);
        content.setPadding(padding, padding, padding, padding);
        scrollView.addView(content);

        enabledSwitch = new Switch(activity);
        enabledSwitch.setText("Ativar teclado e mouse");
        enabledSwitch.setChecked(enabled);
        monitorMouse(enabledSwitch);
        content.addView(enabledSwitch);
        enabledSwitch.setOnCheckedChangeListener((button, checked) -> {
            enabled = checked;
            notifySettingsChanged();
        });

        addSectionTitle(content, "Sensibilidade do mouse");
        sensitivityXLabel = addSensitivityControl(content, "Eixo X", true);
        sensitivityYLabel = addSensitivityControl(content, "Eixo Y", false);

        invertXBox = new CheckBox(activity);
        invertXBox.setText("Inverter eixo X");
        invertXBox.setChecked(invertX);
        monitorMouse(invertXBox);
        content.addView(invertXBox);
        invertXBox.setOnCheckedChangeListener((button, checked) -> {
            invertX = checked;
            notifyMouseSettingsChanged();
        });

        invertYBox = new CheckBox(activity);
        invertYBox.setText("Inverter eixo Y");
        invertYBox.setChecked(invertY);
        monitorMouse(invertYBox);
        content.addView(invertYBox);
        invertYBox.setOnCheckedChangeListener((button, checked) -> {
            invertY = checked;
            notifyMouseSettingsChanged();
        });

        addSectionTitle(content, "Mapeamento (toque em um controle e pressione uma tecla)");
        for (int action = 0; action < ACTION_COUNT; action++) {
            final int actionId = action;
            TextView row = new TextView(activity);
            row.setTextColor(Color.WHITE);
            row.setTextSize(15);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(12), dp(8), dp(12));
            row.setBackgroundColor(0x33222222);
            monitorMouse(row);
            row.setOnClickListener(view -> {
                captureAction = actionId;
                updateBindingLabels();
                stateView.setText("Pressione uma tecla para: " + ACTION_NAMES[actionId]);
            });
            bindingViews[action] = row;
            content.addView(row);
        }

        addSectionTitle(content, "Estado recebido");
        stateView = new TextView(activity);
        stateView.setTextColor(Color.WHITE);
        stateView.setTextSize(14);
        content.addView(stateView);
        updateBindingLabels();
        updateInputState();

        dialog = new AlertDialog.Builder(activity)
                .setTitle("Teclado e mouse")
                .setView(scrollView)
                .setPositiveButton("Fechar", null)
                .setNegativeButton("Restaurar padrões", null)
                .setNeutralButton("Salvar", null)
                .create();
        dialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                return onKeyDown(keyCode);
            }
            if (event.getAction() == KeyEvent.ACTION_UP) {
                onKeyUp(keyCode);
            }
            return false;
        });
        dialog.setOnShowListener(ignored -> {
            Window window = dialog.getWindow();
            if (window != null) {
                View decor = window.getDecorView();
                decor.setOnTouchListener((view, event) -> {
                    if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
                        setMouseButtons(event.getButtonState());
                    }
                    return false;
                });
                decor.setOnGenericMotionListener((view, event) -> {
                    if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
                        setMouseButtons(event.getButtonState());
                    }
                    return false;
                });
            }
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(view -> restoreDefaults());
            dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(view -> save());
            handler.post(stateRefresh);
        });
        dialog.setOnDismissListener(ignored -> {
            handler.removeCallbacks(stateRefresh);
            captureAction = -1;
            receivedKeys.clear();
            listener.onVisibilityChanged(false);
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    activity.getResources().getDisplayMetrics().widthPixels * 3 / 4,
                    activity.getResources().getDisplayMetrics().heightPixels * 4 / 5);
        }
        listener.onVisibilityChanged(true);
    }

    boolean onKeyDown(int keyCode) {
        receivedKeys.add(keyCode);
        lastKeyCode = keyCode;
        if (captureAction < 0) {
            updateInputState();
            return false;
        }
        bindings[captureAction] = keyCode;
        captureAction = -1;
        notifySettingsChanged();
        updateBindingLabels();
        updateInputState();
        return true;
    }

    void onKeyUp(int keyCode) {
        receivedKeys.remove(keyCode);
        updateInputState();
    }

    void setMouseButtons(int buttons) {
        mouseButtons = buttons;
        updateInputState();
    }

    void noteMouseMotion() {
        lastKeyCode = KeyEvent.KEYCODE_UNKNOWN;
        updateInputState();
    }

    private TextView addSensitivityControl(LinearLayout parent, String label, boolean isX) {
        TextView value = new TextView(activity);
        value.setTextColor(Color.WHITE);
        parent.addView(value);
        SeekBar bar = new SeekBar(activity);
        bar.setMax(275);
        float currentSensitivity = isX ? sensitivityX : sensitivityY;
        bar.setProgress(Math.round((currentSensitivity - 0.25f) * 100.0f));
        parent.addView(bar);
        monitorMouse(bar);
        if (isX) {
            sensitivityXBar = bar;
        } else {
            sensitivityYBar = bar;
        }
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float sensitivity = 0.25f + progress / 100.0f;
                value.setText(label + ": " + String.format(java.util.Locale.US, "%.2f", sensitivity));
                if (isX) {
                    sensitivityX = sensitivity;
                } else {
                    sensitivityY = sensitivity;
                }
                notifyMouseSettingsChanged();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        value.setText(label + ": " + String.format(java.util.Locale.US, "%.2f", currentSensitivity));
        return value;
    }

    private void addSectionTitle(LinearLayout parent, String title) {
        TextView view = new TextView(activity);
        view.setText(title);
        view.setTextColor(0xFFFFCC66);
        view.setTextSize(16);
        view.setPadding(0, dp(16), 0, dp(6));
        parent.addView(view);
    }

    private void monitorMouse(View view) {
        view.setOnTouchListener((target, event) -> {
            if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
                setMouseButtons(event.getButtonState());
            }
            return false;
        });
        view.setOnGenericMotionListener((target, event) -> {
            if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
                setMouseButtons(event.getButtonState());
                if (event.getActionMasked() == MotionEvent.ACTION_HOVER_MOVE) {
                    noteMouseMotion();
                }
            }
            return false;
        });
    }

    private void load() {
        enabled = preferences.getBoolean("enabled", true);
        for (int i = 0; i < ACTION_COUNT; i++) {
            bindings[i] = preferences.getInt("binding_" + i, DEFAULT_BINDINGS[i]);
            if (bindings[i] < 0 || bindings[i] >= 512) {
                bindings[i] = DEFAULT_BINDINGS[i];
            }
        }
        sensitivityX = preferences.getFloat("sensitivity_x", 1.0f);
        sensitivityY = preferences.getFloat("sensitivity_y", 1.0f);
        sensitivityX = Math.max(0.25f, Math.min(3.0f, sensitivityX));
        sensitivityY = Math.max(0.25f, Math.min(3.0f, sensitivityY));
        invertX = preferences.getBoolean("invert_x", false);
        invertY = preferences.getBoolean("invert_y", false);
    }

    private void save() {
        SharedPreferences.Editor editor = preferences.edit()
                .putBoolean("enabled", enabled)
                .putFloat("sensitivity_x", sensitivityX)
                .putFloat("sensitivity_y", sensitivityY)
                .putBoolean("invert_x", invertX)
                .putBoolean("invert_y", invertY);
        for (int i = 0; i < ACTION_COUNT; i++) {
            editor.putInt("binding_" + i, bindings[i]);
        }
        editor.apply();
        Toast.makeText(activity, "Configuração salva", Toast.LENGTH_SHORT).show();
    }

    private void restoreDefaults() {
        enabled = true;
        System.arraycopy(DEFAULT_BINDINGS, 0, bindings, 0, ACTION_COUNT);
        sensitivityX = 1.0f;
        sensitivityY = 1.0f;
        invertX = false;
        invertY = false;
        enabledSwitch.setChecked(enabled);
        sensitivityXBar.setProgress(75);
        sensitivityYBar.setProgress(75);
        invertXBox.setChecked(false);
        invertYBox.setChecked(false);
        notifySettingsChanged();
        updateBindingLabels();
        save();
    }

    private void updateBindingLabels() {
        for (int i = 0; i < ACTION_COUNT; i++) {
            TextView view = bindingViews[i];
            if (view == null) {
                continue;
            }
            String key = bindings[i] == KeyEvent.KEYCODE_UNKNOWN
                    ? "Nenhuma" : keyName(bindings[i]);
            if (captureAction == i) {
                view.setText(ACTION_NAMES[i] + ": pressione uma tecla...");
            } else {
                view.setText(ACTION_NAMES[i] + ": " + key);
            }
        }
    }

    private void updateInputState() {
        if (stateView == null) {
            return;
        }
        StringBuilder text = new StringBuilder("Ativo: ").append(enabled ? "sim" : "não");
        text.append("\nTeclas pressionadas: ");
        if (receivedKeys.isEmpty()) {
            text.append("nenhuma");
        } else {
            boolean first = true;
            for (Integer keyCode : receivedKeys) {
                if (!first) {
                    text.append(", ");
                }
                text.append(keyName(keyCode));
                first = false;
            }
        }
        text.append("\nMouse: ");
        text.append((mouseButtons & MotionEvent.BUTTON_PRIMARY) != 0 ? "[esquerdo] " : "");
        text.append((mouseButtons & MotionEvent.BUTTON_SECONDARY) != 0 ? "[direito] " : "");
        text.append((mouseButtons & MotionEvent.BUTTON_TERTIARY) != 0 ? "[central] " : "");
        if (mouseButtons == 0) {
            text.append("nenhum botão");
        }
        text.append("\nÚltima entrada: ");
        text.append(lastKeyCode == KeyEvent.KEYCODE_UNKNOWN ? "movimento/clique do mouse" : keyName(lastKeyCode));
        stateView.setText(text);
    }

    private void notifySettingsChanged() {
        listener.onSettingsChanged(enabled, bindings.clone());
    }

    private void notifyMouseSettingsChanged() {
        listener.onMouseSettingsChanged(sensitivityX, sensitivityY, invertX, invertY);
    }

    private String keyName(int keyCode) {
        String name = KeyEvent.keyCodeToString(keyCode);
        return name.startsWith("KEYCODE_") ? name.substring("KEYCODE_".length()) : name;
    }

    private int dp(int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + 0.5f);
    }
}
