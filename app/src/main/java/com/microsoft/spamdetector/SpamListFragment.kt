package com.microsoft.spamdetector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.microsoft.spamdetector.databinding.SpamListFragmentBinding

class SpamListFragment : Fragment() {

    private lateinit var _binding: SpamListFragmentBinding
    private lateinit var _viewModel: MainActivityViewModel
    private lateinit var _adapter: MessageListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SpamListFragmentBinding.inflate(inflater, container, false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _viewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory(SpamDetectorApplication.Instance)
        )[MainActivityViewModel::class.java]
        setupUI()
        _viewModel.spamMessages.observe(viewLifecycleOwner) {
            if (it != null) {
                _adapter.updateList(it)
            }
        }
    }

    private fun setupUI() {
        _adapter = MessageListAdapter(ArrayList())
        _binding.messageList.adapter = _adapter
        _binding.messageList.layoutManager = LinearLayoutManager(requireContext())
    }
}