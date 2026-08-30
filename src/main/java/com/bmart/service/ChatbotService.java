package com.bmart.service;

import com.bmart.dto.ChatbotRequest;
import com.bmart.dto.ChatbotResponse;
import com.bmart.entity.Order;
import com.bmart.entity.Product;
import com.bmart.entity.SupportTicket;
import com.bmart.entity.User;
import com.bmart.repository.OrderRepository;
import com.bmart.repository.ProductRepository;
import com.bmart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final GroqService groqService;
    private final SupportTicketService supportTicketService;

    private static final String SYSTEM_PROMPT = 
            "You are B-MART Assistant ✨, official 24/7 AI Shopping & Customer Support Assistant for B-MART online store.\n" +
            "Specialties: Backpacks, Handbags, Travel Bags, Tech Bags, Wallets, and Fashion Accessories.\n" +
            "ROLE: Help users search products, compare bags, track orders, process returns, explain refunds/payments, and assist with checkout.\n" +
            "TONE: Friendly, concise, helpful store assistant. Keep replies short (2-4 sentences).\n" +
            "RULES:\n" +
            "1. NEVER invent product prices, stock, or order status. Use real database values.\n" +
            "2. Redirect out-of-scope topics politely back to B-MART shopping.\n" +
            "3. Never ask for or store sensitive credentials (passwords, OTPs, full card details).\n" +
            "4. Ask 1 clarifying question if a product request is vague.";

    public ChatbotResponse processQuery(ChatbotRequest request, String username) {
        String rawMsg = request.getMessage() != null ? request.getMessage().trim() : "";
        String msg = rawMsg.toLowerCase();

        User currentUser = null;
        if (username != null && !username.isBlank()) {
            currentUser = userRepository.findByEmail(username)
                    .orElseGet(() -> userRepository.findByUsername(username).orElse(null));
        }

        List<String> defaultChips = Arrays.asList(
                "📦 Where is my order?",
                "🛍️ Recommend products",
                "🔄 Return my order",
                "💰 Check refund",
                "🏷️ Today's offers",
                "🛒 Help with my cart"
        );

        // RULE 4: SENSITIVE DATA PROTECTION
        if (containsAny(msg, "password", "otp", "cvv", "card number", "pin", "secret")) {
            return ChatbotResponse.builder()
                    .intent("ACCOUNT_SECURITY")
                    .reply("🔒 Safety First: Please never share passwords, OTPs, or card details in chat. All payments on B-MART are processed through our 256-bit SSL encrypted Checkout page.")
                    .quickChips(defaultChips)
                    .build();
        }

        // PLATFORM / ABOUT INTENT
        if (containsAny(msg, "platform", "about", "tell me about", "what is bmart", "what is b-mart", "who are you", "store", "company")) {
            return ChatbotResponse.builder()
                    .intent("ABOUT_PLATFORM")
                    .reply("🛒 Welcome to B-MART! We are a premier 24/7 online e-commerce platform specializing in Backpacks, Handbags, Travel Luggage, Wallets, and Fashion Accessories. We feature 100% genuine products, free express delivery on orders over ₹499, 10-day replacement policies, and instant refunds.")
                    .quickChips(Arrays.asList("🛍️ Recommend products", "🏷️ Today's offers", "💳 Payment options"))
                    .build();
        }

        // PAYMENT INTENT
        if (containsAny(msg, "payment", "pay", "cod", "cash on delivery", "upi", "card", "razorpay", "gpay", "phonepe", "netbanking")) {
            return ChatbotResponse.builder()
                    .intent("PAYMENT_SUPPORT")
                    .reply("💳 Payment Methods Supported on B-MART:\n\n• Cash on Delivery (COD)\n• Online UPI (Google Pay, PhonePe, Paytm, BHIM)\n• Credit/Debit Cards & Net Banking via Razorpay\n• 100% Secure Checkout with 256-bit SSL Encryption.")
                    .quickChips(Arrays.asList("📦 Where is my order?", "🛍️ Recommend products", "🔄 Return my order"))
                    .build();
        }

        // HUMAN AGENT / ESCALATION INTENT
        if (containsAny(msg, "human", "agent", "representative", "person", "escalate", "operator", "complain", "talk to support", "speak to someone", "support ticket")) {
            SupportTicket ticket = supportTicketService.createTicket(
                    username,
                    "GENERAL_SUPPORT",
                    "Customer requested live agent support: " + rawMsg,
                    "User: " + rawMsg
            );

            return ChatbotResponse.builder()
                    .intent("HUMAN_AGENT")
                    .reply(String.format("👨‍💼 I have created a live support ticket for you!\n\n🎫 Ticket ID: %s\n• Priority: High\n• Status: OPEN\n\nOur customer care team will reach out to your registered email (%s) or you can call us directly at 1800-123-BMART.",
                            ticket.getTicketId(),
                            ticket.getUserEmail()))
                    .ticket(ticket)
                    .quickChips(Arrays.asList("📦 Where is my order?", "🛍️ Recommend products", "🔄 Return my order"))
                    .build();
        }

        // OUT OF SCOPE REDIRECTION
        if (containsAny(msg, "weather", "cricket", "movie", "news", "code", "python", "java", "politics", "recipe", "song")) {
            return ChatbotResponse.builder()
                    .intent("OUT_OF_SCOPE")
                    .reply("I am B-MART Assistant ✨, specialized in helping you discover Backpacks, Handbags, Travel Bags, Wallets, and managing your store orders. How can I assist your shopping today?")
                    .quickChips(defaultChips)
                    .build();
        }

        // ORDER TRACKING INTENT (Supports Multilingual: Hindi/Telugu)
        if (containsAny(msg, "order", "track", "status", "delivery", "package", "where is my order", "shipped", "arrive", "dispatch", "mera order", "kaha hai", "ekkada undi")) {
            if (currentUser == null) {
                return ChatbotResponse.builder()
                        .intent("ORDER_TRACKING")
                        .reply("Please log in to view your orders. Once logged in, I can pull up your exact live order status, tracking number, and delivery date.")
                        .quickChips(Arrays.asList("Sign In / Register", "🛍️ Recommend products", "🔄 Return my order"))
                        .build();
            }

            List<Order> userOrders = orderRepository.findByUserUserIdOrderByCreatedAtDesc(currentUser.getUserId());
            if (userOrders.isEmpty()) {
                return ChatbotResponse.builder()
                        .intent("ORDER_TRACKING")
                        .reply("Hi " + (currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername()) + "! You don't have any placed orders yet. Explore our catalog to place your first order!")
                        .quickChips(Arrays.asList("🛍️ Recommend products", "🏷️ Today's offers", "📞 Contact Support"))
                        .build();
            }

            Order latestOrder = userOrders.get(0);
            List<Order> topOrders = userOrders.subList(0, Math.min(userOrders.size(), 3));

            String replyText = String.format(
                    "Here is your latest order status:\n\n📦 Order #%s\n• Status: 🚚 %s\n• Payment: %s (%s)\n• Amount: ₹%.2f\n• Placed On: %s",
                    latestOrder.getOrderId(),
                    latestOrder.getStatus(),
                    latestOrder.getPaymentMode(),
                    latestOrder.getPaymentStatus(),
                    latestOrder.getTotalAmount(),
                    latestOrder.getCreatedAt() != null ? latestOrder.getCreatedAt().toLocalDate().toString() : "Recent"
            );

            return ChatbotResponse.builder()
                    .intent("ORDER_TRACKING")
                    .reply(replyText)
                    .orders(topOrders)
                    .quickChips(Arrays.asList("🔄 Return my order", "🛍️ Recommend products", "📞 Contact Support"))
                    .build();
        }

        // RETURN & REFUND INTENT
        if (containsAny(msg, "return", "refund", "replace", "exchange", "damaged", "wrong product", "missing item")) {
            if (currentUser != null) {
                List<Order> userOrders = orderRepository.findByUserUserIdOrderByCreatedAtDesc(currentUser.getUserId());
                if (!userOrders.isEmpty()) {
                    return ChatbotResponse.builder()
                            .intent("RETURN_REQUEST")
                            .reply("Sure! I can help you with your return or refund. Please select the order you would like to return:")
                            .orders(userOrders.subList(0, Math.min(userOrders.size(), 3)))
                            .quickChips(Arrays.asList("Damaged product", "Wrong product", "Product not as expected", "Missing item"))
                            .build();
                }
            }

            String replyText = "🔄 B-MART Easy 10-Day Return Policy:\n\n" +
                    "• Eligible items can be returned or exchanged within 10 days of delivery.\n" +
                    "• Instant Refunds are processed directly to your bank account/UPI within 24-48 hours after item pickup.\n" +
                    "• How to initiate: Visit Your Profile > Orders, choose the item, and select Request Return.";

            return ChatbotResponse.builder()
                    .intent("RETURN_STATUS")
                    .reply(replyText)
                    .quickChips(Arrays.asList("📦 Where is my order?", "💰 Check refund", "📞 Contact Support"))
                    .build();
        }

        // PRODUCT COMPARISON INTENT
        if (containsAny(msg, "compare", "which is better", "difference", "versus", "vs")) {
            Page<Product> pageRes = productRepository.filterProducts(null, null, null, null, null, null, PageRequest.of(0, 2));
            List<Product> compProducts = pageRes.getContent();

            String replyText = "Here is a comparison between our top-selling catalog items:";
            if (compProducts.size() >= 2) {
                Product p1 = compProducts.get(0);
                Product p2 = compProducts.get(1);
                replyText += String.format("\n\n📊 Summary:\n• %s (₹%.2f, ⭐%.1f): Best for daily use & budget.\n• %s (₹%.2f, ⭐%.1f): Best for premium durability & extra capacity.",
                        p1.getName(), p1.getPrice(), p1.getRating() != null ? p1.getRating() : 4.5,
                        p2.getName(), p2.getPrice(), p2.getRating() != null ? p2.getRating() : 4.6);
            }

            return ChatbotResponse.builder()
                    .intent("PRODUCT_COMPARISON")
                    .reply(replyText)
                    .comparisonProducts(compProducts)
                    .quickChips(Arrays.asList("🛍️ Recommend products", "🏷️ Today's offers", "🛒 Help with my cart"))
                    .build();
        }

        // PRODUCT SEARCH & RECOMMENDATION INTENT
        if (containsAny(msg, "recommend", "bag", "backpack", "handbag", "laptop", "purse", "price", "under", "cheap", "best", "buy", "product", "item", "search", "top", "clutch", "travel", "wallet", "college")) {
            String searchQuery = null;
            if (msg.contains("laptop")) searchQuery = "laptop";
            else if (msg.contains("handbag") || msg.contains("purse")) searchQuery = "handbag";
            else if (msg.contains("backpack") || msg.contains("travel") || msg.contains("college")) searchQuery = "backpack";
            else if (msg.contains("clutch") || msg.contains("wallet")) searchQuery = "clutch";

            Double maxPriceDouble = null;
            if (msg.contains("under") || msg.contains("below") || msg.contains("less than")) {
                if (msg.contains("1000")) maxPriceDouble = 1000.0;
                else if (msg.contains("2000")) maxPriceDouble = 2000.0;
                else if (msg.contains("5000")) maxPriceDouble = 5000.0;
                else if (msg.contains("50000") || msg.contains("50,000")) maxPriceDouble = 50000.0;
            }
            java.math.BigDecimal maxPrice = maxPriceDouble != null ? java.math.BigDecimal.valueOf(maxPriceDouble) : null;

            Page<Product> pageRes = productRepository.filterProducts(null, null, maxPrice, null, null, searchQuery, PageRequest.of(0, 4));
            List<Product> products = pageRes.getContent();

            if (products.isEmpty()) {
                Page<Product> fallbackPage = productRepository.filterProducts(null, null, null, null, null, null, PageRequest.of(0, 3));
                return ChatbotResponse.builder()
                        .intent("PRODUCT_RECOMMENDATION")
                        .reply("Here are our top-rated college & travel bags available in stock:")
                        .products(fallbackPage.getContent())
                        .quickChips(Arrays.asList("📦 Where is my order?", "💳 Payment options", "🔄 Return my order"))
                        .build();
            }

            String replyText = "Here are the top " + (searchQuery != null ? searchQuery : "bag") + " options matching your query:";

            return ChatbotResponse.builder()
                    .intent("PRODUCT_RECOMMENDATION")
                    .reply(replyText)
                    .products(products)
                    .quickChips(Arrays.asList("📦 Where is my order?", "💳 Payment options", "🛒 Help with my cart"))
                    .build();
        }

        // CART & WISHLIST SUPPORT
        if (containsAny(msg, "cart", "basket", "checkout", "total")) {
            return ChatbotResponse.builder()
                    .intent("CART_SUPPORT")
                    .reply("🛒 Cart Assistance:\n\nYou can view your cart items, update quantities, apply coupon codes, and proceed to secure checkout by clicking the Cart icon at the top right of the page.")
                    .quickChips(Arrays.asList("🛍️ Recommend products", "🏷️ Today's offers", "💳 Payment options"))
                    .build();
        }

        if (containsAny(msg, "wishlist", "favorite", "saved")) {
            return ChatbotResponse.builder()
                    .intent("WISHLIST_SUPPORT")
                    .reply("❤️ Wishlist Assistance:\n\nYou can save items to your personal wishlist by clicking the heart icon on any product card! Access your saved items anytime from your profile menu.")
                    .quickChips(Arrays.asList("🛍️ Recommend products", "🛒 Help with my cart", "🏷️ Today's offers"))
                    .build();
        }

        // OFFERS & DISCOUNTS
        if (containsAny(msg, "offer", "discount", "coupon", "deal", "sale", "promo")) {
            return ChatbotResponse.builder()
                    .intent("OFFER_SEARCH")
                    .reply("🎉 Today's B-MART Deals:\n\n• Flat 20% OFF on all premium bag collections!\n• FREE Express Delivery on orders above ₹499.\n• Extra ₹100 Instant Discount on Cash on Delivery & UPI payments.")
                    .quickChips(Arrays.asList("🛍️ Recommend products", "💳 Payment options", "📦 Where is my order?"))
                    .build();
        }

        // GROQ AI GENERATIVE RESPONSE (Fallback to informative local string if Groq API fails)
        String aiReply = groqService.generateChatReply(SYSTEM_PROMPT, rawMsg);
        String finalReply = (aiReply != null && !aiReply.isBlank())
                ? aiReply
                : "I'd be glad to help you with that! You can ask me to track your orders, recommend top bags, check return policies, or explain payment options.";

        return ChatbotResponse.builder()
                .intent("GENERAL_FAQ")
                .reply(finalReply)
                .quickChips(defaultChips)
                .build();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
