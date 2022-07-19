import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.google.gson.Gson
import com.khumbusystems.tenzing.framework.common.domain.entities.orders.*
import com.khumbusystems.tenzing.framework.common.domain.entities.orders.Order
import org.json.simple.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset


class Mapping  :RequestStreamHandler{
    fun convertProviderToPlatform(data: String): Order {
        val gson = Gson()
        val provider = gson.fromJson(data, ProviderDetails::class.java)
        val order = Order()

        val cs=CustomerInformation()
        cs.phoneNumber = provider.metadata?.payload?.customer?.phone
        cs.firstName = provider.metadata?.payload?.customer?.name
        order.customerInformation=cs

        val add = DeliveryAddress()
        add.addressLine1 = provider.metadata?.payload?.deliveryInfo?.destination?.addressLines?.toString()
        add.addressLine1 = provider.metadata?.payload?.deliveryInfo?.destination?.addressLines?.toString()
        add.addressLine2 = provider.metadata?.payload?.deliveryInfo?.destination?.addressLines?.toString()
        add.city = provider.metadata?.payload?.deliveryInfo?.destination?.city
        add.zipCode = provider.metadata?.payload?.deliveryInfo?.destination?.postalCode?.toLong()
        order.deliveryAddress=add



        order.fulfillmentMode = provider.metadata?.payload?.fulfillmentInfo?.fulfillmentMode
        order.deliveryTime = provider.metadata?.payload?.fulfillmentInfo?.deliveryTime

        var orderItemCategories = ArrayList<OrderItemCategory>()
        var orderItemCategory = OrderItemCategory()
        var orderIt = ArrayList<OrderItem>()

        for (item in provider?.metadata?.payload?.items!!) {
            var orderIts = OrderItem()
            orderIts.name = item.name
            orderIts.orderItemCategoryId = item.categoryId
            orderIts.orderItemCategoryName = item.categoryName
            orderIts.price = item.price?.toInt()
            orderIts.quantity = item.quantity
            orderIts.specialInstructions = item.note
            orderIt.add(orderIts)
        }
        orderItemCategory.orderItems = orderIt
        //orderItemCategory.orderItems = orderIt
        orderItemCategories.add(orderItemCategory)
        order.orderItemCategories = orderItemCategories

        val pay = Payments()
        pay.taxAmount = provider.metadata?.payload?.orderTotal?.tax?.toInt()
        pay.totalAmount = provider.metadata?.payload?.orderTotal?.total?.toInt()
        order.payments = pay

        return order
    }
    override fun handleRequest(inputStream : InputStream, outputStream : OutputStream, context: Context) {
        val reader = BufferedReader(InputStreamReader(inputStream, Charset.forName("US-ASCII")))
        val data = Gson().fromJson(reader, Mappings::class.java)
        val provider=data.body
        println(provider)
        val order=convertProviderToPlatform(provider)
        println(order)
        val partner = convertPlatformToPartner(order)
        println(partner)
        val jsonObject = JSONObject()
        jsonObject["body"] = Gson().toJson(partner)
        jsonObject["statusCode"] = 200

        val writer = OutputStreamWriter(outputStream, Charset.defaultCharset())
        writer.write(jsonObject.toString())
        writer.close()
    }
    fun convertPlatformToPartner(order: Order):PartnerDetailsX
    {
        val partner = PartnerDetailsX()
        var cl = Client()
        cl.Tel =  order.customerInformation?.phoneNumber
        cl.Postal  = order.deliveryAddress?.zipCode.toString()
        cl.City = order.deliveryAddress?.city
        cl.Street =order.deliveryAddress?.addressLine1
        cl.Name = order.customerInformation?.firstName
        cl.StNumber = order.deliveryAddress?.addressLine1
        var vr = VeloceRequest()
       vr.Client=cl
       partner.veloceRequest=vr

        var ord = OrderPartner()
        ord.ID = order.ksOrderId?.toInt()
        ord.ExpectedTime = order.deliveryTime
        ord.Type = order.source
        vr.Order = ord
        partner.veloceRequest = vr

        var orderseats = ArrayList<Seat>()


        for (cat in order.orderItemCategories!!)
        {
            var orderseat = Seat()
            var orderitems = ArrayList<Any>()
            cat.orderItems?.let { orderitems.add(it) }
            orderseat.Invoice
            orderseats.add(orderseat)
        }
        return partner
    }
}
data class Mappings(
    val body: String,
    )


