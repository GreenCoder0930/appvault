package com.dacksec.appvault.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dacksec.appvault.R
import com.dacksec.appvault.data.AppInfo
import com.dacksec.appvault.data.LockedAppsManager
import com.dacksec.appvault.databinding.FragmentAppListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListFragment : Fragment() {

    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!
    private lateinit var manager: LockedAppsManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAppListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        manager = LockedAppsManager(requireContext())
        val adapter = AppAdapter { app, locked -> manager.setLocked(app.packageName, locked) }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) { manager.getInstalledApps() }
            binding.progressBar.visibility = View.GONE
            adapter.submitList(apps)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class AppAdapter(
    private val onToggle: (AppInfo, Boolean) -> Unit
) : ListAdapter<AppInfo, AppAdapter.VH>(DIFF) {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val label: TextView = view.findViewById(R.id.appLabel)
        val toggle: Switch = view.findViewById(R.id.appSwitch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = getItem(position)
        holder.icon.setImageDrawable(app.icon)
        holder.label.text = app.label
        holder.toggle.isChecked = app.isLocked
        holder.toggle.setOnCheckedChangeListener { _, checked -> onToggle(app, checked) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(a: AppInfo, b: AppInfo) = a.packageName == b.packageName
            override fun areContentsTheSame(a: AppInfo, b: AppInfo) = a == b
        }
    }
}
