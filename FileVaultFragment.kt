package com.dacksec.appvault.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dacksec.appvault.R
import com.dacksec.appvault.data.VaultFile
import com.dacksec.appvault.data.VaultFileManager
import com.dacksec.appvault.databinding.FragmentFileVaultBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileVaultFragment : Fragment() {

    private var _binding: FragmentFileVaultBinding? = null
    private val binding get() = _binding!!
    private lateinit var fileManager: VaultFileManager
    private lateinit var adapter: FileAdapter

    private val pickFile = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri -> importFile(uri) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFileVaultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fileManager = VaultFileManager(requireContext())
        adapter = FileAdapter(
            onOpen = { openFile(it) },
            onDelete = { deleteFile(it) }
        )

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerView.adapter = adapter

        binding.fabImport.setOnClickListener { pickFile.launch("*/*") }

        loadFiles()
    }

    private fun importFile(uri: Uri) {
        val name = getFileName(uri) ?: "file_${System.currentTimeMillis()}"
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { fileManager.importFile(uri, name) }
            if (result != null) {
                Toast.makeText(requireContext(), "\"$name\" importiert", Toast.LENGTH_SHORT).show()
                loadFiles()
            } else {
                Toast.makeText(requireContext(), "Import fehlgeschlagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openFile(vaultFile: VaultFile) {
        viewLifecycleOwner.lifecycleScope.launch {
            val tempFile = withContext(Dispatchers.IO) { fileManager.decryptToTemp(vaultFile) }
            if (tempFile != null) {
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    tempFile
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, vaultFile.mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Öffnen mit"))
            }
        }
    }

    private fun deleteFile(vaultFile: VaultFile) {
        fileManager.deleteFile(vaultFile)
        Toast.makeText(requireContext(), "Gelöscht", Toast.LENGTH_SHORT).show()
        loadFiles()
    }

    private fun loadFiles() {
        viewLifecycleOwner.lifecycleScope.launch {
            val files = withContext(Dispatchers.IO) { fileManager.listFiles() }
            adapter.submitList(files)
            binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
        }
        return name
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class FileAdapter(
    private val onOpen: (VaultFile) -> Unit,
    private val onDelete: (VaultFile) -> Unit
) : ListAdapter<VaultFile, FileAdapter.VH>(DIFF) {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.fileThumbnail)
        val name: TextView = view.findViewById(R.id.fileName)
        val size: TextView = view.findViewById(R.id.fileSize)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_vault_file, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val f = getItem(position)
        holder.name.text = f.name
        holder.size.text = VaultFileManager(holder.itemView.context).formatSize(f.sizeBytes)

        if (f.mimeType.startsWith("image")) {
            Glide.with(holder.itemView).load(f.file).centerCrop().into(holder.thumbnail)
        } else {
            holder.thumbnail.setImageResource(R.drawable.ic_file_generic)
        }

        holder.itemView.setOnClickListener { onOpen(f) }
        holder.itemView.setOnLongClickListener { onDelete(f); true }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<VaultFile>() {
            override fun areItemsTheSame(a: VaultFile, b: VaultFile) = a.file.path == b.file.path
            override fun areContentsTheSame(a: VaultFile, b: VaultFile) = a == b
        }
    }
}
