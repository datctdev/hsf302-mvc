// --- Tables ---

Table users {
id uuid [pk]
email varchar [unique, not null]
password varchar
full_name varchar
phone_number varchar
avatar_url varchar
is_active boolean
created_at timestamp
updated_at timestamp
}

Table user_addresses {
id uuid [pk]
user_id uuid
full_name varchar
phone_number varchar
address_line1 varchar
address_line2 varchar
is_default boolean
}

Table shops {
id uuid [pk]
user_id uuid [unique] // 1 User owns 1 Shop
name varchar [unique]
description text
logo_url varchar
cover_image_url varchar
phone_number varchar
address varchar
status varchar
average_rating float
created_at timestamp
updated_at timestamp
}

Table products {
id uuid [pk]
shop_id uuid
name varchar
description text
sku varchar [unique]
status varchar
created_at timestamp
updated_at timestamp
}

Table product_variants {
id uuid [pk]
product_id uuid
name varchar [note: "e.g., Color, Size"]
value varchar [note: "e.g., Red, XL"]
price_modifier decimal
stock_quantity int
sku varchar [unique]
}

Table product_images {
id uuid [pk]
product_id uuid
image_url varchar
is_thumbnail boolean
}

Table product_categories {
id uuid [pk]
parent_id uuid [note: "Danh mục cha"]
name varchar [unique]
created_at timestamp
updated_at timestamp
}

Table carts {
id uuid [pk]
user_id uuid
created_at timestamp
updated_at timestamp
}

Table cart_items {
id uuid [pk]
cart_id uuid
product_variant_id uuid
quantity int
}

Table orders {
id uuid [pk]
buyer_id uuid
sub_total decimal [note: "Tổng tiền hàng"]
shipping_fee decimal [note: "Phí vận chuyển"]
discount_amount decimal [note: "Tiền giảm giá"]
total_amount decimal [note: "Tổng cuối cùng"]
shipping_address varchar
shipping_phone varchar
shipping_method varchar
status varchar
created_at timestamp
updated_at timestamp
}

Table sub_orders {
id uuid [pk]
order_id uuid
shop_id uuid
amount decimal
shipping_fee decimal
shipping_code varchar [note: "Mã vận đơn"]
status varchar
created_at timestamp
updated_at timestamp
}

Table order_items {
id uuid [pk]
sub_order_id uuid
product_variant_id uuid
quantity int
price_per_unit decimal
}

Table payments {
id uuid [pk]
order_id uuid
method varchar [note: "e.g., VNPay, Momo, COD"]
amount decimal
transaction_id varchar [note: "Mã giao dịch từ cổng thanh toán"]
status varchar [note: "e.g., PENDING, SUCCESS, FAILED"]
created_at timestamp
updated_at timestamp
}

Table reviews {
id uuid [pk]
product_id uuid
user_id uuid
sub_order_id uuid
rating int
comment text
seller_reply text [note: "Phản hồi từ người bán"]
is_verified_purchase boolean
created_at timestamp
updated_at timestamp
}

Table discounts {
id uuid [pk]
code varchar [unique]
type varchar [note: "PERCENTAGE or FIXED_AMOUNT"]
value decimal
quantity int
valid_from timestamp
valid_to timestamp
created_at timestamp
updated_at timestamp
}

Table order_discounts {
order_id uuid
discount_id uuid

// Composite Primary Key
indexes {
(order_id, discount_id) [pk]
}
}

// --- Relationships ---

Ref: user_addresses.user_id > users.id
Ref: shops.user_id - users.id // 1-to-1 relationship (User owns Shop)

Ref: carts.user_id > users.id
Ref: cart_items.cart_id > carts.id
Ref: cart_items.product_variant_id > product_variants.id

Ref: products.shop_id > shops.id
Ref: product_variants.product_id > products.id
Ref: product_images.product_id > products.id

Ref: product_categories.parent_id > product_categories.id // Recursive relationship

Ref: orders.buyer_id > users.id
Ref: sub_orders.order_id > orders.id
Ref: sub_orders.shop_id > shops.id

Ref: order_items.sub_order_id > sub_orders.id
Ref: order_items.product_variant_id > product_variants.id

Ref: payments.order_id > orders.id

Ref: reviews.product_id > products.id
Ref: reviews.user_id > users.id
Ref: reviews.sub_order_id > sub_orders.id

Ref: order_discounts.order_id > orders.id
Ref: order_discounts.discount_id > discounts.id