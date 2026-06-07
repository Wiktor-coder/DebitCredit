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
            categoryName = it.getString("category_name") ?: "Категория"
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
        amountEditText.hint = "Сумма для добавления"

        // Скрываем/показываем кнопку редактирования для режима дохода
        val editButton = view.findViewById<Button>(R.id.editButton)
        if (isIncomeMode) {
            editButton.visibility = View.GONE  // ← Скрываем кнопку "Редактировать" для дохода
        } else {
            // Для категорий показываем текущую сумму где-то в UI
            val currentAmountHint = view.findViewById<TextView>(R.id.currentAmountHint)
            currentAmountHint?.text = "Текущая сумма: ${String.format("%.2f", originalAmount)} ₽"
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
            // Для дохода - своя иконка и заголовок
            categoryIcon.setImageResource(android.R.drawable.stat_sys_upload)

            titleTextView.text = "Добавить доход"
        } else {
            when (categoryName) {
                "Продукты" -> categoryIcon.setImageResource(android.R.drawable.ic_menu_agenda)
                "Развлечения" -> categoryIcon.setImageResource(android.R.drawable.ic_menu_gallery)
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
        view.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            findNavController().popBackStack()
        }

        view.findViewById<Button>(R.id.editButton).setOnClickListener {
            amountEditText.isEnabled = true
            amountEditText.requestFocus()
            showKeyboard()
            Toast.makeText(
                requireContext(),
                "Режим редактирования",
                Toast.LENGTH_SHORT
            ).show()
        }

        view.findViewById<Button>(R.id.confirmButton).setOnClickListener {
            val newAmount = amountEditText.text.toString().toFloatOrNull() ?: 0f

            if (isIncomeMode) {
                // Режим дохода - добавляем доход через ViewModel
                if (newAmount > 0) {
                    viewModel.addIncome(newAmount)
                    Toast.makeText(
                        requireContext(),
                        "Доход добавлен: ${String.format("%.2f", newAmount)} ₽",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                // Режим категории - ПРИБАВЛЯЕМ сумму к существующей
                if (newAmount > 0) {
                    // Новая сумма = старая + добавленная
                    val updatedAmount = originalAmount + newAmount

                    val result = Bundle().apply {
                        putString("category_name", categoryName)
                        putFloat("new_amount", updatedAmount)
                    }
                    parentFragmentManager.setFragmentResult("category_update", result)

                    Toast.makeText(
                        requireContext(),
                        "Добавлено: ${
                            String.format("%.2f", newAmount)
                        } ₽\nНовая сумма: ${String.format("%.2f", updatedAmount)} ₽",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Введите сумму",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
    }
}