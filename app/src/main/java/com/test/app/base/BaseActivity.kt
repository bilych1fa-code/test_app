package com.test.app.base

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.databinding.ViewDataBinding

abstract class BaseActivity<T : ViewDataBinding> : AppCompatActivity() {
    private var viewDataBinding: T? = null
    private var immersiveRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewDataBinding = createBinding()
        setContentView(viewDataBinding?.root)

    }

    abstract fun createBinding() : T
    fun getBinding() = viewDataBinding

    fun getBaseActivity(): BaseActivity<ViewDataBinding> = this as BaseActivity<ViewDataBinding>

    fun showFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                val navInsets = insets.getInsets(WindowInsets.Type.navigationBars())
                val bottomInset = navInsets.bottom

                if (bottomInset > 0) {
                    view.setPadding(navInsets.left, 0, navInsets.right, bottomInset)
                } else {
                    view.setPadding(navInsets.left, 0, navInsets.right, 0)
                }
                insets
            }

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )

            ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
                val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                val stableBottom = insets.systemGestureInsets.bottom

                val bottomInset = when {
                    navInsets.bottom > 0 -> navInsets.bottom
                    stableBottom > 0 -> stableBottom
                    else -> 0
                }

                if (bottomInset > 0) {
                    view.setPadding(navInsets.left, 0, navInsets.right, bottomInset)
                } else {
                    view.setPadding(navInsets.left, 0, navInsets.right, 0)
                }

                insets
            }
        }

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
    fun isButtonNavMode() : Boolean{
        val id = resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
        val mode = if(id != 0) resources.getInteger(id) else -1
        return mode == 1 || mode == 0
    }
    fun hideBottomNavBar(enable: Boolean) {
        immersiveRequested = enable
        if (enable && isButtonNavMode()) hideNavBarImmersive() else restoreSystemBars()
    }
    fun fixBottomPadding(target : View){
        ViewCompat.setOnApplyWindowInsetsListener(target){v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = if(isButtonNavMode()) 0 else sb.bottom)
            insets
        }
    }
    override fun onResume() {
        super.onResume()
        if (immersiveRequested && isButtonNavMode()) hideNavBarImmersive()
    }
    private fun hideNavBarImmersive() {
        val w = window
        WindowCompat.setDecorFitsSystemWindows(w, false)
        val c = WindowInsetsControllerCompat(w, w.decorView)
        c.hide(WindowInsetsCompat.Type.navigationBars())
        c.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            w.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    fun restoreSystemBars() {
        val w = window
        WindowCompat.setDecorFitsSystemWindows(w, true)
        WindowInsetsControllerCompat(w, w.decorView).show(WindowInsetsCompat.Type.systemBars())
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            w.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }



}