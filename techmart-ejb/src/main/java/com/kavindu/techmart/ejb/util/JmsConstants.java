package com.kavindu.techmart.ejb.util;

public final class JmsConstants {

    private JmsConstants() {
    }

    public static final String ORDER_QUEUE = "java:/jms/queue/OrderQueue";
    public static final String INVENTORY_QUEUE = "java:/jms/queue/InventoryQueue";
    public static final String EMAIL_QUEUE = "java:/jms/queue/EmailQueue";
    public static final String NOTIFICATION_TOPIC = "java:/jms/topic/NotificationTopic";

    public static final String PROP_ACTION = "action";
    public static final String PROP_ORDER_ID = "orderId";
    public static final String PROP_PRODUCT_ID = "productId";
    public static final String PROP_INVENTORY_ID = "inventoryId";
    public static final String PROP_QUANTITY = "quantity";
    public static final String PROP_BACK_IN_STOCK = "backInStock";
    public static final String PROP_USER_ID = "userId";
    public static final String PROP_BROADCAST = "broadcast";
    public static final String PROP_EMAIL_TYPE = "emailType";
    public static final String PROP_TO_EMAIL = "toEmail";

    public static final String ACTION_AWAITING_PAYMENT = "AWAITING_PAYMENT";
    public static final String ACTION_PAYMENT_SUCCESS = "PAYMENT_SUCCESS";
    public static final String ACTION_PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String ACTION_CANCEL = "CANCEL";

    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_SYNC = "SYNC";
    public static final String ACTION_REORDER_CHECK = "REORDER_CHECK";

    public static final String EMAIL_ORDER_CONFIRMATION = "ORDER_CONFIRMATION";
    public static final String EMAIL_STOCK_BACK = "STOCK_BACK";
}
