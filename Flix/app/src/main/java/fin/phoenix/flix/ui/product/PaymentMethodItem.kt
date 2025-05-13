//package fin.phoenix.flix.ui.product
//
//import androidx.compose.foundation.BorderStroke
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.material3.Card
//import androidx.compose.material3.CardDefaults
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import fin.phoenix.flix.ui.colors.RoseRed
//
//data class PaymentMethod(
//    val id: String,
//    val name: String,
//    val iconRes: Int
//)
//
//data class DeliveryMethod(
//    val id: String,
//    val name: String,
//    val iconRes: Int,
//    val baseFee: Float
//)
////
////@OptIn(ExperimentalMaterial3Api::class)
////@Composable
////fun PaymentMethodItem(
////    paymentMethod: PaymentMethod,
////    isSelected: Boolean,
////    onClick: () -> Unit
////) {
////    Card(
////        onClick = onClick,
////        border = BorderStroke(1.dp, if (isSelected) RoseRed else Color.LightGray),
////        colors = CardDefaults.cardColors(
////            containerColor = if (isSelected) Color.White else Color.White
////        ),
////        modifier = Modifier.padding(4.dp)
////    ) {
////        Column(
////            horizontalAlignment = Alignment.CenterHorizontally,
////            modifier = Modifier.padding(8.dp)
////        ) {
////            Image(
////                painter = painterResource(id = paymentMethod.iconRes),
////                contentDescription = paymentMethod.name,
////                modifier = Modifier.size(40.dp)
////            )
////
////            Text(
////                text = paymentMethod.name,
////                fontSize = 12.sp,
////                textAlign = TextAlign.Center,
////                color = if (isSelected) RoseRed else Color.Black,
////                modifier = Modifier.padding(top = 4.dp)
////            )
////        }
////    }
////}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun DeliveryMethodItem(
//    deliveryMethod: DeliveryMethod,
//    isSelected: Boolean,
//    onClick: () -> Unit
//) {
//    Card(
//        onClick = onClick,
//        border = BorderStroke(1.dp, if (isSelected) RoseRed else Color.LightGray),
//        colors = CardDefaults.cardColors(
//            containerColor = if (isSelected) Color.White else Color.White
//        ),
//        modifier = Modifier.padding(4.dp)
//    ) {
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            modifier = Modifier.padding(8.dp)
//        ) {
//            Image(
//                painter = painterResource(id = deliveryMethod.iconRes),
//                contentDescription = deliveryMethod.name,
//                modifier = Modifier.size(40.dp)
//            )
//
//            Text(
//                text = deliveryMethod.name,
//                fontSize = 12.sp,
//                textAlign = TextAlign.Center,
//                color = if (isSelected) RoseRed else Color.Black,
//                modifier = Modifier.padding(top = 4.dp)
//            )
//
//            if (deliveryMethod.baseFee > 0) {
//                Text(
//                    text = "+¥${deliveryMethod.baseFee}",
//                    fontSize = 10.sp,
//                    color = if (isSelected) RoseRed else Color.Gray
//                )
//            }
//        }
//    }
//}
