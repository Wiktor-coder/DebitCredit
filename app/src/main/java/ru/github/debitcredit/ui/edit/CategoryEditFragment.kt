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
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ru.github.debitcredit.R
import ru.github.debitcredit.viewmodel.MainViewModel

class CategoryEditFragment : Fragment() {
    private val viewModel: MainViewModel by viewModels(
        ownerProducer = { requireActivity() }
    )
    private var isIncomeMode = false
    private lateinit var categoryName: String
    private var originalAmount: Float = 0f
    private var currentAmount: Float = 0f
    private lateinit var amountEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isIncomeMode = it.getBoolean("is_income_mode", false)
            categoryName = it.getString("category_name") ?: getString(R.string.other)
            originalAmount = it.getFloat("category_amount", 0f)
            currentAmount = it.getFloat("category_amount", 0f)
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
        amountEditText.setText("")
        amountEditText.hint = getString(R.string.add_amount)

        val editButton = view.findViewById<ImageButton>(R.id.editButton)
        if (isIncomeMode) {
            editButton.visibility = View.GONE
        } else {
            val currentAmountHint = view.findViewById<TextView>(R.id.currentAmountHint)
            currentAmountHint?.text = "${getString(R.string.current_amount)}: ${String.format("%.2f", originalAmount)} ₽"
            currentAmountHint?.visibility = View.VISIBLE
        }

        val iconContainer = view.findViewById<View>(R.id.iconContainer)
        val categoryColor = arguments?.getInt(
            "category_color",
            Color.parseColor("#FF6B6B")
        ) ?: Color.parseColor("#FF6B6B")
        iconContainer.setBackgroundColor(categoryColor)

        val categoryIcon = view.findViewById<ImageView>(R.id.categoryIcon)
        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)

        if (isIncomeMode) {
            categoryIcon.setImageResource(R.drawable.ic_ruble)
            iconContainer.setBackgroundColor(R.drawable.button_background_edit)
            titleTextView.text = getString(R.string.add_income)
        } else {
            when (categoryName) {
                getString(R.string.products) -> categoryIcon.setImageResource(android.R.drawable.ic_menu_agenda)
                getString(R.string.entertainment) -> categoryIcon.setImageResource(android.R.drawable.ic_menu_gallery)
                else -> categoryIcon.setImageResource(android.R.drawable.ic_menu_edit)
            }
            titleTextView.text = categoryName
        }
    }

    private fun setupTextWatcher() {
        amountEditText.doOnTextChanged { text, _, _, _ ->
            currentAmount = text?.toString()?.toFloatOrNull() ?: 0f
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
        view.findViewById<ImageButton>(R.id.cancelButton).setOnClickListener {
            findNavController().popBackStack()
        }

        view.findViewById<ImageButton>(R.id.editButton).setOnClickListener {
            amountEditText.isEnabled = true
            amountEditText.requestFocus()
            showKeyboard()
            Toast.makeText(requireContext(), getString(R.string.edit_mode), Toast.LENGTH_SHORT).show()
        }

        view.findViewById<ImageButton>(R.id.confirmButton).setOnClickListener {
            val newAmount = amountEditText.text.toString().toFloatOrNull() ?: 0f

            if (isIncomeMode) {
                if (newAmount > 0) {
                    viewModel.addIncome(newAmount)
                    Toast.makeText(
                        requireContext(),
                        "${getString(R.string.income_added)}: ${String.format("%.2f", newAmount)} ₽",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                if (newAmount > 0) {
                    val updatedAmount = originalAmount + newAmount

                    Log.d("CategoryEdit", "Updating category: $categoryName")
                    Log.d("CategoryEdit", "Original: $originalAmount, Added: $newAmount, New: $updatedAmount")

                    val result = Bundle().apply {
                        putString("category_name", categoryName)
                        putFloat("new_amount", updatedAmount)
                    }
                    parentFragmentManager.setFragmentResult("category_update", result)

                    Toast.makeText(
                        requireContext(),
                        "${getString(R.string.amount_added)}: ${String.format("%.2f", newAmount)} ₽\n${getString(R.string.new_amount)}: ${String.format("%.2f", updatedAmount)} ₽",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.enter_amount),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            findNavController().popBackStack()
        }
    }

    private fun showKeyboard() {
        val imm = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        amountEditText.post {
            imm.showSoftInput(amountEditText, 0)
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(amountEditText.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideKeyboard()
    }
}