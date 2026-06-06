package ru.github.debitcredit.ui.edit

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import ru.github.debitcredit.R

class CategoryEditFragment : Fragment() {

    private lateinit var categoryName: String
    private var originalAmount: Float = 0f
    private var currentAmount: Float = 0f
    private lateinit var amountEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryName = it.getString("category_name") ?: "Категория"
            originalAmount = it.getFloat("category_amount", 0f)
            currentAmount = it.getFloat("category_amount", 0f)

            // ЛОГ 1: При создании фрагмента
            Log.d("CategoryEdit", "=== onCreate ===")
            Log.d("CategoryEdit", "Category: $categoryName, Original amount: $originalAmount")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_category_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        setupTextWatcher()
        setupClickListeners(view)
        setupKeyboardListeners()
    }

    private fun setupViews(view: View) {
        amountEditText = view.findViewById(R.id.amountEditText)
        amountEditText.setText(String.format("%.2f", currentAmount))

        val iconContainer = view.findViewById<View>(R.id.iconContainer)
        val categoryColor = arguments?.getInt(
            "category_color",
            Color.parseColor("#FF6B6B")
        ) ?: Color.parseColor("#FF6B6B")
        iconContainer.setBackgroundColor(categoryColor)

        val categoryIcon = view.findViewById<ImageView>(R.id.categoryIcon)
        when (categoryName) {
            "Продукты" -> categoryIcon.setImageResource(android.R.drawable.ic_menu_agenda)
            "Развлечения" -> categoryIcon.setImageResource(android.R.drawable.ic_menu_gallery)
            else -> categoryIcon.setImageResource(android.R.drawable.ic_menu_edit)
        }

        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
        titleTextView.text = categoryName
    }

    private fun setupTextWatcher() {
        amountEditText.doOnTextChanged { text, _, _, _ ->
            currentAmount = text?.toString()?.toFloatOrNull() ?: 0f
            // ЛОГ 2: При изменении текста
            Log.d("CategoryEdit", "Text changed: $currentAmount")
        }
    }

    private fun setupKeyboardListeners() {
        amountEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                true
            } else false
        }
        amountEditText.requestFocus()
        showKeyboard()
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            // ЛОГ 3: При нажатии Отмена
            Log.d("CategoryEdit", "=== CANCEL BUTTON ===")
            Log.d("CategoryEdit", "No update sent, closing")
            findNavController().popBackStack()
        }

        view.findViewById<Button>(R.id.editButton).setOnClickListener {
            amountEditText.isEnabled = true
            amountEditText.requestFocus()
            showKeyboard()
            Toast.makeText(requireContext(), "Режим редактирования", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.confirmButton).setOnClickListener {
            val newAmount = amountEditText.text.toString().toFloatOrNull() ?: originalAmount

            // ЛОГ 4: При нажатии Подтвердить
            Log.d("CategoryEdit", "=== CONFIRM BUTTON ===")
            Log.d("CategoryEdit", "Category: $categoryName")
            Log.d("CategoryEdit", "Original amount: $originalAmount")
            Log.d("CategoryEdit", "New amount: $newAmount")
            Log.d("CategoryEdit", "Text from EditText: ${amountEditText.text.toString()}")

            if (newAmount != originalAmount) {
                Log.d("CategoryEdit", "Sending update to MainFragment...")
                val result = Bundle().apply {
                    putString("category_name", categoryName)
                    putFloat("new_amount", newAmount)
                }
                parentFragmentManager.setFragmentResult("category_update", result)
            } else {
                Log.d("CategoryEdit", "Amount unchanged, not sending update")
            }

            findNavController().popBackStack()
        }
    }

    private fun showKeyboard() {
        val imm = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE)
                as InputMethodManager
        amountEditText.post {
            imm.showSoftInput(amountEditText, 0)
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE)
                as InputMethodManager
        imm.hideSoftInputFromWindow(amountEditText.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard()
        // ЛОГ 5: При уничтожении фрагмента
        Log.d("CategoryEdit", "=== onDestroyView ===")
    }
}