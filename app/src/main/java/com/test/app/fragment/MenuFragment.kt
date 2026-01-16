package com.test.app.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.app.adapters.LogAdapter
import com.test.app.base.BaseFragment
import com.test.app.databinding.FragmentMenuBinding
import com.test.app.utils.Constants
import com.test.app.utils.UrlProvider
import com.test.app.utils.changeFragmentWithoutAnimation
import com.test.app.viewModels.MenuViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MenuFragment : BaseFragment<FragmentMenuBinding>() {

    private val viewModel: MenuViewModel by viewModels()
    private lateinit var logAdapter: LogAdapter

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentMenuBinding {
        return FragmentMenuBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeLogs()
        observeUiState()
        parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun setupRecyclerView() {
        logAdapter = LogAdapter()
        getBinding()?.rvAnalyticsLog?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = logAdapter
        }
    }

    private fun setupClickListeners() {
        getBinding()?.let { binding ->
            binding.btnSendAnalytics.setOnClickListener {
                viewModel.log("custom_event: test_send")
            }

            binding.switchContent.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleContentAvailability(isChecked)
            }

            binding.btnCopyDeepLink.setOnClickListener {
                val deepLink = binding.etDeepLink.text.toString()
                copyToClipboard(deepLink)
                viewModel.log("deep_link_copied: $deepLink")
            }

            binding.btnSimulateDeepLink.setOnClickListener {
                val deepLink = binding.etDeepLink.text.toString()
                simulateDeepLink(deepLink)
            }

            binding.btnOpenWebView.setOnClickListener {
                val url = binding.etUrl.text.toString().trim()
                val finalUrl = if (url.isEmpty()) {
                    UrlProvider.getDefaultUrl(getBaseActivity())
                } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    "https://$url"
                } else {
                    url
                }

                viewModel.log("navigate_to_content: $finalUrl")
                navigateToWebView(finalUrl)
            }
            binding.btnClearLogs.setOnClickListener {
                viewModel.clearLogs()
            }
        }
    }

    private fun observeLogs() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.logs.collect { logs ->
                    logAdapter.submitList(logs)
                }
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    getBinding()?.switchContent?.isChecked = state.isContentAvailable
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Deep Link", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun simulateDeepLink(deepLinkUrl: String) {
        try {
            val uri = Uri.parse(deepLinkUrl)
            if (uri.scheme == "myapp" && uri.host == "game") {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage(requireContext().packageName)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                viewModel.log("deep_link_simulated: $deepLinkUrl")
            } else {
                viewModel.log("invalid_deep_link_format: $deepLinkUrl")
            }
        } catch (e: Exception) {
            viewModel.log("deep_link_simulation_error: ${e.message}")
        }
    }

    private fun navigateToWebView(url: String) {
        if (viewModel.uiState.value.isContentAvailable) {
            WebViewContentFragment.newInstance(url).changeFragmentWithoutAnimation(
                Constants.HOME_CONTAINER,
                getBaseActivity(),
                true
            )
        } else {
            ContentUnavailableFragment().changeFragmentWithoutAnimation(
                Constants.HOME_CONTAINER,
                getBaseActivity(),
                true
            )
            viewModel.log("navigation_blocked: content_unavailable")
        }
    }
}