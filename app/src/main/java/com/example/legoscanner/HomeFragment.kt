package com.example.legoscanner

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.legoscanner.data.PartRow
import com.example.legoscanner.ui.ErrorReason
import com.example.legoscanner.ui.HomeUiState
import com.example.legoscanner.ui.HomeViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: HomeViewModel by viewModels()

    private val adapter = PartsAdapter { part, delta -> viewModel.adjust(part, delta) }

    private lateinit var partsList: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var status: TextView
    private lateinit var setTitle: TextView
    private lateinit var setSubtitle: TextView
    private lateinit var setInput: EditText
    private lateinit var loadButton: Button
    private lateinit var resetButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupList()
        setupInput()
        observeState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshProgress()
    }

    private fun bindViews(view: View) {
        partsList = view.findViewById(R.id.partsList)
        progress = view.findViewById(R.id.progress)
        status = view.findViewById(R.id.status)
        setTitle = view.findViewById(R.id.setTitle)
        setSubtitle = view.findViewById(R.id.setSubtitle)
        setInput = view.findViewById(R.id.setInput)
        loadButton = view.findViewById(R.id.loadButton)
        resetButton = view.findViewById(R.id.resetButton)
    }

    private fun setupList() {
        partsList.layoutManager = LinearLayoutManager(requireContext())
        partsList.adapter = adapter
        partsList.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
    }

    private fun setupInput() {
        setInput.setText(viewModel.currentSetNum)

        loadButton.setOnClickListener { submitSetNumber() }

        resetButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.reset_confirm_title)
                .setMessage(R.string.reset_confirm_message)
                .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.resetProgress() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        setInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitSetNumber()
                true
            } else {
                false
            }
        }
    }

    private fun submitSetNumber() {
        hideKeyboard()
        viewModel.onSetNumberEntered(setInput.text.toString())
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: HomeUiState) {
        when (state) {
            is HomeUiState.Loading -> renderLoading(state.setNum)
            is HomeUiState.Success -> renderSuccess(state.setNum, state.parts)
            is HomeUiState.Error -> renderError(state.setNum, state.reason)
        }
    }

    private fun renderLoading(setNum: String) {
        setTitle.text = getString(R.string.set_title_format, setNum)
        setSubtitle.text = getString(R.string.loading_parts)
        setInput.setText(setNum)
        loadButton.isEnabled = false

        progress.visibility = View.VISIBLE
        partsList.visibility = View.GONE
        status.visibility = View.GONE
    }

    private fun renderSuccess(setNum: String, parts: List<PartRow>) {
        adapter.submitList(parts)

        setTitle.text = getString(R.string.set_title_format, setNum)
        setSubtitle.text = getString(
            R.string.summary_format,
            parts.distinctBy { it.partNum }.size,
            parts.size,
            parts.sumOf { it.required }
        )
        loadButton.isEnabled = true

        progress.visibility = View.GONE
        status.visibility = View.GONE
        partsList.visibility = View.VISIBLE
    }

    private fun renderError(setNum: String, reason: ErrorReason) {
        setTitle.text = getString(R.string.set_title_format, setNum)
        setSubtitle.text = getString(R.string.error_header)
        status.text = errorMessage(setNum, reason)
        loadButton.isEnabled = true

        progress.visibility = View.GONE
        partsList.visibility = View.GONE
        status.visibility = View.VISIBLE
    }

    private fun errorMessage(setNum: String, reason: ErrorReason): String = when (reason) {
        ErrorReason.INVALID_KEY -> getString(R.string.error_invalid_key)
        ErrorReason.SET_NOT_FOUND -> getString(R.string.error_set_not_found, setNum)
        ErrorReason.RATE_LIMIT -> getString(R.string.error_rate_limit)
        ErrorReason.NO_NETWORK -> getString(R.string.error_no_network)
        ErrorReason.EMPTY_INVENTORY -> getString(R.string.error_empty_inventory, setNum)
        ErrorReason.UNKNOWN -> getString(R.string.error_unknown)
    }

    private fun hideKeyboard() {
        requireContext().getSystemService<InputMethodManager>()
            ?.hideSoftInputFromWindow(setInput.windowToken, 0)
    }
}
