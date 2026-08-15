package com.example.legoscanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class CameraFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_camera, container, false)
        view.findViewById<MaterialButton>(R.id.btn_capture).setOnClickListener {
            Toast.makeText(requireContext(), "Skanowanie...", Toast.LENGTH_SHORT).show()
        }
        return view
    }
}