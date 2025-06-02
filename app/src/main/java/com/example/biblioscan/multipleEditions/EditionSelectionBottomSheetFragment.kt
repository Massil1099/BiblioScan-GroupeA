package com.example.biblioscan.multipleEditions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.biblioscan.Book
import com.example.biblioscan.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EditionSelectionBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var editions: List<Book>
    private var onEditionSelected: ((Book) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val editionsArg = arguments?.getParcelableArrayList<Book>(ARG_EDITIONS)
        editions = editionsArg ?: emptyList()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(
            R.layout.fragment_edition_selection_bottom_sheet,
            container,
            false
        )

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewEditions)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val adapter = EditionAdapter(editions) { selectedBook ->
            onEditionSelected?.invoke(selectedBook)
            dismiss()
        }

        recyclerView.adapter = adapter

        return view
    }

    fun setOnEditionSelectedListener(listener: (Book) -> Unit) {
        onEditionSelected = listener
    }

    companion object {
        const val TAG = "EditionSelectionBottomSheetFragment"
        private const val ARG_EDITIONS = "editions"

        fun newInstance(editions: List<Book>): EditionSelectionBottomSheetFragment {
            val fragment = EditionSelectionBottomSheetFragment()
            val bundle = Bundle().apply {
                putParcelableArrayList(ARG_EDITIONS, ArrayList(editions))
            }
            fragment.arguments = bundle
            return fragment
        }
    }
}
