package com.zomdroid

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.hardware.input.InputManager
import android.os.Bundle
import android.system.ErrnoException
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.zomdroid.data.SettingsManager
import com.zomdroid.game.GameInstance
import com.zomdroid.game.GameInstanceManager
import com.zomdroid.input.InputControlsView
import com.zomdroid.input.InputNativeInterface
import com.zomdroid.input.PhysicalGamepadHandler
import org.fmod.FMOD


class GameActivity : ComponentActivity() {

    private lateinit var settingsManager: SettingsManager
    private val gamepadHandler = PhysicalGamepadHandler()

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        settingsManager = SettingsManager(this)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val gameInstanceName = intent.getStringExtra(EXTRA_GAME_INSTANCE_NAME)
            ?: throw RuntimeException("Expected game instance name to be passed as intent extra")
        val gameInstance =
            GameInstanceManager.requireSingleton().getInstanceByName(gameInstanceName)
                ?: throw RuntimeException("Game instance with name $gameInstanceName not found")

        System.loadLibrary("zomdroid")

        System.load(
            AppStorage.requireSingleton()
                .homePath + "/" + gameInstance.fmodLibraryPath + "/libfmod.so"
        )
        System.load(
            AppStorage.requireSingleton()
                .homePath + "/" + gameInstance.fmodLibraryPath + "/libfmodstudio.so"
        )

        /*        System.loadLibrary("fmod");
        System.loadLibrary("fmodstudio");*/
        FMOD.init(this)

        setContent {
            GameScreen(gameInstance)
        }

        // Register the virtual on-screen controller so GLFW recognizes it
        // before InputControlsView starts sending button/axis events.
        InputNativeInterface.sendJoystickConnected(InputNativeInterface.VIRTUAL_CONTROLLER_ID, null)

        val inputManager = getSystemService(INPUT_SERVICE) as InputManager
        inputManager.registerInputDeviceListener(gamepadHandler, null)
        // Notify native side about already-connected physical gamepads.
        // Deduplication inside PhysicalGamepadHandler prevents double-connect
        // if onInputDeviceAdded fires for the same device.
        gamepadHandler.notifyAlreadyConnectedDevices(inputManager)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (gamepadHandler.onKeyDown(keyCode, event)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (gamepadHandler.onKeyUp(keyCode, event)) return true
        return super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (gamepadHandler.onGenericMotionEvent(event)) return true
        return super.onGenericMotionEvent(event)
    }

    @Composable
    private fun GameScreen(gameInstance: GameInstance) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    SurfaceView(context).apply {
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                Log.d(LOG_TAG, "Game surface created.")
                                val settings = settingsManager.getSettingsSync()
                                val renderScale = settings.renderScale
                                post {
                                    val width = (this@apply.width * renderScale).toInt()
                                    val height = (this@apply.height * renderScale).toInt()
                                    holder.setFixedSize(width, height)
                                }
                            }

                            override fun surfaceChanged(
                                holder: SurfaceHolder,
                                format: Int,
                                width: Int,
                                height: Int
                            ) {
                                Log.d(LOG_TAG, "Game surface changed.")

                                val gameSurface = holder.surface ?: throw RuntimeException()

                                if (format != PixelFormat.RGBA_8888) {
                                    Log.w(
                                        LOG_TAG,
                                        "Using unsupported pixel format $format"
                                    )
                                }

                                GameLauncher.setSurface(gameSurface, width, height)
                                if (!isGameStarted) {
                                    val thread = Thread {
                                        try {
                                            val settings = settingsManager.getSettingsSync()
                                            GameLauncher.launch(gameInstance, settings)
                                        } catch (e: ErrnoException) {
                                            throw RuntimeException(e)
                                        }
                                    }
                                    thread.start()
                                    isGameStarted = true
                                }
                            }

                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                Log.d(LOG_TAG, "Game surface destroyed.")
                                GameLauncher.destroySurface()
                            }
                        })
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            AndroidView(
                factory = { context ->
                    InputControlsView(context, null)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    companion object {
        const val EXTRA_GAME_INSTANCE_NAME: String =
            "com.zomdroid.GameActivity.EXTRA_GAME_INSTANCE_NAME"
        private val LOG_TAG: String = GameActivity::class.java.name

        private var isGameStarted = false
    }
}