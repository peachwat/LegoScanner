package com.example.legoscanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AlertDialog
import com.example.legoscanner.data.Detection
import com.example.legoscanner.data.DetectionStatus
import com.example.legoscanner.data.PartRow
import com.example.legoscanner.data.ScanResult
import com.example.legoscanner.ui.CameraViewModel
import com.example.legoscanner.ui.DetectionOverlayView
import com.example.legoscanner.ui.DetectionsAdapter
import com.example.legoscanner.ui.ScanErrorReason
import com.example.legoscanner.ui.ScanState
import com.example.legoscanner.util.ImageUtils
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class CameraFragment : Fragment(R.layout.fragment_camera) {

    private val viewModel: CameraViewModel by viewModels()

    private val detectionsAdapter = DetectionsAdapter(
        onConfirm = { viewModel.confirm(it) },
        onCorrect = { showManualPicker(it) }
    )

    private lateinit var previewView: PreviewView
    private lateinit var permissionPanel: LinearLayout
    private lateinit var grantPermissionButton: MaterialButton
    private lateinit var captureButton: MaterialButton
    private lateinit var pickButton: MaterialButton
    private lateinit var scanAgainButton: MaterialButton
    private lateinit var scanStatus: TextView
    private lateinit var scanProgress: ProgressBar
    private lateinit var hint: TextView
    private lateinit var resultPanel: LinearLayout
    private lateinit var resultImage: ImageView
    private lateinit var resultHeader: TextView
    private lateinit var detectionOverlay: DetectionOverlayView
    private lateinit var detectionsList: RecyclerView

    private var imageCapture: ImageCapture? = null
    private var currentSetParts: List<PartRow> = emptyList()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else showPermissionPanel()
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching { ImageUtils.prepare(requireContext(), uri) }
            .onSuccess { viewModel.analyze(it) }
            .onFailure { viewModel.onImageReadFailed() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupList()
        setupButtons()
        observeScanState()

        if (hasCameraPermission()) startCamera() else requestCameraPermission()
    }

    private fun bindViews(view: View) {
        previewView = view.findViewById(R.id.cameraPreview)
        permissionPanel = view.findViewById(R.id.permissionPanel)
        grantPermissionButton = view.findViewById(R.id.grantPermissionButton)
        captureButton = view.findViewById(R.id.btn_capture)
        pickButton = view.findViewById(R.id.btn_pick)
        scanAgainButton = view.findViewById(R.id.btn_scan_again)
        scanStatus = view.findViewById(R.id.scanStatus)
        scanProgress = view.findViewById(R.id.scanProgress)
        hint = view.findViewById(R.id.hint)
        resultPanel = view.findViewById(R.id.resultPanel)
        resultImage = view.findViewById(R.id.resultImage)
        resultHeader = view.findViewById(R.id.resultHeader)
        detectionOverlay = view.findViewById(R.id.detectionOverlay)
        detectionsList = view.findViewById(R.id.detectionsList)
    }

    private fun setupList() {
        detectionsList.layoutManager = LinearLayoutManager(requireContext())
        detectionsList.adapter = detectionsAdapter
    }

    private fun setupButtons() {
        grantPermissionButton.setOnClickListener { requestCameraPermission() }
        captureButton.setOnClickListener { capture() }
        scanAgainButton.setOnClickListener { viewModel.reset() }
        pickButton.setOnClickListener {
            imagePickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun showPermissionPanel() {
        permissionPanel.visibility = View.VISIBLE
        captureButton.visibility = View.GONE
        hint.visibility = View.GONE
    }

    private fun startCamera() {
        permissionPanel.visibility = View.GONE
        captureButton.visibility = View.VISIBLE
        hint.visibility = View.VISIBLE

        val providerFuture = ProcessCameraProvider.getInstance(requireContext())
        providerFuture.addListener({
            val provider = providerFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            provider.unbindAll()
            provider.bindToLifecycle(
                viewLifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun capture() {
        val capture = imageCapture ?: return

        capture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    runCatching { image.use { ImageUtils.prepare(it) } }
                        .onSuccess { viewModel.analyze(it) }
                        .onFailure { viewModel.onImageReadFailed() }
                }

                override fun onError(exception: ImageCaptureException) {
                    viewModel.onImageReadFailed()
                }
            }
        )
    }

    private fun observeScanState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.scanState.collect { render(it) }
            }
        }
    }

    private fun render(state: ScanState) {
        when (state) {
            is ScanState.Idle -> renderIdle()
            is ScanState.Analyzing -> renderAnalyzing()
            is ScanState.Done -> renderDone(state)
            is ScanState.Failed -> renderFailed(state)
        }
    }

    private fun renderIdle() {
        resultPanel.visibility = View.GONE
        scanProgress.visibility = View.GONE
        scanStatus.visibility = View.GONE
        captureButton.isEnabled = true
        pickButton.isEnabled = true
        detectionOverlay.clear()
    }

    private fun renderAnalyzing() {
        resultPanel.visibility = View.GONE
        scanProgress.visibility = View.VISIBLE
        captureButton.isEnabled = false
        pickButton.isEnabled = false
        showStatus(getString(R.string.camera_analyzing))
    }

    private fun renderDone(state: ScanState.Done) {
        currentSetParts = state.setParts

        scanProgress.visibility = View.GONE
        scanStatus.visibility = View.GONE
        captureButton.isEnabled = true
        pickButton.isEnabled = true

        resultImage.setImageBitmap(state.photo)
        detectionOverlay.setDetections(
            state.result.accepted,
            state.result.imageWidth,
            state.result.imageHeight
        )
        detectionsAdapter.submitList(state.result.detections)
        resultHeader.text = headerText(state.result)
        resultPanel.visibility = View.VISIBLE
    }

    private fun headerText(result: ScanResult): String {
        if (result.detections.isEmpty()) return getString(R.string.result_nothing)

        val counted = result.detections.count {
            it.status == DetectionStatus.AUTO_ACCEPTED ||
                it.status == DetectionStatus.CONFIRMED ||
                it.status == DetectionStatus.CORRECTED
        }
        return getString(
            R.string.result_summary_format,
            counted,
            result.needingReview.size,
            result.rejected.size
        )
    }

    private fun showManualPicker(detection: Detection) {
        val parts = currentSetParts
        if (parts.isEmpty()) return

        val labels = parts.map { part ->
            getString(
                R.string.pick_part_format,
                part.name,
                part.colorName,
                part.found,
                part.required
            )
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.review_pick_title)
            .setItems(labels) { _, index -> viewModel.correct(detection, parts[index]) }
            .setNegativeButton(android.R.string.cancel) { _, _ -> viewModel.dismiss(detection) }
            .show()
    }

    private fun renderFailed(state: ScanState.Failed) {
        resultPanel.visibility = View.GONE
        scanProgress.visibility = View.GONE
        captureButton.isEnabled = true
        pickButton.isEnabled = true
        showStatus(errorMessage(state.reason, state.code))
    }

    private fun errorMessage(reason: ScanErrorReason, code: Int): String = when (reason) {
        ScanErrorReason.NOT_CONFIGURED -> getString(R.string.scan_error_not_configured)
        ScanErrorReason.NO_NETWORK -> getString(R.string.scan_error_no_network)
        ScanErrorReason.SERVER_ERROR -> getString(R.string.scan_error_server, code)
        ScanErrorReason.IMAGE_READ -> getString(R.string.camera_pick_failed)
        ScanErrorReason.UNKNOWN -> getString(R.string.scan_error_unknown)
    }

    private fun showStatus(text: String) {
        scanStatus.text = text
        scanStatus.visibility = View.VISIBLE
    }
}
