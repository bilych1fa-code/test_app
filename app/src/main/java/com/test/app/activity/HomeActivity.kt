package com.test.app.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import com.test.app.base.BaseActivity
import com.test.app.databinding.ActivityHomeBinding
import com.test.app.fragment.MenuFragment
import com.test.app.fragment.WebViewContentFragment
import com.test.app.utils.Constants
import com.test.app.utils.changeFragmentWithoutAnimation
import com.test.app.viewModels.OpenWebViewViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : BaseActivity<ActivityHomeBinding>() {

    private val sharedViewModel: OpenWebViewViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = getBinding()
        setSupportActionBar(binding?.toolbar)

        if (savedInstanceState == null) {
            handleIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data: Uri? = intent?.data

        if (data != null && data.scheme == "myapp" && data.host == "game") {
            handleDeepLink(data)
        } else {
            setMenuContent()
        }
    }

    private fun handleDeepLink(uri: Uri) {
        val url = uri.getQueryParameter("url")
        val title = uri.getQueryParameter("title")

        if (!url.isNullOrEmpty()) {
            WebViewContentFragment.newInstance(
                url = url,
                title = title,
                isDeepLink = true
            ).changeFragmentWithoutAnimation(
                Constants.HOME_CONTAINER,
                getBaseActivity(),
                false
            )
        } else {
            setMenuContent()
        }
    }

    private fun setMenuContent() {
        MenuFragment().changeFragmentWithoutAnimation(
            Constants.HOME_CONTAINER,
            getBaseActivity(),
            false
        )
    }

    override fun createBinding(): ActivityHomeBinding {
        return ActivityHomeBinding.inflate(layoutInflater)
    }
}