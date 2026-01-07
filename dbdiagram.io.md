// --- Tables ---

Table users {
id integer [pk, increment] // Thay cho BIGSERIAL
email varchar [unique, not null]
password varchar [not null]
full_name varchar [not null]
created_at timestamp [default: `CURRENT_TIMESTAMP`]
updated_at timestamp [default: `CURRENT_TIMESTAMP`]
}

Table roles {
id integer [pk, increment]
name varchar [unique, not null]
created_at timestamp [default: `CURRENT_TIMESTAMP`]
updated_at timestamp [default: `CURRENT_TIMESTAMP`]
}

Table user_roles {
user_id integer
role_id integer

// Composite Primary Key
indexes {
(user_id, role_id) [pk]
}
}

Table shops {
id integer [pk, increment]
user_id integer [unique, not null] // 1 User - 1 Shop
name varchar [unique, not null]
description text
status varchar [not null, note: 'PENDING, ACTIVE, INACTIVE']
created_at timestamp [default: `CURRENT_TIMESTAMP`]
updated_at timestamp [default: `CURRENT_TIMESTAMP`]
}

Table products {
id integer [pk, increment]
shop_id integer [not null]
name varchar [not null]
description text
price decimal(10, 2) [not null]
stock_quantity integer [not null]
created_at timestamp [default: `CURRENT_TIMESTAMP`]
updated_at timestamp [default: `CURRENT_TIMESTAMP`]
}

Table product_categories {
id integer [pk, increment]
name varchar [unique, not null]
created_at timestamp [default: `CURRENT_TIMESTAMP`]
updated_at timestamp [default: `CURRENT_TIMESTAMP`]
}

Table product_category_mappings {
product_id integer
category_id integer

indexes {
(product_id, category_id) [pk]
}
}

Table orders {
id integer [pk, increment]
buyer_id integer [not null]
total_amount decimal(12, 2) [not null]
status varchar [not null, note: 'PENDING_PAYMENT, PAID, CANCELLED']
created_at timestamp [default: `CURRENT_TIMESTAMP`]
updated_at timestamp [default: `CURRENT_TIMESTAMP`]
}

Table sub_orders {
id integer [pk, increment]
order_id integer [not null]
shop_id integer [not null]
amount decimal(12, 2) [not null]
status varchar [not null, note: 'PENDING, PROCESSING, SHIPPED...']
created_at timestamp [default: `CURRENT_TIMESTAMP`]
updated_at timestamp [default: `CURRENT_TIMESTAMP`]
}

Table order_items {
id integer [pk, increment]
sub_order_id integer [not null]
product_id integer [not null]
quantity integer [not null]
price_per_unit decimal(10, 2) [not null]
}

Table reviews {
id integer [pk, increment]
product_id integer [not null]
user_id integer [not null]
rating integer [not null, note: 'Check constraint: 1 <= rating <= 5']
comment text
created_at timestamp [default: `CURRENT_TIMESTAMP`]
updated_at timestamp [default: `CURRENT_TIMESTAMP`]
}

// --- Relationships (Foreign Keys) ---

// Ref: table1.column_name > table2.column_name (Many-to-One)
// Ref: table1.column_name - table2.column_name (One-to-One)

Ref: user_roles.user_id > users.id [delete: cascade]
Ref: user_roles.role_id > roles.id [delete: cascade]

Ref: shops.user_id - users.id [delete: cascade] // Quan hệ 1-1 vì user_id là unique

Ref: products.shop_id > shops.id [delete: cascade]

Ref: product_category_mappings.product_id > products.id [delete: cascade]
Ref: product_category_mappings.category_id > product_categories.id [delete: cascade]

Ref: orders.buyer_id > users.id [delete: cascade]

Ref: sub_orders.order_id > orders.id [delete: cascade]
Ref: sub_orders.shop_id > shops.id [delete: cascade]

Ref: order_items.sub_order_id > sub_orders.id [delete: cascade]
Ref: order_items.product_id > products.id [delete: cascade]

Ref: reviews.product_id > products.id [delete: cascade]
Ref: reviews.user_id > users.id [delete: cascade]