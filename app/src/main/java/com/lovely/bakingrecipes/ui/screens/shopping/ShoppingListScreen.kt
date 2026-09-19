package com.lovely.bakingrecipes.ui.screens.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.lovely.bakingrecipes.data.IngredientUnit
import com.lovely.bakingrecipes.data.ShoppingListItem
import com.lovely.bakingrecipes.data.formatAmount
import com.lovely.bakingrecipes.ui.components.brandedTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    items: List<ShoppingListItem>,
    onBackClick: () -> Unit,
    onToggleChecked: (Int, Boolean) -> Unit,
    onDelete: (Int) -> Unit,
    onClearChecked: () -> Unit,
    onClearAll: () -> Unit,
    onAddManual: (String, Double, IngredientUnit) -> Unit,
    onAddAllRecipes: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = brandedTopAppBarColors(),
                title = { Text("Shopping List") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More actions")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add all recipe ingredients") },
                                onClick = {
                                    showMenu = false
                                    onAddAllRecipes()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear checked") },
                                onClick = {
                                    showMenu = false
                                    onClearChecked()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear all") },
                                onClick = {
                                    showMenu = false
                                    onClearAll()
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add item")
            }
        }
    ) { innerPadding ->
        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Your shopping list is empty",
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Add items, use \"Add to shopping list\" from a recipe, or " +
                        "\"Add all recipe ingredients\" from the menu — then check off what you already have.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, start = 32.dp, end = 32.dp)
                )
            }
        } else {
            val toBuy = items.filter { !it.isChecked }
            val haveIt = items.filter { it.isChecked }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "${haveIt.size} of ${items.size} in your kitchen",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                if (toBuy.isNotEmpty()) {
                    item { SectionHeader("Need to buy (${toBuy.size})") }
                    items(toBuy, key = { it.id }) { item ->
                        ShoppingRow(
                            item = item,
                            onToggleChecked = { onToggleChecked(item.id, !item.isChecked) },
                            onDelete = { onDelete(item.id) }
                        )
                    }
                }

                if (haveIt.isNotEmpty()) {
                    item { SectionHeader("In my kitchen (${haveIt.size})") }
                    items(haveIt, key = { it.id }) { item ->
                        ShoppingRow(
                            item = item,
                            onToggleChecked = { onToggleChecked(item.id, !item.isChecked) },
                            onDelete = { onDelete(item.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, amount, unit ->
                onAddManual(name, amount, unit)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun ShoppingRow(
    item: ShoppingListItem,
    onToggleChecked: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.isChecked, onCheckedChange = { onToggleChecked() })
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                    color = if (item.isChecked) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = "${formatAmount(item.amount)} ${item.unit.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove item")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, IngredientUnit) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(IngredientUnit.GRAMS) }
    var unitExpanded by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = amount,
                        onValueChange = { v ->
                            if (v.isEmpty() || v.matches(Regex("^\\d*\\.?\\d*$"))) amount = v
                        },
                        label = { Text("Amount") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { unitExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(unit.label)
                        }
                        DropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            IngredientUnit.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.displayName) },
                                    onClick = {
                                        unit = option
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    onConfirm(name.trim(), amount.toDoubleOrNull() ?: 0.0, unit)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
