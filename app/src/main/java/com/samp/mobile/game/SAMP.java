package com.samp.mobile.game;

import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.joom.paranoid.Obfuscate;
import com.samp.mobile.game.ui.AttachEdit;
import com.samp.mobile.game.ui.CustomKeyboard;
import com.samp.mobile.game.ui.LoadingScreen;
import com.samp.mobile.game.ui.dialog.DialogManager;
import com.samp.mobile.launcher.util.SharedPreferenceCore;
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
    private final Set<Integer> mPressedKeys = new HashSet<>();
    private boolean mMouseLookTouchStarted;
    private float mMouseLookX;
    private float mMouseLookY;

    public native void sendDialogResponse(int i, int i2, int i3, byte[] str);

    public static SAMP getInstance() {
        return instance;
    }

    private void showTab()
    {

    }

    private void hideTab()
    {

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
                    mDialog.hideWithoutReset();
                    mAttachEdit.hideWithoutReset();
                }
                else {
                    if(mDialog.isShow)
                        mDialog.showWithOldContent();
                    if(mAttachEdit.isShow)
                        mAttachEdit.showWithoutReset();
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
        runOnUiThread(() -> { this.mDialog.show(dialogId, dialogTypeId, caption, content, leftBtnText, rightBtnText); });
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
            }
        });
    }

    private void hideKeyboard()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mKeyboard.HideInputLayout();
            }
        });
    }

    private void showEditObject()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAttachEdit.show();
            }
        });
    }

    private void hideEditObject()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAttachEdit.hide();
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

        SurfaceView surfaceView = GetSurfaceView();
        if (surfaceView != null) {
            surfaceView.setOnCapturedPointerListener(this::onCapturedPointerEvent);
            surfaceView.setOnGenericMotionListener(this::onGenericMotionEvent);
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
        //mHeightProvider.init(view);
    }

    public native void onEventBackPressed();

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onEventBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE && isMouseCaptured()) {
            releaseMouseCapture();
            return true;
        }
        if (isGameInputActive()) {
            mPressedKeys.add(keyCode);
            setKeyboardKeyState(keyCode, true);
        }
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            onEventBackPressed();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        mPressedKeys.remove(keyCode);
        setKeyboardKeyState(keyCode, false);
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            boolean handled = super.onTouch(view, event);
            setMouseButtonState(event.getButtonState());
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN && isGameInputActive()) {
                view.requestPointerCapture();
            }
            return handled;
        }
        return super.onTouch(view, event);
    }

    private boolean onCapturedPointerEvent(View view, MotionEvent event) {
        setMouseButtonState(event.getButtonState());

        if (!mMouseLookTouchStarted) {
            mMouseLookX = view.getWidth() / 2.0f;
            mMouseLookY = view.getHeight() / 2.0f;
            sendMouseLookTouch(MotionEvent.ACTION_DOWN);
            mMouseLookTouchStarted = true;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            mMouseLookX = Math.max(0, Math.min(view.getWidth(),
                    mMouseLookX + event.getAxisValue(MotionEvent.AXIS_RELATIVE_X)));
            mMouseLookY = Math.max(0, Math.min(view.getHeight(),
                    mMouseLookY + event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y)));
            sendMouseLookTouch(MotionEvent.ACTION_MOVE);
        }
        return true;
    }

    private boolean onGenericMotionEvent(View view, MotionEvent event) {
        if (event.isFromSource(InputDevice.SOURCE_MOUSE) &&
                event.getActionMasked() == MotionEvent.ACTION_SCROLL) {
            setMouseWheelDirection((int) Math.signum(event.getAxisValue(MotionEvent.AXIS_VSCROLL)));
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
                (mDialog == null || !mDialog.isShow);
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
