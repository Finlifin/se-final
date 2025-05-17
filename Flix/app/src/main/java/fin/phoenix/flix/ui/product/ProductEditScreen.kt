package fin.phoenix.flix.ui.product

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import fin.phoenix.flix.data.Product
import fin.phoenix.flix.data.ProductStatus
import fin.phoenix.flix.repository.ProductRepository
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.util.Resource
import fin.phoenix.flix.util.imageUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductEditScreen(navController: NavController, productId: String) {
    val context = LocalContext.current
    val productRepository = remember { ProductRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    // State variables
    var product by remember { mutableStateOf<Product?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Form state
    val initialImages = remember { mutableStateListOf<String>() }
    val newImages = remember { mutableStateListOf<Uri>() }
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    
    // Tags and delivery methods
    val tags = remember { mutableStateListOf<String>() }
    var currentTag by remember { mutableStateOf("") }
    val availableDeliveryMethods = remember { mutableStateListOf<String>() }
    val allDeliveryMethods = listOf("express", "pickup", "self_delivery", "courier")
    val deliveryMethodLabels = mapOf(
        "express" to "快递",
        "pickup" to "自提",
        "self_delivery" to "卖家送货",
        "courier" to "同城跑腿"
    )

    var isUpdating by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf<String?>(null) }

    val categories = listOf("数码", "服装", "图书", "家具", "运动", "生活用品", "学习用品", "其他")
    val conditions = listOf("全新", "99新", "95新", "9成新", "8成新", "7成新", "6成新", "5成新及以下")

    var categoryExpanded by remember { mutableStateOf(false) }
    var conditionExpanded by remember { mutableStateOf(false) }

    // Add this state for delete confirmation dialog
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // Handle product deletion
    fun deleteProduct() {
        isDeleting = true
        coroutineScope.launch {
            try {
                val result = productRepository.deleteProduct(productId)

                withContext(Dispatchers.Main) {
                    isDeleting = false
                    when (result) {
                        is Resource.Success -> {
                            Toast.makeText(context, "商品已删除", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                        is Resource.Error -> {
                            Toast.makeText(context, result.message ?: "删除失败", Toast.LENGTH_LONG).show()
                        }
                        is Resource.Loading -> {
                            // Nothing to do
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isDeleting = false
                    Toast.makeText(context, "删除失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Load product data
    LaunchedEffect(productId) {
        isLoading = true
        when (val result = productRepository.getProductById(productId)) {
            is Resource.Success -> {
                product = result.data
                
                // Initialize form with product data
                title = product?.title ?: ""
                price = product?.price?.toString() ?: ""
                description = product?.description ?: ""
                category = product?.category ?: ""
                condition = product?.condition ?: ""
                location = product?.location ?: ""
                
                // Initialize images
                initialImages.clear()
                product?.images?.forEach { initialImages.add(imageUrl(it)) }
                
                // Initialize tags
                tags.clear()
                product?.tags?.forEach { tags.add(it) }
                
                // Initialize delivery methods
                availableDeliveryMethods.clear()
                product?.availableDeliveryMethods?.forEach { availableDeliveryMethods.add(it) }
                
                isLoading = false
            }
            is Resource.Error -> {
                error = result.message
                isLoading = false
            }
            is Resource.Loading -> {
                // Already set isLoading to true
            }
        }
    }

    // Display error if any
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            error = null
        }
    }
    
    // Display update error if any
    LaunchedEffect(updateError) {
        updateError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            updateError = null
        }
    }

    // Image picker launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (initialImages.size + newImages.size < 5) { // Limit to 5 images total
                newImages.add(it)
            }
        }
    }


    // Add delete confirmation dialog
    if (showDeleteConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除此商品吗？若要撤销请联系管理员") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        deleteProduct()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑商品") }, 
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RoseRed)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Existing images section
                Text("当前图片", fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    initialImages.forEachIndexed { index, imageUrl ->
                        Box(
                            modifier = Modifier.size(100.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(imageUrl),
                                contentDescription = "商品图片 $index",
                                modifier = Modifier
                                    .size(100.dp)
                                    .align(Alignment.Center)
                            )
                            
                            IconButton(
                                onClick = { initialImages.removeAt(index) },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "删除图片",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                    
                    // New images placeholder
                    newImages.forEachIndexed { index, uri ->
                        Box(
                            modifier = Modifier.size(100.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "新添加的图片 $index",
                                modifier = Modifier
                                    .size(100.dp)
                                    .align(Alignment.Center)
                            )
                            
                            IconButton(
                                onClick = { newImages.removeAt(index) },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "删除图片",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                    
                    // Add image button
                    if (initialImages.size + newImages.size < 5) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clickable { imagePicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "添加图片",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    placeholder = { Text("请输入商品标题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Price
                OutlinedTextField(
                    value = price,
                    onValueChange = {
                        // Only accept numbers and one decimal point
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            price = it
                        }
                    },
                    label = { Text("价格") },
                    placeholder = { Text("¥") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    leadingIcon = { Text("¥", fontWeight = FontWeight.Bold) }
                )

                // Category dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        label = { Text("分类") },
                        placeholder = { Text("请选择商品分类") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "选择分类",
                                modifier = Modifier.clickable { categoryExpanded = true }
                            )
                        }
                    )

                    // Invisible clickable overlay
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { categoryExpanded = true }
                    )

                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        categories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Condition dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = condition,
                        onValueChange = {},
                        label = { Text("商品成色") },
                        placeholder = { Text("请选择商品成色") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "选择成色",
                                modifier = Modifier.clickable { conditionExpanded = true }
                            )
                        }
                    )

                    // Invisible clickable overlay
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { conditionExpanded = true }
                    )

                    DropdownMenu(
                        expanded = conditionExpanded,
                        onDismissRequest = { conditionExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        conditions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    condition = option
                                    conditionExpanded = false
                                }
                            )
                        }
                    }
                }

                // Location
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("所在地") },
                    placeholder = { Text("请输入所在地，例如：北京市海淀区") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = "所在地")
                    }
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述") },
                    placeholder = { Text("请详细描述您的商品，例如：品牌、规格、使用时长等") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoseRed,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 标签输入
                Text("商品标签", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = currentTag,
                        onValueChange = { currentTag = it },
                        label = { Text("添加标签") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (currentTag.isNotBlank() && tags.size < 5) {
                                tags.add(currentTag)
                                currentTag = ""
                            }
                        },
                        enabled = currentTag.isNotBlank() && tags.size < 5
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加标签")
                    }
                }
                
                if (tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tags.forEach { tag ->
                            FilterChip(
                                selected = true,
                                onClick = { /* nothing */ },
                                label = { Text(tag) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "移除标签",
                                        modifier = Modifier.clickable { tags.remove(tag) }
                                    )
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // 配送方式
                Text("配送方式", fontWeight = FontWeight.Bold)
                Column {
                    allDeliveryMethods.forEach { method ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (availableDeliveryMethods.contains(method)) {
                                        availableDeliveryMethods.remove(method)
                                    } else {
                                        availableDeliveryMethods.add(method)
                                    }
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = availableDeliveryMethods.contains(method),
                                onCheckedChange = {
                                    if (it) availableDeliveryMethods.add(method)
                                    else availableDeliveryMethods.remove(method)
                                }
                            )
                            Text(deliveryMethodLabels[method] ?: method)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Update button
                Button(
                    onClick = {
                        if (validateForm(
                                title,
                                price,
                                category,
                                condition,
                                description,
                                location,
                                initialImages.size + newImages.size
                            )
                        ) {
                            isUpdating = true

                            coroutineScope.launch {
                                try {
                                    val priceValue = price.toDoubleOrNull() ?: 0.0

                                    val result = productRepository.updateProduct(
                                        productId = productId,
                                        title = title,
                                        description = description,
                                        price = priceValue,
                                        category = category,
                                        condition = condition,
                                        location = location,
                                        status = ProductStatus.AVAILABLE,  // Keep the same status
                                        imageUris = if (newImages.isEmpty()) null else newImages,
                                        tags = tags.toList(),
                                        availableDeliveryMethods = availableDeliveryMethods.toList()
                                    )

                                    withContext(Dispatchers.Main) {
                                        isUpdating = false

                                        when (result) {
                                            is Resource.Success -> {
                                                Toast.makeText(
                                                    context,
                                                    "商品更新成功！",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                navController.popBackStack()
                                            }

                                            is Resource.Error -> {
                                                updateError = result.message
                                            }

                                            is Resource.Loading -> {
                                                // Do nothing
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isUpdating = false
                                        updateError = "商品更新失败: ${e.message}"
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                    enabled = validateForm(
                        title,
                        price,
                        category,
                        condition,
                        description,
                        location,
                        initialImages.size + newImages.size
                    ) && !isUpdating
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text("更新", modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Delete button
                Button(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    enabled = !isUpdating && !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text("删除商品", modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

// Validation function
private fun validateForm(
    title: String,
    price: String,
    category: String,
    condition: String,
    description: String,
    location: String,
    imageCount: Int
): Boolean {
    return title.isNotBlank() && 
           price.isNotBlank() && 
           category.isNotBlank() && 
           condition.isNotBlank() && 
           description.isNotBlank() && 
           location.isNotBlank() && 
           imageCount > 0
}
