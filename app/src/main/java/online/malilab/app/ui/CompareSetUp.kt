package online.malilab.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.malilab.app.data.COMPARE_KINDS
import online.malilab.app.data.COMPARE_SPEC
import online.malilab.app.data.CompareForm
import online.malilab.app.data.CompareOption
import online.malilab.app.data.Fmt
import online.malilab.app.ui.theme.Mali

@Composable
private fun PlainField(value: String, onChange: (String) -> Unit, label: String, error: String?, hint: String = "", money: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onChange(raw.filter { it.isDigit() || it == '.' || it == ',' }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = error != null,
        supportingText = {
            if (error != null) Text(error, color = Mali.Pink)
            else if (money && value.isNotBlank()) Text(Fmt.kes(value.replace(",", "")))
            else if (hint.isNotBlank()) Text(hint)
        },
        modifier = Modifier.fillMaxWidth(),
        colors = fieldColors(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionCard(index: Int, opt: CompareOption, errors: Map<String, String>, canRemove: Boolean, onRemove: () -> Unit) {
    Section("Option " + (index + 1)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            COMPARE_KINDS.forEach { (k, title) -> ActionChip(title, selected = opt.kind == k) { opt.kind = k } }
        }
        errors["options[$index].kind"]?.let { Text(it, color = Mali.Pink, fontSize = 13.sp) }
        PlainField(
            opt.yieldPct, { opt.yieldPct = it },
            if (opt.kind == "sacco") "Dividend rate (% a year)" else "Yield (% a year)",
            errors["options[$index].yield_pct"],
            when (opt.kind) {
                "tbill" -> "Use the latest auction result published by CBK."
                "mmf" -> "As advertised by the fund."
                "sacco" -> "What your SACCO paid recently. Not promised."
                else -> "The interest rate on the account."
            },
        )
        if (opt.kind == "tbill") {
            Text("Bill length", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Mali.InkSoft)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("91", "182", "364").forEach { d -> ActionChip("$d days", selected = opt.tenor == d) { opt.tenor = d } }
            }
            errors["options[$index].tenor_days"]?.let { Text(it, color = Mali.Pink, fontSize = 13.sp) }
        }
        if (opt.kind == "mmf" || opt.kind == "bank") {
            Text("The yield is", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Mali.InkSoft)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("gross" to "Before tax", "net" to "After tax").forEach { (v, t) -> ActionChip(t, selected = opt.basis == v) { opt.basis = v } }
            }
        }
        if (canRemove) {
            Text("Remove this option", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Pink, modifier = Modifier.clickable(onClick = onRemove).padding(top = 4.dp))
        }
    }
}

@Composable
internal fun CompareSetUp(form: CompareForm, errors: Map<String, String>, netError: String?) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
        Section(COMPARE_SPEC.title, COMPARE_SPEC.blurb) {
            netError?.let { Text(it, color = Mali.Pink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            errors["options"]?.let { Text(it, color = Mali.Pink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            PlainField(form.text["amount"].orEmpty(), { form.text["amount"] = it }, "Amount to put in", errors["amount"], money = true)
            PlainField(form.text["years"].orEmpty(), { form.text["years"] = it }, "For how many years (1 to 30)", errors["years"])
        }
        for (i in form.options.indices) {
            val opt = form.options[i]
            OptionCard(i, opt, errors, form.options.size > 2) { form.options.removeAt(i) }
        }
        if (form.options.size < 5) {
            Text(
                "+ Add another option", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Teal,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp).clickable { form.options.add(CompareOption("mmf")) },
            )
        }
        Section("Tax") {
            SwitchRow("I am exempt from these taxes", form.taxExempt) { form.taxExempt = it }
            Text("Each option uses its own sourced tax rule, shown in the Sources tab.", fontSize = 12.sp, lineHeight = 17.sp, color = Mali.InkSoft)
        }
        Text(
            "Every option is calculated with the same amount and time. Rates change, so enter the figure you expect, not a promise.",
            fontSize = 12.sp, lineHeight = 17.sp, color = Mali.InkSoft, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}
