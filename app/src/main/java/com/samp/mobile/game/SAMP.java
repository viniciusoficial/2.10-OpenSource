package com.samp.mobile.game;

import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.joom.paranoid.Obfuscate;
import com.samp.mobile.game.ui.AttachEdit;
import com.samp.mobile.game.ui.CustomKeyboard;
import com.samp.mobile.game.ui.LoadingScreen;
import com.samp.mobile.game.ui.dialog.DialogManager;
import com.samp.mobile.launcher.util.SignatureChecker;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
@Obfuscate
public class SAMP extends GTASA implements CustomKeyboard.InputListener, HeightProvider.HeightListener {
    private static final String TAG = "SAMP";
    private static SAMP instance;

    private CustomKeyboard mKeyboard;
    private DialogManager mDialog;
    private HeightProvider mHeightProvider;

    private AttachEdit mAttachEdit;
    private LoadingScreen mLoadingScreen;
    private PcInputConfig mPcInputConfig;
    private final Set<Integer> mPressedKeys = new HashSet<>();
    private boolean mMouseLookTouchStarted;
    private boolean mCameraModeRequested = true;
    private boolean mNativeInterfaceVisible;
    private float mMouseLookX;
    private float mMouseLookY;
    private int mMouseButtons;

    public native void sendDialogResponse(int i, int i2, int i3, byte[] str);

    public static SAMP getInstance() {
        return instance;
    }

    private void showTab()
    {
        setNativeInterfaceVisible(true);
    }

    private void hideTab()
    {
        setNativeInterfaceVisible(false);
    }

    private void setTab(int id, String name, int score, int ping)
    {

    }

    private void clearTab()
    {

    }

    private void showLoadingScreen()
    {

    }

