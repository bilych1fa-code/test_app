package com.test.app.utils

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.test.app.R
import com.test.app.base.BaseActivity

inline fun <reified T : AppCompatActivity> AppCompatActivity.startIntentActivity(
    isFinish: Boolean,
    extras: Bundle? = null
) {
    Intent(this, T::class.java).apply {
        extras?.let { putExtras(it) }
        this@startIntentActivity.startActivity(this)
    }

    if (isFinish) {
        finish()
    }
}

fun Fragment.changeFragmentWithoutAnimation(
    containerId: Int,
    requireActivity: FragmentActivity,
    isAddToBakStack: Boolean,
) {
    val fragment = this
    val fragmentManager = requireActivity.supportFragmentManager

    val transaction = fragmentManager.beginTransaction()
    transaction.replace(containerId, fragment)

    if (isAddToBakStack) {
        transaction.addToBackStack(fragment::class.java.simpleName)
    }

    try {
        transaction.commitNow()
    } catch (e: IllegalStateException) {
        transaction.commitAllowingStateLoss()
        fragmentManager.executePendingTransactions()
    }
}

fun Fragment.changeFragment(
    containerId: Int,
    requireActivity: BaseActivity<ViewDataBinding>,
    isAddToBakStack: Boolean,
    sharedView: View? = null,
    transitionName: String? = null
) {
    val transaction = requireActivity.supportFragmentManager.beginTransaction()

    if (sharedView != null && transitionName != null) {
        transaction.addSharedElement(sharedView, transitionName)
    } else {
        transaction.setCustomAnimations(
            R.anim.slide_in_up,
            R.anim.fade_out,
            R.anim.fade_in,
            R.anim.slide_out_down
        )
    }

    transaction.replace(containerId, this)

    if (isAddToBakStack) {
        transaction.addToBackStack(this::class.java.simpleName)
    }
    transaction.commitAllowingStateLoss()
}