package ru.github.debitcredit.presentation.ui.edit

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
import androidx.core.graphics.toColorInt
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.github.debitcredit.R
import ru.github.debitcredit.presentation.viewmodel.MainViewModel
import java.util.Locale
@AndroidEntryPoint
class CategoryEditFragment : Fragment() {
    private val viewModel: MainViewModel by viewModels(
        ownerProducer = { requireActivity() }
    )
    private var isIncomeMode = false
    private lateinit var categoryKey: String
    private var categoryId: Int = 0
    private var categoryColor: Int = 0
    private var categoryIconRes: Int = android.R.drawable.ic_menu_edit
    private var originalAmount: Float = 0f
    private var currentAmount: Float = 0f
    private lateinit var amountEditText: EditText

    private val categoryDisplayNames = mapOf(
        "products" to R.string.products,
        "utilities" to R.string.utilities,
        "transport" to R.string.transport,
        "health" to R.string.health,
        "clothing" to R.string.clothing,
        "entertainment" to R.string.entertainment,
        "other" to R.string.other,
        "income" to R.string.income
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isIncomeMode = it.getBoolean("is_income_mode", false)
            categoryKey = it.getString("category_name") ?: "other"
            categoryId = it.getInt("category_id", 0)
            categoryColor = it.getInt("category_color", "#FF6B6B".toColorInt())
            categoryIconRes = it.getInt("category_icon", android.R.drawable.ic_menu_edit)
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

        val iconContainer = view.findViewById<View>(R.id.iconContainer)
        val categoryIcon = view.findViewById<ImageView>(R.id.categoryIcon)
        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)

        if (isIncomeMode) {
            categoryIcon.setImageResource(R.drawable.ic_ruble)
            iconContainer.setBackgroundResource(R.drawable.button_background_edit)
            titleTextView.text = getString(R.string.add_income)
            titleTextView.visibility = View.VISIBLE
        } else {
            val displayNameRes = categoryDisplayNames[categoryKey]
            val displayName = if (displayNameRes != null) {
                getString(displayNameRes)
            } else {
                categoryKey
            }
            titleTextView.text = displayName

            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(categoryColor)
                setSize(120, 120)
            }
            iconContainer.background = drawable

            categoryIcon.setImageResource(categoryIconRes)
            categoryIcon.setColorFilter(Color.WHITE)

            val currentAmountHint = view.findViewById<TextView>(R.id.currentAmountHint)
            currentAmountHint?.text = getString(R.string.current_amount, originalAmount)
            currentAmountHint?.visibility = View.VISIBLE
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

        view.findViewById<ImageButton>(R.id.confirmButton).setOnClickListener {
            val newAmount = amountEditText.text.toString().toFloatOrNull() ?: 0f

            if (newAmount <= 0) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.enter_amount),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (isIncomeMode) {
                viewModel.addTransaction("income", newAmount, "income")
                Toast.makeText(
                    requireContext(),
                    "${getString(R.string.income_added)}: ${String.format("%.2f", newAmount)} ₽",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().popBackStack()
            } else {
                // Передаем контекст через ViewModel
                viewModel.addTransaction(categoryKey, newAmount, "expense")

                val updatedAmount = originalAmount + newAmount
                val result = Bundle().apply {
                    putString("category_key", categoryKey)
                    putFloat("new_amount", updatedAmount)
                }
                parentFragmentManager.setFragmentResult("category_update", result)

                Toast.makeText(
                    requireContext(),
                    "${getString(R.string.amount_added)}: ${String.format("%.2f", newAmount)} ₽\n${getString(R.string.new_amount)}: ${String.format("%.2f", updatedAmount)} ₽",
                    Toast.LENGTH_LONG
                ).show()
                findNavController().popBackStack()
            }
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