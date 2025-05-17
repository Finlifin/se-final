package fin.phoenix.flix.ui.product

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import fin.phoenix.flix.repository.ProductRepository
import fin.phoenix.flix.ui.colors.RoseRed
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PublishScreen(navController: NavController) {
    val context = LocalContext.current
    val productRepository = remember { ProductRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    val images = remember { mutableStateListOf<Uri>() }
//    var title by remember { mutableStateOf("") }
//    var price by remember { mutableStateOf("") }
//    var description by remember { mutableStateOf("") }
//    var category by remember { mutableStateOf("") }
//    var condition by remember { mutableStateOf("") }
//    var location by remember { mutableStateOf("") }
    // while developing, use the following lines to initialize the fields
    var title by remember { mutableStateOf("iPhone 12 Pro Max") }
    var price by remember { mutableStateOf("9999.99") }
    var description by remember { mutableStateOf("全新未拆封，支持分期付款") }
    var category by remember { mutableStateOf("数码") }
    var condition by remember { mutableStateOf("全新") }
    var location by remember { mutableStateOf("北京市海淀区") }
    
    // 新增标签和配送方式
    val tags = remember { mutableStateListOf<String>() }
    var currentTag by remember { mutableStateOf("") }
    val availableDeliveryMethods = remember { mutableStateListOf("express", "pickup") }
    val allDeliveryMethods = listOf("express", "pickup", "self_delivery", "courier")
    val deliveryMethodLabels = mapOf(
        "express" to "快递",
        "pickup" to "自提",
        "self_delivery" to "卖家送货",
        "courier" to "同城跑腿"
    )

    var isPublishing by remember { mutableStateOf(false) }
    var publishError by remember { mutableStateOf<String?>(null) }

    val categories = listOf("数码", "服装", "图书", "家具", "运动", "生活用品", "学习用品", "其他")
    val conditions =
        listOf("全新", "99新", "95新", "9成新", "8成新", "7成新", "6成新", "5成新及以下")

    var categoryExpanded by remember { mutableStateOf(false) }
    var conditionExpanded by remember { mutableStateOf(false) }

    // 显示错误信息
    LaunchedEffect(publishError) {
        publishError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            publishError = null
        }
    }

    // Image picker launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (images.size < 5) { // Limit to 5 images
                images.add(it)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("发布商品") }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            })
        }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image selection section
            PhotoSelectionGrid(
                images = images,
                onAddPhoto = { imagePicker.launch("image/*") },
                onRemovePhoto = { uri -> images.remove(uri) },
                maxPhotos = 5
            )

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
                leadingIcon = { Text("¥", fontWeight = FontWeight.Bold) })

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
                            modifier = Modifier.clickable { categoryExpanded = true })
                    })

                // Invisible clickable overlay
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { categoryExpanded = true })

                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    categories.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = {
                            category = option
                            categoryExpanded = false
                        })
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
                            modifier = Modifier.clickable { conditionExpanded = true })
                    })

                // Invisible clickable overlay
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { conditionExpanded = true })

                DropdownMenu(
                    expanded = conditionExpanded,
                    onDismissRequest = { conditionExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    conditions.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = {
                            condition = option
                            conditionExpanded = false
                        })
                    }
                }
            }

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("所在地") },  // 修改标签名
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
                    focusedBorderColor = RoseRed, unfocusedBorderColor = Color.Gray
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

            // Publish button
            Button(
                onClick = {
                    if (validateForm(
                            title,
                            price,
                            category,
                            condition,
                            description,
                            location,
                            images.size
                        )
                    ) {
                        isPublishing = true

                        coroutineScope.launch {
                            try {
                                // 从data store中获取sellerId
                                val sharedPref =
                                    context.getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
                                val sellerId = sharedPref.getString("user_id", null)
                                if (sellerId.isNullOrEmpty()) {
                                    Toast.makeText(
                                        context,
                                        "身份异常，请重新登录",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                val priceValue = price.toDoubleOrNull() ?: 0.0

                                val result = productRepository.publishProduct(
                                    sellerId = sellerId!!,
                                    title = title,
                                    description = description,
                                    price = priceValue,
                                    category = category,
                                    condition = condition,
                                    location = location,
                                    imageUris = images,
                                    tags = tags.toList(),
                                    availableDeliveryMethods = availableDeliveryMethods.toList()
                                )

                                withContext(Dispatchers.Main) {
                                    isPublishing = false

                                    when (result) {
                                        is Resource.Success -> {
                                            Toast.makeText(
                                                context,
                                                "商品发布成功！",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            navController.navigate("/home") {
                                                popUpTo("/home") { inclusive = true }
                                            }
                                        }

                                        is Resource.Error -> {
                                            publishError = result.message
                                        }

                                        is Resource.Loading -> {
                                            // Do nothing
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isPublishing = false
                                    publishError = "商品发布失败: ${e.message}"
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
                    images.size
                ) && !isPublishing
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp), color = Color.White
                    )
                } else {
                    Text("发布", modifier = Modifier.padding(vertical = 8.dp))
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
    return title.isNotBlank() && price.isNotBlank() && category.isNotBlank() && condition.isNotBlank() && description.isNotBlank() && location.isNotBlank() && imageCount > 0
}
