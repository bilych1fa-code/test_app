package com.test.app.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.test.app.R
import com.test.app.base.BaseFragment
import com.test.app.databinding.FragmentWebviewContentBinding
import com.test.app.utils.Constants
import com.test.app.utils.UrlProvider
import com.test.app.utils.changeFragmentWithoutAnimation
import com.test.app.viewModels.MenuAction
import com.test.app.viewModels.OpenWebViewViewModel
import com.test.app.viewModels.WebViewContentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WebViewContentFragment : BaseFragment<FragmentWebviewContentBinding>() {

    private val viewModel: WebViewContentViewModel by viewModels()
    private val sharedViewModel: OpenWebViewViewModel by activityViewModels()

    private var customTitle: String? = null
    private var isDeepLink: Boolean = false

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentWebviewContentBinding {
        return FragmentWebviewContentBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val url = arguments?.getString(ARG_URL) ?: UrlProvider.getDefaultUrl(getBaseActivity())
        customTitle = arguments?.getString(ARG_TITLE)
        isDeepLink = arguments?.getBoolean(ARG_IS_DEEP_LINK, false) ?: false

        checkInternetPermission()
        setupToolbar()
        setupMenu()
        setupDeepLinkChip()
        setupWebView()
        setupBackNavigation()
        observeUiState()
        observeMenuActions()
        viewModel.loadUrl(url)

        if (isDeepLink) {
            viewModel.log("deep_link_opened: $url")
        }
    }

    private fun setupToolbar() {
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.apply {
            title = customTitle ?: "Secure WebView Demo"
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.toolbar_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_refresh -> {
                        refreshWebView()
                        true
                    }
                    R.id.action_browser -> {
                        openCurrentUrlInBrowser()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupDeepLinkChip() {
        if (isDeepLink) {
            getBinding()?.scrollViewDeepLink?.visibility = View.VISIBLE
            getBinding()?.normalModeContainer?.visibility = View.GONE
        } else {
            getBinding()?.scrollViewDeepLink?.visibility = View.GONE
            getBinding()?.normalModeContainer?.visibility = View.VISIBLE
        }
    }

    private fun observeMenuActions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.menuActions.collect { action ->
                    when (action) {
                        is MenuAction.Refresh -> refreshWebView()
                        is MenuAction.OpenInBrowser -> openCurrentUrlInBrowser()
                    }
                }
            }
        }
    }

    private fun refreshWebView() {
        val webView = if (isDeepLink) {
            getBinding()?.webViewScrollable
        } else {
            getBinding()?.webView
        }
        webView?.reload()
        viewModel.log("webview_refresh")
    }

    private fun openCurrentUrlInBrowser() {
        val webView = if (isDeepLink) {
            getBinding()?.webViewScrollable
        } else {
            getBinding()?.webView
        }
        val currentUrl = webView?.url
        if (currentUrl != null) {
            viewModel.log("open_in_browser: $currentUrl")
            openInCustomTabs(currentUrl)
        }
    }

    private fun setupWebView() {
        val webView = if (isDeepLink) {
            getBinding()?.webViewScrollable
        } else {
            getBinding()?.webView
        }

        webView?.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false

            if (isDeepLink) {
                setOnTouchListener { v, event ->
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    false
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    viewModel.onPageStarted(url ?: "")
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    viewModel.onPageFinished(url ?: "", view?.title)

                    if (customTitle == null) {
                        updateToolbarTitle(view?.title)
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)

                    val errorCode = error?.errorCode ?: return

                    if (errorCode == ERROR_HOST_LOOKUP ||
                        errorCode == ERROR_CONNECT ||
                        errorCode == ERROR_TIMEOUT ||
                        errorCode == ERROR_UNKNOWN) {
                        if (!checkInternetConnectivity()) {
                            view?.post {
                                showNoInternetFragment()
                            }
                        }
                    }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString() ?: return false

                    val uri = Uri.parse(url)
                    if (uri.scheme != "http" && uri.scheme != "https") {
                        viewModel.log("blocked_scheme: ${uri.scheme} - $url")
                        return true
                    }

                    if (viewModel.shouldRedirectToExternalBrowser(url)) {
                        viewModel.log("external_redirect: $url")
                        openInCustomTabs(url)
                        return true
                    }

                    return false
                }
            }
        }
    }

    private fun updateToolbarTitle(pageTitle: String?) {
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.apply {
            title = pageTitle ?: "Secure WebView Demo"
        }
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val webView = if (isDeepLink) {
                        getBinding()?.webViewScrollable
                    } else {
                        getBinding()?.webView
                    }

                    if (webView?.canGoBack() == true) {
                        webView.goBack()
                        viewModel.log("webview_back_pressed")
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateLoadingState(state.isLoading)
                    updateLinkBar(state.currentUrl, customTitle ?: state.pageTitle)

                    state.urlToLoad?.let { url ->
                        val webView = if (isDeepLink) {
                            getBinding()?.webViewScrollable
                        } else {
                            getBinding()?.webView
                        }

                        val currentWebViewUrl = webView?.url
                        if (currentWebViewUrl != url) {
                            webView?.loadUrl(url)
                        }
                        viewModel.onUrlLoaded()
                    }

                    state.errorMessage?.let { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        viewModel.onErrorShown()
                    }
                }
            }
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        getBinding()?.let { binding ->
            if (isDeepLink) {
                binding.progressBarScrollable?.visibility = if (isLoading) View.VISIBLE else View.GONE
            } else {
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }

            binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE

            if (isLoading && isDeepLink) {
                binding.tvLoadingMessage.text = "Opened via deep link chip"
            } else {
                binding.tvLoadingMessage.text = "Loading URL..."
            }
        }
    }

    private fun updateLinkBar(url: String, title: String?) {
        if (url.isNotEmpty()) {
            val uri = Uri.parse(url)
            val domain = uri.host ?: "Unknown"

            val displayTitle = title ?: domain.replaceFirst("www.", "")
                .split(".")
                .firstOrNull()
                ?.replaceFirstChar { it.uppercase() } ?: "Domain"

            if (isDeepLink) {
                getBinding()?.customLinkBarScrollable?.findViewById<android.widget.TextView>(R.id.tvDomainScrollable)?.text = displayTitle
                getBinding()?.customLinkBarScrollable?.findViewById<android.widget.TextView>(R.id.tvFullUrlScrollable)?.text = url
            } else {
                updateToolbarTitle(displayTitle)
                getBinding()?.customLinkBar?.findViewById<android.widget.TextView>(R.id.tvDomain)?.text = displayTitle
                getBinding()?.customLinkBar?.findViewById<android.widget.TextView>(R.id.tvFullUrl)?.text = url
            }
        }
    }

    private fun openInCustomTabs(url: String) {
        try {
            val intent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            intent.launchUrl(requireContext(), Uri.parse(url))
            viewModel.logExternalNavigation(url)
        } catch (e: Exception) {
            openInSystemBrowser(url)
        }
    }

    private fun openInSystemBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            viewModel.logExternalNavigation(url)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Cannot open URL", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkInternetPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.INTERNET
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.log("internet_permission: GRANTED")
            if (!checkInternetConnectivity()) {
                view?.post {
                    showNoInternetFragment()
                }
            }
        } else {
            viewModel.logWarning("internet_permission: DENIED")
        }
    }

    private fun showNoInternetFragment() {
        val url = arguments?.getString(ARG_URL) ?: UrlProvider.getDefaultUrl(getBaseActivity())

        if (!isAdded || isDetached) return

        NoInternetFragment.newInstance(
            url = url,
            title = customTitle,
            isDeepLink = isDeepLink
        ).changeFragmentWithoutAnimation(
            Constants.HOME_CONTAINER,
            getBaseActivity(),
            false
        )
    }

    private fun checkInternetConnectivity(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        val hasInternet =
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val isConnected =
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

        if (hasInternet && isConnected) {
            viewModel.log("internet_connectivity: AVAILABLE")
            return true
        } else {
            viewModel.log("internet_connectivity: UNAVAILABLE")
            return false
        }
    }

    override fun onPause() {
        super.onPause()
        if (isDeepLink) {
            getBinding()?.webViewScrollable?.onPause()
        } else {
            getBinding()?.webView?.onPause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isDeepLink) {
            getBinding()?.webViewScrollable?.onResume()
        } else {
            getBinding()?.webView?.onResume()
        }
    }

    override fun onDestroyView() {
        if (isDeepLink) {
            getBinding()?.webViewScrollable?.destroy()
        } else {
            getBinding()?.webView?.destroy()
        }

        (requireActivity() as? AppCompatActivity)?.supportActionBar?.apply {
            title = "Secure WebView Demo"
        }

        super.onDestroyView()
    }

    companion object {
        private const val ARG_URL = "arg_url"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_IS_DEEP_LINK = "arg_is_deep_link"

        fun newInstance(
            url: String,
            title: String? = null,
            isDeepLink: Boolean = false
        ): WebViewContentFragment {
            return WebViewContentFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_URL, url)
                    putString(ARG_TITLE, title)
                    putBoolean(ARG_IS_DEEP_LINK, isDeepLink)
                }
            }
        }
    }
}