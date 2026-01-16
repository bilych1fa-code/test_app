package com.test.app.fragment

import android.animation.ObjectAnimator
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.viewModels
import com.test.app.base.BaseFragment
import com.test.app.databinding.FragmentNoConnectionBinding
import com.test.app.utils.Constants
import com.test.app.utils.UrlProvider
import com.test.app.utils.changeFragmentWithoutAnimation
import com.test.app.viewModels.WebViewContentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NoInternetFragment : BaseFragment<FragmentNoConnectionBinding>() {

    private val viewModel: WebViewContentViewModel by viewModels()
    private var urlToLoad: String? = null
    private var customTitle: String? = null
    private var isDeepLink: Boolean = false

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentNoConnectionBinding {
        return FragmentNoConnectionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        urlToLoad = arguments?.getString(ARG_URL)
        customTitle = arguments?.getString(ARG_TITLE)
        isDeepLink = arguments?.getBoolean(ARG_IS_DEEP_LINK, false) ?: false

        setupClickListeners()
    }

    private fun setupClickListeners() {
        getBinding()?.btnReload?.setOnClickListener {
            checkInternetAndNavigate()
        }
    }

    private fun checkInternetAndNavigate() {
        showLoadingAnimation()

        if (isInternetAvailable()) {
            navigateToWebView()
        } else {
            showNoInternetAnimation()
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun showLoadingAnimation() {
        getBinding()?.let { binding ->
            binding.btnReload.isEnabled = false
            binding.progressReload.visibility = View.VISIBLE

            ObjectAnimator.ofFloat(binding.ivNoInternet, "rotation", 0f, 360f).apply {
                duration = 1000
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
    }

    private fun showNoInternetAnimation() {
        getBinding()?.let { binding ->
            binding.progressReload.visibility = View.GONE

            ObjectAnimator.ofFloat(binding.ivNoInternet, "translationX", 0f, 20f, -20f, 20f, -20f, 0f).apply {
                duration = 500
                start()
            }

            binding.btnReload.postDelayed({
                binding.btnReload.isEnabled = true
            }, 500)
        }
    }

    private fun navigateToWebView() {
        getBinding()?.progressReload?.visibility = View.GONE

        val url = urlToLoad ?: UrlProvider.getDefaultUrl(getBaseActivity())

        WebViewContentFragment.newInstance(
            url = url,
            title = customTitle,
            isDeepLink = isDeepLink
        ).changeFragmentWithoutAnimation(
            Constants.HOME_CONTAINER,
            getBaseActivity(),
            false
        )
    }

    companion object {
        private const val ARG_URL = "arg_url"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_IS_DEEP_LINK = "arg_is_deep_link"

        fun newInstance(
            url: String,
            title: String? = null,
            isDeepLink: Boolean = false
        ): NoInternetFragment {
            return NoInternetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_URL, url)
                    putString(ARG_TITLE, title)
                    putBoolean(ARG_IS_DEEP_LINK, isDeepLink)
                }
            }
        }
    }
}