package com.test.app.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.test.app.base.BaseFragment
import com.test.app.databinding.FragmentContentUnavailableBinding
import com.test.app.utils.Constants
import com.test.app.utils.changeFragmentWithoutAnimation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContentUnavailableFragment : BaseFragment<FragmentContentUnavailableBinding>() {

    override fun onCreateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentContentUnavailableBinding {
        return FragmentContentUnavailableBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        getBinding()?.btnBackToSettings?.setOnClickListener {
            navigateBackToMenu()
        }
    }

    private fun navigateBackToMenu() {
        MenuFragment().changeFragmentWithoutAnimation(
            Constants.HOME_CONTAINER,
            getBaseActivity(),
            false
        )
    }
}