    private void hideLoadingScreen()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoadingScreen.hide();
            }
        });
    }

    public void setPauseState(boolean pause) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (pause) {
                    setGameInterfaceVisible(true);
                    mDialog.hideWithoutReset();
                    mAttachEdit.hideWithoutReset();
                }
                else {
                    if(mDialog.isShow)
                        mDialog.showWithOldContent();
                    if(mAttachEdit.isShow)
                        mAttachEdit.showWithoutReset();
                    setGameInterfaceVisible(false);
                }
            }
        });
    }

    public void exitGame(){
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false);

        finishAndRemoveTask();
        System.exit(0);
    }

    public void showDialog(int dialogId, int dialogTypeId, byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4) {
        final String caption = new String(bArr, StandardCharsets.UTF_8);
        final String content = new String(bArr2, StandardCharsets.UTF_8);
        final String leftBtnText = new String(bArr3, StandardCharsets.UTF_8);
        final String rightBtnText = new String(bArr4, StandardCharsets.UTF_8);
        runOnUiThread(() -> {
            setGameInterfaceVisible(true);
            this.mDialog.show(dialogId, dialogTypeId, caption, content, leftBtnText, rightBtnText);
        });
    }

    private native void onInputEnd(byte[] str);
    @Override
    public void OnInputEnd(String str)
    {
        byte[] toReturn = null;
        try
        {
            toReturn = str.getBytes("windows-1251");
        }
        catch(UnsupportedEncodingException e)
        {

        }

        try {
            onInputEnd(toReturn);
        }
        catch (UnsatisfiedLinkError e5) {
            Log.e(TAG, e5.getMessage());
        }
    }

    private void showKeyboard()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("AXL", "showKeyboard()");
                mKeyboard.ShowInputLayout();
                setGameInterfaceVisible(true);
            }
        });
    }

    private void hideKeyboard()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mKeyboard.HideInputLayout();
                setGameInterfaceVisible(false);
            }
        });
    }

    private void showEditObject()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAttachEdit.show();
                setGameInterfaceVisible(true);
            }
        });
    }

    private void hideEditObject()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAttachEdit.hide();
                setGameInterfaceVisible(false);
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "**** onCreate");
        super.onCreate(savedInstanceState);

        if(!SignatureChecker.isSignatureValid(this, getPackageName()))
        {
            Toast.makeText(this, "Use original launcher! No remake", Toast.LENGTH_LONG).show();
            return;
        }

        //mHeightProvider = new HeightProvider(this);

        mKeyboard = new CustomKeyboard(this);

        mDialog = new DialogManager(this);

        mAttachEdit = new AttachEdit(this);

        mLoadingScreen = new LoadingScreen(this);

        instance = this;
        mPcInputConfig = new PcInputConfig(this, new PcInputConfig.Listener() {
            @Override
            public void onSettingsChanged(boolean enabled, int[] bindings) {
                for (int action = 0; action < bindings.length; action++) {
                    setPCInputBinding(action, bindings[action]);
                }
                clearHardwareKeyboardState();
                setPCInputEnabled(enabled);
                if (!enabled) {
                    mCameraModeRequested = false;
                } else {
                    mCameraModeRequested = true;
                }
                updatePointerCapture();
            }

            @Override
            public void onMouseSettingsChanged(float sensitivityX, float sensitivityY,
                                               boolean invertX, boolean invertY) {
            }

            @Override
            public void onVisibilityChanged(boolean visible) {
                if (visible) {
                    setGameInterfaceVisible(true);
                } else {
                    setGameInterfaceVisible(false);
                }
            }
        });
        for (int action = 0; action < PcInputConfig.ACTION_COUNT; action++) {
            setPCInputBinding(action, mPcInputConfig.getBinding(action));
        }
        setPCInputEnabled(mPcInputConfig.isEnabled());
        updatePhysicalKeyboardAvailability();

        SurfaceView surfaceView = GetSurfaceView();
        if (surfaceView != null) {
            surfaceView.setOnCapturedPointerListener(this::onCapturedPointerEvent);
            surfaceView.setOnGenericMotionListener(this::onGenericMotionEvent);
            surfaceView.setOnTouchListener(this::onMouseTouch);
        }

        try {
            initializeSAMP();
        } catch (UnsatisfiedLinkError e5) {
            Log.e(TAG, e5.getMessage());
        }

    }

    private native void initializeSAMP();
    private native void setKeyboardKeyState(int keyCode, boolean isDown);
    private native void setMouseButtonState(int buttonState);
    private native void setMouseWheelDirection(int direction);
    private native void setPCInputEnabled(boolean enabled);
    private native void setPCInputBinding(int action, int keyCode);
    private native void setPhysicalKeyboardAvailable(boolean available);
    private native void openChatInput();
    private native void closeChatInput();


    @Override
    public void onStart() {
        Log.i(TAG, "**** onStart");
        super.onStart();
    }

    @Override
    public void onRestart() {
        Log.i(TAG, "**** onRestart");
        super.onRestart();
    }

    @Override
    public void onResume() {
        Log.i(TAG, "**** onResume");
        super.onResume();
        updatePointerCapture();
        //mHeightProvider.init(view);
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updatePhysicalKeyboardAvailability();
    }

    public void showPCInputConfig() {
        runOnUiThread(() -> {
            if (mPcInputConfig != null) {
                mPcInputConfig.show();
            }
        });
    }

    public native void onEventBackPressed();

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onEventBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mPcInputConfig != null && mPcInputConfig.isVisible()) {
            if (mPcInputConfig.onKeyDown(keyCode) || event.getRepeatCount() > 0 ||
                    keyCode != mPcInputConfig.getBinding(PcInputConfig.TOGGLE_MOUSE)) {
                return true;
            }
        }

        if (mPcInputConfig != null && event.getRepeatCount() > 0 &&
                keyCode == mPcInputConfig.getBinding(PcInputConfig.CHAT)) {
            return true;
        }
        if (mPcInputConfig != null && keyCode == mPcInputConfig.getBinding(PcInputConfig.TOGGLE_MOUSE)) {
            if (event.getRepeatCount() > 0) {
                return true;
            }
            if (mPcInputConfig.isVisible()) {
                mPcInputConfig.dismiss();
                return true;
            }
            if (mDialog != null && mDialog.isShow) {
                return true;
            }
            if (mKeyboard != null && mKeyboard.IsShowing()) {
                closeChatInput();
                return true;
            }
            if (mAttachEdit != null && mAttachEdit.isShow) {
                return true;
            }
            if (mNativeInterfaceVisible) {
                return true;
            }
            if (isGameInputActive() && mPcInputConfig.isEnabled()) {
                mCameraModeRequested = !mCameraModeRequested;
                updatePointerCapture();
                return true;
            }
        }

        if (isGameInputActive() && mPcInputConfig != null && mPcInputConfig.isEnabled()) {
            if (keyCode == mPcInputConfig.getBinding(PcInputConfig.CHAT)) {
                if (event.getRepeatCount() == 0) {
                    openChatInput();
                }
                return true;
            }
            if (event.getRepeatCount() == 0) {
                mPressedKeys.add(keyCode);
                setKeyboardKeyState(keyCode, true);
            }
        } else if (isGameInputActive() && mPcInputConfig != null &&
                !mPcInputConfig.isEnabled() && event.isFromSource(InputDevice.SOURCE_KEYBOARD)) {
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onEventBackPressed();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mPcInputConfig != null && mPcInputConfig.isVisible()) {
            mPcInputConfig.onKeyUp(keyCode);
            return true;
        }
        mPressedKeys.remove(keyCode);
        setKeyboardKeyState(keyCode, false);
        if (isGameInputActive() && mPcInputConfig != null &&
                !mPcInputConfig.isEnabled() && event.isFromSource(InputDevice.SOURCE_KEYBOARD)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private boolean onMouseTouch(View view, MotionEvent event) {
        if (!event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            return false;
        }
        mMouseButtons = event.getButtonState();
        if (mPcInputConfig != null && mPcInputConfig.isVisible()) {
            mPcInputConfig.setMouseButtons(mMouseButtons);
            return false;
        }
        if (mPcInputConfig == null || !mPcInputConfig.isEnabled()) {
            setMouseButtonState(0);
            return true;
        }
        if (!isGameInputActive()) {
            return false;
        }
        setMouseButtonState(mMouseButtons);
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mCameraModeRequested = true;
            updatePointerCapture();
        }
        return true;
    }

    private boolean onCapturedPointerEvent(View view, MotionEvent event) {
        if (mPcInputConfig == null || !mPcInputConfig.isEnabled() || !isGameInputActive()) {
            releaseMouseCapture();
            return true;
        }

        mMouseButtons = event.getButtonState();
        setMouseButtonState(event.getButtonState());

        if (!mMouseLookTouchStarted) {
            mMouseLookX = view.getWidth() / 2.0f;
            mMouseLookY = view.getHeight() / 2.0f;
            sendMouseLookTouch(MotionEvent.ACTION_DOWN);
            mMouseLookTouchStarted = true;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            float deltaX = event.getAxisValue(MotionEvent.AXIS_RELATIVE_X) *
                    mPcInputConfig.getSensitivityX() * (mPcInputConfig.isInvertX() ? -1.0f : 1.0f);
            float deltaY = event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y) *
                    mPcInputConfig.getSensitivityY() * (mPcInputConfig.isInvertY() ? -1.0f : 1.0f);
            mMouseLookX = Math.max(0, Math.min(view.getWidth(),
                    mMouseLookX + deltaX));
            mMouseLookY = Math.max(0, Math.min(view.getHeight(),
                    mMouseLookY + deltaY));
            sendMouseLookTouch(MotionEvent.ACTION_MOVE);
            if (mPcInputConfig.isVisible()) {
                mPcInputConfig.noteMouseMotion();
            }
        }
        return true;
    }

    private boolean onGenericMotionEvent(View view, MotionEvent event) {
        if (!event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            return false;
        }
        if (mPcInputConfig != null && mPcInputConfig.isVisible()) {
            mMouseButtons = event.getButtonState();
            mPcInputConfig.setMouseButtons(mMouseButtons);
            if (event.getActionMasked() == MotionEvent.ACTION_HOVER_MOVE) {
                mPcInputConfig.noteMouseMotion();
            }
            return false;
        }
        if (mPcInputConfig == null || !mPcInputConfig.isEnabled() || !isGameInputActive()) {
            return false;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_SCROLL) {
            setMouseWheelDirection((int) Math.signum(event.getAxisValue(MotionEvent.AXIS_VSCROLL)));
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_BUTTON_PRESS ||
                event.getActionMasked() == MotionEvent.ACTION_BUTTON_RELEASE) {
            mMouseButtons = event.getButtonState();
            setMouseButtonState(mMouseButtons);
            return true;
        }
        return false;
    }

    private void sendMouseLookTouch(int action) {
        customMultiTouchEvent(action, 2, 0, 0, 0, 0, (int) mMouseLookX, (int) mMouseLookY);
    }

    private boolean isMouseCaptured() {
        SurfaceView surfaceView = GetSurfaceView();
        return surfaceView != null && surfaceView.hasPointerCapture();
    }

    private void releaseMouseCapture() {
        SurfaceView surfaceView = GetSurfaceView();
        if (surfaceView != null && surfaceView.hasPointerCapture()) {
            surfaceView.releasePointerCapture();
        }
        stopMouseInput();
    }

    private void stopMouseInput() {
        setMouseButtonState(0);
        if (mMouseLookTouchStarted) {
            sendMouseLookTouch(MotionEvent.ACTION_UP);
            mMouseLookTouchStarted = false;
        }
    }

    private boolean isGameInputActive() {
        return (mKeyboard == null || !mKeyboard.IsShowing()) &&
                (mDialog == null || !mDialog.isShow) &&
                (mAttachEdit == null || !mAttachEdit.isShow) &&
                (mPcInputConfig == null || !mPcInputConfig.isVisible()) &&
                !mNativeInterfaceVisible;
    }

    public void setGameInterfaceVisible(boolean visible) {
        if (visible) {
            mCameraModeRequested = false;
            clearHardwareKeyboardState();
            releaseMouseCapture();
            return;
        }
        if (isGameInputActive()) {
            mCameraModeRequested = true;
        }
        updatePointerCapture();
    }

    public void setNativeInterfaceVisible(boolean visible) {
        runOnUiThread(() -> {
            mNativeInterfaceVisible = visible;
            setGameInterfaceVisible(visible);
        });
    }

    private void updatePointerCapture() {
        if (mCameraModeRequested && mPcInputConfig != null && mPcInputConfig.isEnabled() &&
                isGameInputActive()) {
            SurfaceView surfaceView = GetSurfaceView();
            if (surfaceView != null && surfaceView.isAttachedToWindow() && hasWindowFocus()) {
                surfaceView.post(() -> {
                    if (mCameraModeRequested && isGameInputActive() &&
                            mPcInputConfig.isEnabled() && hasWindowFocus()) {
                        surfaceView.setFocusableInTouchMode(true);
                        surfaceView.requestFocus();
                        surfaceView.requestPointerCapture();
                    }
                });
            }
        } else {
            releaseMouseCapture();
        }
    }

    private void updatePhysicalKeyboardAvailability() {
        boolean available = getResources().getConfiguration().keyboard ==
                android.content.res.Configuration.KEYBOARD_QWERTY;
        setPhysicalKeyboardAvailable(available);
    }

    private void clearHardwareKeyboardState() {
        for (int keyCode : mPressedKeys) {
            setKeyboardKeyState(keyCode, false);
        }
        mPressedKeys.clear();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "**** onPause");
        clearHardwareKeyboardState();
        releaseMouseCapture();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
            releaseMouseCapture();
        } else {
            updatePointerCapture();
        }
    }

    @Override
    public void onStop() {
        Log.i(TAG, "**** onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "**** onDestroy");
        super.onDestroy();
    }

    @Override
    public void onHeightChanged(int orientation, int height) {
        //mKeyboard.onHeightChanged(height);
        //mDialog.onHeightChanged(height);
    }
}
